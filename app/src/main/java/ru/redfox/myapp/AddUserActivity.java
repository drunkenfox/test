package ru.redfox.myapp;

import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

public class AddUserActivity extends AppCompatActivity {

    private static final int RESULT_LOAD_IMAGE = 100;
    ImageView imageView;
    EditText etFirstName;
    EditText etLastName;
    Bitmap imageBitmap;
    ProgressDialog progressDialog;
    String picturePath;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_user);
        imageView = (ImageView) findViewById(R.id.ivAddAvatar);
        etFirstName = (EditText)findViewById(R.id.etFirstName);
        etLastName = (EditText)findViewById(R.id.etLastName);
    }

    //кнопка Добавить изображение
    public void onAddPic(View v){
        Intent i = new Intent(Intent.ACTION_PICK,android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(i, RESULT_LOAD_IMAGE);
    }

    //Кнопка сохранить
    public void onSaveBtn(View v){
        User u = new User();
        u.setId(UUID.randomUUID().toString());
        u.setFirst_name(etFirstName.getText().toString());
        u.setLast_name(etLastName.getText().toString());

        if(imageBitmap != null){

            Calendar c = Calendar.getInstance();
            System.out.println("Current time => "+c.getTime());

            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
            String formattedDate = df.format(c.getTime());

            String s3url = "https://s3-us-west-2.amazonaws.com/mbi3/" + formattedDate + ".jpg";

            u.setAvatar(s3url);

            File file = new File(picturePath);
            // Initialize the Amazon Cognito credentials provider
            CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
                    getApplicationContext(),
                    "us-west-2:90821c83-99a2-437d-996e-fe52ba2b09cf", // Identity Pool ID
                    Regions.US_WEST_2 // Region
            );
            AmazonS3 s3 = new AmazonS3Client(credentialsProvider);

            TransferUtility transferUtility = new TransferUtility(s3, getApplicationContext());
            TransferObserver observer = transferUtility.upload(
                    "mbi3",     /* The bucket to upload to */
                    formattedDate + ".jpg",    /* The key for the uploaded object */
                    file        /* The file where the data to upload exists */
            );

        }
        else {
            u.setAvatar("https://s3-us-west-2.amazonaws.com/mbi3/user.png");
        }

        Gson gson = new Gson();
        String json = gson.toJson(u);

        json = "{\"TableName\": \"users\",\n" +
                "      \"Item\":" + json +"}";

        new postAsync(json).execute("https://3hpdtfuza3.execute-api.us-west-2.amazonaws.com/prod/myFunc");

        Toast.makeText(this,"Сохранено",Toast.LENGTH_LONG).show();

        this.finish();

    }

    //Получаем картинку из галереи
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && null != data) {
            Uri selectedImage = data.getData();
            String[] filePathColumn = { MediaStore.Images.Media.DATA };
            Cursor cursor = getContentResolver().query(selectedImage,filePathColumn, null, null, null);
            cursor.moveToFirst();
            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            picturePath = cursor.getString(columnIndex);
            cursor.close();
            imageBitmap = decodeSampledBitmapFromFile(picturePath, 128, 128);
            imageView.setImageBitmap(imageBitmap);
        }
    }

    /**
     * Функция изменения размера изображения
     * @param path путь к изображению
     * @param reqWidth заданная ширина
     * @param reqHeight заданная высота
     * @return преобразованный bitmap
     */
    private static Bitmap decodeSampledBitmapFromFile(String path, int reqWidth, int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(path, options);
    }

    /**
     * Функция вычисления размера изображения
     * @param options
     * @param reqWidth
     * @param reqHeight
     * @return
     */
    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    /**
     * Функция преобразования изображения в байты, а байты в вид String
     * @param bitmap
     * @return
     */
    public String BitmapToString(Bitmap bitmap){
        ByteArrayOutputStream baos=new  ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG,100, baos);
        byte [] b=baos.toByteArray();
        String temp= Base64.encodeToString(b, Base64.DEFAULT);
        return temp;
    }

    //POST - запрос на вставку данных
    private class postAsync extends AsyncTask<String,Void,Void> {
        private String json;

        public postAsync(String _json){
            json = _json;
        }

        @Override
        protected Void doInBackground(String... params) {

            try {
                URL url = new URL(params[0]);
                HttpURLConnection httpURLConnection = (HttpURLConnection)url.openConnection();
                httpURLConnection.setDoOutput(true);
                httpURLConnection.setRequestMethod("POST");
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
                Log.d("PostResult", sb.toString());

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }


    }
}
