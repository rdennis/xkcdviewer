package com.rada.xkcd;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.concurrent.Executors;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

public class ComicView extends Activity {
  
  private final Activity thisContext= this;

  private ComicDbAdapter dbHelper;
  private volatile Long comicNumber= null;
  private long maxNumber;
  
  private EditText comicText;
  private Button goButton;
  private Button nextButton;
  private Button prevButton;
  private ImageView comicImage;
  
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.comic_view);
    
    dbHelper= new ComicDbAdapter(this);
    dbHelper.open();

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
        if (comicNumber.equals(maxNumber))
          comicNumber= 1L;
        else
          ++comicNumber;
        updateDisplay();
      }
    });

    prevButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View arg0) {
        if (comicNumber.equals(1L))
          comicNumber= maxNumber;
        else
          --comicNumber;
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
    outState.putSerializable(Comics.KEY_NUMBER, comicNumber);
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
  
  private void updateDisplay() {
    // TODO write this function, it should update the current views based on the number member
    comicText.setText(comicNumber.toString());
    File file= new File(Comics.SD_DIR_PATH + comicNumber);
    if (!file.exists()) {
      Executors.newSingleThreadExecutor().execute(new ImageGetter());
    } else {
      try {
        BufferedInputStream bi= new BufferedInputStream(new FileInputStream(file));
        BitmapDrawable drawable= new BitmapDrawable(BitmapFactory.decodeStream(bi));
        comicImage.setImageDrawable(drawable);
//        comicImage.getParent().childDrawableStateChanged(comicImage);
        comicImage.setOnClickListener(new OnClickListener() {
          @Override
          public void onClick(View v) {
            // TODO Auto-generated method stub
            Cursor cursor= dbHelper.fetchComic(comicNumber);
            String text= cursor.getString(cursor.getColumnIndexOrThrow(Comics.KEY_TEXT));
            AlertDialog.Builder builder= new AlertDialog.Builder(thisContext);
            builder.setCancelable(true);
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
              public void onClick(DialogInterface dialog, int which) {
                // do nothing, hope it cancels
                dialog.dismiss();
              }
            });
            builder.setMessage(text);
            builder.create().show();
          }
        });
      } catch (FileNotFoundException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
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
          if (file.createNewFile()) {
            dbHelper.updateComic(comicNumber);
            Cursor cursor= dbHelper.fetchComic(comicNumber);
            String url= cursor.getString(cursor.getColumnIndexOrThrow(Comics.KEY_URL));
            BufferedInputStream bi= new BufferedInputStream(Comics.download(url));
            Bitmap image= BitmapFactory.decodeStream(bi);
            FileOutputStream ostream= new FileOutputStream(file);
            BufferedOutputStream bo= new BufferedOutputStream(ostream);
            image.compress(CompressFormat.PNG, 100, bo);
            bi.close();
            bo.close();
            ostream.close();
          }
        }
        
        result= Comics.STATUS_SUCCESS;
      } catch (MalformedURLException e) {
        result= Comics.STATUS_ERROR;
      } catch (IOException e) {
        result= Comics.STATUS_FAILURE;
      }
      
      runOnUiThread(new ImageGetDone(result));
    }
  }
  
  private class ImageGetDone implements Runnable {
    int status;
    ImageGetDone(int status) {
      this.status= status;
    }
    
    @Override
    public void run() {
      switch (status) {
        case Comics.STATUS_SUCCESS: {
          updateDisplay();
        } break;
        case Comics.STATUS_ERROR:
        case Comics.STATUS_FAILURE: {
          Toast.makeText(thisContext, "It failed!!!", Toast.LENGTH_LONG);
          thisContext.finish();
        } break;
      }
    }
  }
}
