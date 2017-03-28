package com.smartdevicelink.transport;

import java.lang.ref.WeakReference;

import com.smartdevicelink.util.AndroidTools;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

public class SdlRouterStatusProvider {

	private static final String TAG = "SdlRouterStateProvider";
		
	private Context context = null;
	private boolean isBound = false;
	ConnectedStatusCallback cb = null;
	Messenger routerServiceMessenger = null;
	private ComponentName routerService = null;
	private int flags = 0;

	final Messenger clientMessenger; 
	
	private ServiceConnection routerConnection= new ServiceConnection() {

		public void onServiceConnected(ComponentName className, IBinder service) {
			Log.d(TAG, "Bound to service " + className.toString());
			routerServiceMessenger = new Messenger(service);
			isBound = true;
			//So we just established our connection
			//Register with router service
			Message msg = Message.obtain();
			msg.what = TransportConstants.ROUTER_STATUS_CONNECTED_STATE_REQUEST;
			msg.arg1 = flags;
			msg.replyTo = clientMessenger;
			try {
				routerServiceMessenger.send(msg);
			} catch (RemoteException e) {
				e.printStackTrace();
				if(cb!=null){
					cb.onConnectionStatusUpdate(false, routerService, context);
				}
			}			
		}

		public void onServiceDisconnected(ComponentName className) {
			Log.d(TAG, "UN-Bound from service " + className.getClassName());
			routerServiceMessenger = null;
			isBound = false;
		}
	};

	public SdlRouterStatusProvider(Context context, ComponentName service, ConnectedStatusCallback callback){
		if(context == null || service == null || callback == null){
			throw new IllegalStateException("Supplied params are not correct. Context == null? "+ (context==null) + " ComponentName == null? " + (service == null) + " ConnectedStatusListener == null? " + callback);
		}
		this.context = context;
		this.routerService = service;
		this.cb = callback;
		this.clientMessenger = new Messenger(new ClientHandler(this));

	}
	public void setFlags(int flags){
		this.flags = flags;
	}
	public void checkIsConnected(){
		if(!AndroidTools.isServiceExported(context,routerService) || !bindToService()){
			//We are unable to bind to service
			cb.onConnectionStatusUpdate(false, routerService, context);
			unBindFromService();
		}
	}
	
	public void cancel(){
		if(isBound){
			unBindFromService();
		}
	}

	private boolean bindToService(){
		if(isBound){
			return true;
		}
		if(clientMessenger == null){
			return false;
		}
		Intent bindingIntent = new Intent();
		bindingIntent.setClassName(this.routerService.getPackageName(), this.routerService.getClassName());//This sets an explicit intent
		//Quickly make sure it's just up and running
		context.startService(bindingIntent);
		bindingIntent.setAction( TransportConstants.BIND_REQUEST_TYPE_STATUS);
		return context.bindService(bindingIntent, routerConnection, Context.BIND_AUTO_CREATE);
	}
	
	private void unBindFromService(){
		try{
			if(context!=null && routerConnection!=null){
				context.unbindService(routerConnection);
			}else{
				Log.w(TAG, "Unable to unbind from router service, context was null");
			}
			
		}catch(IllegalArgumentException e){
			//This is ok
		}
	}
	
	private void handleRouterStatusConnectedResponse(int connectedStatus){
		if(cb!=null){
			  cb.onConnectionStatusUpdate(connectedStatus == 1, routerService,context);
		  }
		  unBindFromService();
		  routerServiceMessenger =null;
	}
	
	static class ClientHandler extends Handler {
		final WeakReference<SdlRouterStatusProvider> provider;

		 public ClientHandler(SdlRouterStatusProvider provider){
			 this.provider = new WeakReference<SdlRouterStatusProvider>(provider);
		 }
		 
    	@Override
        public void handleMessage(Message msg) {
    		if(provider.get()==null){
    			return; 
    		}
    		  switch (msg.what) {
    		  case TransportConstants.ROUTER_STATUS_CONNECTED_STATE_RESPONSE:
    			  provider.get().handleRouterStatusConnectedResponse(msg.arg1);
    			  break;
    		  default:
    			  break;
    		  }
    	}
	};
	
	public interface ConnectedStatusCallback{
		void onConnectionStatusUpdate(boolean connected, ComponentName service, Context context);
	}
	
}
