package com.anqingchen.onthemap;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mongodb.stitch.android.core.Stitch;
import com.mongodb.stitch.android.core.StitchAppClient;
import com.mongodb.stitch.android.services.mongodb.remote.RemoteMongoClient;
import com.mongodb.stitch.android.services.mongodb.remote.RemoteMongoCollection;
import com.mongodb.stitch.core.internal.common.BsonUtils;
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;
import com.wdullaer.materialdatetimepicker.time.TimePickerDialog;

import org.bson.codecs.configuration.CodecRegistries;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;


public class EventActivity extends AppCompatActivity {

    EditText name, desc, org, addr;
    Button doBtn;
    Spinner typeSpinner;
    DatePickerDialog startDpd, endDpd;
    TimePickerDialog startTpd, endTpd;
    Calendar startDate, endDate;
    TextView startDateText, startTimeText, endDateText, endTimeText;

    RemoteMongoCollection<Event> events;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event);
        name = findViewById(R.id.eventNameText);
        desc = findViewById(R.id.editText2);
        org = findViewById(R.id.editText4);
        addr = findViewById(R.id.editText7);
        doBtn = findViewById(R.id.button);
        startDateText = findViewById(R.id.textView7);
        startTimeText = findViewById(R.id.textView8);
        endDateText = findViewById(R.id.textView9);
        endTimeText = findViewById(R.id.textView10);

        // Initialize top tool bar
        Toolbar toolbar = findViewById(R.id.eventToolBar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.getNavigationIcon().setColorFilter(getColor(android.R.color.black), PorterDuff.Mode.SRC_ATOP);

        // Configure Date/Time displays
        String datePattern = "MMMM dd,yyyy";
        String timePattern = "KK:mm a";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(datePattern);
        SimpleDateFormat simpleTimeFormat = new SimpleDateFormat(timePattern);

        // Initialize MongoDB
        StitchAppClient client = Stitch.getDefaultAppClient();
        final RemoteMongoClient mongoClient = client.getServiceClient(
                RemoteMongoClient.factory, "mongodb-atlas");

        events = mongoClient.getDatabase("app")
                .getCollection("events", Event.class)
                .withCodecRegistry(CodecRegistries.fromRegistries(
                        BsonUtils.DEFAULT_CODEC_REGISTRY,
                        CodecRegistries.fromCodecs(Event.full_codec)
                ));

        // Timestamp for reference
        Calendar now = Calendar.getInstance();
        startDate = Calendar.getInstance();
        endDate = Calendar.getInstance();

        // Configure Date/Time Picker Dialogs
        startDpd = DatePickerDialog.newInstance(
                (view, year, monthOfYear, dayOfMonth) -> {
                    startDate.set(year, monthOfYear, dayOfMonth);
                    startDateText.setText(simpleDateFormat.format(new Date(startDate.getTimeInMillis())));
                },
                now.get(Calendar.YEAR),
                now.get(Calendar.MONTH),
                now.get(Calendar.DAY_OF_MONTH)
        );

        endDpd = DatePickerDialog.newInstance(
                (view, year, monthOfYear, dayOfMonth) -> {
                    endDate.set(year, monthOfYear, dayOfMonth);
                    endDateText.setText(simpleDateFormat.format(new Date(endDate.getTimeInMillis())));
                },
                now.get(Calendar.YEAR),
                now.get(Calendar.MONTH),
                now.get(Calendar.DAY_OF_MONTH)
        );

        startTpd = TimePickerDialog.newInstance(
                (view, hourOfDay, minute, second) -> {
                    startDate.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    startDate.set(Calendar.MINUTE, minute);
                    startDate.set(Calendar.SECOND, second);
                    startTimeText.setText(simpleTimeFormat.format(new Date(startDate.getTimeInMillis())));
                },
                now.get(Calendar.HOUR_OF_DAY),
                now.get(Calendar.MINUTE),
                true
        );

        endTpd = TimePickerDialog.newInstance(
                (view, hourOfDay, minute, second) -> {
                    endDate.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    endDate.set(Calendar.MINUTE, minute);
                    endDate.set(Calendar.SECOND, second);
                    endTimeText.setText(simpleTimeFormat.format(new Date(endDate.getTimeInMillis())));
                },
                now.get(Calendar.HOUR_OF_DAY),
                now.get(Calendar.MINUTE),
                true
        );



        // OnClickListener for button to submit event add request
        doBtn.setOnClickListener(view -> {
            // Check for form completion
            if(name.getText().toString().isEmpty() || desc.getText().toString().isEmpty() || org.getText().toString().isEmpty() || addr.getText().toString().isEmpty() || startTimeText.getText().toString().equals("Start Time")
            || startDateText.getText().toString().equals("Start Date") || endTimeText.getText().toString().equals("End Time") || endDateText.getText().toString().equals("End Date")) {
                Toast.makeText(EventActivity.this, "NOT ENOUGH INFO", Toast.LENGTH_SHORT).show();
            } else if(startDate.after(endDate)) {   // Check if start time is configured after end time!
               Toast.makeText(EventActivity.this, "Start Date/Time is After the End Time, Please Check Your Inputs", Toast.LENGTH_LONG).show();
            } else {
                // store event
                Event newEvent;
                LatLng tempLatLng;
                String address = addr.getText().toString();
                tempLatLng = getLocationFromAddress(getApplicationContext(), address);
                if (tempLatLng != null) {
                    newEvent = new Event(tempLatLng, name.getText().toString(), desc.getText().toString(), org.getText().toString(), startDate.getTimeInMillis(), endDate.getTimeInMillis());
                    writeNewEvent(newEvent);
                }
                else {
                    Log.i("DEBUG< ADDR", "ADDRESS HANDLING ERROR");
                }
            }
        });
    }

    // Update MongoDB Realtime with newEvent
    private void writeNewEvent(final Event newEvent) {
        events.insertOne(newEvent).addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Toast.makeText(EventActivity.this, "Your Event Failed To Be Added", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(EventActivity.this, "Your Event " + newEvent.getEventName() + " Has Been Successfully Added", Toast.LENGTH_SHORT).show();
                Stitch.getDefaultAppClient().getAuth().logout();
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
            }
        });
    }

    // Get LatLng objects from an address string
    public LatLng getLocationFromAddress(Context context, String strAddress) {
        Geocoder coder = new Geocoder(context);
        List<Address> address;
        LatLng p1 = null;
        try {
            // May throw an IOException
            address = coder.getFromLocationName(strAddress, 5);
            if (address.isEmpty()) {
                return null;
            }
            Address location = address.get(0);
            p1 = new LatLng(location.getLatitude(), location.getLongitude() );
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return p1;
    }

    // OnClick functions for Date/Time Picker TextViews
    public void startDatePicker(View v) {
        startDpd.show(getSupportFragmentManager(), "Start Date");
    }

    public void startTimePicker(View v) {
        startTpd.show(getSupportFragmentManager(), "Start Time");
    }

    public void endDatePicker(View v) {
        endDpd.show(getSupportFragmentManager(), "End Date");
    }

    public void endTimePicker(View v) {
        endTpd.show(getSupportFragmentManager(), "End Time");
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        super.onBackPressed();
    }


}
