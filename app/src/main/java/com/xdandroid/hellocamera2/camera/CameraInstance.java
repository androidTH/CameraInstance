package com.xdandroid.hellocamera2.camera;

import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Build;
import android.util.Log;

import com.xdandroid.hellocamera2.util.CameraUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static android.content.ContentValues.TAG;

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
    private int mDefaultCameraID = 0;
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

    public void initCamera() {
        if (mCameraDevice == null) {
            Log.e(LOG_TAG, "initCamera: Camera is not opened!");
            return;
        }

        mParams = mCameraDevice.getParameters();

        mParams.setPictureFormat(PixelFormat.JPEG);

        List<Camera.Size> picSizes = mParams.getSupportedPictureSizes();
        Log.i(TAG,"宽高"+mPictureWidth+"*"+mPictureHeight);
        Camera.Size picSz = findBestSize(true,picSizes,mPictureWidth*mPictureHeight);

        List<Camera.Size> prevSizes = mParams.getSupportedPreviewSizes();
        Camera.Size prevSz = findBestSize(false,prevSizes,mPreferPreviewWidth*mPreferPreviewHeight); //getBestSize(prevSizes,mPreviewWidth,mPreviewHeight);;

        mParams.setPreviewSize(prevSz.width, prevSz.height);
        mParams.setPictureSize(picSz.width, picSz.height);

        List<String> focusModes = mParams.getSupportedFocusModes();
        if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
            mParams.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);//自动聚焦
        }
        mParams.setPreviewFrameRate(DEFAULT_PREVIEW_RATE); //设置相机预览帧率

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
                mParams.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);// params.setFocusMode(Camera.Parameters.FOCUS_MODE_MACRO);
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

    public  int compares(int lhs, int rhs) {
        return lhs < rhs ? -1 : (lhs == rhs ? 0 : 1);
    }

    /**
     * 判断是否是16：9的Size, 允许误差5%.
     *
     * @param size Size
     * @return 是否是16：9的Size
     */
    public  boolean isWide(Camera.Size size) {
        double ratio = ((double) size.width) / ((double) size.height);
        return ratio > 1.68 && ratio < 1.87;
    }

    public Camera.Size findBestSize(boolean forTakingPicture,List<Camera.Size> sizeList, long maxPicturePixels) {
        List<Camera.Size> tooLargeSizes = new ArrayList<>();

        Collections.sort(sizeList, new Comparator<Camera.Size>() {
            @Override
            public int compare(Camera.Size o1, Camera.Size o2) {
                return compares(o1.width*o1.height, o2.width*o2.height);
            }
        });

        boolean notTooLarge;
        for (Camera.Size size : sizeList) {
            if (isWide(size)) {
                if (forTakingPicture) {
                    //若是为了拍摄照片，则尺寸不要超过指定的maxPicturePixels.
                    Log.i("screenSize", size.width+"*"+size.height+"最大" + maxPicturePixels);
                    notTooLarge = ((long) size.width) * ((long) size.height) < maxPicturePixels;
                }else{
                    Log.i("screenSize", size.width+"*"+size.height+"最大" + maxPicturePixels);
                    notTooLarge = ((long) size.width) * ((long) size.height) < 1920 * 1080;//1280 * 720
                }
                if (!notTooLarge) tooLargeSizes.add(size);
            }
        }

        if (tooLargeSizes.size() > 0) {
            return tooLargeSizes.get(0);
        }
        Log.i("findBestSize",tooLargeSizes.toString());
        return sizeList.get(0);
    };
}
