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
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Dialog;
import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.os.IBinder;
import android.text.Html;

public class ComicManager extends Service {
  
  Dialog dialog;
  
  private ComicDbAdapter mDbAdapter;
  
  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
//    String action= intent.getAction();
//    if (action == "update") {
//      ProgressDialog dialog= ProgressDialog.show(this, null, "Updating list...", true, true);
      new Thread() { 
          public void run() {
            updateList();
          }
      }.start();
      return Service.START_NOT_STICKY;
//    }
//    return Service.START_NOT_STICKY;
  }

  @Override
  public IBinder onBind(Intent arg0) {
    // TODO Auto-generated method stub
    return null;
  }
  
  @Override
  public void onCreate() {
    mDbAdapter= new ComicDbAdapter(this);
    mDbAdapter.open();
  }
  
  public void updateList() {
    URL url;
    try {
      url= new URL("http://www.xkcd.com/archive/index.html");
      HttpURLConnection conn= (HttpURLConnection) url.openConnection();
      conn.setDoInput(true);
      BufferedInputStream bi= new BufferedInputStream(conn.getInputStream());
      DataInputStream archive= new DataInputStream(bi);
      
      String line;
      for (int i= 0; i < 67 && archive.available() > 0; ++i) {
        line= archive.readLine();
      }
      
      Cursor mostRecentCursor= mDbAdapter.fetchMostRecentComic();
      long newest= (mostRecentCursor != null) ?
          mostRecentCursor.getLong(mostRecentCursor.getColumnIndexOrThrow("MAX(" + ComicDbAdapter.KEY_NUMBER + ")")) + 1 : 1;
      long number= Long.MAX_VALUE;
      String title;
      Pattern numberPattern= Pattern.compile("(?<=href=\"/).*?(?=/\")"),
              titlePattern= Pattern.compile("(?<=>).*?(?=<)");
      Matcher m;
      while (number > newest && archive.available() > 0) {
        line= archive.readLine();
        if (line.startsWith("<a")) {
          m= numberPattern.matcher(line);
          if (m.find()) {
            number= Long.parseLong(m.group());
            m= titlePattern.matcher(line);
            if (m.find()) {
              title= Html.fromHtml(m.group()).toString();
              mDbAdapter.insertComic(number, title);
              for (int i= 0; i < 3; ++i)
                archive.readLine();
            }
          }
        }
      }
    } catch (MalformedURLException e1) {
      // TODO Auto-generated catch block
    } catch (IOException e) {
      // TODO Auto-generated catch block
    }
  }
}
