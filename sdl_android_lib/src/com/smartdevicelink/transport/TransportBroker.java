package com.smartdevicelink.transport;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.Parcelable;
import android.os.RemoteException;
import android.os.TransactionTooLargeException;
import android.util.Log;

import com.smartdevicelink.protocol.SdlPacket;
import com.smartdevicelink.transport.enums.TransportType;
import com.smartdevicelink.transport.utl.ByteAraryMessageAssembler;
import com.smartdevicelink.transport.utl.ByteArrayMessageSpliter;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


public class TransportBroker {
	
	private static final String TAG = "SdlTransportBroker";
	private final String WHERE_TO_REPLY_PREFIX	 = "com.sdl.android.";
	private Long appId = null;
	private String whereToReply = null;
	private Context currentContext = null;
	
	private final Object INIT_LOCK = new Object();
	
	private TransportType queuedOnTransportConnect = null;
	
	Messenger routerServiceMessenger = null;
	final Messenger clientMessenger;

	boolean isBound = false, registeredWithRouterService = false;
	private String routerPackage = null, routerClassName = null;
	private ComponentName routerService = null;
	
	
	private SdlPacket bufferedPacket = null;
	private ByteAraryMessageAssembler bufferedPayloadAssembler = null;
	
	private ServiceConnection routerConnection;
	
	private void initRouterConnection(){
		routerConnection= new ServiceConnection() {

			public void onServiceConnected(ComponentName className, IBinder service) {
				Log.d(TAG, "Bound to service " + className.toString());
				routerServiceMessenger = new Messenger(service);
				isBound = true;
				//So we just established our connection
				//Register with router service
				sendRegistrationMessage();    
			}

			public void onServiceDisconnected(ComponentName className) {
				Log.d(TAG, "Unbound from service " + className.getClassName());
				routerServiceMessenger = null;
				registeredWithRouterService = false;
				isBound = false;
				onHardwareDisconnected(null);
			}
		};
	}

    protected synchronized boolean sendMessageToRouterService(Message message){
    	return sendMessageToRouterService(message,0);
    }
    
    protected synchronized boolean sendMessageToRouterService(Message message, int retryCount){
    	if(message == null){
    		Log.w(TAG, "Attempted to send null message");
    		return false;
    	}
    	//Log.i(TAG, "Attempting to send message type - " + message.what);
    	if(isBound && routerServiceMessenger !=null){
    		if(registeredWithRouterService 
    				|| message.what == TransportConstants.ROUTER_REGISTER_CLIENT){ //We can send a message if we are registered or are attempting to register
    			try {
    				routerServiceMessenger.send(message);
    				return true;
    			} catch (RemoteException e) {
    				e.printStackTrace();
    				//Let's check to see if we should retry
    				if(e instanceof TransactionTooLargeException
    						|| (retryCount<5 && routerServiceMessenger.getBinder().isBinderAlive() && routerServiceMessenger.getBinder().pingBinder())){ //We probably just failed on a small transaction =\
    					try {
    						Thread.sleep(100);
    					} catch (InterruptedException e1) {
    						e1.printStackTrace();
    					}
    					return sendMessageToRouterService(message, retryCount++);
    				}else{
    					//DeadObject, time to kill our connection
    					Log.d(TAG, "Dead object while attempting to send packet");
    					routerServiceMessenger = null;
    					registeredWithRouterService = false;
    					isBound = false;
    					onHardwareDisconnected(null);
    					return false;
    				}
    			} catch (NullPointerException e){
					Log.d(TAG, "Null messenger while attempting to send packet"); // NPE, routerServiceMessenger is null
					routerServiceMessenger = null;
					registeredWithRouterService = false;
					isBound = false;
					onHardwareDisconnected(null);
					return false;
				}
    		}else{
    			Log.e(TAG, "Unable to send message to router service. Not registered.");
    			return false;
    		}
    	}else{
    		Log.e(TAG, "Unable to send message to router service. Not bound.");
    		return false;
    	}
    }
    
    
    /**
     * Handler of incoming messages from service.
     */
	static class ClientHandler extends Handler {
        ClassLoader loader;
        final WeakReference<TransportBroker> provider;

		 public ClientHandler(TransportBroker provider){
			 this.provider = new WeakReference<TransportBroker>(provider);
			 loader =  getClass().getClassLoader();
		 }
    	@Override
        public void handleMessage(Message msg) {
        	TransportBroker broker = provider.get();
        	if(broker == null){
        		Log.e(TAG, "Broker object null, unable to process message");
        		return;
        	}
        	Bundle bundle = msg.getData();

        	if(bundle!=null){
        		bundle.setClassLoader(loader);
        	}
        	//Log.d(TAG, "Bundle: "  + bundle.toString());
            /* DO NOT MOVE
             * This needs to be first to make sure we already know if we are attempting to enter legacy mode or not
             */
        	if(bundle !=null 
        			&& bundle.containsKey(TransportConstants.ENABLE_LEGACY_MODE_EXTRA)){
				boolean enableLegacy = bundle.getBoolean(TransportConstants.ENABLE_LEGACY_MODE_EXTRA, false);
				broker.enableLegacyMode(enableLegacy);
			}
            
        	//Find out what message we have and what to do with it
            switch (msg.what) {
            	case TransportConstants.ROUTER_REGISTER_CLIENT_RESPONSE:
            		switch(msg.arg1){
            		case TransportConstants.REGISTRATION_RESPONSE_SUCESS:
            			// yay! we have been registered. Now what?
            			broker.registeredWithRouterService = true;
            			if(bundle !=null){
            				if(bundle.containsKey(TransportConstants.HARDWARE_CONNECTED)){
            					if(bundle.containsKey(TransportConstants.CONNECTED_DEVICE_STRING_EXTRA_NAME)){
            						//Keep track if we actually get this
            					}
            					broker.onHardwareConnected(TransportType.valueOf(bundle.getString(TransportConstants.HARDWARE_CONNECTED)));
            				}
            				/*if(bundle.containsKey(TransportConstants.ROUTER_SERVICE_VERSION)){
            					//Keep track if we actually get this
            				}*/
            			}
            			break;
            		case TransportConstants.REGISTRATION_RESPONSE_DENIED_LEGACY_MODE_ENABLED:
            			Log.d(TAG, "Denied registration because router is in legacy mode" );
            			broker.registeredWithRouterService = false; 
            			broker.enableLegacyMode(true);
        				//We call this so we can start the process of legacy connection
        				//onHardwareDisconnected(TransportType.BLUETOOTH);
            			broker.onLegacyModeEnabled();
        				break;
            		default:
            			broker.registeredWithRouterService = false; 
            			Log.w(TAG, "Registration denied from router service. Reason - " + msg.arg1);
            			break;
            		};
            		
            	
            		break;
            	case TransportConstants.ROUTER_UNREGISTER_CLIENT_RESPONSE:
            		if(msg.arg1==TransportConstants.UNREGISTRATION_RESPONSE_SUCESS){
            			// We've been unregistered. Now what?
            			
            			
            		}else{ //We were denied our unregister request to the router service, let's see why
            			Log.w(TAG, "Unregister request denied from router service. Reason - " + msg.arg1);
            			//Do we care?
            		}
            	
            		break;
            	case TransportConstants.ROUTER_RECEIVED_PACKET:
            		//So the intent has a packet with it. PEFRECT! Let's send it through the library
        			int flags = bundle.getInt(TransportConstants.BYTES_TO_SEND_FLAGS, TransportConstants.BYTES_TO_SEND_FLAG_NONE);

        			if(bundle.containsKey(TransportConstants.FORMED_PACKET_EXTRA_NAME)){
            			Parcelable packet = bundle.getParcelable(TransportConstants.FORMED_PACKET_EXTRA_NAME);

            			if(flags == TransportConstants.BYTES_TO_SEND_FLAG_NONE){
            				if(packet!=null){ //Log.i(TAG, "received packet to process "+  packet.toString());
            				broker.onPacketReceived(packet);
            				}else{
            					Log.w(TAG, "Received null packet from router service, not passing along");
            				}
            			}else if(flags == TransportConstants.BYTES_TO_SEND_FLAG_SDL_PACKET_INCLUDED){
            				broker.bufferedPacket = (SdlPacket) packet;
            				if(broker.bufferedPayloadAssembler !=null){
            					broker.bufferedPayloadAssembler.close();
            					broker.bufferedPayloadAssembler = null;            					
            				}
            				
            				broker.bufferedPayloadAssembler = new ByteAraryMessageAssembler();
            				broker.bufferedPayloadAssembler.init();
            			}
            		}else if(bundle.containsKey(TransportConstants.BYTES_TO_SEND_EXTRA_NAME)){
            				//This should contain the payload
            				if(broker.bufferedPayloadAssembler!=null){
            					byte[] chunk = bundle.getByteArray(TransportConstants.BYTES_TO_SEND_EXTRA_NAME); 
            					if(!broker.bufferedPayloadAssembler.handleMessage(flags, chunk)){
            						//If there was a problem
            						Log.e(TAG, "Error handling bytes for split packet");
            					}
            					if(broker.bufferedPayloadAssembler.isFinished()){
            						broker.bufferedPacket.setPayload(broker.bufferedPayloadAssembler.getBytes());
            						
            						broker.bufferedPayloadAssembler.close();
            						broker.bufferedPayloadAssembler = null;
            						broker.onPacketReceived(broker.bufferedPacket);
            						broker.bufferedPacket = null;
            					}
            				}
            			//}
            		//}
            		}else{
            			Log.w(TAG, "Flase positive packet reception");
            		}
            		break;
            	case TransportConstants.HARDWARE_CONNECTION_EVENT:
        			if(bundle.containsKey(TransportConstants.HARDWARE_DISCONNECTED)){
        				//We should shut down, so call 
        				Log.d(TAG, "Hardware disconnected");
        				if(isLegacyModeEnabled()){
        					broker.onLegacyModeEnabled();
        				}else{
        					broker.onHardwareDisconnected(TransportType.valueOf(bundle.getString(TransportConstants.HARDWARE_DISCONNECTED)));
        				}
        				break;
        			}
        			
        			if(bundle.containsKey(TransportConstants.HARDWARE_CONNECTED)){
            			if(bundle!=null && bundle.containsKey(TransportConstants.CONNECTED_DEVICE_STRING_EXTRA_NAME)){
        					//Keep track if we actually get this
        				}
            			broker.onHardwareConnected(TransportType.valueOf(bundle.getString(TransportConstants.HARDWARE_CONNECTED)));
        				break;
        			}
            		break;
            	default:
                    super.handleMessage(msg);
            }   
            
        }
    }
		
		
		
		/***************************************************************************************************************************************
		***********************************************  Life Cycle  **************************************************************
		****************************************************************************************************************************************/	
			
		
		@SuppressLint("SimpleDateFormat")
		public TransportBroker(Context context, String appId, ComponentName service){
			synchronized(INIT_LOCK){
				clientMessenger = new Messenger(new ClientHandler(this));
				initRouterConnection();
				//So the user should have set the AppId, lets define where the intents need to be sent
				SimpleDateFormat s = new SimpleDateFormat("hhmmssss"); //So we have a time stamp of the event
				String timeStamp = s.format(new Date(System.currentTimeMillis()));
				if(whereToReply==null){
					if(appId==null){ //This should really just throw an error
						whereToReply = WHERE_TO_REPLY_PREFIX + "."+ timeStamp;
					}else{
						whereToReply = WHERE_TO_REPLY_PREFIX + appId +"."+ timeStamp; 
					}
				}
				this.appId = Long.valueOf(appId.concat(timeStamp));
				queuedOnTransportConnect = null;
				currentContext = context;
				//Log.d(TAG, "Registering our reply receiver: " + whereToReply);
				this.routerService = service;
			}
		}

		/**
		 * This beings the initial connection with the router service.
		 */
		public boolean start(){
			//Log.d(TAG, "Starting up transport broker for " + whereToReply);
			synchronized(INIT_LOCK){
				if(currentContext==null){
					throw new IllegalStateException("This instance can't be started since it's local reference of context is null. Ensure when suppling a context to the TransportBroker that it is valid");
				}
				if(routerConnection==null){
					initRouterConnection();
				}
				//Log.d(TAG, "Registering our reply receiver: " + whereToReply);
				if(!isBound){
					return registerWithRouterService();
				}else{
					return false;
				}
			}
		}
		
		public void resetSession(){
			synchronized(INIT_LOCK){
				unregisterWithRouterService();
				routerServiceMessenger = null;
				queuedOnTransportConnect = null;
				unBindFromRouterService();
			}
		}
		/**
		 * This method will end our communication with the router service. 
		 */
		public void stop(){
			//Log.d(TAG, "STOPPING transport broker for " + whereToReply);
			synchronized(INIT_LOCK){
				unregisterWithRouterService();
				unBindFromRouterService();
				routerServiceMessenger = null;
				queuedOnTransportConnect = null;
				currentContext = null;
				
			}
		}
		
		private void unBindFromRouterService(){
			try{
				if(getContext()!=null && routerConnection!=null){
					getContext().unbindService(routerConnection);
				}else{
					Log.w(TAG, "Unable to unbind from router service, context was null");
				}
				
			}catch(IllegalArgumentException e){
				//This is ok
			}
		}
		/***************************************************************************************************************************************
		***********************************************  Event Callbacks  **************************************************************
		****************************************************************************************************************************************/	
		
		
		public void onServiceUnregsiteredFromRouterService(int unregisterCode){
			queuedOnTransportConnect = null;
		}
		
		public void onHardwareDisconnected(TransportType type){
			synchronized(INIT_LOCK){
				unBindFromRouterService();
				routerServiceMessenger = null;
				routerConnection = null;
				queuedOnTransportConnect = null;
			}
		}
		
		public boolean onHardwareConnected(TransportType type){
			synchronized(INIT_LOCK){
				if(routerServiceMessenger==null){
					queuedOnTransportConnect = type;
					return false;
				}
				return true;
			}
			
		}
		
		public void onPacketReceived(Parcelable packet){
			
		}

		public void onLegacyModeEnabled(){
			
		}
		
		/**
		 * We want to check to see if the Router service is already up and running
		 * @param context
		 * @return
		 */
		private boolean isRouterServiceRunning(Context context){
			if(context==null){
				
				return false;
			}
			ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		    for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
		    	//We will check to see if it contains this name, should be pretty specific
		    	if ((service.service.getClassName()).toLowerCase(Locale.US).contains(SdlBroadcastReceiver.SDL_ROUTER_SERVICE_CLASS_NAME)) { 
		            this.routerClassName = service.service.getClassName();
		            this.routerPackage = service.service.getPackageName();
		    		return true;
		        }
		    }			
			return false;
		}
		
		
		public boolean sendPacketToRouterService(SdlPacket packet){ //We use ints because that is all that is supported by the outputstream class
			//Log.d(TAG,whereToReply + "Sending packet to router service");
			
			if(routerServiceMessenger==null){
				Log.d(TAG,whereToReply + " tried to send packet, but no where to send");
				return false;
			}
			if(packet == null 
					//|| offset<0 
					//|| count<0 
					){//|| count>(bytes.length-offset)){
				Log.w(TAG,whereToReply + "incorrect params supplied");
				return false;
			}
			byte[] bytes = packet.constructPacket();
			if(bytes.length<ByteArrayMessageSpliter.MAX_BINDER_SIZE){//Determine if this is under the packet length.
				Message message = Message.obtain(); //Do we need to always obtain new? or can we just swap bundles?
				message.what = TransportConstants.ROUTER_SEND_PACKET;
				Bundle bundle = new Bundle();
				bundle.putLong(TransportConstants.APP_ID_EXTRA, appId);
				bundle.putByteArray(TransportConstants.BYTES_TO_SEND_EXTRA_NAME, bytes); //Do we just change this to the args and objs
				bundle.putInt(TransportConstants.BYTES_TO_SEND_EXTRA_OFFSET, 0);
				bundle.putInt(TransportConstants.BYTES_TO_SEND_EXTRA_COUNT, bytes.length);
				bundle.putInt(TransportConstants.BYTES_TO_SEND_FLAGS, TransportConstants.BYTES_TO_SEND_FLAG_NONE);
				bundle.putInt(TransportConstants.PACKET_PRIORITY_COEFFICIENT, packet.getPriorityCoefficient());
				message.setData(bundle);
				
				sendMessageToRouterService(message);
				return true;
			}else{ //Message is too big for IPC transaction 
				//Log.w(TAG, "Message too big for single IPC transaction. Breaking apart. Size - " +  bytes.length);
				ByteArrayMessageSpliter splitter = new ByteArrayMessageSpliter(appId,TransportConstants.ROUTER_SEND_PACKET,bytes,packet.getPriorityCoefficient() );
				while(splitter.isActive()){
					sendMessageToRouterService(splitter.nextMessage());
				}
				return splitter.close();
			}
			
		}
		
		/**
		 * This registers this service with the router service
		 */
		private boolean registerWithRouterService(){ 
			if(getContext()==null){
				Log.e(TAG, "Context set to null, failing out");
				return false;
			}

			if(routerServiceMessenger!=null){
				Log.w(TAG, "Already registered with router service");
				return false;
			}
			//Make sure we know where to bind to
			if(this.routerService==null){ 
				if(!isRouterServiceRunning(getContext())){//We should be able to ignore this case because of the validation now
					Log.d(TAG,whereToReply + " found no router service. Shutting down.");
					this.onHardwareDisconnected(null);
					return false;
				}
			}else{//We were already told where to bind. This should be the case.
				this.routerClassName = this.routerService.getClassName();
				this.routerPackage = this.routerService.getPackageName();
			}
			
			if(!sendBindingIntent()){
				Log.e(TAG, "Something went wrong while trying to bind with the router service.");
				return false;
			}
			return true;

		}
		
		@SuppressLint("InlinedApi")
		private boolean sendBindingIntent(){
			if(this.routerPackage !=null && this.routerClassName !=null){
				Log.d(TAG, "Sending bind request to " + this.routerPackage + " - " + this.routerClassName);
				Intent bindingIntent = new Intent();
				bindingIntent.setClassName(this.routerPackage, this.routerClassName);//This sets an explicit intent
				//Quickly make sure it's just up and running
				getContext().startService(bindingIntent);
				bindingIntent.setAction( TransportConstants.BIND_REQUEST_TYPE_CLIENT);
				return getContext().bindService(bindingIntent, routerConnection, Context.BIND_AUTO_CREATE);
			}else{
				return false;
			}
		}
		
		private void sendRegistrationMessage(){
			Message msg = Message.obtain();
			msg.what = TransportConstants.ROUTER_REGISTER_CLIENT;
			msg.replyTo = this.clientMessenger;
			Bundle bundle = new Bundle();
			bundle.putLong(TransportConstants.APP_ID_EXTRA, appId);
			msg.setData(bundle);
			sendMessageToRouterService(msg);
		}
		
		private void unregisterWithRouterService(){
			Log.i(TAG, "Attempting to unregister with Sdl Router Service");
			if(isBound && routerServiceMessenger!=null){
				Message msg = Message.obtain();
				msg.what = TransportConstants.ROUTER_UNREGISTER_CLIENT;
				msg.replyTo = this.clientMessenger; //Including this in case this app isn't actually registered with the router service
				Bundle bundle = new Bundle();
				bundle.putLong(TransportConstants.APP_ID_EXTRA, appId);
				msg.setData(bundle);
				sendMessageToRouterService(msg);
			}else{
				Log.w(TAG, "Unable to unregister, not bound to router service");
			}
			
			routerServiceMessenger = null;
		}
		

		
		/**
		 * Since it doesn't always make sense to add another service, use this method to get
		 * the appropriate context that the rest of this class is using.
		 * @return The currently used context for this class
		 */
		private Context getContext(){
			return currentContext;
		}
		
		/***************************************************************************************************************************************
		***********************************************  LEGACY  *******************************************************************************
		****************************************************************************************************************************************/	
		/* 
		 * Due to old implementations of SDL/Applink, old versions can't support multiple sessions per RFCOMM channel. 
		 * This causes a race condition in the router service where only the first app registered will be able to communicate with the
		 * head unit. With this additional code, the router service will:
		 * 1) Acknowledge it's connected to an old system
		 * 2) d/c its bluetooth
		 * 3) Send a message to all clients connected that legacy mode is enabled
		 * 4) Each client spins up their own bluetooth RFCOMM listening channel
		 * 5) Head unit will need to query apps again
		 * 6) HU should then connect to each app by their own RFCOMM channel bypassing the router service
		 * 7) When the phone is D/C from the head unit the router service will reset and tell clients legacy mode is now off
		 */
		
		private static boolean legacyModeEnabled = false;
		private static Object LEGACY_LOCK = new Object();
		
		protected void enableLegacyMode(boolean enable){
			synchronized(LEGACY_LOCK){
				legacyModeEnabled = enable;
			}
		}
		protected static boolean isLegacyModeEnabled(){
			synchronized(LEGACY_LOCK){
				return legacyModeEnabled;
			}
			
		}
		
		/***************************************************************************************************************************************
		****************************************************  LEGACY END ***********************************************************************
		****************************************************************************************************************************************/
		
		/**
		 * Use this method to let the router service know that you are requesting a new session from the head unit. 
		 */
		public void requestNewSession(){
			Message msg = Message.obtain();
			msg.what = TransportConstants.ROUTER_REQUEST_NEW_SESSION;
			msg.replyTo = this.clientMessenger; //Including this in case this app isn't actually registered with the router service
			Bundle bundle = new Bundle();
			bundle.putLong(TransportConstants.APP_ID_EXTRA, appId);
			msg.setData(bundle);
			this.sendMessageToRouterService(msg);
		}
		
		public void removeSession(long sessionId){
			Message msg = Message.obtain();
			msg.what = TransportConstants.ROUTER_REMOVE_SESSION;
			msg.replyTo = this.clientMessenger; //Including this in case this app isn't actually registered with the router service
			Bundle bundle = new Bundle();
			bundle.putLong(TransportConstants.APP_ID_EXTRA, appId);
			bundle.putLong(TransportConstants.SESSION_ID_EXTRA, sessionId);
			msg.setData(bundle);
			this.sendMessageToRouterService(msg);
		}
}
