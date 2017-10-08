/**
 * Copyright 2017 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.api.sample;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import ai.api.android.AIConfiguration;
import ai.api.android.GsonFactory;
import ai.api.model.AIError;
import ai.api.model.AIResponse;
import ai.api.model.Metadata;
import ai.api.model.Result;
import ai.api.model.Status;
import ai.api.ui.AIButton;

import android.app.Activity;
import android.database.Cursor;
import android.view.View;
import android.widget.Toast;
import android.widget.EditText;
import android.database.sqlite.SQLiteDatabase;
public class AIButtonSampleActivity extends BaseActivity implements AIButton.AIButtonListener {

    public static final String TAG = AIButtonSampleActivity.class.getName();
    SQLiteDatabase db;
    TextView tv;
    EditText et1,et2;
    private AIButton aiButton;
    private TextView resultTextView;

    private Gson gson = GsonFactory.getGson();

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_aibutton_sample);
        tv=(TextView)findViewById(R.id.textView1);
        et1=(EditText)findViewById(R.id.editText1);
        et2=(EditText)findViewById(R.id.editText2);
        //create database if not already exist
        db= openOrCreateDatabase("Mydb", MODE_PRIVATE, null);
        //create new table if not already exist
        db.execSQL("create table if not exists collegetable(name varchar, location varchar)");
        resultTextView = (TextView) findViewById(R.id.resultTextView);
        aiButton = (AIButton) findViewById(R.id.micButton);

        final AIConfiguration config = new AIConfiguration(Config.ACCESS_TOKEN,
                AIConfiguration.SupportedLanguages.English,
                AIConfiguration.RecognitionEngine.System);

        config.setRecognizerStartSound(getResources().openRawResourceFd(R.raw.test_start));
        config.setRecognizerStopSound(getResources().openRawResourceFd(R.raw.test_stop));
        config.setRecognizerCancelSound(getResources().openRawResourceFd(R.raw.test_cancel));

        aiButton.initialize(config);
        aiButton.setResultsListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // use this method to disconnect from speech recognition service
        // Not destroying the SpeechRecognition object in onPause method would block other apps from using SpeechRecognition service
        aiButton.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // use this method to reinit connection to recognition service
        aiButton.resume();
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.menu_aibutton_sample, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        final int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            startActivity(AISettingsActivity.class);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResult(final AIResponse response) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "onResult");
                String inte = "",query= "";
                int count = 0;
                String[] key = new String[5];
                String[] value = new String[5];
                //resultTextView.setText(gson.toJson(response));

                Log.i(TAG, "Received success response");

                // this is example how to get different parts of result object
                final Status status = response.getStatus();
                Log.i(TAG, "Status code: " + status.getCode());
                Log.i(TAG, "Status type: " + status.getErrorType());

                final Result result = response.getResult();
                Log.i(TAG, "Resolved query: " + result.getResolvedQuery());
                query = result.getResolvedQuery();
                TTS.speak(query);
                Log.i(TAG, "Action: " + result.getAction());
                final String speech = result.getFulfillment().getSpeech();
                Log.i(TAG, "Speech: " + speech);
                TTS.speak(speech);

                final Metadata metadata = result.getMetadata();
                if (metadata != null) {
                    Log.i(TAG, "Intent id: " + metadata.getIntentId());
                    Log.i(TAG, "Intent name: " + metadata.getIntentName());
                    inte = metadata.getIntentName();
                }

                final HashMap<String, JsonElement> params = result.getParameters();
                if (params != null && !params.isEmpty()) {
                    Log.i(TAG, "Parameters: ");
                    for (final Map.Entry<String, JsonElement> entry : params.entrySet()) {
                        Log.i(TAG, String.format("%s: %s", entry.getKey(), entry.getValue().toString()));
                        key[count] = entry.getKey();
                        value[count] = entry.getValue().toString();
                        count++;
                    }
                }
                resultTextView.setText("value ng value[0]= " +value[0] );
                if(value[0]!=null){
                    resultTextView.append("Intent: "+ inte);
                    if(inte.equals("get_location")){

                        location(inte,key[0],value[0]);
                    }


                } else {

                    TTS.speak("I'm sorry I cant understand what you're saying.Can you say that again? ");
                }


                // resultTextView.setText("Intent; " + inte + " Key; " + key[0] + " Value ; " + value[0]+ " Key; " + key[1] + " Value ; " + value[1] + " Key; " + key[2] + " Value ; " + value[2]);

            }

        });
    }

    private void StoreDatabase() {
        File DbFile = new File(
                "data/data/packagename/tuper.sqlite");
        if (DbFile.exists()) {
            System.out.println("file already exist ,No need to Create");
        } else {
            try {
                DbFile.createNewFile();
                System.out.println("File Created successfully");
                InputStream is = this.getAssets().open("tuper.sqlite");
                FileOutputStream fos = new FileOutputStream(DbFile);
                byte[] buffer = new byte[1024];
                int length = 0;
                while ((length = is.read(buffer)) > 0) {
                    fos.write(buffer, 0, length);
                }
                System.out.println("File succesfully placed on sdcard");
                // Close the streams
                fos.flush();
                fos.close();
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
    public void location(String inte, String key,String value ){

        //    resultTextView.setText("Intent; " + inte + " Key; " + key + " Value ; " + value);
        Cursor c=db.rawQuery("select name,location from collegetable where name = ?",new String[]{value});
        tv.setText("");
        //move cursor to first position
        c.moveToFirst();
        //fetch all data one by one
        int meron = 0;
        do {
            //we can use c.getString(0) here
            //or we can get data using column index
            String name = c.getString(c.getColumnIndex("name"));
            String location = c.getString(1);
            //display on text view
            //resultTextView.append("Name:" + name + " and SurName:" + location + "\n");
            // TTS.speak("You where looking for "+ name);
            TTS.speak(" " + location);
            //move next position until end of the data
            meron = 1;
        } while (c.moveToNext());

        c.close();




    }

    public void insert(View v)
    {
        String name=et1.getText().toString();
        String sur_name=et2.getText().toString();
        et1.setText("");
        et2.setText("");
        //insert data into able
        db.execSQL("insert into collegetable values('"+name+"','"+sur_name+"')");
        //display Toast
        Toast.makeText(this, "values inserted successfully.", Toast.LENGTH_LONG).show();
    }
    public void display(View v)
    {
        //String value = "COS";
        //use cursor to keep all data
        //cursor can keep data of any data type
        Cursor c=db.rawQuery("select * from collegetable",null);
        tv.setText("");
        //move cursor to first position
        c.moveToFirst();
        //fetch all data one by one
        do
        {
            //we can use c.getString(0) here
            //or we can get data using column index
            String name=c.getString(c.getColumnIndex("name"));
            String surname=c.getString(1);
            //display on text view
            tv.append("Name:"+name+" and SurName:"+surname+"\n");
            //move next position until end of the data
        }while(c.moveToNext());
        c.close();
    }
    @Override
    public void onError(final AIError error) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "onError");
                resultTextView.setText(error.toString());
            }
        });
    }

    @Override
    public void onCancelled() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "onCancelled");
                resultTextView.setText("");
            }
        });
    }

    private void startActivity(Class<?> cls) {
        final Intent intent = new Intent(this, cls);
        startActivity(intent);
    }
}
