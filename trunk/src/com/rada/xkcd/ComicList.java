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
import java.net.MalformedURLException;

import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

public class ComicList extends ListActivity {
  
  final ListActivity thisContext= this;
  
  private static final int ID_LISTUPDATED= 1000;
  private static final int ID_UPDATELIST= 1001;
  
  private ComicDbAdapter mDbAdapter;
  
  public static final int UPDATE_DIALOGID= 0;
  
  boolean mIsBound= false;
  
  private class UpdateThread extends Thread {
    
    private boolean ready= false;
    public Handler handler;
    
    public void run() {
      Looper.prepare();
      handler= new IncomingHandler();
      ready= true;
      Looper.loop();
    }
    
    public boolean isReady() {
      return ready;
    }
    
    private class IncomingHandler extends Handler {
      @Override
      public void handleMessage(Message message) {
        switch (message.what) {
          case ID_UPDATELIST: {
            int result= 0;
            try {
              mDbAdapter.updateList();
            } catch (MalformedURLException e) {
              result= 1;
            } catch (IOException e) {
              result= 2;
            }
            Message answer= Message.obtain(null, message.arg1, result, 0);
            try {
              message.replyTo.send(answer);
            } catch (RemoteException e) {
              //it failed!!!
            }
          } break;
          default: {
            super.handleMessage(message);
          } break;
        }
      }
    }
  }
  
  private UpdateThread updateThread= new UpdateThread();
  
  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.comic_list);
    mDbAdapter= new ComicDbAdapter(this);
    mDbAdapter.open();
    showDialog(UPDATE_DIALOGID);
    if (!updateThread.isAlive())
      updateThread.start();
    
    Message message= Message.obtain(null, ID_UPDATELIST, ID_LISTUPDATED, 0);
    message.replyTo= messenger;
    try {
      while (!updateThread.isReady()) {/* wait */}
      new Messenger(updateThread.handler).send(message);
    } catch (RemoteException e) {
      // TODO Auto-generated catch block
      dismissDialog(UPDATE_DIALOGID);
    }
  }
  
  @Override
  protected void onSaveInstanceState(Bundle outState) {
      super.onSaveInstanceState(outState);
  }
  
  @Override
  public void onRestart() {
    super.onRestart();
  }
  
  @Override
  public void onStart() {
    super.onStart();
  }
  
  @Override
  public void onResume() {
    super.onResume();
    populateList();
  }
  
  @Override
  public void onPause() {
    super.onPause();
    updateThread.interrupt();
  }
  
  @Override
  public void onStop() {
    super.onStop();
  }
  
  @Override
  public void onDestroy() {
    super.onDestroy();
  }
  
  private synchronized void populateList() {
    Cursor cursor= mDbAdapter.fetchAllComics();
    startManagingCursor(cursor);
    
    String[] from= new String[] { ComicDbAdapter.KEY_NUMBER, ComicDbAdapter.KEY_TITLE };
    int [] to= new int[] { R.id.row_number, R.id.row_title };
    
    SimpleCursorAdapter comics=
      new SimpleCursorAdapter(this, R.layout.comic_row, cursor, from, to);
    setListAdapter(comics);
  }
  
  class IncomingHandler extends Handler {
    @Override
    public void handleMessage(Message message) {
      switch (message.what) {
        case ID_LISTUPDATED: {
          dismissDialog(UPDATE_DIALOGID);
          switch (message.arg1) {
            case 0: {
              populateList();
              Toast.makeText(thisContext, R.string.update_success, Toast.LENGTH_SHORT).show();
            } break;
            case 1: {
              Toast.makeText(thisContext, R.string.update_failure, Toast.LENGTH_SHORT).show();
            } break;
            case 2: {
              Toast.makeText(thisContext, "Fatal error!!!", Toast.LENGTH_LONG).show();
            } break;
            default: {
            } super.handleMessage(message);
          }
        }
      }
    }
  }
  
  final Messenger messenger= new Messenger(new IncomingHandler());
  
  @Override
  protected Dialog onCreateDialog(int id) {
    switch (id) {
      case UPDATE_DIALOGID: {
        ProgressDialog dialog= new ProgressDialog(this);
        dialog.setMessage(this.getText(R.string.updating_comics));
        dialog.setIndeterminate(true);
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);
        return dialog;
      }
      default:
        return null;
    }
  }
  
  public final void updateFinished(boolean success) {
    Toast toast= Toast.makeText(this, success? R.string.update_success : R.string.update_failure, Toast.LENGTH_SHORT);
    dismissDialog(UPDATE_DIALOGID);
    toast.show();
  }
}