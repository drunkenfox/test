package ru.redfox.myapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

/**
 * Created by Ilshat on 14.03.2017.
 */

/**
 * класс кастомный адаптер для 2-х строк текста и картинки
 */
public class UsersListViewAdapter extends BaseAdapter {
    Context ctx;
    LayoutInflater lInflater;
    ArrayList<User> objects;

    UsersListViewAdapter(Context context, ArrayList<User> devices) {
        ctx = context;
        objects = devices;
        lInflater = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return objects.size();
    }

    public boolean removeItem(User i) {
        return objects.remove(i);
    }

    public User getItem(int i) {
        return objects.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View convertView, ViewGroup viewGroup) {
        View view = convertView;
        if (view == null) {
            view = lInflater.inflate(R.layout.users_list_layout, null, false);
        }

        User p = getItem(i);

        ImageView iv = (ImageView) view.findViewById(R.id.ivAvatar);
        new DownloadImageTask(iv).execute(p.getAvatar());
        ((TextView) view.findViewById(R.id.tvFirstName)).setText(p.getFirst_name());
        ((TextView) view.findViewById(R.id.tvLastName)).setText(p.getLast_name());

        return view;
    }

    //фоновая загрузка изображения
    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        ImageView bmImage;

        public DownloadImageTask(ImageView bmImage) {
            this.bmImage = bmImage;
        }

        protected Bitmap doInBackground(String... urls) {
            String urldisplay = urls[0];
            Bitmap mIcon11 = null;
            try {
                InputStream in = new java.net.URL(urldisplay).openStream();
                mIcon11 = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e("Error", e.getMessage());
                e.printStackTrace();
            }
            return mIcon11;
        }

        protected void onPostExecute(Bitmap result) {
            bmImage.setImageBitmap(result);
        }
    }
}
