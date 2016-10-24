package com.xdandroid.hellocamera2.camera;

import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Build;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class CameraInstance {
    public static final String LOG_TAG = CameraInstance.class.getSimpleName();
    private static final String ASSERT_MSG = "检测到CameraDevice为空,请检查";
    public static final int DEFAULT_PREVIEW_RATE = 30;
    public static final int CAMERA_FRONT = Camera.CameraInfo.CAMERA_FACING_FRONT;
    public static final int CAMERA_BACK = Camera.CameraInfo.CAMERA_FACING_BACK;

    private static CameraInstance mThisInstance;
    private Camera mCameraDevice;
    private Camera.Parameters mParams;

    private boolean mIsPreviewing = false;
    private int mDefaultCameraID = -1;
    private int mFacing = 0;

    private int mPreviewWidth;
    private int mPreviewHeight;
    private int mPictureWidth = 1280;
    private int mPictureHeight = 720;
    private int mPreferPreviewWidth = 1280;
    private int mPreferPreviewHeight = 720;
    private int mOrientationDegree;

    private CameraInstance() {
    }

    public static synchronized CameraInstance getInstance() {
        if (mThisInstance == null) {
            mThisInstance = new CameraInstance();
        }
        return mThisInstance;
    }

    public boolean isPreviewing() {
        return mIsPreviewing;
    }

    public void setPreferPreviewSize(int w, int h) {
        this.mPreferPreviewWidth = w;
        this.mPreferPreviewHeight = h;
        this.mPictureWidth=w;
        this.mPictureHeight=h;
    }

    public void setDisplayOrientation(int degrees) {
        this.mOrientationDegree = degrees;
    }

    public int getDisplayOrientation(){
        return mOrientationDegree;
    }

    public interface CameraOpenCallback {
        void cameraReady();
    }

    public boolean tryOpenCamera(CameraOpenCallback callback) {
        return tryOpenCamera(callback, CAMERA_BACK);
    }

    public int getFacing() {
        return mFacing;
    }

    public synchronized boolean tryOpenCamera(CameraOpenCallback callback, int facing) {
        Log.i(LOG_TAG, "try open camera...");

        try {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.FROYO) {
                int numberOfCameras = Camera.getNumberOfCameras();

                Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
                for (int i = 0; i < numberOfCameras; i++) {
                    Camera.getCameraInfo(i, cameraInfo);
                    if (cameraInfo.facing == facing) {
                        mDefaultCameraID = i;
                        mFacing = facing;
                    }
                }
            }
            stopPreview();
            if (mCameraDevice != null)
                mCameraDevice.release();

            if (mDefaultCameraID >= 0) {
                mCameraDevice = Camera.open(mDefaultCameraID);
            } else {
                mCameraDevice = Camera.open();
                mFacing = CAMERA_BACK; //default: back facing
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "Open Camera Failed!");
            e.printStackTrace();
            stopCamera();
            mCameraDevice = null;
            return false;
        }

        if (mCameraDevice != null) {
            Log.i(LOG_TAG, "Camera opened!");

            try {
                initCamera();
            } catch (Exception e) {
                mCameraDevice.release();
                mCameraDevice = null;
                return false;
            }

            if (callback != null) {
                callback.cameraReady();
            }

            return true;
        }

        return false;
    }

    public void unlockCamera() {
        if (mCameraDevice != null) {
            mCameraDevice.unlock();//解锁Camera —— 调用Camera.unlock()解锁，便于MediaRecorder 使用摄像头
        }
    }

    public synchronized void stopCamera() {
        if (mCameraDevice != null) {
            mIsPreviewing = false;
            mCameraDevice.stopPreview();
            mCameraDevice.setPreviewCallback(null);
            mCameraDevice.release();
            mCameraDevice = null;
        }
    }

    public boolean isCameraOpened() {
        return mCameraDevice != null;
    }

    public synchronized void startPreview(SurfaceTexture texture) {
        Log.i(LOG_TAG, "Camera startPreview...");
        if (mIsPreviewing) {
            Log.e(LOG_TAG, "Err: camera is previewing...");
//            stopPreview();
            return;
        }

        if (mCameraDevice != null) {
            try {
                mCameraDevice.setPreviewTexture(texture);
            } catch (IOException e) {
                e.printStackTrace();
            }

            mCameraDevice.startPreview();
            mIsPreviewing = true;
        }
    }

    public synchronized void stopPreview() {
        if (mIsPreviewing && mCameraDevice != null) {
            Log.i(LOG_TAG, "Camera stopPreview...");
            mIsPreviewing = false;
            mCameraDevice.stopPreview();
            mCameraDevice.release();
            mCameraDevice = null;
        }
    }

    public synchronized Camera.Parameters getParams() {
        if (mCameraDevice != null)
            return mCameraDevice.getParameters();
        assert mCameraDevice != null : ASSERT_MSG;
        return null;
    }

    public synchronized void setParams(Camera.Parameters param) {
        if (mCameraDevice != null) {
            mParams = param;
            mCameraDevice.setParameters(mParams);
        }
        assert mCameraDevice != null : ASSERT_MSG;
    }

    public Camera getCameraDevice() {
        return mCameraDevice;
    }


    public int getCameraOrientation(int facing) {
        Camera.CameraInfo camInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(facing, camInfo);
        return camInfo.orientation;
    }

    //保证从大到小排列
    private Comparator<Camera.Size> comparatorBigger = new Comparator<Camera.Size>() {
        @Override
        public int compare(Camera.Size lhs, Camera.Size rhs) {
            int w = rhs.width - lhs.width;
            if (w == 0)
                return rhs.height - lhs.height;
            return w;
        }
    };

    //保证从小到大排列
    private Comparator<Camera.Size> comparatorSmaller = new Comparator<Camera.Size>() {
        @Override
        public int compare(Camera.Size lhs, Camera.Size rhs) {
            int w = lhs.width - rhs.width;
            if (w == 0)
                return lhs.height - rhs.height;
            return w;
        }
    };

    public void initCamera() {
        if (mCameraDevice == null) {
            Log.e(LOG_TAG, "initCamera: Camera is not opened!");
            return;
        }

        mParams = mCameraDevice.getParameters();
//        List<Integer> supportedPictureFormats = mParams.getSupportedPictureFormats();

//        for (int fmt : supportedPictureFormats) {
//            Log.i(LOG_TAG, String.format("Picture Format: %x", fmt));
//        }

        mParams.setPictureFormat(PixelFormat.JPEG);

        List<Camera.Size> picSizes = mParams.getSupportedPictureSizes();
        Camera.Size picSz = getBestSize(picSizes,mPictureWidth,mPictureHeight);

        List<Camera.Size> prevSizes = mParams.getSupportedPreviewSizes();
        Camera.Size prevSz = getBestSize(prevSizes,mPreviewWidth,mPreviewHeight);;

//        List<Integer> frameRates = mParams.getSupportedPreviewFrameRates();
//        int fpsMax = 0;
//        for (Integer n : frameRates) {
//            Log.i(LOG_TAG, "Supported frame rate: " + n);
//            if (fpsMax < n) {
//                fpsMax = n;
//            }
//        }

        mParams.setPreviewSize(prevSz.width, prevSz.height);
        mParams.setPictureSize(picSz.width, picSz.height);

        List<String> focusModes = mParams.getSupportedFocusModes();
        if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
            mParams.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        }
        mParams.setPreviewFrameRate(DEFAULT_PREVIEW_RATE); //设置相机预览帧率
//        mParams.setPreviewFpsRange(20, 60);

        try {
            mCameraDevice.setParameters(mParams);
            mCameraDevice.setDisplayOrientation(mOrientationDegree);
        } catch (Exception e) {
            e.printStackTrace();
        }

        mParams = mCameraDevice.getParameters();
        Camera.Size szPic = mParams.getPictureSize();
        Camera.Size szPrev = mParams.getPreviewSize();
        mPreviewWidth = szPrev.width;
        mPreviewHeight = szPrev.height;
        mPictureWidth = szPic.width;
        mPictureHeight = szPic.height;
        Log.i(LOG_TAG, String.format("Camera Picture Size: %d x %d", szPic.width, szPic.height));
        Log.i(LOG_TAG, String.format("Camera Preview Size: %d x %d", szPrev.width, szPrev.height));
    }

    public synchronized Camera.Size  getBestSize(List<Camera.Size> sizeArray,int width,int height){
        Collections.sort(sizeArray, comparatorBigger);
        Camera.Size mBestSize=null;
        for (Camera.Size sz : sizeArray) {
//            Log.i(LOG_TAG, String.format("Supported preview size: %d x %d", sz.width, sz.height));
            if (sizeArray == null || (sz.width >= mPreferPreviewWidth && sz.height >= mPreferPreviewHeight)) {
                mBestSize = sz;
            }
        }
        return mBestSize;
    };

    public synchronized void setFocusMode(String focusMode) {

        if (mCameraDevice == null)
            return;

        mParams = mCameraDevice.getParameters();
        List<String> focusModes = mParams.getSupportedFocusModes();
        if (focusModes.contains(focusMode)) {
            mParams.setFocusMode(focusMode);
        }
    }

    public void focusAtPoint(float x, float y, final Camera.AutoFocusCallback callback) {
        focusAtPoint(x, y, 0.2f, callback);
    }

    /**
     * 在FOCUS_MODE_AUTO模式下使用，触发一次自动对焦.
     */
    public void focusAtPoint() {
        try {
            mCameraDevice.autoFocus(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized void focusAtPoint(float x, float y, float radius, final Camera.AutoFocusCallback callback) {
        if (mCameraDevice == null) {
            Log.e(LOG_TAG, "Error: focus after release.");
            return;
        }

        mParams = mCameraDevice.getParameters();

        if (mParams.getMaxNumMeteringAreas() > 0) {

            int focusRadius = (int) (radius * 1000.0f);
            int left = (int) (x * 2000.0f - 1000.0f) - focusRadius;
            int top = (int) (y * 2000.0f - 1000.0f) - focusRadius;

            Rect focusArea = new Rect();
            focusArea.left = Math.max(left, -1000);
            focusArea.top = Math.max(top, -1000);
            focusArea.right = Math.min(left + focusRadius, 1000);
            focusArea.bottom = Math.min(top + focusRadius, 1000);
            List<Camera.Area> meteringAreas = new ArrayList<Camera.Area>();
            meteringAreas.add(new Camera.Area(focusArea, 800));

            try {
                mCameraDevice.cancelAutoFocus();
                mParams.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                mParams.setFocusAreas(meteringAreas);
                mCameraDevice.setParameters(mParams);
                mCameraDevice.autoFocus(callback);
            } catch (Exception e) {
                Log.e(LOG_TAG, "Error: focusAtPoint failed: " + e.toString());
            }
        } else {
            Log.i(LOG_TAG, "The device does not support metering areas...");
            try {
                mCameraDevice.autoFocus(callback);
            } catch (Exception e) {
                Log.e(LOG_TAG, "Error: focusAtPoint failed: " + e.toString());
            }
        }
    }
}