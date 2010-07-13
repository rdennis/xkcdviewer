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

import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.SimpleCursorAdapter;

public class ComicList extends ListActivity {
  
  private ComicDbAdapter mDbAdapter;
  
  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.comic_list);
    mDbAdapter= new ComicDbAdapter(this);
    mDbAdapter.open();
    Intent intent= new Intent(this, ComicManager.class);
    intent.setAction("update");
    startService(intent);
  }
  
  @Override
  public void onResume() {
    super.onResume();
    populateList();
  }
  
  private void populateList() {
    Cursor cursor= mDbAdapter.fetchAllComics();
    startManagingCursor(cursor);
    
    String[] from= new String[] { ComicDbAdapter.KEY_NUMBER, ComicDbAdapter.KEY_TITLE };
    int [] to= new int[] { R.id.row_number, R.id.row_title };
    
    SimpleCursorAdapter comics=
      new SimpleCursorAdapter(this, R.layout.comic_row, cursor, from, to);
    setListAdapter(comics);
  }
}