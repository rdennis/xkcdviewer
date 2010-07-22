package com.rada.xkcd;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

public class ComicView extends Activity {
  
  private final Activity thisContext= this;
  
  private static final int HOVERTEXT_DIALOGID= 800;
  private static final int DOWNLOAD_DIALOGID= 801;

  private ComicDbAdapter dbHelper;
  private volatile Long comicNumber= null;
  private long maxNumber;
  
  private EditText comicText;
  private Button goButton;
  private Button nextButton;
  private Button prevButton;
  private ImageView comicImage;
  
  private ExecutorService executor;
  
//  private static final String TAG= "ComicView";
  
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.comic_view);
    
    dbHelper= new ComicDbAdapter(this);
    dbHelper.open();
    
    executor= Executors.newSingleThreadExecutor();

    Cursor cursor= dbHelper.fetchMostRecentComic();
    maxNumber= cursor.getLong(cursor.getColumnIndexOrThrow(Comics.KEY_NUMBER));
    
    comicNumber= (savedInstanceState == null) ? null :
      (Long) savedInstanceState.getSerializable(Comics.KEY_NUMBER);
    if (comicNumber == null) {
      Bundle extras= getIntent().getExtras();
      comicNumber= (Long) extras.getSerializable(Comics.KEY_NUMBER);
    }
    if (comicNumber == null) {
      comicNumber= maxNumber;
    }
    
    comicText= (EditText) findViewById(R.id.edit_number);
    goButton= (Button) findViewById(R.id.button_go);
    nextButton= (Button) findViewById(R.id.button_next);
    prevButton= (Button) findViewById(R.id.button_prev);
    comicImage= (ImageView) findViewById(R.id.image_comic);
    
    comicImage.setFocusable(true);
    
    comicText.setOnFocusChangeListener(new OnFocusChangeListener() {
      @Override
      public void onFocusChange(View v, boolean hasFocus) {
        EditText view= (EditText) v;
        
        if (!hasFocus) {
          InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
          imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        } else {
          view.selectAll();
        }
      }
    });
    comicText.setOnKeyListener(new View.OnKeyListener() {
      
      @Override
      public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_ENTER) {
          goButton.performClick();
          return true;
        }
        return false;
      }      
    });
    
    goButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View view) {
        long newNumber= Long.parseLong(comicText.getText().toString());
        if (newNumber > 0 && newNumber <= maxNumber)
          comicNumber= newNumber;
        updateDisplay();
      }
    });
    
    nextButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View arg0) {
        if (comicNumber.equals(1l))
          comicNumber= maxNumber;
        else
          --comicNumber;
        updateDisplay();
      }
    });

    prevButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View arg0) {
        if (comicNumber.equals(maxNumber))
          comicNumber= 1l;
        else
          ++comicNumber;
        updateDisplay();
      }
    });
    
    updateDisplay();
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
  }
  
  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putLong(Comics.KEY_NUMBER, comicNumber);
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
  }
  
  @Override
  public Dialog onCreateDialog(int id) {
    switch (id) {
      case DOWNLOAD_DIALOGID: {
        ProgressDialog dialog= new ProgressDialog(this);
        dialog.setMessage("Downloading comic...");
        dialog.setIndeterminate(true);
        dialog.setOnCancelListener(new OnCancelListener() {
          @Override
          public void onCancel(DialogInterface dialog) {
            thisContext.finish();
          }
        });
        return dialog;
      }
      case HOVERTEXT_DIALOGID: {
        Cursor cursor= dbHelper.fetchComic(comicNumber);
        String text= cursor.getString(cursor.getColumnIndexOrThrow(Comics.KEY_TEXT));
        if (text == null)
          try {
            dbHelper.updateComic(comicNumber);
            cursor= dbHelper.fetchComic(comicNumber);
            text= cursor.getString(cursor.getColumnIndexOrThrow(Comics.KEY_TEXT));
          } catch (IOException e) {
            text= "Could not connect to update comic hover text.";
          }
        AlertDialog.Builder builder= new AlertDialog.Builder(thisContext);
        builder.setCancelable(true);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int which) {
            // do nothing, hope it cancels
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
    comicText.setText(comicNumber.toString());
    comicText.clearFocus();
    
    Cursor cursor= dbHelper.fetchComic(comicNumber);
    String newTitle= comicNumber + ". " + cursor.getString(cursor.getColumnIndexOrThrow(Comics.KEY_TITLE));
    setTitle(newTitle);
    
    File file= new File(Comics.SD_DIR_PATH + comicNumber);
    if (file.length() == 0) {
      showDialog(DOWNLOAD_DIALOGID);
      executor.execute(new ImageGetter());
    } else {
      try {
        BufferedInputStream bi= new BufferedInputStream(new FileInputStream(file));
        BitmapDrawable drawable= new BitmapDrawable(BitmapFactory.decodeStream(bi));
        comicImage.setImageDrawable(drawable);
        comicImage.setImageMatrix(new Matrix());
        comicImage.setOnTouchListener(new ImageViewTouchListener());
        comicImage.setOnClickListener(new OnClickListener() {
          @Override
          public void onClick(View v) {
            showDialog(HOVERTEXT_DIALOGID);
          }
        });
      } catch (FileNotFoundException e) {
        // the file got deleted between exist check and file open, try it again
        updateDisplay();
      }
    }
  }
  
  private class ImageGetter implements Runnable {    
    @Override
    public void run() {
      int result;
      try {
        if (!Comics.SD_DIR.exists())
          Comics.SD_DIR.mkdirs();
        
        File file= new File(Comics.SD_DIR_PATH + comicNumber);
        
        synchronized(comicNumber) {
          dbHelper.updateComic(comicNumber);
          Cursor cursor= dbHelper.fetchComic(comicNumber);
          String url= cursor.getString(cursor.getColumnIndexOrThrow(Comics.KEY_URL));
          BufferedInputStream bi= new BufferedInputStream(Comics.download(url));
          Bitmap image= BitmapFactory.decodeStream(bi);
          FileOutputStream ostream= new FileOutputStream(file);
          BufferedOutputStream bo= new BufferedOutputStream(ostream);
          image.compress(CompressFormat.PNG, 50, bo);
          bi.close();
          bo.close();
          ostream.close();
        }
        
        result= Comics.STATUS_SUCCESS;
      } catch (MalformedURLException e) {
        result= Comics.STATUS_ERROR;
      } catch (IOException e) {
        result= Comics.STATUS_FAILURE;
      }
      
      runOnUiThread(new ImageGottenFinisher(result));
    }
  }
  
  private class ImageGottenFinisher implements Runnable {
    int status;
    ImageGottenFinisher(int status) {
      this.status= status;
    }
    
    @Override
    public void run() {
      dismissDialog(DOWNLOAD_DIALOGID);
      switch (status) {
        case Comics.STATUS_SUCCESS: {
          updateDisplay();
        } break;
        case Comics.STATUS_ERROR:
        case Comics.STATUS_FAILURE: {
          AlertDialog.Builder builder= new AlertDialog.Builder(thisContext);
          builder.setCancelable(true);
          builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
              // do nothing, hope it cancels
              thisContext.finish();
            }
          });
          builder.setMessage("Failed to get comic.");
          builder.create().show();
        } break;
      }
    }
  }
}
