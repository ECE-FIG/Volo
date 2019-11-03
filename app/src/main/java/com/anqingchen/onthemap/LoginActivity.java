package com.anqingchen.onthemap;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.mongodb.stitch.android.core.Stitch;
import com.mongodb.stitch.core.auth.providers.userpassword.UserPasswordCredential;

import java.util.Arrays;

public class LoginActivity extends AppCompatActivity {

    Button doBtn;
    EditText email, pwd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        doBtn = findViewById(R.id.button);
        email = findViewById(R.id.editText);
        pwd = findViewById(R.id.editText2);

        doBtn.setOnClickListener(view -> {
            Organization temp = new Organization(email.getText().toString(), pwd.getText().toString().toCharArray());
            authenticate(temp);
        });
    }

    public void authenticate(Organization organization) {
        UserPasswordCredential credential = new UserPasswordCredential(organization.getEmail(), String.valueOf(organization.getPassword()));

        Stitch.getDefaultAppClient().getAuth().loginWithCredential(credential)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Log.d("stitch", "Successfully logged in as organization " + task.getResult().getId());
                    Intent intent = new Intent(this, EventActivity.class);
                    startActivity(intent);
                } else {
                    Log.e("stitch", "Error logging in with email/password auth:", task.getException());
                }
            });
    }
}
