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
import java.util.Calendar;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

public class ComicList extends ListActivity {
  
  final ListActivity thisContext= this;
  
  public static final int STATUS_SUCCESS= 0;
  public static final int STATUS_FAILURE= 1;
  public static final int STATUS_ERROR= 2;
  public static final int STATUS_CANCELLED= 3;
  public static final int UPDATE_DIALOGID= 500;

  private static ComicDbAdapter dbAdapter;
  private static ExecutorService updateExecutor;
  private static Calendar lastUpdate;
  
  private class Update implements Runnable {
    @Override
    public void run() {
      int result;
      try {
        dbAdapter.updateList();
        result= STATUS_SUCCESS;
      } catch (MalformedURLException e) {
        result= STATUS_ERROR;
      } catch (IOException e) {
        result= STATUS_FAILURE;
      }
      
      thisContext.runOnUiThread(new UpdateDone(result));
    }
  }
  
  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.comic_list);
    if (dbAdapter == null)
      dbAdapter= new ComicDbAdapter(this);
    dbAdapter.open();
    
    if (updateExecutor == null)
      updateExecutor= Executors.newSingleThreadExecutor();

    Calendar now= Calendar.getInstance();
    int lastYear, lastWeek, lastDay, nowYear, nowWeek, nowDay;
    
    if (lastUpdate != null) {
      lastYear= lastUpdate.get(Calendar.YEAR);
      lastWeek= lastUpdate.get(Calendar.WEEK_OF_YEAR);
      lastDay= lastUpdate.get(Calendar.DAY_OF_WEEK);
    } else {
      lastYear= 0;
      lastWeek= 0;
      lastDay= 0;
    }
    
    nowYear= now.get(Calendar.YEAR);
    nowWeek= now.get(Calendar.WEEK_OF_YEAR);
    nowDay= now.get(Calendar.DAY_OF_WEEK);

    // This is a mess, I'm aware of that, but it makes it only update if it's necessary
    if (// if it's a new year, update no matter what (this will affect updates in December and January mostly)
        (nowYear > lastYear) ||

        // if it's an update day and it hasn't been updated today
        ((nowDay == Calendar.MONDAY || nowDay == Calendar.WEDNESDAY || nowDay == Calendar.FRIDAY) &&
         ((nowWeek > lastWeek) || (nowDay - lastDay > 0))) ||

        // if it's not an update day but it's past due
        ((nowDay == Calendar.TUESDAY || nowDay == Calendar.THURSDAY || nowDay == Calendar.SATURDAY) &&
         ((nowWeek > lastWeek) || (nowDay - lastDay > 1))) ||

        // if it's Sunday and it wasn't updated since last Friday
        ((nowDay == Calendar.SUNDAY) &&
         ((nowWeek - lastWeek > 1) || ((nowWeek > lastWeek) && (lastDay < Calendar.FRIDAY))))
       ) {
      showDialog(UPDATE_DIALOGID);
      updateExecutor.execute(new Update());
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
    populateList();
  }
  
  @Override
  public void onResume() {
    super.onResume();
  }
  
  @Override
  public void onPause() {
    super.onPause();
  }
  
  @Override 
  public void onStop() {
    super.onStop();
  }
  
  @Override
  public void onDestroy() {
    super.onDestroy();
    if (isFinishing()) {
      updateExecutor= null;
      dbAdapter= null;
    }
  }
  
  private synchronized void populateList() {
    Cursor cursor= dbAdapter.fetchAllComics();
    startManagingCursor(cursor);
    
    String[] from= new String[] { ComicDbAdapter.KEY_NUMBER, ComicDbAdapter.KEY_TITLE };
    int [] to= new int[] { R.id.row_number, R.id.row_title };
    
    SimpleCursorAdapter comics=
      new SimpleCursorAdapter(this, R.layout.comic_row, cursor, from, to);
    setListAdapter(comics);
  }
  
  @Override
  protected Dialog onCreateDialog(int id) {
    switch (id) {
      case UPDATE_DIALOGID: {
        ProgressDialog dialog= new ProgressDialog(this);
        dialog.setMessage(getText(R.string.updating_comics));
        dialog.setIndeterminate(true);
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {

          @Override
          public void onCancel(DialogInterface dialog) {
            updateExecutor.shutdownNow();
            runOnUiThread(new UpdateDone(STATUS_CANCELLED));
          }
        });
        return dialog;
      }
      default:
        return null;
    }
  }
  
  private class UpdateDone implements Runnable {
    
    private int status;
    
    UpdateDone(int status) {
      setStatus(status);
    }
    
    public void setStatus(int status) {
      this.status= status;
    }
    
    @Override
    public void run() {
      updateFinished(status);
    }
  }
  
  public final void updateFinished(int status) {
    dismissDialog(UPDATE_DIALOGID);
    Toast message;
    switch (status) {
      case STATUS_SUCCESS: {
        message= Toast.makeText(this, R.string.update_success, Toast.LENGTH_SHORT);
        populateList();
        lastUpdate= Calendar.getInstance();
      } break;
      case STATUS_FAILURE: {
        message= Toast.makeText(this, R.string.update_failure, Toast.LENGTH_SHORT);
      } break;
      case STATUS_CANCELLED: {
        message= Toast.makeText(this, R.string.update_cancelled, Toast.LENGTH_SHORT);
      } break;
      case STATUS_ERROR: {
        message= Toast.makeText(this, R.string.update_error, Toast.LENGTH_LONG);
      } break;
      default: {
        message= Toast.makeText(this, "Unkown update status: " + status, Toast.LENGTH_LONG);
      }
    }
    message.show();
  }
}