package com.xdandroid.hellocamera2;

import android.hardware.Camera;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.xdandroid.hellocamera2.app.App;
import com.xdandroid.hellocamera2.app.BaseCameraActivity;
import com.xdandroid.hellocamera2.camera.CameraTextureView;
import com.xdandroid.hellocamera2.camera.RectOnCamera;

import java.io.File;
import java.io.FileOutputStream;

import butterknife.BindView;

/**
 * Camera API. Android KitKat 及以前版本的 Android 使用 Camera API.
 */
public class Camera3Activity extends BaseCameraActivity {

    private static final String TAG=Camera3Activity.class.getSimpleName();

    @BindView(R.id.fl_camera_preview) FrameLayout flCameraPreview;
    @BindView(R.id.iv_camera_button) ImageView ivCameraButton;
    @BindView(R.id.tv_camera_hint) TextView tvCameraHint;
    @BindView(R.id.view_camera_dark0) View viewDark0;
    @BindView(R.id.view_camera_dark1) LinearLayout viewDark1;
    @BindView(R.id.framelayout) FrameLayout mFrameLayout;
    @BindView(R.id.camera_textureview)
    public CameraTextureView mCameraTextureView;
    @BindView(R.id.topview)
    public View mTopView;
//    @BindView(R.id.rectOnCamera)
//    public RectOnCamera mRectOnCamera;
//    @BindView(R.id.iv_camera_switch)
//    public ImageView mIvCameraSwitch;

    private File file;
    private long mMaxPicturePixels;

    /**
     * 预览的最佳尺寸是否已找到
     */
    private volatile boolean previewBestFound;

    /**
     * 拍照的最佳尺寸是否已找到
     */
    private volatile boolean pictureBestFound;

    /**
     * finish()是否已调用过
     */
    private volatile boolean finishCalled;

    private ToneGenerator tone;


    @Override
    protected int getContentViewResId() {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        return R.layout.activity_camera3;
    }

    @Override
    protected void preInitData() {
        Display display = ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay();
        Log.i("getViewTreeObserver","手机分辨率"+display.getWidth()+"*"+display.getHeight());
//        display.getOrientation();
        file = new File(getIntent().getStringExtra("file"));
        tvCameraHint.setText(getIntent().getStringExtra("hint"));
        if (getIntent().getBooleanExtra("hideBounds", false)) {
            viewDark0.setVisibility(View.INVISIBLE);
            viewDark1.setVisibility(View.INVISIBLE);
        }
        mMaxPicturePixels = getIntent().getIntExtra("maxPicturePixels", 1920 * 1080);//3840 * 2160
//        initCamera();
        ivCameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               mCameraTextureView.cameraInstance().getCameraDevice().takePicture(new Camera.ShutterCallback() {
                   @Override
                   public void onShutter() {
                       if(tone == null){
                           //发出提示用户的声音
                           tone = new ToneGenerator(AudioManager.STREAM_MUSIC,
                                   ToneGenerator.MAX_VOLUME);
                       }
                       tone.startTone(ToneGenerator.TONE_PROP_BEEP2);//TONE_PROP_NACK
                   }
               }, null, new Camera.PictureCallback() {
                   @Override
                   public void onPictureTaken(byte[] data, Camera camera) {
                       try {
                           if (file.exists()) file.delete();
                           FileOutputStream fos = new FileOutputStream(file);
                           fos.write(data);
                           try {
                               fos.close();
                           } catch (Exception ignored) {
                           }
                       } catch (Exception e) {
                           e.printStackTrace();
                       }
                       setResult(App.TAKE_PHOTO_SYSTEM, getIntent().putExtra("file", file.toString()));
                       finishCalled = true;
                       finish();
                   }
               });
            }
        });

        flCameraPreview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                mCameraTextureView.cameraInstance().focusAtPoint();
            }
        });
        mTopView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                int[] location = new int[2];
                mTopView.getLocationOnScreen(location);
                int x = location[0];
                int y = location[1];
                Log.i("getViewTreeObserver","mTopView左边坐标="+x+"y="+y+"width="+mTopView.getWidth()+"height="+mTopView.getHeight());
            }
        });

        mFrameLayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                int[] location = new int[2];
                mFrameLayout.getLocationOnScreen(location);
                int x = location[0];
                int y = location[1];
                Log.i("getViewTreeObserver","viewDark0x="+x+"y="+y+"width="+mFrameLayout.getWidth()+"height="+mFrameLayout.getHeight());
            }
        });
        mFrameLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mCameraTextureView.focusAtPoint(event.getX(), event.getY(), new Camera.AutoFocusCallback() {
                    @Override
                    public void onAutoFocus(boolean success, Camera camera) {
                        Log.i("focusAtPoint","onclick"+success);
                    }
                });
                return false;
            }
        });
//        mIvCameraSwitch.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                mCameraTextureView.switchCamera();
//            }
//        });
    }

    private void initCamera() {
    }

    private void initParams() {

    }

    private void initFocusParams(Camera.Parameters params) {

    }

    private void setParameters(Camera.Parameters params) {

    }

    @Override
    public void onBackPressed() {
        finishCalled = true;
        finish();
    }

    @Override
    protected void onPause() {
        super.onPause();
        releasCamera();//释放摄像头
        if (!finishCalled) finish();
    }

    public void releasCamera(){

    }
}
