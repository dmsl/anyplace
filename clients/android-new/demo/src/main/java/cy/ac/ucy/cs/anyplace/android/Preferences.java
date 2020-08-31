package cy.ac.ucy.cs.anyplace.android;

import android.content.Intent;
import android.os.Bundle;
//import android.support.v7.app.AppCompatActivity;
//import android.support.v7.app.AppCompatActivity;
//import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

//import android.support.v7.app.AppCompatActivity;

//import android.support.v7.app.AppCompatActivity;

//import android.support.v7.app.AppCompatActivity;

public class Preferences extends AppCompatActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_preferences);


    Button preferencesBtn = (Button) findViewById(R.id.preferencesBtn);
    preferencesBtn.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        Intent startIntent = new Intent(getApplicationContext(), MainActivity.class);


        EditText hostEditText = (EditText) findViewById(R.id.hostEditText);
        EditText portEditText = (EditText) findViewById(R.id.portEditText);
        EditText apikeyEditText = (EditText) findViewById(R.id.apikeyEditText);

        //startIntent.putExtra("cy.ac.ucy.cs.anyplace.android.apikey", apikeyEditText.getText().toString());
        //startIntent.putExtra("cy.ac.ucy.cs.anyplace.android.host", hostEditText.getText().toString());
        //startIntent.putExtra("cy.ac.ucy.cs.anyplace.android.port", portEditText.getText().toString());


        startIntent.putExtra("cy.ac.ucy.cs.anyplace.android.apikey", "eyJhbGciOiJSUzI1NiIsImtpZCI6IjhjNThlMTM4NjE0YmQ1ODc0MjE3MmJkNTA4MGQxOTdkMmIyZGQyZjMiLCJ0eXAiOiJKV1QifQ");
        startIntent.putExtra("cy.ac.ucy.cs.anyplace.android.host", "ap-dev.cs.ucy.ac.cy");
        startIntent.putExtra("cy.ac.ucy.cs.anyplace.android.port", "443");

        startActivity(startIntent);
      }
    });



  }
}