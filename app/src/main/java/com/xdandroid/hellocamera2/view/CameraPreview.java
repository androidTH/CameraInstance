package com.xdandroid.hellocamera2.view;

import android.annotation.*;
import android.content.*;
import android.hardware.*;
import android.util.Log;
import android.view.*;

@SuppressLint("ViewConstructor")
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {

    public static String TAG=CameraPreview.class.getSimpleName();
    /**
     * 需要让Activity/Fragment处理Throwable时，提供的回调监听类.
     */
    public interface ThrowableListener {
        void onThrowable(Throwable throwable, boolean showToast);
    }

    private SurfaceHolder mHolder;
    private Camera mCamera;
    private ThrowableListener mThrowableListener;

    public CameraPreview(Context context, Camera camera, ThrowableListener l) {
        super(context);
        mCamera = camera;
        mHolder = getHolder();
        mHolder.addCallback(this);
        mThrowableListener = l;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            Log.i(TAG,"surfaceCreated");
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
        } catch (Exception e) {
            e.printStackTrace();
            if (mThrowableListener != null) mThrowableListener.onThrowable(e, true);
        }
    }

    /**
     * 由Activity/Fragment处理Camera的释放.
     *
     * @param holder SurfaceHolder
     */
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        try {
            Log.i(TAG,"surfaceDestroyed");
            mHolder.removeCallback(this);
        } catch (Exception ignored) {}
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        if (mHolder.getSurface() == null) return;
        try {
            mCamera.stopPreview();
        } catch (Exception ignored) {}
        try {
//            mCamera.setDisplayOrientation(90);//预览设置90度
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();
            Log.i(TAG,"surfaceChanged");
        } catch (Exception e) {
            e.printStackTrace();
            if (mThrowableListener != null) mThrowableListener.onThrowable(e, false);
        }
    }
}
