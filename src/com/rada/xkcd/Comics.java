/*
 * Copyright (C) 2010  Alex Avance
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-
 * 1307, USA.
 */
package com.rada.xkcd;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.widget.Toast;

public final class Comics {
  
  public static final Executor BACKGROUND_EXECUTOR= Executors.newSingleThreadExecutor();
  
  public static final String ACTION_VIEW= "com.rada.xkcd.action.VIEW";
  public static final String ACTION_DOWNLOAD= "com.rada.xkcd.action.DOWNLOAD";
  public static final String ACTION_CLEAR= "com.rada.xkcd.action.CLEAR";
  public static final String ACTION_VIEW_FAVORITES= "com.rada.xkcd.action.VIEW_FAVORITES";
  
  public static final String KEY_NUMBER= "_id";
  public static final String KEY_TITLE= "title";
  public static final String KEY_TEXT= "hover";
  public static final String KEY_URL= "url";
  public static final String KEY_FAVORITE= "favorite";
  public static final String[] ALL_COLUMNS= new String[] { KEY_NUMBER, KEY_TITLE, KEY_TEXT, KEY_URL, KEY_FAVORITE };
  
  public static final int STATUS_SUCCESS= 0;
  public static final int STATUS_FAILURE= 1;
  public static final int STATUS_ERROR= 2;
  public static final int STATUS_CANCELLED= 3;
  
  public static final int MESSAGE_DOWNLOAD= 500;
  public static final int MESSAGE_CLEAR= 501;
  
  public static final int BUFFER_SIZE= 10000;
  public static final int MAX_DOWNLOAD_ATTEMPTS= 99;
  
  public static final String SD_DIR_PATH= "/sdcard/xkcd/";
  public static final File SD_DIR= new File(SD_DIR_PATH);
  
  public static final String MAIN_URL= "http://www.xkcd.com/";
  public static final String ARCHIVE_URL= MAIN_URL + "archive/index.html";
  
  public static class ToastRunnable implements Runnable {
    Toast toast;
    
    ToastRunnable(Context context, CharSequence text, int duration) {
      toast= Toast.makeText(context, text, duration);
    }
    
    public void run() {
      toast.show();
    }
  }
  
  public static final InputStream download(URL url) throws IOException {
    HttpURLConnection conn= (HttpURLConnection) url.openConnection();
    conn.setDoInput(true);
    return conn.getInputStream();
  }
  
  public static final InputStream download(String url) throws MalformedURLException, IOException {
    return download(new URL(url));
  }

  public static final void downloadComic(long comicNumber, ComicDbAdapter dbAdapter) throws MalformedURLException, IOException, Exception {
    if (!Comics.SD_DIR.exists())
      Comics.SD_DIR.mkdirs();
    
    File file= new File(SD_DIR_PATH + comicNumber);
    
    Cursor cursor= dbAdapter.fetchComic(comicNumber);
    String url= cursor.getString(cursor.getColumnIndexOrThrow(KEY_URL));

    if (url == null) {
      dbAdapter.updateComic(comicNumber);
      cursor= dbAdapter.fetchComic(comicNumber);
      url= cursor.getString(cursor.getColumnIndexOrThrow(KEY_URL));
    }
    cursor.close();

    Bitmap image= null;
    for (int i= 0; i < MAX_DOWNLOAD_ATTEMPTS && image == null; ++i) {
      // I'm not using a buffered input stream because it has in the past
      // caused more download issues than it's worth for the performance boost
      image= BitmapFactory.decodeStream(Comics.download(url));
    }
    if (image == null)
      throw new Exception("image is null");
    
    FileOutputStream ostream= new FileOutputStream(file);
    BufferedOutputStream bo= new BufferedOutputStream(ostream, BUFFER_SIZE);
    image.compress(CompressFormat.PNG, 100, bo);
    bo.close();
    ostream.close();
  }
}
