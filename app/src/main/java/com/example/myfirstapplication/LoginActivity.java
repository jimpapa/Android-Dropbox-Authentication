package com.example.myfirstapplication;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.dropbox.core.android.Auth;

public class LoginActivity extends AppCompatActivity {
    private TextView edittextview;
    private Button btn;
    private String Name ="JIM";
    private static final String ACCESS_TOKEN = "DYhjLfye55AAAAAAAAAAQG54Gz2aAtOqu4Cf_JvnMXEUpvg1Regv64JyO7dy1fbM";
    // Replace APP_KEY from your APP_KEY
    final static private String APP_KEY = "olo5v0ja8wfz4g7";
    // Relace APP_SECRET from your APP_SECRET
    final static private String APP_SECRET = "2xtjtmuvq1fixtu";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET)
                != PackageManager.PERMISSION_GRANTED) {
            //NOTHING
        } else{
            Toast.makeText(LoginActivity.this,"TEST",Toast.LENGTH_SHORT).show();
        }


        btn = (Button) findViewById(R.id.button);
        btn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {

                Auth.startOAuth2Authentication(getApplicationContext(), getString(R.string.app_key));

                edittextview = (TextView) findViewById(R.id.textView);
                edittextview.setText(Name);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        getAccessToken();
    }

    public void getAccessToken() {
        String accessToken = Auth.getOAuth2Token(); //generate Access Token
        if (accessToken != null) {
            //Store accessToken in SharedPreferences
            SharedPreferences prefs = getSharedPreferences("com.example.myfirstapplication", Context.MODE_PRIVATE);
            prefs.edit().putString("access-token", accessToken).apply();

            //Proceed to MainActivity
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
        }
    }
}
