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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

import com.radadev.xkcd.compat.ComicAsync.AsyncDownload;
import com.radadev.xkcd.compat.database.ComicDbAdapter;

public class ComicView extends Activity {
  
  private static final int HOVERTEXT_DIALOGID= 800;

  private volatile ComicDbAdapter mDbAdapter;
  private volatile Integer mComicNumber= null;
  private Integer mMaxNumber;

  private CheckBox mCheckBox;
  private EditText mComicText;
  private Button mGoButton;
  private Button mNextButton;
  private Button mPrevButton;
  private ImageView mComicImage;
  
  @SuppressWarnings("unused")
  private static final String TAG= "ComicView";
  
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.comic_view);
    
    mDbAdapter= new ComicDbAdapter(this);
    mDbAdapter.open();

    Cursor cursor= mDbAdapter.fetchMostRecentComic();
    mMaxNumber= cursor.getInt(cursor.getColumnIndexOrThrow(Comics.SQL_KEY_NUMBER));
    cursor.close();
    
    mComicNumber= (savedInstanceState == null) ? null :
      (Integer) savedInstanceState.getSerializable(Comics.KEY_NUMBER);
    if (mComicNumber == null) {
      Bundle extras= getIntent().getExtras();
      if (extras != null) {
        mComicNumber= (Integer) extras.getSerializable(Comics.KEY_NUMBER);
      }
    }
    if (mComicNumber == null) {
      mComicNumber= mMaxNumber;
    }
    
    mCheckBox= (CheckBox) findViewById(R.id.star);
    mComicText= (EditText) findViewById(R.id.edit_number);
    mGoButton= (Button) findViewById(R.id.button_go);
    mNextButton= (Button) findViewById(R.id.button_next);
    mPrevButton= (Button) findViewById(R.id.button_prev);
    mComicImage= (ImageView) findViewById(R.id.image_comic);
    
    mComicImage.setFocusable(true);
    
    mCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        mDbAdapter.updateComic(mComicNumber, isChecked);
      }
    });
    
    mComicText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
      public void onFocusChange(View v, boolean hasFocus) {
        EditText view= (EditText) v;
        
        if (!hasFocus) {
          InputMethodManager imm= (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
          imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        } else {
          view.selectAll();
        }
      }
    });
    mComicText.setOnKeyListener(new View.OnKeyListener() {
      public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_ENTER) {
          mGoButton.performClick();
          return true;
        }
        return false;
      }      
    });
    
    mGoButton.setOnClickListener(new View.OnClickListener() {
      
      public void onClick(View view) {
        String text= mComicText.getText().toString();
        if (text.length() > 0) {
          int newNumber= Integer.parseInt(text);
          if (newNumber != 404l)
            mComicNumber= newNumber;
        }
        updateDisplay();
      }
    });
    
    mNextButton.setOnClickListener(new View.OnClickListener() {
      public void onClick(View view) {
        if (mComicNumber.equals(mMaxNumber))
          mComicNumber= 1;
        else
          if (++mComicNumber == 404)
            ++mComicNumber;
        updateDisplay();
      }
    });

    mPrevButton.setOnClickListener(new View.OnClickListener() {
      public void onClick(View view) {
        if (mComicNumber.equals(1))
          mComicNumber= mMaxNumber;
        else
          if (--mComicNumber == 404)
            --mComicNumber;
        updateDisplay();
      }
    });
  }
  
  @Override
  public void onResume() {
    super.onResume();
    updateDisplay();
  }
  
  @Override
  public void onPause() {
    super.onPause();
  }
  
  @Override
  public void onSaveInstanceState(Bundle outState) {
    outState.putInt(Comics.KEY_NUMBER, mComicNumber);
  }
  
  @Override
  public void onDestroy() {
    super.onDestroy();
    mDbAdapter.close();
  }
  
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.viewer_menu, menu);
    return true;
  }
  
  @Override
  public boolean onMenuItemSelected(int featureId, MenuItem item) {
    switch(item.getItemId()) {
      case R.id.menu_hover: {
        showDialog(HOVERTEXT_DIALOGID);
        return true;
      }
      case R.id.menu_random: {
        Integer newNumber= Comics.RANDOM.nextInt() % mMaxNumber + 1;
        newNumber= Math.abs(newNumber);
        if (newNumber != 404)
          mComicNumber= newNumber;
        updateDisplay();
        return true;
      }
      case R.id.menu_online: {
        Intent intent= new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.addCategory(Intent.CATEGORY_BROWSABLE);
        intent.setData(Uri.parse(Comics.URL_MAIN + mComicNumber));
        startActivity(intent);
      }
      default: {
        return super.onMenuItemSelected(featureId, item);
      }
    }
  }
  
  @Override
  public Dialog onCreateDialog(int id) {
    switch (id) {
      case HOVERTEXT_DIALOGID: {
        Cursor cursor= mDbAdapter.fetchComic(mComicNumber);
        String text= cursor.getString(cursor.getColumnIndexOrThrow(Comics.SQL_KEY_TEXT));
        cursor.close();
        AlertDialog.Builder builder= new AlertDialog.Builder(ComicView.this);
        builder.setCancelable(true);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int which) {
            removeDialog(HOVERTEXT_DIALOGID);
          }
        });
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
          public void onCancel(DialogInterface dialog) {
            removeDialog(HOVERTEXT_DIALOGID);
          }
        });
        builder.setMessage(text);
        return builder.create();
      }
      default:
        return null;
    }
  }
  
  private void updateDisplay() {
    if (mComicNumber > mMaxNumber)
      mComicNumber= mMaxNumber;
    if (mComicNumber < 1)
      mComicNumber= 1;
    
    mComicText.setText(mComicNumber.toString());
    mComicText.clearFocus();
    
    Cursor cursor= mDbAdapter.fetchComic(mComicNumber);
    String newTitle= mComicNumber + ". " + cursor.getString(cursor.getColumnIndexOrThrow(Comics.SQL_KEY_TITLE));
    cursor.close();
    setTitle(newTitle);

    File file= new File(Comics.getSdDir(this), mComicNumber.toString());
    if (file.length() == 0) {
      AsyncDownload downloader= new AsyncDownload(this);
      downloader.setShowProgress(true);
      downloader.setCancelledCallBack(new Runnable() {
        public void run() {
          finish();
        }
      });
      downloader.setPostCallBack(new Runnable() {
        public void run() {
          updateDisplay();
        }
      });
      downloader.execute(mComicNumber);
    } else {
      try {
        FileInputStream istream= new FileInputStream(file);
        BufferedInputStream bi= new BufferedInputStream(istream, Comics.BUFFER_SIZE);
        BitmapDrawable drawable= new BitmapDrawable(BitmapFactory.decodeStream(bi));
        bi.close();
        istream.close();
        BitmapDrawable d= (BitmapDrawable) mComicImage.getDrawable();
        if (d != null) {
          d.getBitmap().recycle();
        }
        mComicImage.setImageDrawable(drawable);
        mComicImage.setScaleType(ScaleType.FIT_START);
        mComicImage.setOnTouchListener(new ImageViewTouchListener());
        mComicImage.setOnClickListener(new View.OnClickListener() {
          public void onClick(View v) {
            showDialog(HOVERTEXT_DIALOGID);
          }
        });
      } catch (FileNotFoundException e) {
        // the file got deleted between exist check and file open, try it again
        updateDisplay();
      } catch (IOException e) {
        updateDisplay();
      }
    }
    mCheckBox.setChecked(mDbAdapter.isFavorite(mComicNumber));
  }
}
