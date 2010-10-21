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
package com.radadev.xkcd.compat;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;

import com.radadev.xkcd.compat.database.ComicDbAdapter;

public class ComicService extends Service {

  @SuppressWarnings("unused")
  private static final String TAG= "ComicService";

  private ComicDbAdapter mDbAdapter;
  private Messenger mMessenger= new Messenger(new IncomingHandler());

  @Override
  public IBinder onBind(Intent intent) {
    return mMessenger.getBinder();
  }

  @Override
  public void onCreate() {
    super.onCreate();    
    mDbAdapter= new ComicDbAdapter(getApplicationContext());
    mDbAdapter.open();
  }

  private class IncomingHandler extends Handler {
    
    public void handleMessage(Message message) {
      // TODO implement the messaging interface
      switch (message.what) {
      case Comics.MSG_DOWNLOAD_ALL:
        break;
      default:
      }
    }
  };
}
