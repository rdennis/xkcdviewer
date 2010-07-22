package com.rada.xkcd;

import android.graphics.Matrix;
import android.graphics.PointF;
import android.util.FloatMath;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ImageView;

public class ImageViewTouchListener implements OnTouchListener {
  
  private Matrix matrix= new Matrix();
  private Matrix lastMatrix= new Matrix();
  private Matrix savedMatrix= new Matrix();
  
  private PointF mid= new PointF();
  private PointF start= new PointF();
  private PointF lastPoint= new PointF();

  private boolean moved;
  private float oldDist;
  
  public static final int NONE= 0;
  public static final int DRAG= 1;
  public static final int ZOOM= 2;
  
  private int mode;
  
  private static final String TAG= "ImageViewTouchListener";
  
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

  long startTime;
  
  @Override
  public boolean onTouch(View v, MotionEvent event) {
    ImageView view= (ImageView) v;
    
    if (!view.hasFocus())
      view.requestFocus();

    final float viewWidth= (float) view.getWidth();
    final float viewHeight= (float) view.getHeight();
    final float intrinsicWidth= view.getDrawable().getIntrinsicWidth();
    final float intrinsicHeight= view.getDrawable().getIntrinsicHeight();
    final float minScaleX= viewWidth / intrinsicWidth;
    final float minScaleY= viewHeight / intrinsicHeight;
    final float minScale= (minScaleX < minScaleY) ? minScaleX : minScaleY;
    final float maxScale= 4f; // or instead: 2f * ((minScaleX > minScaleY) ? minScaleX : minScaleY);

    switch (event.getAction() & MotionEvent.ACTION_MASK) {
      case MotionEvent.ACTION_DOWN: {
        moved= false;
        savedMatrix.set(matrix);
        start.set(event.getX(), event.getY());
        mode= DRAG;
        lastPoint.set(event.getX(), event.getY());
        startTime= event.getEventTime();
        return false;
      }
      case MotionEvent.ACTION_POINTER_DOWN: {
        oldDist = spacing(event);
        Log.d(TAG, "oldDist=" + oldDist);
        if (oldDist > 10f) {
          savedMatrix.set(matrix);
          midPoint(mid, event);
          mode= ZOOM;
        }
      } break;
      case MotionEvent.ACTION_POINTER_UP:
      case MotionEvent.ACTION_UP: {
        mode= NONE;
        long thisTime= event.getEventTime();
        if (thisTime - startTime < 150)
          return false;
        return moved;
      }
      case MotionEvent.ACTION_MOVE: {
        moved= true;
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
      scale*= newScale;
    }
    if (scale > maxScale) {
      float newScale= maxScale / scale;
      matrix.postScale(newScale, newScale, mid.x, mid.y);
      scale*= newScale;
    }
    
    matrix.getValues(values);
    
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
    
    return true;
  }  
}
