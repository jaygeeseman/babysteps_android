package com.example.babysteps_android;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;

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
                String postBody = getPostJson(params[0], params[1], params[2]);
                return doPost(postBody);
            }
            catch (IOException e) {
                e.printStackTrace();
                return "Connection failed! Error - " + e.toString();
            }
            catch (JSONException e) {
                e.printStackTrace();
                return "Error parsing JSON: " + e.toString();
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
        private String doPost(String postBody) throws IOException {
            InputStream inputStream = null;
            HttpURLConnection conn = null;

            try {
                // Create the connection
                URL url = new URL("http://10.0.2.2:9000/users");
                conn = (HttpURLConnection) url.openConnection();
                conn.setReadTimeout(10000 /* milliseconds */);
                conn.setConnectTimeout(15000 /* milliseconds */);
                conn.setDoInput(true);
                conn.setDoOutput(true);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.connect();
                
                // Send the POST body
                DataOutputStream out = new DataOutputStream(conn.getOutputStream());
                out.writeBytes(postBody);
                out.flush();
                out.close();

                // Get the response
                int response = conn.getResponseCode();
                Log.d(DEBUG_TAG, "The response code is: " + response);

                if (response > 199 && response < 300) {
                    inputStream = conn.getInputStream();
                } else {
                    inputStream = conn.getErrorStream();
                }
                
                String contentAsString = inputStreamToString(inputStream);
                Log.d(DEBUG_TAG, "The response body is: " + contentAsString);

                return contentAsString;
            } finally {
                if (inputStream != null) {
                    inputStream.close();
                }
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }
        
        // Reads an InputStream and converts it to a String.
        private String inputStreamToString(InputStream stream) throws IOException, UnsupportedEncodingException {
            StringWriter writer = new StringWriter();
            IOUtils.copy(stream, writer, "UTF-8");
            return writer.toString();
        }
        
        private String getPostJson(String username, String device_id, String device_type) throws JSONException {
            JSONObject jsonParam = new JSONObject();
            jsonParam.put("device_type", device_type);
            jsonParam.put("device_id", device_id);
            jsonParam.put("username", username);
            return jsonParam.toString();
        }
    }
}
