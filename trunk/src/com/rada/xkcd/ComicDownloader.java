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

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.IBinder;

public class ComicDownloader extends Service {

  public static final String ACTION= "action";
  public static final int ACTION_DOWNLOAD= 0;
  public static final int ACTION_DELETE= 1;
  
  private ComicDbAdapter mDbHelper;
  private Downloader mDownloader;

  /** Called when the activity is first created. */
  @Override
  public void onCreate() {
    super.onCreate();
    mDbHelper= new ComicDbAdapter(this);
    mDbHelper.open();
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    boolean success;
    Bundle extras= intent.getExtras();
    if (!extras.containsKey(ComicDbAdapter.KEY_NUMBER))
      if (extras.getInt(ACTION) == ACTION_DOWNLOAD)
        success= mDownloader.downloadAll();
      else
        success= mDownloader.deleteAll();
    else
      if (extras.getInt(ACTION) == ACTION_DOWNLOAD)
        success= mDownloader.download(extras.getLong(ComicDbAdapter.KEY_NUMBER));
      else
        success= mDownloader.delete(extras.getLong(ComicDbAdapter.KEY_NUMBER));
    if (!success)
      //TODO print failure message
      ;
    return START_NOT_STICKY;
  }
  
  public class Downloader {
    
    public boolean download(long number) {
      String url= "http://www.xkcd.com/" + number + "/index.html";
      URL fileUrl= null;
      try {
        fileUrl= new URL(url);
      } catch (MalformedURLException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      try {
        HttpURLConnection conn= (HttpURLConnection) fileUrl.openConnection();
        conn.setDoInput(true);
        conn.connect();
        BufferedInputStream bi= new BufferedInputStream(conn.getInputStream());
        DataInputStream file= new DataInputStream(bi);
        String line, picture= null, hoverText= null;
        Pattern urlPattern= Pattern.compile("(?<=src=\").*?(?=\")");
        Pattern textPattern= Pattern.compile("(?<=title=\").*?(?=\")");
        Matcher m;
        while (file.available() > 0 && !(line= file.readLine()).startsWith("<img")) {
          m= urlPattern.matcher(line);
          if (m.find())
            picture= m.group();
          m= textPattern.matcher(line);
          if (m.find())
            hoverText= m.group();
          mDbHelper.updateComic(number, hoverText, picture);
        }
        Bitmap bitmap;
        URL pictureUrl= new URL(picture);
        conn.disconnect();
        conn= (HttpURLConnection) pictureUrl.openConnection();
        conn.setDoInput(true);
        InputStream is= conn.getInputStream();
        bitmap= BitmapFactory.decodeStream(is);
        FileOutputStream out= new FileOutputStream("/sdcard/xkcd/" + number);
        bitmap.compress(Bitmap.CompressFormat.PNG, 0, out);
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      return true;
    }
    
    public boolean delete(long number) {
      return true;
    }
    
    public boolean downloadAll() {
      return true;
    }
    
    public boolean deleteAll() {
      return true;
    }
  }

  @Override
  public IBinder onBind(Intent intent) {
    // TODO Auto-generated method stub
    return null;
  }
}
