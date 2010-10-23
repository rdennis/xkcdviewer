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
package com.radadev.xkcd;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.app.ListActivity;
import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

import com.radadev.xkcd.ComicAsync.AsyncClear;
import com.radadev.xkcd.ComicAsync.AsyncDownload;
import com.radadev.xkcd.ComicAsync.AsyncDownloadAll;
import com.radadev.xkcd.ComicAsync.AsyncUpdate;
import com.radadev.xkcd.database.ComicDbAdapter;

public class ComicList extends ListActivity {

  @SuppressWarnings("unused")
  private static final String TAG= "ComicList";
  
  private final Messenger mMessenger = new Messenger(new IncomingHandler());

  private static final int NORMAL_VIEW= 10000;
  private static final int SEARCH_VIEW= 10001;
  private static final int FAVORITE_VIEW= 10002;
  
  private int mCurrentView;

  private ComicDbAdapter mDbAdapter;
  
  private Set<Integer> mDownloadingList= new HashSet<Integer>();

  private boolean mIsBound= false;
  private Messenger mService= null;

  private ServiceConnection mConnection = new ServiceConnection() {
    public void onServiceConnected(ComponentName className, IBinder service) {
      mService = new Messenger(service);
    }

    public void onServiceDisconnected(ComponentName className) {
      mService = null;
      doBindService();
    }
  };
  
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.comic_list);
    
    Intent intent= getIntent();
    if (intent == null) {
      mCurrentView= NORMAL_VIEW;
      setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL);
    } else if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
      mCurrentView= SEARCH_VIEW;
      setTitle("Search results");
    } else if (Comics.ACTION_VIEW_FAVORITES.equals(intent.getAction())) {
      mCurrentView= FAVORITE_VIEW;
      setTitle("Favorites");
    } else {
      mCurrentView= NORMAL_VIEW;
    }
    
    mDbAdapter= new ComicDbAdapter(getApplicationContext());
    mDbAdapter.open();
    
    startService(new Intent(this, ComicService.class));
    
    if (mCurrentView == NORMAL_VIEW) {
      AsyncUpdate updater= new AsyncUpdate(this);
      updater.setShowProgress(true);
      updater.setFinishedCallBack(new Runnable() {
        public void run() {
          requeryCursor();
        }
      });
      updater.execute();
    }

    populateList();
    registerForContextMenu(getListView());
  }
  
  @Override
  protected void onResume() {
    super.onResume();
    doBindService();
  }
  
  @Override
  protected void onPause() {
    super.onPause();
    doUnbindService();
  }
  
  @Override
  protected void onDestroy() {
    super.onDestroy();
    mDbAdapter.close();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    super.onCreateOptionsMenu(menu);
    getMenuInflater().inflate(R.menu.selector_menu, menu);

    if (mCurrentView == FAVORITE_VIEW) {
      menu.findItem(R.id.menu_favorites).setVisible(false);
    }
    
    return true;
  }
  
  @Override
  public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
    super.onCreateContextMenu(menu, v, menuInfo);
    
    AdapterContextMenuInfo info= (AdapterContextMenuInfo) menuInfo;
    getMenuInflater().inflate(R.menu.selector_context, menu);

    File file= new File(Comics.getSdDir(this), info.id + ".png");
    if (file.exists()) {
      menu.findItem(R.id.menu_download).setVisible(false);
    } else {
      menu.findItem(R.id.menu_clear).setVisible(false);
    }
    if (mDbAdapter.isFavorite((int) info.id)) {
      menu.findItem(R.id.menu_favorite).setVisible(false);
    } else {
      menu.findItem(R.id.menu_unfavorite).setVisible(false);
    }
  }

  @Override
  public boolean onContextItemSelected(MenuItem item) {
    AdapterContextMenuInfo info= (AdapterContextMenuInfo) item.getMenuInfo();
    final int comicNumber= (int) info.id;
    switch (item.getItemId()) {
      case R.id.menu_view: {
        Intent intent= new Intent(this, ComicView.class);
        intent.putExtra(Comics.KEY_NUMBER, comicNumber);
        startActivity(intent);
        return true;
      }
      case R.id.menu_download: {
        Toast.makeText(ComicList.this, "Downloading comic " + comicNumber, Toast.LENGTH_SHORT / 2).show();
        ImageView view= (ImageView) info.targetView.findViewById(R.id.row_arrow);
        view.setImageDrawable(getResources().getDrawable(R.drawable.active));
        view.setClickable(false);
        mDownloadingList.add(comicNumber);
        AsyncDownload downloader= new AsyncDownload(this);
        downloader.setShowProgress(false);
        downloader.setPostCallBack(new Runnable() {
          public void run() {
            Toast.makeText(ComicList.this, "Downloaded comic " + comicNumber, Toast.LENGTH_SHORT);
            requeryCursor();
          }
        });
        downloader.execute((int) comicNumber);
        return true;
      }
      case R.id.menu_clear: {
        File file= new File(Comics.getSdDir(this), comicNumber + ".png");
        if (file.exists()) {
          file.delete();
          Toast.makeText(ComicList.this, "Deleted comic number " + comicNumber, Toast.LENGTH_SHORT).show();
          requeryCursor();
        }
        return true;
      }
      case R.id.menu_favorite:
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
  public boolean onMenuItemSelected(int featureId, MenuItem item) {
    switch(item.getItemId()) {
      case R.id.menu_downloadall: {
        Cursor cursor= mDbAdapter.fetchMostRecentComic();
        int max= cursor.getInt(cursor.getColumnIndexOrThrow(Comics.SQL_KEY_NUMBER));
        cursor.close();

        Set<String> fileList= new HashSet<String>(Arrays.asList(Comics.getSdDir(this).list()));
        List<Integer> downloadList= new ArrayList<Integer>();
        for (Integer i= max; i > 0; --i) {
          if (i != 404 && !fileList.contains(i + ".png")) {
            downloadList.add(i);
          }
        }
        AsyncDownloadAll downloader= new AsyncDownloadAll(this);
        downloader.setShowProgress(true);
        downloader.setFinishedCallBack(new Runnable() {
          public void run() {
            requeryCursor();
          }
        });
        downloader.execute(downloadList.toArray(new Integer[0]));
        return true;
      }
      case R.id.menu_clearall: {
        Set<String> fileList= new HashSet<String>(Arrays.asList(Comics.getSdDir(this).list()));
        List<String> deleteList= new ArrayList<String>();
        for (String file : fileList) {
          deleteList.add(file);
        }
        AsyncClear deleter= new AsyncClear(this);
        deleter.setShowProgress(true);
        deleter.setFinishedCallBack(new Runnable() {
          public void run() {
            requeryCursor();
          }
        });
        deleter.execute(deleteList.toArray(new String[0]));
        return true;
      }
      case R.id.menu_favorites: {
        Intent intent= new Intent(this, ComicList.class);
        intent.setAction(Comics.ACTION_VIEW_FAVORITES);
        startActivity(intent);
        return true;
      }
      case R.id.menu_random: {
        Cursor cursor= mDbAdapter.fetchMostRecentComic();
        int maxNumber= cursor.getInt(cursor.getColumnIndexOrThrow(Comics.SQL_KEY_NUMBER));
        int number= Math.abs(Comics.RANDOM.nextInt() % maxNumber + 1);
        cursor.close();
        
        if (number == 404)
          number= maxNumber;
        
        Intent intent= new Intent(this, ComicView.class);
        intent.putExtra(Comics.KEY_NUMBER, number);
        startActivity(intent);
        return true;
      }
      case R.id.menu_goto: {
        Intent intent= new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.addCategory(Intent.CATEGORY_BROWSABLE);
        intent.setData(Uri.parse(Comics.URL_MAIN));
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
  protected void onListItemClick(ListView l, View v, int position, long id) {
    Intent intent= new Intent(this, ComicView.class);
    intent.putExtra(Comics.KEY_NUMBER, (int) id);
    startActivity(intent);
  }

  @Override
  public SimpleCursorAdapter getListAdapter() {
    return (SimpleCursorAdapter) super.getListAdapter();
  }
  
  protected Cursor getListCursor() {
    return getListAdapter().getCursor();
  }
  
  protected void doBindService() {
    // Establish a connection with the service.  We use an explicit
    // class name because there is no reason to be able to let other
    // applications replace our component.
    bindService(new Intent(this, ComicService.class), mConnection, Context.BIND_AUTO_CREATE);
    mIsBound= true;
  }

  protected void doUnbindService() {
    if (mIsBound) {
      if (mService != null) {
        try {
          Message msg= Message.obtain(null, Comics.MSG_UNREGISTER_CLIENT);
          msg.replyTo= mMessenger;
          mService.send(msg);
        } catch (RemoteException e) {
          // There is nothing special we need to do if the service
          // has crashed.
        }
      }

      unbindService(mConnection);
      mIsBound= false;
    }
  }

  protected void populateList() {
    Cursor listCursor;
    switch (mCurrentView) {
    case FAVORITE_VIEW: {
      listCursor= mDbAdapter.fetchFavoriteComics();
    } break;
    case SEARCH_VIEW: {
      String query= getIntent().getStringExtra(SearchManager.QUERY);
      listCursor= mDbAdapter.fetchSearchedComics(query);
    } break;
    case NORMAL_VIEW:
    default: {
      listCursor= mDbAdapter.fetchAllComics();
    } break;
    }
    
    startManagingCursor(listCursor);

    String[] from= new String[] { Comics.SQL_KEY_FAVORITE, Comics.SQL_KEY_NUMBER, Comics.SQL_KEY_TITLE };
    int [] to= new int[] { R.id.row_star, R.id.row_number, R.id.row_title };

    SimpleCursorAdapter adapter= new SimpleCursorAdapter(this, R.layout.comic_row, listCursor, from, to);
    adapter.setViewBinder(new CustomViewBinder());
    setListAdapter(adapter);
  }
  
  private boolean requeryCursor() {
    return getListCursor().requery();
  }
  
  private class CustomViewBinder implements SimpleCursorAdapter.ViewBinder {
    
    public boolean setViewValue(View v, Cursor cursor, int columnIndex) {
      
      if (columnIndex == cursor.getColumnIndexOrThrow(Comics.SQL_KEY_FAVORITE)) {
        final CheckBox checkBox= (CheckBox) v;
        final int id= cursor.getInt(cursor.getColumnIndexOrThrow(Comics.SQL_KEY_NUMBER));
        final boolean isFavorite= cursor.getLong(columnIndex) != 0;
        checkBox.setChecked(isFavorite);
        checkBox.setOnClickListener(new View.OnClickListener() {
          private boolean isFavorited= isFavorite;
          private int number= id;
          public void onClick(View v) {
            isFavorited= !isFavorited;
            mDbAdapter.updateComic(number, isFavorited);
            requeryCursor();
          }
        });
        
        ImageView view= (ImageView) ((ViewGroup) v.getParent()).findViewById(R.id.row_arrow);
        if (new File(Comics.getSdDir(ComicList.this), id + ".png").exists()) {
          view.setImageDrawable(getResources().getDrawable(R.drawable.arrow));
          view.setClickable(false);
        } else if (mDownloadingList.contains(id)) {
          view.setImageDrawable(getResources().getDrawable(R.drawable.active));
          view.setClickable(false);
        } else {
          view.setImageDrawable(getResources().getDrawable(R.drawable.down));
          view.setClickable(true);
          view.setOnClickListener(new View.OnClickListener() {
            int comicNumber= id;
            public void onClick(View v) {
              ImageView view= (ImageView) v;
              view.setImageDrawable(getResources().getDrawable(R.drawable.active));
              view.setClickable(false);
              mDownloadingList.add(comicNumber);
              AsyncDownload downloader= new AsyncDownload(ComicList.this);
              downloader.setShowProgress(false);
              downloader.setFinishedCallBack(new Runnable() {
                public void run() {
                  requeryCursor();
                  mDownloadingList.remove(comicNumber);
                  Toast.makeText(ComicList.this, "Downloaded comic " + comicNumber, Toast.LENGTH_SHORT).show();
                }
              });
              downloader.execute(comicNumber);
            }
          });
        }
        return true;
      } else {
        return false;
      }
    }
  }

  private class IncomingHandler extends Handler {
    
    public void handleMessage(Message message) {
      // TODO implement the messaging interface
      switch (message.what) {
      default:
      }
    }
  };
}
