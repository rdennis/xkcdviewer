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

import java.io.File;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class ComicDownloader extends Service {
  
  private ComicDbAdapter dbAdapter;
  
  @Override
  public void onCreate() {
    dbAdapter= new ComicDbAdapter(this);
    dbAdapter.open();
    
    if (!Comics.SD_DIR.exists())
      if (!Comics.SD_DIR.mkdirs())
        throw new Error("Couldn't make directory" + Comics.SD_DIR_PATH);
  }
  
  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    // TODO fill this thing out
    onStart(intent, startId);
    return START_NOT_STICKY;
  }

  @Override
  public void onStart(Intent intent, int startId) {
    super.onStart(intent, startId);
    String action= intent.getAction();
    Long number= (Long) intent.getSerializableExtra(Comics.KEY_NUMBER);
    
    if (action.equals(Comics.ACTION_DOWNLOAD)) {
      if (number == null) {
        
      } else {
        
      }
    } else if (action.equals(Comics.ACTION_CLEAR)) {
      if (number == null) {
        File[] fileList= Comics.SD_DIR.listFiles();
        for (int i= 0; i < fileList.length; ++i) {
          fileList[i].delete();
        }
      } else {
        File file= new File(Comics.SD_DIR_PATH + number);
        file.delete();
      }
    }
    stopSelf();
  }

  @Override
  public IBinder onBind(Intent arg0) {
    // TODO Auto-generated method stub
    return null;
  }

}
