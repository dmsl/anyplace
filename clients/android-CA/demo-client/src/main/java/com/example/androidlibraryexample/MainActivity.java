package com.example.androidlibraryexample;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;

import cy.ac.ucy.cs.anyplace.Anyplace;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Anyplace client = new Anyplace("ap.cs.ucy.ac.cy","443","res/");
        Log.println(Log.DEBUG,"Test", client.buildingAll());
    }
}
