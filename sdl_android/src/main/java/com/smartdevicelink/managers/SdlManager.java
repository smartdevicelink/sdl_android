package com.smartdevicelink.managers;

import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.smartdevicelink.managers.file.FileManager;
import com.smartdevicelink.managers.permission.PermissionManager;
import com.smartdevicelink.managers.audio.AudioStreamManager;
import com.smartdevicelink.managers.file.filetypes.SdlArtwork;
import com.smartdevicelink.managers.lockscreen.LockScreenConfig;
import com.smartdevicelink.managers.lockscreen.LockScreenManager;
import com.smartdevicelink.managers.screen.ScreenManager;
import com.smartdevicelink.exception.SdlException;
import com.smartdevicelink.managers.video.VideoStreamingManager;
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
import com.smartdevicelink.util.Version;

import java.util.ArrayList;
import java.util.List;
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

	private static String TAG = "Sdl Manager";
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
	public LockScreenConfig lockScreenConfig;


	// Managers

	private PermissionManager permissionManager;
	private FileManager fileManager;
	private LockScreenManager lockScreenManager;
    private ScreenManager screenManager;
	private VideoStreamingManager videoStreamingManager;
	private AudioStreamManager audioStreamManager;


	// Initialize proxyBridge with anonymous lifecycleListener
	private final ProxyBridge proxyBridge= new ProxyBridge(new ProxyBridge.LifecycleListener() {
		@Override
		public void onProxyConnected() {
			Log.d(TAG, "Proxy is connected. Now initializing.");
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
				Log.d(TAG, "Sub manager failed to initialize");
			}
			if(
					permissionManager != null && permissionManager.getState() != BaseSubManager.SETTING_UP &&
					fileManager != null && fileManager.getState() != BaseSubManager.SETTING_UP &&
					audioStreamManager != null && audioStreamManager.getState() != BaseSubManager.SETTING_UP &&
					(videoStreamingManager == null || (videoStreamingManager != null && videoStreamingManager.getState() != BaseSubManager.SETTING_UP)) &&
					lockScreenManager != null &&  lockScreenManager.getState() != BaseSubManager.SETTING_UP	&&
					screenManager != null && screenManager.getState() != BaseSubManager.SETTING_UP

					){
				state = BaseSubManager.READY;
				if(managerListener != null){
					managerListener.onStart();
				}

				// Set the app icon
				if (SdlManager.this.appIcon != null && SdlManager.this.appIcon.getName() != null) {
					if (!fileManager.hasUploadedFile(SdlManager.this.appIcon)) {
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
		}
	};

	protected void initialize(){
		// instantiate managers

		this.permissionManager = new PermissionManager(_internalInterface);
		this.permissionManager.start(subManagerListener);

		this.fileManager = new FileManager(_internalInterface, context);
		this.fileManager.start(subManagerListener);

		if (lockScreenConfig.isEnabled()) {
			this.lockScreenManager = new LockScreenManager(lockScreenConfig, context, _internalInterface);
			this.lockScreenManager.start(subManagerListener);
		}

		this.screenManager = new ScreenManager(_internalInterface, this.fileManager);
		this.screenManager.start(subManagerListener);


		if(getAppTypes().contains(AppHMIType.NAVIGATION) || getAppTypes().contains(AppHMIType.PROJECTION)){
			if (videoStreamingManager == null) {
				this.videoStreamingManager = new VideoStreamingManager(_internalInterface);
			}
			this.videoStreamingManager.start(subManagerListener);
		}


		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
			this.audioStreamManager = new AudioStreamManager(_internalInterface, context);
			this.audioStreamManager.start(subManagerListener);
		} else {
			this.audioStreamManager = null;
		}

	}

	public void dispose() {
		this.permissionManager.dispose();

		this.fileManager.dispose();

		if (this.lockScreenManager != null) {
			this.lockScreenManager.dispose();
		}

		this.screenManager.dispose();

		if(this.videoStreamingManager != null) {
			this.videoStreamingManager.dispose();
		}

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
			if (this.audioStreamManager != null) {
				this.audioStreamManager.dispose();
			}
		}

		if(managerListener != null){
			managerListener.onDestroy();
			managerListener = null;
		}
	}

	/**
	 * Sets the state of SdlManager to one of those defined in BaseSubManager
	 * @param state int representing desired state of SdlManager
	 */
	protected void setState(int state){
		this.state = state;
	}

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

			sdlManager.state = BaseSubManager.SETTING_UP;

			return sdlManager;
		}
	}

	private void checkSdlManagerState(){
		if (state != BaseSubManager.READY){
			throw new IllegalStateException("SdlManager is not ready for use, be sure to initialize with start() method, implement callback, and use SubManagers in the SdlManager's callback");
		}
	}

	// MANAGER GETTERS

	/**
	 * Gets the PermissionManager. <br>
	 * <strong>Note: PermissionManager should be used only after SdlManager.start() CompletionListener callback is completed successfully.</strong>
	 * @return a PermissionManager object
	 */
	public PermissionManager getPermissionManager() {
		checkSdlManagerState();
		return permissionManager;
	}

	/**
	 * Gets the FileManager. <br>
	 * <strong>Note: FileManager should be used only after SdlManager.start() CompletionListener callback is completed successfully.</strong>
	 * @return a FileManager object
	 */
	public FileManager getFileManager() {
		checkSdlManagerState();
		return fileManager;
	}


    /**
     * Gets the VideoStreamingManager. <br>
     * <strong>Note: VideoStreamingManager should be used only after SdlManager.start() CompletionListener callback is completed successfully.</strong>
     * @return a VideoStreamingManager object
     */
    
	public VideoStreamingManager getVideoStreamingManager() {
		checkSdlManagerState();
		return videoStreamingManager;
	}
	

    /**
     * Gets the AudioStreamManager. <br>
     * <strong>Note: AudioStreamManager should be used only after SdlManager.start() CompletionListener callback is completed successfully.</strong>
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
		checkSdlManagerState();
		return screenManager;
	}

	/**
	 * Gets the LockScreenManager. <br>
	 * <strong>Note: LockScreenManager should be used only after SdlManager.start() CompletionListener callback is completed successfully.</strong>
	 * @return a LockScreenManager object
	 */
	public LockScreenManager getLockScreenManager() {
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
	 * @throws SdlException
	 */
	public void sendRPC(RPCMessage message) throws SdlException {

		if (message instanceof RPCRequest){
			proxy.sendRPCRequest((RPCRequest)message);
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
	 * @throws SdlException if an unrecoverable error is encountered
	 */
	public void sendSequentialRPCs(final List<? extends RPCMessage> rpcs, final OnMultipleRequestListener listener) throws SdlException {

		List<RPCRequest> rpcRequestList = new ArrayList<>();
		for (int i = 0; i < rpcs.size(); i++) {
			if (rpcs.get(i) instanceof RPCRequest){
				rpcRequestList.add((RPCRequest)rpcs.get(i));
			}
		}

		if (rpcRequestList.size() > 0) {
			proxy.sendSequentialRequests(rpcRequestList, listener);
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
	 * @throws SdlException if an unrecoverable error is encountered
	 */
	public void sendRPCs(List<? extends RPCMessage> rpcs, final OnMultipleRequestListener listener) throws SdlException {

		List<RPCRequest> rpcRequestList = new ArrayList<>();
		for (int i = 0; i < rpcs.size(); i++) {
			if (rpcs.get(i) instanceof RPCRequest){
				rpcRequestList.add((RPCRequest)rpcs.get(i));
			}
		}

		if (rpcRequestList.size() > 0) {
			proxy.sendRequests(rpcRequestList, listener);
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

							if (videoStreamingManager == null){
								videoStreamingManager = new VideoStreamingManager(_internalInterface);
							}
							//Pass to submanagers that need it
							if(videoStreamingManager != null){
								videoStreamingManager.handleTransportUpdated(connectedTransports, audioStreamTransportAvail, videoStreamTransportAvail);
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

}
