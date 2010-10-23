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

import android.graphics.Matrix;
import android.graphics.PointF;
import android.util.FloatMath;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

public class ImageViewTouchListener implements OnTouchListener {
  
  private Matrix matrix= null;
  private Matrix lastMatrix= new Matrix();
  private Matrix savedMatrix= new Matrix();
  
  private PointF mid= new PointF();
  private PointF start= new PointF();
  private PointF lastPoint= new PointF();

  private float oldDist;
  private float distanceMoved;
  
  public static final int NONE= 0;
  public static final int DRAG= 1;
  public static final int ZOOM= 2;

  private static final float CLICK_MOVE_TOLERANCE= 5f; // pixels
  private static final int CLICK_TIME_TOLERANCE= 300;  // milliseconds
  
  private int mode;
  
  private static final String TAG= "ImageViewTouchListener";
  
  private float spacing(MotionEvent event) {
    float x= event.getX(0) - event.getX(1);
    float y= event.getY(0) - event.getY(1);
    return FloatMath.sqrt(x * x + y * y);
 }
  
  private void midPoint(PointF point, MotionEvent event) {
    float x= event.getX(0) + event.getX(1);
    float y= event.getY(0) + event.getY(1);
    point.set(x / 2, y / 2);
  }
  
  public boolean onTouch(View v, MotionEvent event) {
    ImageView view= (ImageView) v;
    view.setScaleType(ScaleType.MATRIX);
    
    if (!view.hasFocus())
      view.requestFocus();

    final float viewWidth= (float) view.getWidth();
    final float viewHeight= (float) view.getHeight();
    final float intrinsicWidth= view.getDrawable().getIntrinsicWidth();
    final float intrinsicHeight= view.getDrawable().getIntrinsicHeight();
    final float minScaleX= viewWidth / intrinsicWidth;
    final float minScaleY= viewHeight / intrinsicHeight;
    final float minScale= (minScaleX < minScaleY) ? minScaleX : minScaleY;
    final float maxScale= 5f + ((minScaleX < minScaleY) ? minScaleY : minScaleX);
    
    if (matrix == null) {
      matrix= new Matrix();
      matrix.setScale(minScale, minScale);
      lastMatrix.set(matrix);
    }

    switch (event.getAction() & MotionEvent.ACTION_MASK) {
      case MotionEvent.ACTION_DOWN: {
        savedMatrix.set(matrix);
        start.set(event.getX(), event.getY());
        mode= DRAG;
        lastPoint.set(event.getX(), event.getY());
        distanceMoved= 0f;
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
        boolean isClickTime= (event.getEventTime() - event.getDownTime() < CLICK_TIME_TOLERANCE);
        boolean isClickDistance= distanceMoved < CLICK_MOVE_TOLERANCE;
        boolean isClick= isClickTime && isClickDistance;
        
        // if it's valid as a click, we actually need to return false so we don't swallow the event
        return !isClick;
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
    
    distanceMoved+= distance(lastPoint, event);
    lastPoint.set(event.getX(), event.getY());
    lastMatrix.set(matrix);
    view.setImageMatrix(matrix);
    
    return true;
  }
  
  float distance(PointF point, MotionEvent event) {
    float x= point.x - event.getX();
    float y= point.y - event.getY();
    return FloatMath.sqrt(x * x + y * y);
  }
}
