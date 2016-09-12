package com.smartdevicelink.api.lockscreen;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.smartdevicelink.proxy.rpc.enums.LockScreenStatus;

import java.util.HashMap;

public class LockScreenManager implements LockScreenStatusListener{

    private static final Object UPDATE_LOCK = new Object();

    private static LockScreenActivity LOCK_SCREEN_INSTANCE = null;
    private static boolean ACTIVITY_RUNNING = false;

    private boolean isShownInOptional = true;
    private Application mApplication = null;

    private Class<? extends LockScreenActivity> mLockScreenClass;
    private SdlLockScreenListener mSdlLockScreenListener;

    private HashMap<String, LockScreenStatus> mLockScreenStatusMap;
    private LockScreenStatus mLastStatus = LockScreenStatus.OFF;

    private static LockScreenManager mInstance;

    private LockScreenManager(Application androidApplication,
                              Class<? extends LockScreenActivity> lockScreen,
                              boolean isShownInOptional){
        mApplication = androidApplication;
        mLockScreenClass = lockScreen;
        this.isShownInOptional = isShownInOptional;
        mLockScreenStatusMap = new HashMap<>();
    }

    private LockScreenManager(Application androidApplication, SdlLockScreenListener sdlLockScreenListener){
        mApplication = androidApplication;
        mSdlLockScreenListener = sdlLockScreenListener;
        mLockScreenClass = null;
        mLockScreenStatusMap = new HashMap<>();
    }

    @Nullable
    static public LockScreenManager getInstance(){
        synchronized (UPDATE_LOCK) {
            return mInstance;
        }
    }

    public static void initialize(Application app, SdlLockScreenListener listener){
        synchronized (UPDATE_LOCK) {
            if (mInstance == null) {
                mInstance = new LockScreenManager(app, listener);
            }
        }
    }

    public static void initialize(Application app, LockScreenConfig config){
        synchronized (UPDATE_LOCK) {
            if (mInstance == null) {
                mInstance = new LockScreenManager(app, config.lockScreen,
                        config.isShownInOptional);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                    mInstance.mApplication.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
                        @Override
                        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {

                        }

                        @Override
                        public void onActivityStarted(Activity activity) {

                        }

                        @Override
                        public void onActivityResumed(Activity activity) {
                            setActivityRunning(true);
                        }

                        @Override
                        public void onActivityPaused(Activity activity) {
                            setActivityRunning(false);
                        }

                        @Override
                        public void onActivityStopped(Activity activity) {

                        }

                        @Override
                        public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

                        }

                        @Override
                        public void onActivityDestroyed(Activity activity) {

                        }
                    });
                } else {
                    setActivityRunning(true);
                }
            }
        }
    }

    private static void setActivityRunning(boolean activityRunning){
        synchronized (UPDATE_LOCK) {
            ACTIVITY_RUNNING = activityRunning;

            if (mInstance != null) {
                mInstance.updateLockScreen();
            }
        }
    }

    static void setLockScreenInstance(LockScreenActivity lockScreenInstance){
        synchronized (UPDATE_LOCK) {
            LOCK_SCREEN_INSTANCE = lockScreenInstance;
            if (mInstance != null) {
                mInstance.updateLockScreen();
            }
        }
    }

    @Override
    public void onLockScreenStatus(String appId, LockScreenStatus status){
        mLockScreenStatusMap.put(appId, status);
        updateLockScreen();
    }

    private void updateLockScreen() {
        LockScreenStatus highestStatus = LockScreenStatus.OFF;

        for(LockScreenStatus lss: mLockScreenStatusMap.values()){
            if(lss == LockScreenStatus.REQUIRED){
                highestStatus = LockScreenStatus.REQUIRED;
                break;
            } else if(highestStatus == LockScreenStatus.OFF){
                highestStatus = lss;
            }
        }

        if(mLockScreenClass != null) {
            switch (highestStatus) {
                case OPTIONAL:
                    if (isShownInOptional) {
                        launchLockScreen();
                    } else {
                        clearLockScreen();
                    }
                    break;
                case REQUIRED:
                    launchLockScreen();
                    break;
                case OFF:
                    clearLockScreen();
                    break;
            }
        } else if (mSdlLockScreenListener != null && highestStatus != mLastStatus){
            mLastStatus = highestStatus;
            mSdlLockScreenListener.onLockScreenStatus(highestStatus);
        }
    }

    private void launchLockScreen(){
        if(LOCK_SCREEN_INSTANCE == null && ACTIVITY_RUNNING){
            Intent intent = new Intent(mApplication, mLockScreenClass);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                            Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS |
                            Intent.FLAG_ACTIVITY_NO_USER_ACTION);

            mApplication.startActivity(intent);
        }
    }

    private void clearLockScreen(){
        if(LOCK_SCREEN_INSTANCE != null){
            LOCK_SCREEN_INSTANCE.finish();
        }
    }

}
