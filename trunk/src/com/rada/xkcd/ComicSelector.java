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
import java.net.MalformedURLException;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
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

public class ComicSelector extends ListActivity {

  private ComicDbAdapter mDbHelper;
  private MenuInflater inflater;
  
  ProgressDialog updateDialog;

  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.comic_list);
    inflater= new MenuInflater(this);
    mDbHelper= new ComicDbAdapter(this);
    mDbHelper.open();
    populateList();
    registerForContextMenu(getListView());
  }
  
  @Override
  public void onStart() {
    super.onStart();
    ProgressDialog dialog= ProgressDialog.show(this, null,
                                               getResources().getString(R.string.list_updating),
                                               true, true);
//    new Thread(new Threader(this)).start();
    try {
      mDbHelper.updateList();
    } catch (MalformedURLException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    populateList();
  }
  
  private class Threader implements Runnable {
    ComicDbAdapter mDbAdapter;
    
    Threader(Context ctx) {
      mDbAdapter= new ComicDbAdapter(ctx);
      mDbAdapter.open();
    }

    public void run() {
      // TODO Auto-generated method stub
      try {
        mDbAdapter.updateList();
      } catch (MalformedURLException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
  }

  private void populateList() {
    Cursor comicCursor= mDbHelper.fetchAllComics();
    startManagingCursor(comicCursor);

    String[] from=
      new String[] { ComicDbAdapter.KEY_NUMBER, ComicDbAdapter.KEY_TITLE };

    int[] to= new int[] { R.id.rownumber, R.id.rowtitle };

    SimpleCursorAdapter comics= 
      new SimpleCursorAdapter(this, R.layout.comic_row, comicCursor, from, to);
    setListAdapter(comics);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    super.onCreateOptionsMenu(menu);
    inflater.inflate(R.menu.selector_menu, menu);
    return true;
  }
  
  @Override
  public boolean onMenuItemSelected(int featureId, MenuItem item) {
    Intent intent;
    switch (item.getItemId()) {
      case (R.id.menu_search):
        // TODO implement the search and such for this
        break;
      case (R.id.menu_downloadall):
        intent= new Intent(this, ComicDownloader.class);
        intent.putExtra(ComicDownloader.ACTION, ComicDownloader.ACTION_DOWNLOAD);
        startService(intent);
        return true;
      case (R.id.menu_clearall):
        intent= new Intent(this, ComicDownloader.class);
        intent.putExtra(ComicDownloader.ACTION, ComicDownloader.ACTION_DELETE);
        startService(intent);
        return true;
      case (R.id.menu_goto):
        intent= new Intent(this, ComicDownloader.class);
        intent.putExtra(Intent.ACTION_VIEW, "http://www.xkcd.com");
        startActivity(intent);
      case (R.id.menu_settings):
        // TODO implement a settings interface
        break;
    }
    
    return super.onMenuItemSelected(featureId, item);
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
  }

  @Override
  public boolean onContextItemSelected(MenuItem item) {
    Intent intent;
    AdapterContextMenuInfo info= (AdapterContextMenuInfo) item.getMenuInfo();
    switch (item.getItemId()) {
      case (R.id.menu_view):
        intent= new Intent(this, ComicViewer.class);
        intent.putExtra(ComicDbAdapter.KEY_NUMBER, info.id);
        startActivity(intent);
        return true;
      case (R.id.menu_download):
        intent= new Intent(this, ComicDownloader.class);
        intent.putExtra(ComicDbAdapter.KEY_NUMBER, info.id);
        intent.putExtra(ComicDownloader.ACTION, ComicDownloader.ACTION_DOWNLOAD);
        startService(intent);
        return true;
      case (R.id.menu_clear):
        intent= new Intent(this, ComicDownloader.class);
        intent.putExtra(ComicDbAdapter.KEY_NUMBER, info.id);
        intent.putExtra(ComicDownloader.ACTION, ComicDownloader.ACTION_DELETE);
        startService(intent);
        return true;
      case (R.id.menu_favorite):
        mDbHelper.updateComic(info.id, true);
        return true;
      case (R.id.menu_unfavorite):
        mDbHelper.updateComic(info.id, false);
        return true;
    }
    return super.onContextItemSelected(item);
  }

  @Override
  protected void onListItemClick(ListView l, View v, int position, long id) {
    super.onListItemClick(l, v, position, id);
    Intent intent= new Intent(this, ComicViewer.class);
    intent.putExtra(ComicDbAdapter.KEY_NUMBER, id);
    startActivity(intent);
  }
}