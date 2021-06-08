package com.example.uberclone;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.Toast;

import com.parse.LogInCallback;
import com.parse.ParseAnonymousUtils;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseUser;

public class LoginActivity extends AppCompatActivity {

    //Views
    Switch mSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getSupportActionBar().hide();

        //init
        mSwitch=(Switch) findViewById(R.id.switchUserDriver);

        //The user anonomous login
        if(ParseUser.getCurrentUser()==null){
            Log.i("Start","Logging in started");
            ParseAnonymousUtils.logIn(new LogInCallback() {

                @Override
                public void done(ParseUser user, ParseException e) {
                    if(e==null){
                        Log.i("Start","Logged in");
                    }else{
                        Log.i("Start",e.getMessage());
                    }
                }
            });
        }
        //if already a current user
        else{
            Log.i("Start","Logged In user ");
            started(((Button)findViewById(R.id.button)));
        }
    }

    public void started(View view)  {
        Log.i("Start","Button clicked");
        //giving user a driver or rider (autogenerate row)
        ParseUser.getCurrentUser().put("riderOrDriver",mSwitch.isChecked()?"driver":"rider");
        //saving the new field
        try {
            ParseUser.getCurrentUser().save();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        if(mSwitch.isChecked()){
            startDriverActivity();
        }else{
            startRiderActivity();
        }
    }

    public void startRiderActivity(){
        Log.i("Start","Button Clicked and:::"+ParseUser.getCurrentUser().get("riderOrDriver"));
        if(ParseUser.getCurrentUser().get("riderOrDriver").equals("rider")){
            Intent intent = new Intent(getApplicationContext(),RiderActivity.class);
            startActivity(intent);
            finish();
        }else{
            Toast.makeText(getApplicationContext(),"ERROR loging in automatically as rider, please contact admin (Ayush)",Toast.LENGTH_LONG).show();
        }
    }

    public void startDriverActivity(){
        if(ParseUser.getCurrentUser().get("riderOrDriver")=="driver"){
            Intent intent = new Intent(getApplicationContext(),ViewRequestsActivity.class);
            startActivity(intent);
            finish();
        }else{
            Toast.makeText(getApplicationContext(),"ERROR loging in automatically as driver, please contact admin (Ayush)",Toast.LENGTH_LONG).show();
        }
    }

}