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
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

public class ComicList extends ListActivity {
  
  final ListActivity thisContext= this;
  
  public static final int UPDATE_DIALOGID= 500;
  
  private static final int NORMAL_VIEW= 1000;
  private static final int SEARCH_VIEW= 1001;
  private static final int FAVORITE_VIEW= 1002;
  
  private int currentView;

  private ComicDbAdapter dbAdapter;
  private static Calendar lastUpdate;
  
  private Cursor listCursor;
  
  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    
    Intent intent= getIntent();
    if (intent == null) {
      currentView= NORMAL_VIEW;
      setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL);
    } else if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
      currentView= SEARCH_VIEW;
      setTitle("Search results");
    } else if (Comics.ACTION_VIEW_FAVORITES.equals(intent.getAction())) {
      currentView= FAVORITE_VIEW;
      setTitle("Favorites");
    } else {
      currentView= NORMAL_VIEW;
    }
    
    setContentView(R.layout.comic_list);
    dbAdapter= new ComicDbAdapter(this);
    dbAdapter.open();

    Calendar now= Calendar.getInstance();

    int today= now.get(Calendar.DAY_OF_YEAR);
    int weekday= now.get(Calendar.DAY_OF_WEEK);
    int lastUpdateDay= (lastUpdate != null)? lastUpdate.get(Calendar.DAY_OF_YEAR) : -1;

    // This is a mess, I'm aware of that, but it makes it only update if it's necessary
    if (// if it's update day and last update wasn't today
        ((weekday == Calendar.MONDAY || weekday == Calendar.WEDNESDAY || weekday == Calendar.FRIDAY) &&
         (today != lastUpdateDay)) ||
         
        // if it's not update day but it wasn't updated yesterday
        ((weekday == Calendar.TUESDAY || weekday == Calendar.THURSDAY || weekday == Calendar.SATURDAY) &&
         (Math.abs(today - lastUpdateDay) > 1)) ||
         
        // if it's Sunday and it wasn't updated Friday or Saturday
        ((weekday == Calendar.SUNDAY) && (Math.abs(today - lastUpdateDay) > 2))
       ) {
      showDialog(UPDATE_DIALOGID);
      Comics.BACKGROUND_EXECUTOR.execute(new Updater());
    }
    
    populateList();
    registerForContextMenu(getListView());
  }
  
  @Override
  public void onResume() {
    super.onResume();
    listCursor.requery();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    super.onCreateOptionsMenu(menu);
    getMenuInflater().inflate(R.menu.selector_menu, menu);
    
    if (currentView == FAVORITE_VIEW) {
      menu.findItem(R.id.menu_favorites).setVisible(false);
    }
    
    return true;
  }
  
  @Override
  public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
    super.onCreateContextMenu(menu, v, menuInfo);
    
    AdapterContextMenuInfo info= (AdapterContextMenuInfo) menuInfo;
    getMenuInflater().inflate(R.menu.selector_context, menu);

    File file= new File("/sdcard/xkcd/" + info.id);
    if (file.length() > 0) {
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
        if (!Comics.SD_DIR.exists()) {
          Comics.SD_DIR.mkdirs();
        }
        
        Cursor cursor= dbAdapter.fetchMostRecentComic();
        final int max= cursor.getInt(cursor.getColumnIndexOrThrow(Comics.KEY_NUMBER));
        final int count= (max - Comics.SD_DIR.list().length) - 1;
        cursor.close();
        
        final ProgressDialog progress= new ProgressDialog(thisContext);
        progress.setMessage("Downloading comics");
        progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progress.setMax(count);
        progress.setProgress(0);
        progress.setOnDismissListener(new DialogInterface.OnDismissListener() {
          public void onDismiss(DialogInterface dialog) {
            listCursor.requery();
          }
        });
        if (count > 0) {
          progress.show();
          List<String> list= Arrays.asList(Comics.SD_DIR.list());
          final Set<String> fileList= new TreeSet<String>(list);

          Comics.BACKGROUND_EXECUTOR.execute(new Runnable() {
            public void run() {
              Executor executor= Executors.newFixedThreadPool(25);
              for (Integer i= max; i > 0; --i) {
                if (i == 404)
                  continue;
                else if (!fileList.contains(i.toString())) {
                  final int number= i;
                  executor.execute(new Runnable() {
                    
                    public void run() {
                      try {
                        Comics.downloadComic(number, dbAdapter);
                        if (new File(Comics.SD_DIR_PATH + number).length() <= 0)
                          Comics.downloadComic(number, dbAdapter);
                      } catch (Exception e) {
                        runOnUiThread(new Runnable() {
                          public void run() {
                            Toast.makeText(thisContext, "Failed to download comic " + number, Toast.LENGTH_LONG).show();
                          }
                        });
                      }
                      runOnUiThread(new Runnable() {
                        public void run() {
                          progress.incrementProgressBy(1);
                          if (progress.getProgress() == progress.getMax()) {
                            progress.dismiss();
                            Toast.makeText(thisContext, "Finished downloading comics", Toast.LENGTH_SHORT).show();
                          }
                        }
                      });
                    }
                  });
                }
              }
            }
          });
        }
        
        return true;
      }
      case R.id.menu_clearall: {
        AlertDialog.Builder builder= new AlertDialog.Builder(thisContext);
        builder.setCancelable(true);
        builder.setMessage("Once started this action cannot be stopped or undone.");
        builder.setIcon(android.R.drawable.ic_dialog_alert);
        builder.setTitle("Confirm Delete");
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int which) {
            dialog.dismiss();
          }
        });
        builder.setPositiveButton("Continue", new DialogInterface.OnClickListener() {

          public void onClick(DialogInterface dialog, int which) {
            dialog.dismiss();
            final ProgressDialog progress= new ProgressDialog(thisContext);
            progress.setMessage("Deleting comics...");
            progress.setIndeterminate(true);
            progress.setCancelable(false);
            progress.show();
            Comics.BACKGROUND_EXECUTOR.execute(new Runnable() {

              public void run() {
                Executor executor= Executors.newFixedThreadPool(50);
                final File[] fileList= Comics.SD_DIR.listFiles();
                for (int i= 0; i < fileList.length; ++i) {
                  final int number= i;
                  if (fileList[number].exists()) {

                    executor.execute(new Runnable() {
                      public void run() {
                        fileList[number].delete();
                        if (number + 1 == fileList.length) {

                          runOnUiThread(new Runnable() {
                            public void run() {
                              progress.dismiss();
                              Toast.makeText(thisContext, "Cleared all comics", Toast.LENGTH_SHORT).show();
                              listCursor.requery();
                            }
                          });
                        }
                      }
                    });
                  }
                }
              }
            }); 
          }
        });
        builder.show();
        return true;
      }
      case R.id.menu_favorites: {
        Intent intent= new Intent(this, ComicList.class);
        intent.setAction(Comics.ACTION_VIEW_FAVORITES);
        startActivity(intent);
        return true;
      }
      case R.id.menu_random: {
        Cursor cursor= dbAdapter.fetchMostRecentComic();
        long maxNumber= cursor.getLong(cursor.getColumnIndexOrThrow(Comics.KEY_NUMBER));
        long number= Math.abs(Comics.RANDOM.nextLong() % maxNumber + 1);
        cursor.close();
        
        if (number == 404l)
          number= maxNumber;
        
        Intent intent= new Intent(this, ComicView.class);
        intent.setAction(Comics.ACTION_VIEW);
        intent.putExtra(Comics.KEY_NUMBER, number);
        startActivity(intent);
        return true;
      }
      case R.id.menu_goto: {
        Intent intent= new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.addCategory(Intent.CATEGORY_BROWSABLE);
        intent.setData(Uri.parse(Comics.MAIN_URL));
        startActivity(intent);
        return true;
      }
      case R.id.menu_search: {
        onSearchRequested();
      }
//      case R.id.menu_settings: {
//      }
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
        Toast.makeText(thisContext, "Downloading comic " + info.id, Toast.LENGTH_SHORT / 2).show();
        ImageView view= (ImageView) info.targetView.findViewById(R.id.row_arrow);
        view.setImageDrawable(getResources().getDrawable(R.drawable.active));
        view.setClickable(false);
        download(info.id);
        return true;
      }
      case R.id.menu_clear: {
        File file= new File(Comics.SD_DIR_PATH + info.id);
        if (file.exists()) {
          file.delete();
          Toast.makeText(thisContext, "Deleted comic number " + info.id, Toast.LENGTH_SHORT).show();
          listCursor.requery();
        }
        return true;
      }
      case R.id.menu_favorite: {
        long time= Calendar.getInstance().getTimeInMillis();
        MotionEvent downEvent= MotionEvent.obtain(time, time, MotionEvent.ACTION_DOWN, 0, 0, 0);
        MotionEvent upEvent= MotionEvent.obtain(time, time, MotionEvent.ACTION_UP, 0, 0, 0);
        View target= info.targetView.findViewById(R.id.star);
        target.dispatchTouchEvent(downEvent);
        target.dispatchTouchEvent(upEvent);
        return true;
      }
      case R.id.menu_unfavorite: {
        long time= Calendar.getInstance().getTimeInMillis();
        MotionEvent downEvent= MotionEvent.obtain(time, time, MotionEvent.ACTION_DOWN, 0, 0, 0);
        MotionEvent upEvent= MotionEvent.obtain(time, time, MotionEvent.ACTION_UP, 0, 0, 0);
        View target= info.targetView.findViewById(R.id.star);
        target.dispatchTouchEvent(downEvent);
        target.dispatchTouchEvent(upEvent);
        return true;
      }
      default: {
        return super.onContextItemSelected(item);
      }
    }
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
          public void onCancel(DialogInterface dialog) {
            // TODO find a way to shutdown the thread on cancel
            // runOnUiThread(new UpdateFinisher(Comics.STATUS_CANCELLED));
          }
        });
        return dialog;
      }
      default: {
        return null;
      }
    }
  }
  
  private synchronized void populateList() {
    switch (currentView) {
      case FAVORITE_VIEW: {
        listCursor= dbAdapter.fetchFavoriteComics();
      } break;
      case SEARCH_VIEW: {
        String query= getIntent().getStringExtra(SearchManager.QUERY);
        listCursor= dbAdapter.fetchSearchedComics(query);
      } break;
      case NORMAL_VIEW:
      default: {
        listCursor= dbAdapter.fetchAllComics();
      } break;
    }
    startManagingCursor(listCursor);
    
    String[] from= new String[] { Comics.KEY_FAVORITE, Comics.KEY_NUMBER, Comics.KEY_TITLE };
    int [] to= new int[] { R.id.row_star, R.id.row_number, R.id.row_title };
    
    SimpleCursorAdapter adapter=
      new SimpleCursorAdapter(this, R.layout.comic_row, listCursor, from, to);
    adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
      public boolean setViewValue(View v, Cursor cursor, int columnIndex) {
        if (columnIndex == cursor.getColumnIndexOrThrow(Comics.KEY_FAVORITE)) {
          final CheckBox checkBox= (CheckBox) v;
          final long id= cursor.getLong(cursor.getColumnIndexOrThrow(Comics.KEY_NUMBER));
          final boolean isFavorite= cursor.getLong(columnIndex) != 0;
          checkBox.setChecked(isFavorite);
          checkBox.setOnClickListener(new View.OnClickListener() {
            private boolean isFavorited= isFavorite;
            private long number= id;
            public void onClick(View v) {
              isFavorited= !isFavorited;
              dbAdapter.updateComic(number, isFavorited);
              listCursor.requery();
            }
          });
          ImageView view= (ImageView) ((ViewGroup) v.getParent()).findViewById(R.id.row_arrow);
          if (new File(Comics.SD_DIR_PATH + id).length() > 0) {
            view.setImageDrawable(getResources().getDrawable(R.drawable.arrow));
            view.setClickable(false);
          } else {
            view.setImageDrawable(getResources().getDrawable(R.drawable.down));
            view.setClickable(true);
            view.setOnClickListener(new View.OnClickListener() {
              long comicNumber= id;
              public void onClick(View v) {
                Toast.makeText(thisContext, "Downloading comic " + comicNumber, Toast.LENGTH_SHORT / 2).show();
                ImageView view= (ImageView) v;
                view.setImageDrawable(getResources().getDrawable(R.drawable.active));
                view.setClickable(false);
                download(comicNumber);
              }
            });
          }
          return true;
        }
        return false;
      }
    });
    setListAdapter(adapter);
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
    listCursor.requery();
    message.show();
  }
  
  public void download(long number) {
    final long comicNumber= number;
    Comics.BACKGROUND_EXECUTOR.execute(new Runnable() {
      public void run() {
        String statusText;
        try {
          Comics.downloadComic(comicNumber, dbAdapter);
          statusText= "Downloaded comic " + comicNumber;
        } catch (Exception e) {
          statusText= "Failed to download comic " + comicNumber;
        }
        final String finalText= statusText;
        runOnUiThread(new Runnable() {
          public void run() {
            Toast.makeText(thisContext, finalText, Toast.LENGTH_SHORT).show();
            listCursor.requery();
          }
        });
      }
    });
  }
}