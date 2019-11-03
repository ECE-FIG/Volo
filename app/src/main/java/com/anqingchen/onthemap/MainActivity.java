package com.anqingchen.onthemap;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.github.clans.fab.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions;
import com.mapbox.mapboxsdk.location.LocationComponentOptions;

import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.plugins.annotation.Symbol;
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager;
import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions;
import com.mapbox.mapboxsdk.style.layers.Property;
import com.mongodb.stitch.android.core.Stitch;
import com.mongodb.stitch.android.core.StitchAppClient;
import com.mongodb.stitch.android.services.mongodb.remote.AsyncChangeStream;
import com.mongodb.stitch.android.services.mongodb.remote.RemoteFindIterable;
import com.mongodb.stitch.android.services.mongodb.remote.RemoteMongoClient;
import com.mongodb.stitch.android.services.mongodb.remote.RemoteMongoCollection;
import com.mongodb.stitch.core.auth.providers.anonymous.AnonymousCredential;
import com.mongodb.stitch.core.internal.common.BsonUtils;
import com.mongodb.stitch.core.services.mongodb.remote.ChangeEvent;

import org.bson.BsonValue;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    FloatingActionButton addEventBtn, filterButton;

    MapboxMap mapboxMap;
    MapView mapView;
    List<Event> eventsList = new ArrayList<>();

    RemoteMongoCollection<Event> events;

    List<Symbol> currentSymbols = new ArrayList<>();
    Symbol userSymbol;
    LocationManager mLocationManager;
    SymbolManager mSymbolManager;

    Vibrator vibrator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Load Mapbox Map with my API key
        Mapbox.getInstance(this, "pk.eyJ1Ijoic2FtYXJpdGFucyIsImEiOiJjanhjaHN6OXowM2twM3dvY3k1Z2k2bWQzIn0.qNMnSU_p4akStUv8Z8uQ6w");
        setContentView(R.layout.activity_main);

        Toolbar myToolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        vibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);

        // Initialize the map
        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        // Initialize addEventBtn to load the add event page
        addEventBtn = findViewById(R.id.addEventBtn);
        addEventBtn.setOnClickListener(v -> openLoginActivity());

        // Initialize MongoDB
        StitchAppClient client = Stitch.getDefaultAppClient();
        final RemoteMongoClient mongoClient = client.getServiceClient(
                RemoteMongoClient.factory, "mongodb-atlas");

        events = mongoClient.getDatabase("app")
                .getCollection("events", Event.class)
                .withCodecRegistry(CodecRegistries.fromRegistries(
                        BsonUtils.DEFAULT_CODEC_REGISTRY,
                        CodecRegistries.fromCodecs(Event.simple_codec)
                ));

        client.getAuth().loginWithCredential(new AnonymousCredential()).addOnCompleteListener(task-> {
            if (task.isSuccessful()) {
                Log.i("stitch", "logged in anonymously");
            } else {
                Log.i("stitch", "failed to log in anonymously", task.getException());
            }
        });

        RemoteFindIterable<Event> findResults = events.find();

        findResults.forEach(this::addEvent).addOnCompleteListener(task -> {
            if (task.isComplete()) repopulateSymbols();
        });

        events.watch().addOnCompleteListener(task -> {
            AsyncChangeStream<Event, ChangeEvent<Event>> changeStream = task.getResult();
            changeStream.addChangeEventListener((BsonValue documentId, ChangeEvent<Event> event) -> {
                // handle change event
                Event temp = event.getFullDocument();
                addEvent(temp);
            });
            repopulateSymbols();
        });
    }

    @Override
    public void onMapReady(@NonNull final MapboxMap mapboxMap) {
        this.mapboxMap = mapboxMap;
        mapboxMap.setStyle(new Style.Builder().fromUri("mapbox://styles/samaritans/cjxcp0y0s01lg1cnwh18eid9q"), style -> {
            // moves map center to show current phone location
            enableLocationComponent(style);
            FloatingActionButton locationButton = findViewById(R.id.locationButton);
            locationButton.setOnClickListener(v -> {
                Toast.makeText(getApplicationContext(),"Showing Current Location", Toast.LENGTH_SHORT).show();
                if(getLastKnownLocation() != null) {
                    mapboxMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                            new LatLng(getLastKnownLocation().getLatitude(), getLastKnownLocation().getLongitude()), 14));
                }
            });

            // Create SymbolManager layer to draw markers
            SymbolManager symbolManager = new SymbolManager(mapView, mapboxMap, style);

            //Set OnClickListener for individual markers
            symbolManager.addClickListener(symbol -> {
                Toast.makeText(MainActivity.this, "Loading" , Toast.LENGTH_SHORT).show();
                openInfoActivity(new ObjectId(symbol.getData().getAsString()));
            });
            symbolManager.setIconAllowOverlap(true);
            symbolManager.setIconRotationAlignment(Property.ICON_ROTATION_ALIGNMENT_VIEWPORT);
            mSymbolManager = symbolManager;

            // Load Resources
            Bitmap bm = BitmapFactory.decodeResource(getResources(), R.drawable.volunteer);
            mapboxMap.getStyle().addImage("volunteer-marker", bm);
            bm = BitmapFactory.decodeResource(getResources(), R.drawable.icons8_place_marker_64);
            mapboxMap.getStyle().addImage("user-marker", bm);
        });

        // Long-click marker pop-up
        mapboxMap.addOnMapLongClickListener(point -> {
            vibrator.vibrate(100);
            Snackbar.make(findViewById(android.R.id.content), "Custom Location Marked", Snackbar.LENGTH_INDEFINITE)
                    .setAction("Dismiss", view -> deselectUserSymbol()).show();
            deselectUserSymbol();
            SymbolOptions symbolOptions = new SymbolOptions()
                    .withLatLng(point)
                    .withIconImage("user-marker")
                    .withTextJustify("user-marker");
            userSymbol = mSymbolManager.create(symbolOptions);
            return true;
        });
    }

    private void deselectUserSymbol() {
        if(userSymbol != null) {
            mSymbolManager.delete(userSymbol);
        }
    }

    private void addEvent(Event event) {
        eventsList.add(event);
    }

    // Refresh the symbol layer with filteredList
    private void repopulateSymbols() {
        if(mSymbolManager != null) {
            clearSymbols();
            ArrayList<SymbolOptions> symbolOptionsList = new ArrayList<>();
            Log.i("DEBUG S", String.valueOf(eventsList.size()));
            for (int i = 0; i < eventsList.size(); i++) {
                symbolOptionsList.add(eventsList.get(i).toSymbol());
            }
            currentSymbols = mSymbolManager.create(symbolOptionsList);
        }
    }

    // Remove all current symbols from map (preserves the list);
    private void clearSymbols() {
        mSymbolManager.delete(currentSymbols);
    }

    @Override
    @SuppressWarnings({"MissingPermission"})
    public void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
        repopulateSymbols();
    }

    @SuppressWarnings({"MissingPermission"})
    private void enableLocationComponent(@NonNull Style loadedMapStyle) {
        LocationComponent locationComponent;
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            LocationComponentOptions locationComponentOptions = LocationComponentOptions
                    .builder(this)
                    .build();
            LocationComponentActivationOptions activationOptions = LocationComponentActivationOptions
                    .builder(this, loadedMapStyle)
                    .locationComponentOptions(locationComponentOptions)
                    .build();
            locationComponent = mapboxMap.getLocationComponent();
            locationComponent.activateLocationComponent(activationOptions);
            locationComponent.setLocationComponentEnabled(true);
            locationComponent.setCameraMode(CameraMode.TRACKING);
            locationComponent.setRenderMode(RenderMode.COMPASS);

        } else {
            PermissionsManager permissionsManager = new PermissionsManager(new PermissionsListener() {
                @Override
                public void onExplanationNeeded(List<String> permissionsToExplain) {

                }

                @Override
                public void onPermissionResult(boolean granted) {
                    if (granted) enableLocationComponent(loadedMapStyle);
                }
            });
            permissionsManager.requestLocationPermissions(this);
        }
    }


    @SuppressWarnings({"MissingPermission"})
    private Location getLastKnownLocation() {
        mLocationManager = (LocationManager)getApplicationContext().getSystemService(LOCATION_SERVICE);
        Location bestLocation = null;
        if(mLocationManager != null) {
            List<String> providers = mLocationManager.getProviders(true);
            for (String provider : providers) {
                Location l = mLocationManager.getLastKnownLocation(provider);
                if (l == null) {
                    continue;
                }
                if (bestLocation == null || l.getAccuracy() < bestLocation.getAccuracy()) {
                    // Found best last known location: %s", l);
                    bestLocation = l;
                }
            }
        }
        return bestLocation;
    }

    public void openInfoActivity(ObjectId id) {
        Intent intent = new Intent(this, InfoActivity.class);
        intent.putExtra("_id", id);  // Pass event selected to info page
        startActivity(intent);
    }

    public void openLoginActivity() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_action, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId() == R.id.refreshBtn) {
            repopulateSymbols();
        }
        return true;
    }
}