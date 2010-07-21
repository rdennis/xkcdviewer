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
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Calendar;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

public class ComicList extends ListActivity {
  
  final ListActivity thisContext= this;
  
  public static final int UPDATE_DIALOGID= 500;

  private ComicDbAdapter dbAdapter;
  private static Calendar lastUpdate;
  private ExecutorService updateExecutor;
  private MenuInflater inflater= new MenuInflater(this);
  
  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.comic_list);
    dbAdapter= new ComicDbAdapter(this);
    dbAdapter.open();
    
    updateExecutor= Executors.newSingleThreadExecutor();

    Calendar now= Calendar.getInstance();

    int today= now.get(Calendar.DAY_OF_YEAR);
    int weekday= now.get(Calendar.DAY_OF_WEEK);
    int lastDay= (lastUpdate != null)? lastUpdate.get(Calendar.DAY_OF_YEAR) : -1;

    // This is a mess, I'm aware of that, but it makes it only update if it's necessary
    if (// if it's update day and last update wasn't today
        ((weekday == Calendar.MONDAY || weekday == Calendar.WEDNESDAY || weekday == Calendar.FRIDAY) &&
         (today != lastDay)) ||
         
        // if it's not update day but it wasn't updated yesterday
        ((weekday == Calendar.TUESDAY || weekday == Calendar.THURSDAY || weekday == Calendar.SATURDAY) &&
         (Math.abs(today - lastDay) > 1)) ||
         
        // if it's Sunday and it wasn't updated Friday or Saturday
        ((weekday == Calendar.SUNDAY) && (Math.abs(today - lastDay) > 2))
       ) {
      showDialog(UPDATE_DIALOGID);
      updateExecutor.execute(new Updater());
    }
    
    registerForContextMenu(getListView());
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
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
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
      dbAdapter= null;
    }
  }
  
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    super.onCreateOptionsMenu(menu);
    inflater.inflate(R.menu.selector_menu, menu);
    return true;
  }
  
  @Override
  public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
    super.onCreateContextMenu(menu, v, menuInfo);
    
    AdapterContextMenuInfo info= (AdapterContextMenuInfo) menuInfo;
    inflater.inflate(R.menu.selector_context, menu);

    File file= new File("/sdcard/xkcd/" + info.id);
    if (file.exists()) {
      menu.findItem(R.id.menu_download).setVisible(false);
    } else {
      menu.findItem(R.id.menu_clear).setVisible(false);
    }
    if (dbAdapter.isFavorite(info.id)) {
      menu.findItem(R.id.menu_favorite).setVisible(false);
    } else {
      menu.findItem(R.id.menu_unfavorite).setVisible(false);
    }
  }
  
  @Override
  public boolean onMenuItemSelected(int featureId, MenuItem item) {
    switch(item.getItemId()) {
      case R.id.menu_downloadall: {
        Intent intent= new Intent(this, ComicDownloader.class);
        intent.setAction(Comics.ACTION_DOWNLOAD);
        startService(intent);
        return true;
      }
      case R.id.menu_clearall: {
        Intent intent= new Intent(this, ComicDownloader.class);
        intent.setAction(Comics.ACTION_CLEAR);
        startService(intent);
        return true;
      }
      case R.id.menu_goto: {
        Intent intent= new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.addCategory(Intent.CATEGORY_BROWSABLE);
        intent.setData(Uri.parse(Comics.MAIN_URL));
        startActivity(intent);
      }
      case R.id.menu_search: {
      }
      case R.id.menu_settings: {
      }
      default: {
        return super.onMenuItemSelected(featureId, item);
      }
    }
  }
  
  @Override
  public boolean onContextItemSelected(MenuItem item) {
    AdapterContextMenuInfo info= (AdapterContextMenuInfo) item.getMenuInfo();
    switch (item.getItemId()) {
      case R.id.menu_view: {
        Intent intent= new Intent(this, ComicView.class);
        intent.setAction(Comics.ACTION_VIEW);
        intent.putExtra(Comics.KEY_NUMBER, info.id);
        startActivity(intent);
        return true;
      }
      case R.id.menu_download: {
        Intent intent= new Intent(this, ComicDownloader.class);
        intent.setAction(Comics.ACTION_DOWNLOAD);
        intent.putExtra(Comics.KEY_NUMBER, info.id);
        startService(intent);
        return true;
      }
      case R.id.menu_clear: {
        Intent intent= new Intent(this, ComicDownloader.class);
        intent.setAction(Comics.ACTION_CLEAR);
        intent.putExtra(Comics.KEY_NUMBER, info.id);
        startService(intent);
        return true;
      }
      case R.id.menu_favorite: {
        dbAdapter.updateComic(info.id, true);
        return true;
      }
      case R.id.menu_unfavorite: {
        dbAdapter.updateComic(info.id, false);
        return true;
      }
    }
    return super.onContextItemSelected(item);
  }

  @Override
  protected void onListItemClick(ListView l, View v, int position, long id) {
    Intent intent= new Intent(this, ComicView.class);
    intent.setAction(Comics.ACTION_VIEW);
    intent.putExtra(Comics.KEY_NUMBER, id);
    startActivity(intent);
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
            // TODO find a way to shutdown the thread on cancel
            // updateExecutor.shutdownNow();
            // runOnUiThread(new UpdateFinisher(Comics.STATUS_CANCELLED));
          }
        });
        return dialog;
      }
      default:
        return null;
    }
  }
  
  private synchronized void populateList() {
    Cursor cursor= dbAdapter.fetchAllComics();
    startManagingCursor(cursor);
    
    String[] from= new String[] { Comics.KEY_NUMBER, Comics.KEY_TITLE };
    int [] to= new int[] { R.id.row_number, R.id.row_title };
    
    SimpleCursorAdapter comics=
      new SimpleCursorAdapter(this, R.layout.comic_row, cursor, from, to);
    setListAdapter(comics);
  }
  
  private class Updater implements Runnable {
    @Override
    public void run() {
      int result;
      try {
        dbAdapter.updateList();
        result= Comics.STATUS_SUCCESS;
      } catch (MalformedURLException e) {
        result= Comics.STATUS_ERROR;
      } catch (IOException e) {
        result= Comics.STATUS_FAILURE;
      }
      
      thisContext.runOnUiThread(new UpdateFinisher(result));
    }
  }
  
  private class UpdateFinisher implements Runnable {
    
    private int status;
    
    UpdateFinisher(int status) {
      setStatus(status);
    }
    
    public void setStatus(int status) {
      this.status= status;
    }
    
    @Override
    public void run() {
      finishListUpdate(status);
    }
  }
  
  public final void finishListUpdate(int status) {
    removeDialog(UPDATE_DIALOGID);
    Toast message;
    switch (status) {
      case Comics.STATUS_SUCCESS: {
        message= Toast.makeText(this, R.string.update_success, Toast.LENGTH_SHORT);
        populateList();
        lastUpdate= Calendar.getInstance();
      } break;
      case Comics.STATUS_FAILURE: {
        message= Toast.makeText(this, R.string.update_failure, Toast.LENGTH_SHORT);
      } break;
      case Comics.STATUS_CANCELLED: {
        message= Toast.makeText(this, R.string.update_cancelled, Toast.LENGTH_SHORT);
      } break;
      case Comics.STATUS_ERROR: {
        message= Toast.makeText(this, R.string.update_error, Toast.LENGTH_LONG);
      } break;
      default: {
        message= Toast.makeText(this, "Unkown update status: " + status, Toast.LENGTH_LONG);
      }
    }
    message.show();
  }
}