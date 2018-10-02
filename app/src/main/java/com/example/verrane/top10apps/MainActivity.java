package com.example.verrane.top10apps;

import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class MainActivity extends AppCompatActivity
{

    private static final String TAG = "MainActivity";
    private ListView listApps;
    private String fdURL = "http://ax.itunes.apple.com/WebObjects/MZStoreServices.woa/ws/RSS/topfreeapplications/limit=%d/xml";
    private int fdLimit = 10;
    private String fdCachedURL = "INVALIDATED";
    public static final String STATE_URL = "fdURL";
    public static final String STATE_LIMIT = "fdLimit";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listApps = (ListView) findViewById(R.id.xmlListView);

        if (savedInstanceState != null) {
            fdURL = savedInstanceState.getString(STATE_URL);
            fdLimit = savedInstanceState.getInt(STATE_LIMIT);
        }

//        Log.d(TAG, "onCreate: starting AsyncTask");
        downloadURL(String.format(fdURL, fdLimit));
//        Log.d(TAG, "onCreate: Done.");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.feeds_menu_layout, menu);
        if (fdLimit == 10){
            menu.findItem(R.id.mnu10).setChecked(true);
        } else {
            menu.findItem(R.id.mnu25).setChecked(true);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id){
            case R.id.mnuFreeApps :
                fdURL = "http://ax.itunes.apple.com/WebObjects/MZStoreServices.woa/ws/RSS/topfreeapplications/limit=%d/xml";
                break;
            case R.id.mnuPaidApps :
                fdURL = "http://ax.itunes.apple.com/WebObjects/MZStoreServices.woa/ws/RSS/toppaidapplications/limit=%d/xml";
                break;
            case R.id.mnuSongsList :
                fdURL = "http://ax.itunes.apple.com/WebObjects/MZStoreServices.woa/ws/RSS/topsongs/limit=%d/xml";
                break;
            case R.id.mnu10 :
            case R.id.mnu25 :
                if(!item.isChecked()){
                    item.setChecked(true);
                    fdLimit = 10+25-fdLimit;
                    Log.d(TAG, "onOptionsItemSelected: "+ item.getTitle() +" setting limit to "+ fdLimit);
                } else {
                    Log.d(TAG, "onOptionsItemSelected: "+ item.getTitle() +" unchanged");
                }
                break;
            case R.id.mnuRefresh :
                fdCachedURL = "INVALIDATED";
                break;
            default :
                return super.onOptionsItemSelected(item);
        }
        downloadURL(String.format(fdURL, fdLimit));
        return true;
    }

    private void downloadURL(String feedURL){
        if (!fdURL.equalsIgnoreCase(fdCachedURL)) {
            DownloadData downloadData = new DownloadData();
            downloadData.execute(feedURL);
            fdCachedURL = fdURL;
        } else { /*URL not changed*/ }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString(STATE_URL, fdURL);
        outState.putInt(STATE_LIMIT, fdLimit);
        super.onSaveInstanceState(outState);
    }

    private class DownloadData extends AsyncTask<String, Void, String>
    {

        private static final String TAG = "DownloadData";

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
//            Log.d(TAG, "onPostExecute: parameter is : " +s );
            ParseApplications parseApplications = new ParseApplications();
            parseApplications.parse(s);

//            ArrayAdapter<FeedEntry> arrayAdapter = new ArrayAdapter<>(MainActivity.this, R.layout.list_item, parseApplications.getApplications());
//            listApps.setAdapter(arrayAdapter);
            FeedAdapter feedAdapter = new FeedAdapter(MainActivity.this, R.layout.list_record, parseApplications.getApplications());
            listApps.setAdapter(feedAdapter);
        }

        @Override
        protected String doInBackground(String... strings) {
            Log.d(TAG, "doInBackground: starts with : " +strings[0]);
            String rssfeed = downloadfileXML(strings[0]);
            if (rssfeed == null)
            {
                Log.e(TAG, "doInBackground: ERROR IN DOWNLOADING");
            }
            return rssfeed;
        }

        private String downloadfileXML(String urlPath)
        {
            StringBuilder xmlResult = new StringBuilder();

            try
            {
                URL url = new URL(urlPath);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                int response = connection.getResponseCode();
                Log.d(TAG, "downloadfileXML: the response code was "+response);

//The above lines can also be written as :
//                InputStream inputStream = connection.getInputStream();
//                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
//                BufferedReader reader = new BufferedReader(inputStreamReader);
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                int charsRead;
                char[] inputBuffer = new char[1000];
                while (true)
                {
                    charsRead = reader.read(inputBuffer);
                    if (charsRead<0)
                    {    break;    }
                    if (charsRead>0)
                    {
                        xmlResult.append(String.copyValueOf(inputBuffer, 0, charsRead));
                    }
                }
                reader.close();

                return xmlResult.toString();
            } catch (MalformedURLException e){
                Log.e(TAG, "downloadfileXML: Invalid URL : "+e.getMessage());
            } catch (IOException e) {
                Log.e(TAG, "downloadfileXML: IOException error : "+e.getMessage());
            } catch (SecurityException e)
            {
                Log.e(TAG, "downloadfileXML: Security Exception : "+e.getMessage());
                e.printStackTrace();
            }
            return null;
        }

    }

}
