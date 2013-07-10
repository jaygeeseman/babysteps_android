package com.example.babysteps_android;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends Activity {

    private static final String DEBUG_TAG = "HttpExample";
    private TextView resultLabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        resultLabel = (TextView) findViewById(R.id.result_label);
        return true;
    }

    /**
     * Responds to Create Account button click
     * 
     * @param view
     */
    public void createAccount(View view) {
        EditText editText = (EditText) findViewById(R.id.username_field);
        String username = editText.getText().toString();

        TextView resultLabel = (TextView) findViewById(R.id.result_label);
        resultLabel.setText(String.format("Trying to create new account for %s...", username));

        if (!isNetworkAvailable()) {
            resultLabel.setText("No network connection available.");
            return;
        }

        new CreateAccountTask().execute(username, "device_id", "device_type");
    }

    /**
     * Tells if a network connection is available.
     * 
     * @return
     */
    private boolean isNetworkAvailable() {
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Uses AsyncTask to create a task away from the main UI thread. This task takes a
     * URL string and uses it to create an HttpUrlConnection. Once the connection
     * has been established, the AsyncTask downloads the contents of the webpage as
     * an InputStream. Finally, the InputStream is converted into a string, which is
     * displayed in the UI by the AsyncTask's onPostExecute method.
     */
    private class CreateAccountTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            // params comes from the execute() call:
            // params[0] is the username
            // params[1] is device_id
            // params[2] is device_type
            try {
                return doPost(params[0], params[1], params[2]);
            } catch (IOException e) {
                return "Connection failed! Error - " + e.getLocalizedMessage();
            }
        }

        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) {
            resultLabel.setText(result);
        }

        // Given a URL, establishes an HttpUrlConnection and retrieves
        // the web page content as a InputStream, which it returns as
        // a string.
        private String doPost(String username, String device_id, String device_type) throws IOException {
            InputStream is = null;
            // Only display the first 500 characters of the retrieved
            // web page content.
            int len = 500;

            try {
                URL url = new URL("http://10.0.2.2:9000/users");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setReadTimeout(10000 /* milliseconds */);
                conn.setConnectTimeout(15000 /* milliseconds */);
                conn.setRequestMethod("GET");
                conn.setDoInput(true);
                // Starts the query
                conn.connect();
                int response = conn.getResponseCode();
                Log.d(DEBUG_TAG, "The response is: " + response);
                is = conn.getInputStream();

                // Convert the InputStream into a string
                String contentAsString = readIt(is, len);
                return contentAsString;

                // Makes sure that the InputStream is closed after the app is
                // finished using it.
            } finally {
                if (is != null) {
                    is.close();
                }
            }
        }
        
        // Reads an InputStream and converts it to a String.
        private String readIt(InputStream stream, int len) throws IOException, UnsupportedEncodingException {
            Reader reader = null;
            reader = new InputStreamReader(stream, "UTF-8");
            char[] buffer = new char[len];
            reader.read(buffer);
            return new String(buffer);
        }
    }
}
