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

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public final class Comics {
  
  public static final String ACTION_VIEW= "com.rada.xkcd.action.VIEW";
  public static final String ACTION_DOWNLOAD= "com.rada.xkcd.action.DOWNLOAD";
  public static final String ACTION_CLEAR= "com.rada.xkcd.action.CLEAR";
  
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
  
  public static final String MAIN_URL= "http://www.xkcd.com/";
  public static final String ARCHIVE_URL= MAIN_URL + "archive/index.html";
  
  public static final InputStream download(URL url) throws IOException {
    HttpURLConnection conn= (HttpURLConnection) url.openConnection();
    conn.setDoInput(true);
    return conn.getInputStream();
  }
  
  public static final InputStream download(String url) throws MalformedURLException, IOException {
    return download(new URL(url));
  }
}
