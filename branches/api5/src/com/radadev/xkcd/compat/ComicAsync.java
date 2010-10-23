package com.radadev.xkcd.compat;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.htmlcleaner.XPatherException;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.util.Pair;
import android.widget.Toast;

import com.radadev.xkcd.compat.database.ComicDbAdapter;
import com.radadev.xkcd.compat.scraper.ArchiveScraper;

public final class ComicAsync {
  
  public static abstract class ComicAsyncTask<Param, Progress, Result> extends AsyncTask<Param, Progress, Result> {

    Context mContext;
    
    Runnable mPreCallBack= null;
    Runnable mPostCallBack= null;
    Runnable mProgressCallBack= null;
    Runnable mCancelledCallBack= null;
    Runnable mFinishedCallBack= null;
    
    public ComicAsyncTask(Context context) {
      mContext= context;
    }
    
    @Override
    protected void onPreExecute() {
      if (mPreCallBack != null) {
        mPreCallBack.run();
      }
    }
    
    @Override
    protected void onProgressUpdate(Progress... progress) {
      if (mProgressCallBack != null) {
        mProgressCallBack.run();
      }
    }
    
    @Override
    protected void onPostExecute(Result result) {
      if (mPostCallBack != null) {
        mPostCallBack.run();
      }
      if (mFinishedCallBack != null) {
        mFinishedCallBack.run();
      }
    }
    
    @Override
    protected void onCancelled() {
      if (mCancelledCallBack != null) {
        mCancelledCallBack.run();
      }
      if (mFinishedCallBack != null) {
        mFinishedCallBack.run();
      }
    }
    
    public ComicAsyncTask<Param, Progress, Result> setPreCallBack(Runnable callBack) {
      mPreCallBack= callBack;
      return this;
    }
    
    public ComicAsyncTask<Param, Progress, Result> setProgressCallBack(Runnable callBack) {
      mProgressCallBack= callBack;
      return this;
    }
    
    public ComicAsyncTask<Param, Progress, Result> setPostCallBack(Runnable callBack) {
      mPostCallBack= callBack;
      return this;
    }
    
    public ComicAsyncTask<Param, Progress, Result> setCancelledCallBack(Runnable callBack) {
      mCancelledCallBack= callBack;
      return this;
    }
    
    public ComicAsyncTask<Param, Progress, Result> setFinishedCallBack(Runnable callBack) {
      mFinishedCallBack= callBack;
      return this;
    }
  }
  
  public static class AsyncUpdate extends ComicAsyncTask<Void, Void, Boolean> {

    Dialog mDialog;
    ComicDbAdapter mDbAdapter;

    boolean mForceUpdate= false;
    boolean mShowProgress= false;
    
    public AsyncUpdate(Context context) {
      super(context);
      mDbAdapter= new ComicDbAdapter(mContext);
      mDbAdapter.open();
    }
    
    public AsyncUpdate(Context context, boolean forceUpdate, boolean showProgress) {
      this(context);
      mShowProgress= showProgress;
    }
    
    @Override
    protected void onPreExecute() {
      super.onPreExecute();
      if (mShowProgress) {
        mDialog= ProgressDialog.show(mContext, "", "Updating comic list...",
            true,
            true,
            new DialogInterface.OnCancelListener() {
              public void onCancel(DialogInterface dialog) {
                AsyncUpdate.this.cancel(true);
              }
            });
      }
    }

    @Override
    protected Boolean doInBackground(Void... params) {
      boolean success= false;
      try {
        if (mForceUpdate) {
          ArchiveScraper.scrapeArchive();
        }
        SortedMap<Integer, String> comics= ArchiveScraper.getComicList();
        mDbAdapter.insertComics(comics);
        success= true;
      } catch (IOException e) {
        e.printStackTrace();
      } catch (XPatherException e) {
        e.printStackTrace();
      }
      return success;
    }

    @Override
    protected void onPostExecute(Boolean result) {
      super.onPostExecute(result);
      if (mDialog != null && mDialog.isShowing()) {
        mDialog.dismiss();
      }
      Toast toast= Toast.makeText(mContext, "", Toast.LENGTH_SHORT);
      if (result) {
        toast.setText("Successfully updated comic list");
      } else {
        toast.setText("Failed to download comic list");
      }
      toast.show();
    }
    
    public AsyncUpdate setForceUpdate(boolean forceUpdate) {
      if (this.getStatus() == AsyncTask.Status.PENDING) {
        mForceUpdate= forceUpdate;
      }
      return this;
    }
    
    public AsyncUpdate setShowProgress(boolean showProgress) {
      if (this.getStatus() == AsyncTask.Status.PENDING) {
        mShowProgress= showProgress;
      }
      return this;
    }
  }
  
  public static class AsyncDownload extends ComicAsyncTask<Integer, Integer, Boolean> {

    Dialog mDialog;

    boolean mShowProgress= false;
    
    public AsyncDownload(Context context) {
      super(context);
    }
    
    public AsyncDownload(Context context, boolean showProgress) {
      this(context);
      mShowProgress= showProgress;
    }

    @Override
    protected void onPreExecute() {
      super.onPreExecute();
      if (mShowProgress) {
        mDialog= ProgressDialog.show(mContext, "", "Downloading comic...",
            true,
            true,
            new DialogInterface.OnCancelListener() {
              public void onCancel(DialogInterface dialog) {
                AsyncDownload.this.cancel(true);
              }
            });
      }
    }
    
    @Override
    protected Boolean doInBackground(Integer... params) {
      boolean result= false;
      for (Integer comicNumber : params) {
        try {
          synchronized (this) {
            wait(1);
          }
          Comics.downloadComic(comicNumber, mContext);
          publishProgress(comicNumber);
          result= true;
        } catch (IOException e) {
          result= false;
        } catch (InterruptedException e) {
          cancel(true);
        }
      }
      return result;
    }
    
    @Override
    protected void onProgressUpdate(Integer... progress) {
      super.onProgressUpdate(progress);
      for (Integer i : progress) {
        Toast.makeText(mContext, "Downloaded comic " + i, Toast.LENGTH_SHORT);
      }
    }

    @Override
    protected void onPostExecute(Boolean result) {
      super.onPostExecute(result);
      if (mDialog != null && mDialog.isShowing()) {
        mDialog.dismiss();
      }
    }
    
    public AsyncDownload setShowProgress(boolean showProgress) {
      if (getStatus() == AsyncTask.Status.PENDING) {
        mShowProgress= showProgress;
      }
      return this;
    }
  }

  public static class AsyncDownloadAll extends ComicAsyncTask<Integer, Pair<Integer, Boolean>, Void> {

    ProgressDialog mDialog;
    ExecutorService mExecutor;

    boolean mShowProgress= false;
    boolean mShowNotification= false;

    private static final int THREAD_COUNT= 10;
    private static final int MAX_ATTEMPTS= 5;
    
    public AsyncDownloadAll(Context context) {
      super(context);
    }
    
    public AsyncDownloadAll(Context context, boolean showProgress) {
      super(context);
      mShowProgress= showProgress;
    }

    public AsyncDownloadAll(Context context, boolean showProgress, boolean showNotification) {
      super(context);
      mShowProgress= showProgress;
      mShowNotification= showNotification;
    }
    
    @Override
    protected void onPreExecute() {
      super.onPreExecute();
      if (mShowProgress) {
        mDialog= new ProgressDialog(mContext);
        mDialog.setMessage("Downloading comics");
        mDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mDialog.setMax(100);
        mDialog.setProgress(0);
        mDialog.setCancelable(true);
        mDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
          public void onCancel(DialogInterface dialog) {
            cancel(true);
          }
        });
        mDialog.show();
      }
    }
    
    @Override
    protected Void doInBackground(Integer... params) {
      mDialog.setMax(params.length);
      mExecutor= Executors.newFixedThreadPool(THREAD_COUNT);
      List<Callable<Boolean>> callables= new ArrayList<Callable<Boolean>>();
      for (final Integer number : params) {
        callables.add(new Callable<Boolean>() {
          @SuppressWarnings("unchecked")
          public Boolean call() {
            boolean success= false;
            for (int i= 0; i < MAX_ATTEMPTS && !success; ++i) {
              try {
                Comics.downloadComic(number, mContext);
                success= true;
              } catch (IOException e) {
                success= false;
              }
            }
            publishProgress(new Pair(number, success));
            return success;
          }
        });
      }
      try {
        mExecutor.invokeAll(callables);
      } catch (InterruptedException e1) {
        cancel(true);
      }
      return null;
    }
    
    @Override
    protected void onCancelled() {
      super.onCancelled();
      mExecutor.shutdownNow();
    }

    @Override
    protected void onProgressUpdate(Pair<Integer, Boolean>... values) {
      super.onProgressUpdate(values);
      mDialog.incrementProgressBy(values.length);
      for (Pair <Integer, Boolean> pair : values) {
        if (!pair.second) {
          Toast.makeText(mContext, "Failed to download " + pair.first, Toast.LENGTH_SHORT).show();
        }
      }
    }

    @Override
    protected void onPostExecute(Void result) {
      super.onPostExecute(result);
      if (mDialog != null && mDialog.isShowing()) {
        mDialog.dismiss();
      }
    }
    
    public AsyncDownloadAll setShowProgress(boolean showProgress) {
      if (this.getStatus() == AsyncTask.Status.PENDING) {
        mShowProgress= showProgress;
      }
      return this;
    }
  }
  
  public static class AsyncClear extends ComicAsyncTask<Integer, Integer, Void> {

    ProgressDialog mDialog;
    ExecutorService mExecutor;
    
    boolean mShowProgress= false;
    
    private static final int THREAD_COUNT= 10;
    
    public AsyncClear(Context context) {
      super(context);
    }

    public AsyncClear(Context context, boolean showProgress) {
      super(context);
      mShowProgress= showProgress;
    }
    
    @Override
    protected void onPreExecute() {
      super.onPreExecute();
      if (mShowProgress) {
        mDialog= new ProgressDialog(mContext);
        mDialog.setMessage("Deleting comics");
        mDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mDialog.setMax(100);
        mDialog.setProgress(0);
        mDialog.setCancelable(true);
        mDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
          public void onCancel(DialogInterface dialog) {
            cancel(true);
          }
        });
        mDialog.show();
      }
    }

    @Override
    protected Void doInBackground(Integer... params) {
      mDialog.setMax(params.length);
      mExecutor= Executors.newFixedThreadPool(THREAD_COUNT);
      List<Callable<Boolean>> callables= new ArrayList<Callable<Boolean>>();
      for (final Integer number : params) {
        callables.add(new Callable<Boolean>() {
          public Boolean call() {
            boolean result;
            try {
              File file= new File(Comics.getSdDir(mContext), number.toString());
              if (file.exists()) {
                file.delete();
              }
              publishProgress(1);
              result= true;
            } catch (Exception e) {
              result= false;
            }
            return result;
          }
        });
      }
      try {
        mExecutor.invokeAll(callables);
      } catch (InterruptedException e1) {
        cancel(true);
      }
      return null;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
      super.onProgressUpdate(values);
      mDialog.incrementProgressBy(values.length);
    }

    @Override
    protected void onPostExecute(Void result) {
      super.onPostExecute(result);
      if (mDialog != null && mDialog.isShowing()) {
        mDialog.dismiss();
      }
    }
    
    public AsyncClear setShowProgress(boolean showProgress) {
      if (this.getStatus() == AsyncTask.Status.PENDING) {
        mShowProgress= showProgress;
      }
      return this;
    }
  }
}
