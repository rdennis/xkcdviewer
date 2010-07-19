package com.rada.xkcd;

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
    return START_NOT_STICKY;
  }

  @Override
  public IBinder onBind(Intent arg0) {
    // TODO Auto-generated method stub
    return null;
  }

}
