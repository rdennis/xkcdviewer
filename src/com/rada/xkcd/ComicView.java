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
import android.graphics.PointF;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.util.FloatMath;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnTouchListener;
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
  
  private static final String TAG= "ComicView";
  
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
        // TODO Auto-generated method stub
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
    // TODO write this function, it should update the current views based on the number member
    comicText.setText(comicNumber.toString());
    comicText.clearFocus();
    
    Cursor cursor= dbHelper.fetchComic(comicNumber);
    String newTitle= comicNumber + ". " + cursor.getString(cursor.getColumnIndexOrThrow(Comics.KEY_TITLE));
    setTitle(newTitle);
    
    File file= new File(Comics.SD_DIR_PATH + comicNumber);
    if (!file.exists()) {
      showDialog(DOWNLOAD_DIALOGID);
      executor.execute(new ImageGetter());
    } else {
      try {
        BufferedInputStream bi= new BufferedInputStream(new FileInputStream(file));
        BitmapDrawable drawable= new BitmapDrawable(BitmapFactory.decodeStream(bi));
        comicImage.setImageDrawable(drawable);
        comicImage.setImageMatrix(new Matrix());
        comicImage.setOnClickListener(new OnClickListener() {
          @Override
          public void onClick(View v) {
            showDialog(HOVERTEXT_DIALOGID);
          }
        });
        comicImage.setOnTouchListener(new OnTouchListener() {
          
          Matrix matrix= new Matrix();
          Matrix lastMatrix= new Matrix();
          Matrix savedMatrix= new Matrix();
          
          PointF mid= new PointF();
          PointF start= new PointF();
          PointF lastPoint= new PointF();
          
          float oldDist;
          
          static final int NONE= 0;
          static final int DRAG= 1;
          static final int ZOOM= 2;
          
          int mode;
          
          private float spacing(MotionEvent event) {
            float x = event.getX(0) - event.getX(1);
            float y = event.getY(0) - event.getY(1);
            return FloatMath.sqrt(x * x + y * y);
         }
          
          private void midPoint(PointF point, MotionEvent event) {
            float x = event.getX(0) + event.getX(1);
            float y = event.getY(0) + event.getY(1);
            point.set(x / 2, y / 2);
          }
          
          @Override
          public boolean onTouch(View v, MotionEvent event) {
            ImageView view= (ImageView) v;
            
            if (!view.hasFocus())
              view.requestFocus();

            final float viewWidth= (float) view.getWidth();
            final float viewHeight= (float) view.getHeight();
            final float intrinsicWidth= comicImage.getDrawable().getIntrinsicWidth();
            final float intrinsicHeight= comicImage.getDrawable().getIntrinsicHeight();
            final float minScaleX= viewWidth / intrinsicWidth;
            final float minScaleY= viewHeight / intrinsicHeight;
            final float minScale= (minScaleX < minScaleY) ? minScaleX : minScaleY;

            switch (event.getAction() & MotionEvent.ACTION_MASK) {
              case MotionEvent.ACTION_DOWN: {
                savedMatrix.set(matrix);
                start.set(event.getX(), event.getY());
                mode= DRAG;
                lastPoint.set(event.getX(), event.getY());
                return false;
              }
              case MotionEvent.ACTION_POINTER_DOWN: {
                oldDist = spacing(event);
                Log.d(TAG, "oldDist=" + oldDist);
                if (oldDist > 10f) {
                  savedMatrix.set(matrix);
                  midPoint(mid, event);
                  mode = ZOOM;
                }
              } break;
              case MotionEvent.ACTION_POINTER_UP:
              case MotionEvent.ACTION_UP: {
                mode= NONE;
                return !(start.x == event.getX() && start.y == event.getY());
              }
              case MotionEvent.ACTION_MOVE: {
                if (mode == DRAG) {
                  matrix.set(lastMatrix);
                  matrix.postTranslate(event.getX() - lastPoint.x, event.getY() - lastPoint.y);
                } else if (mode == ZOOM) {
                  float newDist= spacing(event);
                  Log.d(TAG, "newDist=" + newDist);
                  if (newDist > 10f) {
                     matrix.set(savedMatrix);
                     float scale = newDist / oldDist;
                     matrix.postScale(scale, scale, mid.x, mid.y);
                  }
                }
              } break;
            }
            
            float[] values= new float[9];
            matrix.getValues(values);
            
            float scale= values[Matrix.MSCALE_X];
            
            if (scale < minScale) {
              float newScale= minScale / scale;
              matrix.postScale(newScale, newScale, mid.x, mid.y);
            }
            
            matrix.getValues(values);
            
            scale= values[Matrix.MSCALE_X];
            float actualWidth= scale * intrinsicWidth;
            float actualHeight= scale * intrinsicHeight;
            float minX= viewWidth - actualWidth;
            float maxX= minX;
            float minY= viewHeight - actualHeight;
            float maxY= minY;
            
            if (maxX < 0f)
              maxX= 0f;
            if (minX > 0f)
              minX= 0f;
            if (maxY < 0f)
              maxY= 0f;
            if (minY > 0f)
              minY= 0f;

            float transX= values[Matrix.MTRANS_X];
            float transY= values[Matrix.MTRANS_Y];
            
            if (transX > maxX)
              matrix.postTranslate(maxX - transX, 0);
            if (transX < minX)
              matrix.postTranslate(minX - transX, 0);
            if (transY > maxY)
              matrix.postTranslate(0, maxY - transY);
            if (transY < minY)
              matrix.postTranslate(0, minY - transY);
            
            lastPoint.set(event.getX(), event.getY());
            lastMatrix.set(matrix);
            view.setImageMatrix(matrix);

            
//          Log.d(TAG, "viewWidth=" + viewWidth);
//          Log.d(TAG, "viewHeight=" + viewHeight);
//          Log.d(TAG, "intrinsicWidth=" + intrinsicWidth);
//          Log.d(TAG, "intrinsicHeight=" + intrinsicHeight);
//          Log.d(TAG, "minX=" + minX);
//          Log.d(TAG, "maxX=" + maxX);
//          Log.d(TAG, "minY=" + minY);
//          Log.d(TAG, "maxY=" + maxY);
//          Log.d(TAG, "transX=" + transX);
//          Log.d(TAG, "transY=" + transY);
            
            return true;
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
            image.compress(CompressFormat.PNG, 50, bo);
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
          dismissDialog(DOWNLOAD_DIALOGID);
        } break;
        case Comics.STATUS_ERROR:
        case Comics.STATUS_FAILURE: {
          AlertDialog.Builder builder= new AlertDialog.Builder(thisContext.getApplicationContext());
          builder.setCancelable(true);
          builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
              // do nothing, hope it cancels
              dialog.dismiss();
            }
          });
          builder.setMessage("Failed to get comic.");
          builder.create().show();
          thisContext.finish();
        } break;
      }
    }
  }
}
