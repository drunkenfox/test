package ru.redfox.myapp;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.ContextMenu;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    String urlGet = "https://3hpdtfuza3.execute-api.us-west-2.amazonaws.com/prod/myFunc?TableName=users";
    ProgressDialog progressDialog;
    String TAG="MyLog";
    ListView lvUsers;
    UsersListViewAdapter myAdapter;
    ArrayList<User> users;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        lvUsers = (ListView)findViewById(R.id.lvList);
        registerForContextMenu(lvUsers);
        users = new ArrayList<>();

        setSupportActionBar(toolbar);

        //кнопка добавить юзера
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //вызов окна добавления юзера
                Intent i = new Intent(getApplicationContext(), AddUserActivity.class);
                startActivity(i);

            }
        });

        //new JsonTask().execute(urlGet);

    }

    @Override
    protected void onResume() {
        super.onResume();
        new JsonTask().execute(urlGet);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    //контекстное меню списка
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        if (v.getId()==R.id.lvList) {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)menuInfo;
            menu.add(Menu.NONE,0,0,"Удалить");
            menu.add(Menu.NONE,1,1,"Отмена");
        }
    }

    //выбор элемента контекстного меню
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
        switch (item.getItemId()){
            //удалить
            case 0:
                Log.d(TAG, "Selected item 0");
                User u = myAdapter.getItem(info.position);
                Log.d("UserId", u.getId());
                Log.d("UserName", u.getFirst_name());
                new deleteAsync(myAdapter.getItem(info.position).getId())
                        .execute("https://3hpdtfuza3.execute-api.us-west-2.amazonaws.com/prod/myFunc");
                myAdapter.removeItem(myAdapter.getItem(info.position));
                myAdapter.notifyDataSetChanged();
                return true;
            //отмена
            case 1:
                Log.d(TAG, "Selected item 1");
                return true;
            default:
                return super.onContextItemSelected(item);

        }

    }

//задача загрузки данных из сервера в фоне
    private class JsonTask extends AsyncTask<String, String, JSONObject> {

        protected void onPreExecute() {
            super.onPreExecute();

            progressDialog = new ProgressDialog(MainActivity.this);
            progressDialog.setMessage("Загрузка...");
            progressDialog.setCancelable(false);
            progressDialog.show();
        }

        protected JSONObject doInBackground(String... params) {

            HttpURLConnection connection = null;
            BufferedReader reader = null;

            try {
                URL url = new URL(params[0]);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                InputStream stream = connection.getInputStream();

                reader = new BufferedReader(new InputStreamReader(stream));

                StringBuffer buffer = new StringBuffer();
                String line = "";

                while ((line = reader.readLine()) != null) {
                    buffer.append(line);
                }
                try {
                    return new JSONObject(buffer.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }


            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
                try {
                    if (reader != null) {
                        reader.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }


        //вывод результатов запроса
        @Override
        protected void onPostExecute(JSONObject response) {
            super.onPostExecute(response);

            //ArrayList<User> users = new ArrayList<>();
            users.clear();
            if (progressDialog.isShowing()){
                progressDialog.dismiss();
            }
            Log.d(TAG, String.valueOf(response));

            try {
                JSONArray jarr = response.getJSONArray("Items");
                Log.d(TAG,jarr.toString());
                for(int i = 0; i<jarr.length();i++){
                    User user = new User();
                    user.setFirst_name(jarr.getJSONObject(i).getString("first_name"));
                    user.setLast_name(jarr.getJSONObject(i).getString("last_name"));
                    user.setAvatar(jarr.getJSONObject(i).getString("avatar"));
                    user.setId(jarr.getJSONObject(i).getString("id"));
                    users.add(user);
                }

                myAdapter = new UsersListViewAdapter(getApplicationContext(), users);
                lvUsers.setAdapter(myAdapter);

            } catch (JSONException e) {
                Log.e(TAG,e.getMessage());
            }

        }
    }

    //DELETE - запрос на удвление данных
    private class deleteAsync extends AsyncTask<String,Void,Void> {
        private String key;
        private String json;

        public deleteAsync(String _key){
            key = _key;
            json = "{\"TableName\": \"users\",\n" +
                    "      \"Key\":" +
                    "{\"id\": "+
                            "\""+ key +"\"" +"}}";
            Log.d("Key", key);
            Log.d("Json", json);
        }

        @Override
        protected Void doInBackground(String... params) {

            try {
                URL url = new URL(params[0]);
                HttpURLConnection httpURLConnection = (HttpURLConnection)url.openConnection();
                httpURLConnection.setDoOutput(true);
                httpURLConnection.setRequestMethod("DELETE");
                httpURLConnection.setRequestProperty("Content-Type", "application/json");
                httpURLConnection.setRequestProperty("Accept", "application/json");
                httpURLConnection.connect();

                //Write
                OutputStream outputStream = httpURLConnection.getOutputStream();
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream, "UTF-8"));
                writer.write(json);
                writer.close();
                outputStream.close();

                //Read
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream(), "UTF-8"));

                String line = null;
                StringBuilder sb = new StringBuilder();

                while ((line = bufferedReader.readLine()) != null) {
                    sb.append(line);
                }

                bufferedReader.close();
                Log.d("DeleteResult", sb.toString());

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }
    }
}
