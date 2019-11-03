package com.anqingchen.onthemap;

import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mongodb.stitch.android.core.Stitch;
import com.mongodb.stitch.android.core.StitchAppClient;
import com.mongodb.stitch.android.services.mongodb.remote.RemoteMongoClient;
import com.mongodb.stitch.android.services.mongodb.remote.RemoteMongoCollection;
import com.mongodb.stitch.core.internal.common.BsonUtils;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.os.Vibrator;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import org.bson.codecs.configuration.CodecRegistries;
import org.bson.types.ObjectId;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static com.mongodb.client.model.Filters.eq;

public class InfoActivity extends AppCompatActivity {

    Event event;
    TextView month, date, name, org, address, startTime, endTime, desc;
    ImageView eventPicture;

    RemoteMongoCollection<Event> events;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.getNavigationIcon().setColorFilter(getColor(android.R.color.black), PorterDuff.Mode.SRC_ATOP);

        // Allocating Views
        month = findViewById(R.id.monthText);
        date = findViewById(R.id.dateText);
        name = findViewById(R.id.nameText);
        address = findViewById(R.id.locationText);
        startTime = findViewById(R.id.startTimeText);
        endTime = findViewById(R.id.endTimeText);
        desc = findViewById(R.id.descText);
        org = findViewById(R.id.organizationText);
        eventPicture = findViewById(R.id.eventPicture);

        // System Vibrator
        final Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        // Grab event passed
        ObjectId id = (ObjectId) getIntent().getSerializableExtra("_id");

        StitchAppClient client = Stitch.getDefaultAppClient();
        final RemoteMongoClient mongoClient = client.getServiceClient(
                RemoteMongoClient.factory, "mongodb-atlas");

        events = mongoClient.getDatabase("app")
                .getCollection("events", Event.class)
                .withCodecRegistry(CodecRegistries.fromRegistries(
                        BsonUtils.DEFAULT_CODEC_REGISTRY,
                        CodecRegistries.fromCodecs(Event.full_codec)
                ));

        events.find(eq("_id", id)).first().addOnCompleteListener(task -> {
            if (task.getResult() == null) {
                Log.d("app", String.format("No document matches the provided query"));
            } else if (task.isSuccessful()) {
                Log.d("app", String.format("Successfully found document"));
                event = task.getResult();

                month.setText(getUTCtoMonth(event.getEventStartDate()));
                date.setText(String.format("%d", getUTCtoDate(event.getEventStartDate())));
                name.setText(event.getEventName().toUpperCase());
                desc.setText(event.getEventDesc());
                org.setText(event.getEventOrg());
                address.setText(getAddressFromLocation(InfoActivity.this, event.getEventLatLng()));

                // Configure Date/Time displays
                String datePattern = "E,  MMM dd";
                String timePattern = "HH:mm  z";
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat(datePattern);
                SimpleDateFormat simpleTimeFormat = new SimpleDateFormat(timePattern);
                Date tempDate = new Date(event.getEventStartDate());
                String tempText = simpleDateFormat.format(tempDate) + " at " + simpleTimeFormat.format(tempDate);
                startTime.setText(tempText);
                tempDate = new Date(event.getEventEndDate());
                tempText = simpleDateFormat.format(tempDate) + " at " + simpleTimeFormat.format(tempDate);
                endTime.setText(tempText);
            } else {
                Log.e("app", "Failed to findOne: ", task.getException());
            }
        });

        // Get Directions to Address
        address.setOnLongClickListener(
                view -> {
                    Intent intent = new Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://www.google.com/maps/dir/?api=1&destination=" + event.getEventLatLng().getLatitude() + "," +
                                    event.getEventLatLng().getLongitude()));
                    if (vibrator != null) {
                        vibrator.vibrate(100);
                    }
                    startActivity(intent);
                    return false;
                }
        );

        // Share the Event
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> {
            Intent sharingIntent = new Intent(Intent.ACTION_SEND);
            sharingIntent.setType("text/plain");
            String shareBody = getSharableString(event);
            String shareSub = event.getEventName();
            sharingIntent.putExtra(Intent.EXTRA_SUBJECT, shareSub);
            sharingIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
            startActivity(Intent.createChooser(sharingIntent, "Share using"));
        });
    }

    public String getAddressFromLocation(Context context, LatLng latLng) {
        Geocoder coder = new Geocoder(context);
        List<Address> address = null;
        try {
            address = coder.getFromLocation(latLng.getLatitude(), latLng.getLongitude(), 1);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        if (address == null) {
            return null;
        } else {
            return address.get(0).getAddressLine(0);
        }
    }

    public String getUTCtoMonth(long time) {
        Date date = new Date(time);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        int monthNum = calendar.get(Calendar.MONTH) + 1;
        String month = "ERR";
        switch (monthNum) {
            case 1:
                month = "JAN";
                break;
            case 2:
                month = "FEB";
                break;
            case 3:
                month = "MAR";
                break;
            case 4:
                month = "APR";
                break;
            case 5:
                month = "MAY";
                break;
            case 6:
                month = "JUN";
                break;
            case 7:
                month = "JUL";
                break;
            case 8:
                month = "AUG";
                break;
            case 9:
                month = "SEP";
                break;
            case 10:
                month = "OCT";
                break;
            case 11:
                month = "NOV";
                break;
            case 12:
                month = "DEC";
                break;
        }
        return month;
    }

    public int getUTCtoDate(long time) {
        Date date = new Date(time);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar.get(Calendar.DAY_OF_MONTH);
    }

    public String getSharableString(Event event) {
        return "Let's go to " + event.getEventName() + ". It's between " + startTime.getText().toString() + " and " +
                endTime.getText().toString() + " at " + address.getText().toString() + ".";
    }
}
