package com.example.babysteps_android;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends Activity {

	public final static String EXTRA_MESSAGE = "com.example.babysteps_android.MESSAGE";
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    
    /***
     * Responds to Create Account button click
     * @param view
     */
    public void createAccount(View view) {
    	EditText editText = (EditText) findViewById(R.id.username_field);
    	String message = editText.getText().toString();
    	
    	TextView resultLabel = (TextView) findViewById(R.id.result_label);
    	resultLabel.setText(message);
    }
}
