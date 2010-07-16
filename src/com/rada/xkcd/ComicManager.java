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

import android.app.Dialog;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class ComicManager extends Service {
  
  Dialog dialog;
  
  public static final int MESSAGE_UPDATE= 200;
  
  private ComicDbAdapter mDbAdapter;
  
  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
//    String action= intent.getAction();
//    if (action == "update") {
//      ProgressDialog dialog= ProgressDialog.show(this, null, "Updating list...", true, true);

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
}
