package com.xdandroid.hellocamera2;

import android.hardware.Camera;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.jakewharton.rxbinding.view.RxView;
import com.xdandroid.hellocamera2.app.App;
import com.xdandroid.hellocamera2.app.BaseCameraActivity;
import com.xdandroid.hellocamera2.camera.CameraTextureView;
import com.xdandroid.hellocamera2.util.CameraUtils;
import com.xdandroid.hellocamera2.view.CameraPreview;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

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
    @BindView(R.id.camera_textureview)
    public CameraTextureView mCameraTextureView;
    @BindView(R.id.iv_camera_switch)
    public ImageView mIvCameraSwitch;

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


    @Override
    protected int getContentViewResId() {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        return R.layout.activity_camera3;
    }

    @Override
    protected void preInitData() {
        Display display = ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay();
        Log.i(TAG,"手机分辨率"+display.getWidth()+"*"+display.getHeight());
//        display.getOrientation();
        file = new File(getIntent().getStringExtra("file"));
        tvCameraHint.setText(getIntent().getStringExtra("hint"));
        if (getIntent().getBooleanExtra("hideBounds", false)) {
            viewDark0.setVisibility(View.INVISIBLE);
            viewDark1.setVisibility(View.INVISIBLE);
        }
        mMaxPicturePixels = getIntent().getIntExtra("maxPicturePixels", 1280 * 720);//3840 * 2160
        initCamera();
        ivCameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               mCameraTextureView.cameraInstance().getCameraDevice().takePicture(null, null, new Camera.PictureCallback() {
                   @Override
                   public void onPictureTaken(byte[] data, Camera camera) {
                       try {
                           if (file.exists()) file.delete();
                           FileOutputStream fos = new FileOutputStream(file);
                           fos.write(data);
                           try {
                               fos.close();
                           } catch (Exception ignored) {}
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
                mCameraTextureView.cameraInstance().focusAtPoint();
            }
        });
        mIvCameraSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCameraTextureView.switchCamera();
            }
        });
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
