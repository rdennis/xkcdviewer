package com.rada.xkcd;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class ComicView extends Activity {

  private ComicDbAdapter dbHelper;
  private Long comicNumber= null;
  private long maxNumber;
  
  private EditText comicText;
  
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
    Button goButton= (Button) findViewById(R.id.button_go);
    Button nextButton= (Button) findViewById(R.id.button_next);
    Button prevButton= (Button) findViewById(R.id.button_prev);
    
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
  }
}
