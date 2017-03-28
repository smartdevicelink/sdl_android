package com.smartdevicelink.proxy;

import java.io.IOException;

import android.graphics.Bitmap;

import com.smartdevicelink.proxy.rpc.OnLockScreenStatus;
import com.smartdevicelink.proxy.rpc.enums.HMILevel;
import com.smartdevicelink.proxy.rpc.enums.LockScreenStatus;
import com.smartdevicelink.util.HttpUtils;

public class LockScreenManager {
	
    public interface OnLockScreenIconDownloadedListener{
        void onLockScreenIconDownloaded(Bitmap icon);
        void onLockScreenIconDownloadError(Exception e);
    }
    
    private Bitmap lockScreenIcon;
	private Boolean bUserSelected = false;
	private Boolean bDriverDistStatus = null;
	private HMILevel  hmiLevel = null;
	@SuppressWarnings("unused")
    private int iSessionID;

	public synchronized void setSessionID(int iVal)
	{
			iSessionID = iVal;
	}
	
	private synchronized void setUserSelectedStatus(boolean bVal)
	{
			bUserSelected = bVal;
	}	

	public synchronized void setDriverDistStatus(boolean bVal)
	{
			bDriverDistStatus = bVal;	
	}

	public synchronized void setHMILevel(HMILevel hmiVal)
	{		
		hmiLevel = hmiVal;
		
		if ( (hmiVal.equals(HMILevel.HMI_FULL)) || (hmiVal.equals(HMILevel.HMI_LIMITED)) )
			setUserSelectedStatus(true);
		else if (hmiVal.equals(HMILevel.HMI_NONE))
			setUserSelectedStatus(false);				
	}
	
	public synchronized OnLockScreenStatus getLockObj(/*int SessionID*/)
	{
		//int iSessionID = SessionID;
		OnLockScreenStatus myLock = new OnLockScreenStatus();
		myLock.setDriverDistractionStatus(bDriverDistStatus);
		myLock.setHMILevel(hmiLevel);
		myLock.setUserSelected(bUserSelected);
		myLock.setShowLockScreen(getLockScreenStatus());
		
		return myLock;
	}
	
	private synchronized LockScreenStatus getLockScreenStatus() 
	{
		
		if ( (hmiLevel == null) || (hmiLevel.equals(HMILevel.HMI_NONE)) )
		{
			return LockScreenStatus.OFF;
		}
		else if ( hmiLevel.equals(HMILevel.HMI_BACKGROUND) )
		{
			if (bDriverDistStatus == null)
			{
				//we don't have driver distraction, lockscreen is entirely based on userselection
				if (bUserSelected)
					return LockScreenStatus.REQUIRED;
				else
					return LockScreenStatus.OFF;
			}
			else if (bDriverDistStatus && bUserSelected)
			{
				return LockScreenStatus.REQUIRED;
			}
			else if (!bDriverDistStatus && bUserSelected)
			{
				return LockScreenStatus.OPTIONAL;
			}
			else
			{
				return LockScreenStatus.OFF;
			}
		}
		else if ( (hmiLevel.equals(HMILevel.HMI_FULL)) || (hmiLevel.equals(HMILevel.HMI_LIMITED)) )
		{
			if ( (bDriverDistStatus != null) && (!bDriverDistStatus) )
			{
				return LockScreenStatus.OPTIONAL;
			}
			else
				return LockScreenStatus.REQUIRED;
		}
		return LockScreenStatus.OFF;
	}

    public void downloadLockScreenIcon(final String url, final OnLockScreenIconDownloadedListener l){
        new Thread(new Runnable(){
            @Override
            public void run(){
                try{
                    lockScreenIcon = HttpUtils.downloadImage(url);
                    if(l != null){
                        l.onLockScreenIconDownloaded(lockScreenIcon);
                    }
                }catch(IOException e){
                    if(l != null){
                        l.onLockScreenIconDownloadError(e);
                    }
                }
            }
        }).start();
    }

    public Bitmap getLockScreenIcon(){
        return this.lockScreenIcon;
    }
}
