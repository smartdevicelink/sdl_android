package com.smartdevicelink.managers;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.smartdevicelink.exception.SdlException;
import com.smartdevicelink.managers.audio.AudioStreamManager;
import com.smartdevicelink.managers.file.FileManager;
import com.smartdevicelink.managers.file.filetypes.SdlArtwork;
import com.smartdevicelink.managers.lockscreen.LockScreenConfig;
import com.smartdevicelink.managers.lockscreen.LockScreenManager;
import com.smartdevicelink.managers.permission.PermissionManager;
import com.smartdevicelink.managers.screen.ScreenManager;
import com.smartdevicelink.managers.video.VideoStreamManager;
import com.smartdevicelink.protocol.enums.FunctionID;
import com.smartdevicelink.protocol.enums.SessionType;
import com.smartdevicelink.proxy.RPCMessage;
import com.smartdevicelink.proxy.RPCRequest;
import com.smartdevicelink.proxy.SdlProxyBase;
import com.smartdevicelink.proxy.SystemCapabilityManager;
import com.smartdevicelink.proxy.callbacks.OnServiceEnded;
import com.smartdevicelink.proxy.callbacks.OnServiceNACKed;
import com.smartdevicelink.proxy.interfaces.IAudioStreamListener;
import com.smartdevicelink.proxy.interfaces.ISdl;
import com.smartdevicelink.proxy.interfaces.ISdlServiceListener;
import com.smartdevicelink.proxy.interfaces.IVideoStreamListener;
import com.smartdevicelink.proxy.interfaces.OnSystemCapabilityListener;
import com.smartdevicelink.proxy.rpc.RegisterAppInterfaceResponse;
import com.smartdevicelink.proxy.rpc.SdlMsgVersion;
import com.smartdevicelink.proxy.rpc.SetAppIcon;
import com.smartdevicelink.proxy.rpc.TTSChunk;
import com.smartdevicelink.proxy.rpc.TemplateColorScheme;
import com.smartdevicelink.proxy.rpc.enums.AppHMIType;
import com.smartdevicelink.proxy.rpc.enums.Language;
import com.smartdevicelink.proxy.rpc.enums.SdlDisconnectedReason;
import com.smartdevicelink.proxy.rpc.enums.SystemCapabilityType;
import com.smartdevicelink.proxy.rpc.listeners.OnMultipleRequestListener;
import com.smartdevicelink.proxy.rpc.listeners.OnRPCListener;
import com.smartdevicelink.proxy.rpc.listeners.OnRPCNotificationListener;
import com.smartdevicelink.security.SdlSecurityBase;
import com.smartdevicelink.streaming.audio.AudioStreamingCodec;
import com.smartdevicelink.streaming.audio.AudioStreamingParams;
import com.smartdevicelink.streaming.video.VideoStreamingParameters;
import com.smartdevicelink.transport.BaseTransportConfig;
import com.smartdevicelink.transport.MultiplexTransportConfig;
import com.smartdevicelink.transport.enums.TransportType;
import com.smartdevicelink.transport.utl.TransportRecord;
import com.smartdevicelink.util.DebugTool;
import com.smartdevicelink.util.Localization;
import com.smartdevicelink.util.Version;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Vector;

/**
 * <strong>SDLManager</strong> <br>
 *
 * This is the main point of contact between an application and SDL <br>
 *
 * It is broken down to these areas: <br>
 *
 * 1. SDLManagerBuilder <br>
 * 2. ISdl Interface along with its overridden methods - This can be passed into attached managers <br>
 * 3. Sending Requests <br>
 * 4. Helper methods
 */
public class SdlManager{
	private static final String TAG = "SdlManager";
	private SdlProxyBase proxy;
	private String appId, appName, shortAppName;
	private boolean isMediaApp;
	private Language hmiLanguage;
	private SdlArtwork appIcon;
	private Vector<AppHMIType> hmiTypes;
	private BaseTransportConfig transport;
	private Context context;
	private Vector<String> vrSynonyms;
	private Vector<TTSChunk> ttsChunks;
	private TemplateColorScheme dayColorScheme, nightColorScheme;
	private SdlManagerListener managerListener;
	private int state = -1;
	private List<Class<? extends SdlSecurityBase>> sdlSecList;
	private LockScreenConfig lockScreenConfig;
	private Localization localization;
	private final Object STATE_LOCK = new Object();


	// Managers
	private PermissionManager permissionManager;
	private FileManager fileManager;
	private LockScreenManager lockScreenManager;
	private ScreenManager screenManager;
	private VideoStreamManager videoStreamManager;
	private AudioStreamManager audioStreamManager;


	// Initialize proxyBridge with anonymous lifecycleListener
	private final ProxyBridge proxyBridge= new ProxyBridge(new ProxyBridge.LifecycleListener() {
		@Override
		public void onProxyConnected() {
			DebugTool.logInfo("Proxy is connected. Now initializing.");
			initialize();
		}

		@Override
		public void onProxyClosed(String info, Exception e, SdlDisconnectedReason reason){
			dispose();
		}

		@Override
		public void onServiceEnded(OnServiceEnded serviceEnded){

		}

		@Override
		public void onServiceNACKed(OnServiceNACKed serviceNACKed){

		}

		@Override
		public void onError(String info, Exception e){

		}
	});

	// Sub manager listener
	private final CompletionListener subManagerListener = new CompletionListener() {
		@Override
		public synchronized void onComplete(boolean success) {
			if(!success){
				Log.e(TAG, "Sub manager failed to initialize");
			}
			checkState();
		}
	};

	void checkState() {
		if (permissionManager != null && fileManager != null && screenManager != null && (!lockScreenConfig.isEnabled() || lockScreenManager != null)) {
			if (permissionManager.getState() == BaseSubManager.READY && fileManager.getState() == BaseSubManager.READY && screenManager.getState() == BaseSubManager.READY && (!lockScreenConfig.isEnabled() || lockScreenManager.getState() == BaseSubManager.READY)) {
				DebugTool.logInfo("Starting sdl manager, all sub managers are in ready state");
				transitionToState(BaseSubManager.READY);
				notifyDevListener(null);
				onReady();
			} else if (permissionManager.getState() == BaseSubManager.ERROR && fileManager.getState() == BaseSubManager.ERROR && screenManager.getState() == BaseSubManager.ERROR && (!lockScreenConfig.isEnabled() || lockScreenManager.getState() == BaseSubManager.ERROR)) {
				String info = "ERROR starting sdl manager, all sub managers are in error state";
				Log.e(TAG, info);
				transitionToState(BaseSubManager.ERROR);
				notifyDevListener(info);
			} else if (permissionManager.getState() == BaseSubManager.SETTING_UP || fileManager.getState() == BaseSubManager.SETTING_UP || screenManager.getState() == BaseSubManager.SETTING_UP || (lockScreenConfig.isEnabled() && lockScreenManager != null && lockScreenManager.getState() == BaseSubManager.SETTING_UP)) {
				DebugTool.logInfo("SETTING UP sdl manager, some sub managers are still setting up");
				transitionToState(BaseSubManager.SETTING_UP);
				// No need to notify developer here!
			} else {
				Log.w(TAG, "LIMITED starting sdl manager, some sub managers are in error or limited state and the others finished setting up");
				transitionToState(BaseSubManager.LIMITED);
				notifyDevListener(null);
				onReady();
			}
		} else {
			// We should never be here, but somehow one of the sub-sub managers is null
			String info = "ERROR one of the sdl sub managers is null";
			Log.e(TAG, info);
			transitionToState(BaseSubManager.ERROR);
			notifyDevListener(info);
		}
	}

	private void notifyDevListener(String info) {
		if (managerListener != null) {
			if (getState() == BaseSubManager.ERROR){
				managerListener.onError(info, null);
			} else {
				managerListener.onStart();
			}
		}
	}

	private void onReady(){
		// Set the app icon
		if (SdlManager.this.appIcon != null && SdlManager.this.appIcon.getName() != null) {
			if (fileManager != null && fileManager.getState() == BaseSubManager.READY && !fileManager.hasUploadedFile(SdlManager.this.appIcon)) {
				fileManager.uploadArtwork(SdlManager.this.appIcon, new CompletionListener() {
					@Override
					public void onComplete(boolean success) {
						if (success) {
							SetAppIcon msg = new SetAppIcon(SdlManager.this.appIcon.getName());
							_internalInterface.sendRPCRequest(msg);
						}
					}
				});
			} else {
				SetAppIcon msg = new SetAppIcon(SdlManager.this.appIcon.getName());
				_internalInterface.sendRPCRequest(msg);
			}
		}
	}

	protected void initialize(){
		// Instantiate sub managers
		this.permissionManager = new PermissionManager(_internalInterface);
		this.fileManager = new FileManager(_internalInterface, context);
		if (lockScreenConfig.isEnabled()) {
			this.lockScreenManager = new LockScreenManager(lockScreenConfig, context, _internalInterface);
		}
		this.screenManager = new ScreenManager(_internalInterface, this.fileManager);
		if(getAppTypes().contains(AppHMIType.NAVIGATION) || getAppTypes().contains(AppHMIType.PROJECTION)){
			this.videoStreamManager = new VideoStreamManager(_internalInterface);
		} else {
			this.videoStreamManager = null;
		}
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN
				&& (getAppTypes().contains(AppHMIType.NAVIGATION) || getAppTypes().contains(AppHMIType.PROJECTION)) ) {
			this.audioStreamManager = new AudioStreamManager(_internalInterface, context);
		} else {
			this.audioStreamManager = null;
		}

		// Start sub managers
		this.permissionManager.start(subManagerListener);
		this.fileManager.start(subManagerListener);
		if (lockScreenConfig.isEnabled()){
			this.lockScreenManager.start(subManagerListener);
		}
		this.screenManager.start(subManagerListener);
	}

	/**
	 * Get the current state for the SdlManager
	 * @return int value that represents the current state
	 * @see BaseSubManager
	 */
	public int getState() {
		synchronized (STATE_LOCK) {
			return state;
		}
	}

	protected void transitionToState(int state) {
		synchronized (STATE_LOCK) {
			this.state = state;
		}
	}

	@SuppressLint("NewApi")
	public void dispose() {
		if (this.permissionManager != null) {
			this.permissionManager.dispose();
		}

		if (this.fileManager != null) {
			this.fileManager.dispose();
		}

		if (this.lockScreenManager != null) {
			this.lockScreenManager.dispose();
		}

		if (this.screenManager != null) {
			this.screenManager.dispose();
		}

		if(this.videoStreamManager != null) {
			this.videoStreamManager.dispose();
		}

		// SuppressLint("NewApi") is used because audioStreamManager is only available on android >= jelly bean
		if (this.audioStreamManager != null) {
			this.audioStreamManager.dispose();
		}

		if(managerListener != null){
			managerListener.onDestroy();
			managerListener = null;
		}
	}


	private void checkSdlManagerState(){
		if (getState() != BaseSubManager.READY && getState() != BaseSubManager.LIMITED){
			Log.e(TAG, "SdlManager is not ready for use, be sure to initialize with start() method, implement callback, and use SubManagers in the SdlManager's callback");
		}
	}

	// MANAGER GETTERS

	/**
	 * Gets the PermissionManager. <br>
	 * <strong>Note: PermissionManager should be used only after SdlManager.start() CompletionListener callback is completed successfully.</strong>
	 * @return a PermissionManager object
	 */
	public PermissionManager getPermissionManager() {
		if (permissionManager.getState() != BaseSubManager.READY && permissionManager.getState() != BaseSubManager.LIMITED){
			Log.e(TAG,"PermissionManager should not be accessed because it is not in READY/LIMITED state");
		}
		checkSdlManagerState();
		return permissionManager;
	}

	/**
	 * Gets the FileManager. <br>
	 * <strong>Note: FileManager should be used only after SdlManager.start() CompletionListener callback is completed successfully.</strong>
	 * @return a FileManager object
	 */
	public FileManager getFileManager() {
		if (fileManager.getState() != BaseSubManager.READY && fileManager.getState() != BaseSubManager.LIMITED){
			Log.e(TAG, "FileManager should not be accessed because it is not in READY/LIMITED state");
		}
		checkSdlManagerState();
		return fileManager;
	}

    /**
     * Gets the VideoStreamManager. <br>
	 * The VideoStreamManager returned will only be not null if the registered app type is
	 * either NAVIGATION or PROJECTION. Once the VideoStreamManager is retrieved, its start()
	 * method will need to be called before use.
     * <br><br><strong>Note: VideoStreamManager should be used only after SdlManager.start() CompletionListener callback is completed successfully.</strong>
     * @return a VideoStreamManager object attached to this SdlManager instance
     */
	public @Nullable
	VideoStreamManager getVideoStreamManager() {
		checkSdlManagerState();
		return videoStreamManager;
	}

    /**
     * Gets the AudioStreamManager. <br>
	 * The AudioStreamManager returned will only be not null if the registered app type is
	 * either NAVIGATION or PROJECTION. Once the AudioStreamManager is retrieved, its start()
	 * method will need to be called before use.
     * <br><strong>Note: AudioStreamManager should be used only after SdlManager.start() CompletionListener callback is completed successfully.</strong>
     * @return a AudioStreamManager object
     */
	public @Nullable AudioStreamManager getAudioStreamManager() {
		checkSdlManagerState();
		return audioStreamManager;
	}

	/**
	 * Gets the ScreenManager. <br>
	 * <strong>Note: ScreenManager should be used only after SdlManager.start() CompletionListener callback is completed successfully.</strong>
	 * @return a ScreenManager object
	 */
	public ScreenManager getScreenManager() {
		if (screenManager.getState() != BaseSubManager.READY && screenManager.getState() != BaseSubManager.LIMITED){
			Log.e(TAG, "ScreenManager should not be accessed because it is not in READY/LIMITED state");
		}
		checkSdlManagerState();
		return screenManager;
	}

	public @NonNull
	Localization getLocalization() {
		return localization;
	}

	/**
	 * Gets the LockScreenManager. <br>
	 * <strong>Note: LockScreenManager should be used only after SdlManager.start() CompletionListener callback is completed successfully.</strong>
	 * @return a LockScreenManager object
	 */
	public LockScreenManager getLockScreenManager() {
		if (lockScreenManager.getState() != BaseSubManager.READY && lockScreenManager.getState() != BaseSubManager.LIMITED){
			Log.e(TAG, "LockScreenManager should not be accessed because it is not in READY/LIMITED state");
		}
		checkSdlManagerState();
		return lockScreenManager;
	}

	/**
	 * Gets the SystemCapabilityManager. <br>
	 * <strong>Note: SystemCapabilityManager should be used only after SdlManager.start() CompletionListener callback is completed successfully.</strong>
	 * @return a SystemCapabilityManager object
	 */
	public SystemCapabilityManager getSystemCapabilityManager(){
		return proxy.getSystemCapabilityManager();
	}

	/**
	 * Method to retrieve the RegisterAppInterface Response message that was sent back from the
	 * module. It contains various attributes about the connected module and can be used to adapt
	 * to different module types and their supported features.
	 *
	 * @return RegisterAppInterfaceResponse received from the module or null if the app has not yet
	 * registered with the module.
	 */
	public RegisterAppInterfaceResponse getRegisterAppInterfaceResponse(){
		if(proxy != null){
			return proxy.getRegisterAppInterfaceResponse();
		}
		return null;
	}

	// PROTECTED GETTERS
	protected String getAppName() { return appName; }

	protected String getAppId() { return appId; }

	protected String getShortAppName() { return shortAppName; }

	protected Language getHmiLanguage() { return hmiLanguage; }

	protected TemplateColorScheme getDayColorScheme() { return dayColorScheme; }

	protected TemplateColorScheme getNightColorScheme() { return nightColorScheme; }

	protected Vector<AppHMIType> getAppTypes() { return hmiTypes; }

	protected Vector<String> getVrSynonyms() { return vrSynonyms; }

	protected Vector<TTSChunk> getTtsChunks() { return ttsChunks; }

	protected BaseTransportConfig getTransport() { return transport; }

	protected LockScreenConfig getLockScreenConfig() { return lockScreenConfig; }

	// SENDING REQUESTS

	/**
	 * Send RPC Message <br>
	 * <strong>Note: Only takes type of RPCRequest for now, notifications and responses will be thrown out</strong>
	 * @param message RPCMessage
	 */
	public void sendRPC(RPCMessage message) {

		if (message instanceof RPCRequest){
			try{
				proxy.sendRPCRequest((RPCRequest)message);
			}catch (SdlException exception){
				handleSdlException(exception);
			}
		}
	}

	/**
	 * Takes a list of RPCMessages and sends it to SDL in a synchronous fashion. Responses are captured through callback on OnMultipleRequestListener.
	 * For sending requests asynchronously, use sendRequests <br>
	 *
	 * <strong>NOTE: This will override any listeners on individual RPCs</strong><br>
	 *
	 * <strong>ADDITIONAL NOTE: This only takes the type of RPCRequest for now, notifications and responses will be thrown out</strong>
	 *
	 * @param rpcs is the list of RPCMessages being sent
	 * @param listener listener for updates and completions
	 */
	public void sendSequentialRPCs(final List<? extends RPCMessage> rpcs, final OnMultipleRequestListener listener){

		List<RPCRequest> rpcRequestList = new ArrayList<>();
		for (int i = 0; i < rpcs.size(); i++) {
			if (rpcs.get(i) instanceof RPCRequest){
				rpcRequestList.add((RPCRequest)rpcs.get(i));
			}
		}

		if (rpcRequestList.size() > 0) {
			try{
				proxy.sendSequentialRequests(rpcRequestList, listener);
			}catch (SdlException exception){
				handleSdlException(exception);
			}
		}
	}

	/**
	 * Takes a list of RPCMessages and sends it to SDL. Responses are captured through callback on OnMultipleRequestListener.
	 * For sending requests synchronously, use sendSequentialRPCs <br>
	 *
	 * <strong>NOTE: This will override any listeners on individual RPCs</strong> <br>
	 *
	 * <strong>ADDITIONAL NOTE: This only takes the type of RPCRequest for now, notifications and responses will be thrown out</strong>
	 *
	 * @param rpcs is the list of RPCMessages being sent
	 * @param listener listener for updates and completions
	 */
	public void sendRPCs(List<? extends RPCMessage> rpcs, final OnMultipleRequestListener listener) {

		List<RPCRequest> rpcRequestList = new ArrayList<>();
		for (int i = 0; i < rpcs.size(); i++) {
			if (rpcs.get(i) instanceof RPCRequest){
				rpcRequestList.add((RPCRequest)rpcs.get(i));
			}
		}

		if (rpcRequestList.size() > 0) {
			try{
				proxy.sendRequests(rpcRequestList, listener);
			}catch (SdlException exception){
				handleSdlException(exception);
			}
		}
	}

	private void handleSdlException(SdlException exception){
		if(exception != null){
			DebugTool.logError("Caught SdlException: " + exception.getSdlExceptionCause());
			// In the future this should handle logic to dispose the manager if it is an unrecoverable error
		}else{
			DebugTool.logError("Caught SdlException" );
		}
	}

	/**
	 * Add an OnRPCNotificationListener
	 * @param listener listener that will be called when a notification is received
	 */
	public void addOnRPCNotificationListener(FunctionID notificationId, OnRPCNotificationListener listener){
		proxy.addOnRPCNotificationListener(notificationId,listener);
	}

	/**
	 * Remove an OnRPCNotificationListener
	 * @param listener listener that was previously added
	 */
	public void removeOnRPCNotificationListener(FunctionID notificationId, OnRPCNotificationListener listener){
		proxy.removeOnRPCNotificationListener(notificationId, listener);
	}

	// LIFECYCLE / OTHER

	// STARTUP

	/**
	 * Starts up a SdlManager, and calls provided callback called once all BaseSubManagers are done setting up
	 */
	@SuppressWarnings("unchecked")
	public void start(){
		if (proxy == null) {
			try {
				if(transport!= null  && transport.getTransportType() == TransportType.MULTIPLEX){
					//Do the thing
					MultiplexTransportConfig multiplexTransportConfig = (MultiplexTransportConfig)(transport);
					final MultiplexTransportConfig.TransportListener devListener = multiplexTransportConfig.getTransportListener();
					multiplexTransportConfig.setTransportListener(new MultiplexTransportConfig.TransportListener() {
						@Override
						public void onTransportEvent(List<TransportRecord> connectedTransports, boolean audioStreamTransportAvail, boolean videoStreamTransportAvail) {

							//Pass to submanagers that need it
							if(videoStreamManager != null){
								videoStreamManager.handleTransportUpdated(connectedTransports, audioStreamTransportAvail, videoStreamTransportAvail);
							}

							if(audioStreamManager != null){
								audioStreamManager.handleTransportUpdated(connectedTransports, audioStreamTransportAvail, videoStreamTransportAvail);
							}
							//If the developer supplied a listener to start, it is time to call that
							if(devListener != null){
								devListener.onTransportEvent(connectedTransports,audioStreamTransportAvail,videoStreamTransportAvail);
							}
						}
					});
				}

				proxy = new SdlProxyBase(proxyBridge, context, appName, shortAppName, isMediaApp, hmiLanguage,
						hmiLanguage, hmiTypes, appId, transport, vrSynonyms, ttsChunks, dayColorScheme,
						nightColorScheme) {};
				if (sdlSecList != null && !sdlSecList.isEmpty()) {
					proxy.setSdlSecurityClassList(sdlSecList);
				}
			} catch (SdlException e) {
				if (managerListener != null) {
					managerListener.onError("Unable to start manager", e);
				}
			}
		}
	}

	protected void setProxy(SdlProxyBase proxy){
		this.proxy = proxy;
	}

	// INTERNAL INTERFACE
	private ISdl _internalInterface = new ISdl() {
		@Override
		public void start() {
			try{
				proxy.initializeProxy();
			}catch (SdlException e){
				e.printStackTrace();
			}
		}

		@Override
		public void stop() {
			try{
				proxy.dispose();
			}catch (SdlException e){
				e.printStackTrace();
			}
		}

		@Override
		public boolean isConnected() {
			return proxy.getIsConnected();
		}

		@Override
		public void addServiceListener(SessionType serviceType, ISdlServiceListener sdlServiceListener) {
			proxy.addServiceListener(serviceType,sdlServiceListener);
		}

		@Override
		public void removeServiceListener(SessionType serviceType, ISdlServiceListener sdlServiceListener) {
			proxy.removeServiceListener(serviceType,sdlServiceListener);
		}

		@Override
		public void startVideoService(VideoStreamingParameters parameters, boolean encrypted) {
			if(proxy.getIsConnected()){
				proxy.startVideoStream(encrypted,parameters);
			}
		}

		@Override
		public IVideoStreamListener startVideoStream(boolean isEncrypted, VideoStreamingParameters parameters){
			return proxy.startVideoStream(isEncrypted, parameters);
		}

		@Override
		public void stopVideoService() {
			if(proxy.getIsConnected()){
				proxy.endVideoStream();
			}
		}

		@Override
		public void startAudioService(boolean isEncrypted, AudioStreamingCodec codec,
		                              AudioStreamingParams params) {
			if(proxy.getIsConnected()){
				proxy.startAudioStream(isEncrypted, codec, params);
			}
		}

		@Override
		public void startAudioService(boolean encrypted) {
			if(isConnected()){
				proxy.startService(SessionType.PCM, encrypted);
			}
		}

		@Override
		public IAudioStreamListener startAudioStream(boolean isEncrypted, AudioStreamingCodec codec,
		                                             AudioStreamingParams params) {
			return proxy.startAudioStream(isEncrypted, codec, params);
		}

		@Override
		public void stopAudioService() {
			if(proxy.getIsConnected()){
				proxy.endAudioStream();
			}
		}

		@Override
		public void sendRPCRequest(RPCRequest message){
			try {
				proxy.sendRPCRequest(message);
			} catch (SdlException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void sendRequests(List<? extends RPCRequest> rpcs, OnMultipleRequestListener listener) {
			try {
				proxy.sendRequests(rpcs, listener);
			} catch (SdlException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void addOnRPCNotificationListener(FunctionID notificationId, OnRPCNotificationListener listener) {
			proxy.addOnRPCNotificationListener(notificationId,listener);
		}

		@Override
		public boolean removeOnRPCNotificationListener(FunctionID notificationId, OnRPCNotificationListener listener) {
			return proxy.removeOnRPCNotificationListener(notificationId,listener);
		}

		@Override
		public void addOnRPCListener(final FunctionID responseId, final OnRPCListener listener) {
			proxyBridge.addRpcListener(responseId, listener);
		}

		@Override
		public boolean removeOnRPCListener(final FunctionID responseId, final OnRPCListener listener) {
			return proxyBridge.removeOnRPCListener(responseId, listener);
		}

		@Override
		public Object getCapability(SystemCapabilityType systemCapabilityType){
			return proxy.getCapability(systemCapabilityType);
		}

		@Override
		public void getCapability(SystemCapabilityType systemCapabilityType, OnSystemCapabilityListener scListener) {
			proxy.getCapability(systemCapabilityType, scListener);
		}

		@Override
		public boolean isCapabilitySupported(SystemCapabilityType systemCapabilityType){
			return proxy.isCapabilitySupported(systemCapabilityType);
		}

		@Override
		public void addOnSystemCapabilityListener(SystemCapabilityType systemCapabilityType, OnSystemCapabilityListener listener) {
			proxy.addOnSystemCapabilityListener(systemCapabilityType, listener);
		}

		@Override
		public boolean removeOnSystemCapabilityListener(SystemCapabilityType systemCapabilityType, OnSystemCapabilityListener listener) {
			return proxy.removeOnSystemCapabilityListener(systemCapabilityType, listener);
		}

		@Override
		public boolean isTransportForServiceAvailable(SessionType serviceType) {
			if(SessionType.NAV.equals(serviceType)){
				return proxy.isVideoStreamTransportAvailable();
			}else if(SessionType.PCM.equals(serviceType)){
				return proxy.isAudioStreamTransportAvailable();
			}
			return false;
		}

		@Override
		public SdlMsgVersion getSdlMsgVersion(){
			try {
				return proxy.getSdlMsgVersion();
			} catch (SdlException e) {
				e.printStackTrace();
			}
			return null;
		}

		@Override
		public @NonNull Version getProtocolVersion() {
			if(proxy.getProtocolVersion() != null){
				return proxy.getProtocolVersion();
			}else{
				return new Version(1,0,0);
			}
		}

	};


	// BUILDER
	public static class Builder {
		SdlManager sdlManager;

		/**
		 * Builder for the SdlManager. Parameters in the constructor are required.
		 * @param context the current context
		 * @param appId the app's ID
		 * @param appName the app's name
		 * @param listener a SdlManagerListener object
		 */
		public Builder(@NonNull Context context, @NonNull final String appId, @NonNull final String appName, @NonNull final SdlManagerListener listener){
			sdlManager = new SdlManager();
			setContext(context);
			setAppId(appId);
			setAppName(appName);
			setManagerListener(listener);
		}

		/**
		 * Sets the App ID
		 * @param appId
		 */
		public Builder setAppId(@NonNull final String appId){
			sdlManager.appId = appId;
			return this;
		}

		/**
		 * Sets the Application Name
		 * @param appName
		 */
		public Builder setAppName(@NonNull final String appName){
			sdlManager.appName = appName;
			return this;
		}

		/**
		 * Sets the Short Application Name
		 * @param shortAppName
		 */
		public Builder setShortAppName(final String shortAppName) {
			sdlManager.shortAppName = shortAppName;
			return this;
		}

		/**
		 * Sets the Language of the App
		 * @param hmiLanguage
		 */
		public Builder setLanguage(final Language hmiLanguage){
			sdlManager.hmiLanguage = hmiLanguage;
			return this;
		}

		/**
		 * Sets the TemplateColorScheme for daytime
		 * @param dayColorScheme
		 */
		public Builder setDayColorScheme(final TemplateColorScheme dayColorScheme){
			sdlManager.dayColorScheme = dayColorScheme;
			return this;
		}

		/**
		 * Sets the TemplateColorScheme for nighttime
		 * @param nightColorScheme
		 */
		public Builder setNightColorScheme(final TemplateColorScheme nightColorScheme){
			sdlManager.nightColorScheme = nightColorScheme;
			return this;
		}

		/**
		 * Sets the LockScreenConfig for the session. <br>
		 * <strong>Note: If not set, the default configuration will be used.</strong>
		 * @param lockScreenConfig - configuration options
		 */
		public Builder setLockScreenConfig (final LockScreenConfig lockScreenConfig){
			sdlManager.lockScreenConfig = lockScreenConfig;
			return this;
		}

		/**
		 * Sets the icon for the app on HU <br>
		 * @param sdlArtwork
		 */
		public Builder setAppIcon(final SdlArtwork sdlArtwork){
			sdlManager.appIcon = sdlArtwork;
			return this;
		}

		/**
		 * Sets the vector of AppHMIType <br>
		 * <strong>Note: This should be an ordered list from most -> least relevant</strong>
		 * @param hmiTypes
		 */
		public Builder setAppTypes(final Vector<AppHMIType> hmiTypes){

			sdlManager.hmiTypes = hmiTypes;

			if (hmiTypes != null) {
				sdlManager.isMediaApp = hmiTypes.contains(AppHMIType.MEDIA);
			}

			return this;
		}

		/**
		 * Sets the vector of vrSynonyms
		 * @param vrSynonyms
		 */
		public Builder setVrSynonyms(final Vector<String> vrSynonyms) {
			sdlManager.vrSynonyms = vrSynonyms;
			return this;
		}

		/**
		 * Sets the TTS Name
		 * @param ttsChunks
		 */
		public Builder setTtsName(final Vector<TTSChunk> ttsChunks) {
			sdlManager.ttsChunks = ttsChunks;
			return this;
		}

		/**
		 * This Object type may change with the transport refactor
		 * Sets the BaseTransportConfig
		 * @param transport
		 */
		public Builder setTransportType(BaseTransportConfig transport){
			sdlManager.transport = transport;
			return this;
		}

		/**
		 * Sets the Context
		 * @param context
		 */
		public Builder setContext(Context context){
			sdlManager.context = context;
			return this;
		}

		/**
		 * Sets the Security library
		 * @param secList The list of security class(es)
		 */
		public Builder setSdlSecurity(List<Class<? extends SdlSecurityBase>> secList) {
			sdlManager.sdlSecList = secList;
			return this;
		}

		/**
		 * Set the SdlManager Listener
		 * @param listener the listener
		 */
		public Builder setManagerListener(@NonNull final SdlManagerListener listener){
			sdlManager.managerListener = listener;
			return this;
		}

		public SdlManager build() {

			if (sdlManager.appName == null) {
				throw new IllegalArgumentException("You must specify an app name by calling setAppName");
			}

			if (sdlManager.appId == null) {
				throw new IllegalArgumentException("You must specify an app ID by calling setAppId");
			}

			if (sdlManager.managerListener == null) {
				throw new IllegalArgumentException("You must set a SdlManagerListener object");
			}

			if (sdlManager.hmiTypes == null) {
				Vector<AppHMIType> hmiTypesDefault = new Vector<>();
				hmiTypesDefault.add(AppHMIType.DEFAULT);
				sdlManager.hmiTypes = hmiTypesDefault;
				sdlManager.isMediaApp = false;
			}

			if (sdlManager.lockScreenConfig == null){
				// if lock screen params are not set, use default
				sdlManager.lockScreenConfig = new LockScreenConfig();
			}

			if (sdlManager.hmiLanguage == null){
				sdlManager.hmiLanguage = Language.EN_US;
			}

			// Create a locale instance based on desired HMI language
			Locale locale;
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				locale = Locale.forLanguageTag(sdlManager.hmiLanguage.toString());
			} else {
				String[] localeString = sdlManager.hmiLanguage.toString().split("-");
				locale = new Locale(localeString[0], localeString[1]);
			}
			sdlManager.localization = new Localization(sdlManager.context, locale);
			sdlManager.transitionToState(BaseSubManager.SETTING_UP);

			return sdlManager;
		}
	}
}
