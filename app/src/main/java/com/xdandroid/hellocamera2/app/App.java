package com.xdandroid.hellocamera2.app;

import android.app.*;
import android.content.Context;
import android.os.*;
import android.support.v7.app.AppCompatDelegate;

import com.facebook.drawee.backends.pipeline.*;
import com.facebook.imagepipeline.core.*;
import com.xdandroid.hellocamera2.util.SharedPreferencesUtil;

public class App extends Application {

    /**
     * 启动照相Intent的RequestCode.自定义相机.
     */
    public static final int TAKE_PHOTO_CUSTOM = 100;
    /**
     * 启动照相Intent的RequestCode.系统相机.
     */
    public static final int TAKE_PHOTO_SYSTEM = 200;
    /**
     * 主线程Handler.
     */
    public static Handler mHandler;
    public static App app;
    public static final String ISNIGHT = "isNight";

    @Override
    public void onCreate() {
        super.onCreate();
        app = this;
        mHandler = new Handler();
        Fresco.initialize(this, ImagePipelineConfig
                .newBuilder(this)
                .setDownsampleEnabled(true)
                .build());
        SharedPreferencesUtil.init(this,getPackageName()+"_preference", Context.MODE_MULTI_PROCESS);
        initNightMode();
    }
    public void initNightMode(){
        boolean isNight= SharedPreferencesUtil.getInstance().getBoolean(ISNIGHT,false);
        if(isNight){
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        }else{
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }
}
