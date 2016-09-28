package com.smartdevicelink.api;

import android.app.Notification;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import java.util.HashMap;

public class SdlManager {

    private static final String TAG = SdlManager.class.getSimpleName();

    private static final Object SYNC_LOCK = new Object();

    private static SdlManager mInstance;
    private SdlConnectionService mSdlService;
    private Context mAndroidContext;
    private Notification mPersistentNotification;
    private boolean isBound = false;

    private HashMap<String, SdlApplicationConfig> mApplicationConfigRegistry;

    private boolean isPrepared = false;

    private SdlManager(){
        mApplicationConfigRegistry = new HashMap<>();
    }

    public static SdlManager getInstance(){
        synchronized (SYNC_LOCK) {
            if (mInstance == null) {
                mInstance = new SdlManager();
            }

            return mInstance;
        }
    }

    public boolean registerSdlApplication(SdlApplicationConfig config){
        synchronized (SYNC_LOCK) {
            mApplicationConfigRegistry.put(config.getAppId(), config);
            return true;
        }
    }

    public boolean unregisterSdlApplication(String appId){
        synchronized (SYNC_LOCK) {
            SdlApplicationConfig config = mApplicationConfigRegistry.remove(appId);
            return config == null;
        }
    }

    public void prepare(Context context, Notification persistentNotification){
        synchronized (SYNC_LOCK) {
            if (!isPrepared) {
                mPersistentNotification = persistentNotification;
            }
            prepare(context);
        }
    }

    public void prepare(Context context){
        synchronized (SYNC_LOCK) {
            if (!isPrepared) {
                mAndroidContext = context;
                bindConnectionService();
                isPrepared = true;
            } else {
                Log.w(TAG, "SdlManager.prepare(Context context) called when SdlManager is already prepared.\n" +
                        "No action taken.");
            }
        }
    }

    void sdlDisconnected(){
        Log.i(TAG, "SDL disconnected.");
        synchronized (SYNC_LOCK){
            if(isBound){
                mAndroidContext.unbindService(mSdlServiceConnection);
                isBound = false;
                mSdlService = null;
            }
        }
    }

    void onBluetoothConnected(){
        synchronized (SYNC_LOCK) {
            if (isPrepared) {
                if(mSdlService != null){
                    mSdlService.startSdlApplication(mApplicationConfigRegistry);
                } else {
                    bindConnectionService();
                }
            }
        }
    }

    private void bindConnectionService(){
        Log.i(TAG, "Binding ConnectionService ...");
        Intent sdlConnectionIntent = new Intent(mAndroidContext, SdlConnectionService.class);
        isBound = mAndroidContext.bindService(sdlConnectionIntent, mSdlServiceConnection, Context.BIND_AUTO_CREATE);
    }

    private ServiceConnection mSdlServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            synchronized (SYNC_LOCK) {
                Log.i(TAG, "ConnectionService bound.");
                mSdlService = ((SdlConnectionService.SdlConnectionBinder) service).getSdlConnectionService();
                mSdlService.setSdlManager(mInstance);
                mSdlService.setPersistentNotification(mPersistentNotification);
                mSdlService.startSdlApplication(mApplicationConfigRegistry);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            synchronized (SYNC_LOCK) {
                Log.i(TAG, "ConnectionService disconnected.");
                mSdlService = null;
            }
        }
    };

}
