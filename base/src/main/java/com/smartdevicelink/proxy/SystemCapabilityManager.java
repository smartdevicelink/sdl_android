/*
 * Copyright (c) 2019, Livio, Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided with the
 * distribution.
 *
 * Neither the name of the Livio Inc. nor the names of its contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package com.smartdevicelink.proxy;

import com.smartdevicelink.protocol.enums.FunctionID;
import com.smartdevicelink.proxy.interfaces.ISdl;
import com.smartdevicelink.proxy.interfaces.OnSystemCapabilityListener;
import com.smartdevicelink.proxy.rpc.*;
import com.smartdevicelink.proxy.rpc.enums.*;
import com.smartdevicelink.proxy.rpc.listeners.OnRPCListener;
import com.smartdevicelink.proxy.rpc.listeners.OnRPCResponseListener;
import com.smartdevicelink.util.CorrelationIdGenerator;
import com.smartdevicelink.util.DebugTool;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.CopyOnWriteArrayList;

public class SystemCapabilityManager {
	private final HashMap<SystemCapabilityType, Object> cachedSystemCapabilities;
	private final HashMap<SystemCapabilityType, CopyOnWriteArrayList<OnSystemCapabilityListener>> onSystemCapabilityListeners;
	private final Object LISTENER_LOCK;
	private final ISdl callback;
	private OnRPCListener rpcListener;
	private boolean shouldConvertDeprecatedDisplayCapabilities;

	public SystemCapabilityManager(ISdl callback){
		this.callback = callback;
		this.LISTENER_LOCK = new Object();
		this.onSystemCapabilityListeners = new HashMap<>();
		this.cachedSystemCapabilities = new HashMap<>();
		this.shouldConvertDeprecatedDisplayCapabilities = true;

		setupRpcListeners();
	}

	private List<DisplayCapability> createDisplayCapabilityList(RegisterAppInterfaceResponse rpc) {
		return createDisplayCapabilityList(rpc.getDisplayCapabilities(), rpc.getButtonCapabilities(), rpc.getSoftButtonCapabilities());
	}

	private List<DisplayCapability> createDisplayCapabilityList(SetDisplayLayoutResponse rpc) {
		return createDisplayCapabilityList(rpc.getDisplayCapabilities(), rpc.getButtonCapabilities(), rpc.getSoftButtonCapabilities());
	}

	private List<DisplayCapability> createDisplayCapabilityList(DisplayCapabilities display, List<ButtonCapabilities> button, List<SoftButtonCapabilities> softButton) {
		// Based on deprecated Display capabilities we don't know if widgets are supported,
		// The Default MAIN window is the only window we know is supported
		WindowTypeCapabilities windowTypeCapabilities = new WindowTypeCapabilities(WindowType.MAIN, 1);

		DisplayCapability displayCapability = new DisplayCapability();
		displayCapability.setDisplayName(display != null ? display.getDisplayName() : display.getDisplayType().toString());
		displayCapability.setWindowTypeSupported(Collections.singletonList(windowTypeCapabilities));

		// Create a window capability object for the default MAIN window
		WindowCapability defaultWindowCapability = new WindowCapability();
		defaultWindowCapability.setWindowID(PredefinedWindows.DEFAULT_WINDOW.getValue());
		defaultWindowCapability.setButtonCapabilities(button);
		defaultWindowCapability.setSoftButtonCapabilities(softButton);

		// return if display capabilities don't exist.
		if (display == null) {
			displayCapability.setWindowCapabilities(Collections.singletonList(defaultWindowCapability));
			return Collections.singletonList(displayCapability);
		}

		// copy all available display capabilities
		defaultWindowCapability.setTemplatesAvailable(display.getTemplatesAvailable());
		defaultWindowCapability.setNumCustomPresetsAvailable(display.getNumCustomPresetsAvailable());
		defaultWindowCapability.setTextFields(display.getTextFields());
		defaultWindowCapability.setImageFields(display.getImageFields());
		ArrayList<ImageType> imageTypeSupported = new ArrayList<>();
		imageTypeSupported.add(ImageType.STATIC); // static images expected to always work on any head unit
		if (display.getGraphicSupported()) {
			imageTypeSupported.add(ImageType.DYNAMIC);
		}
		defaultWindowCapability.setImageTypeSupported(imageTypeSupported);

		displayCapability.setWindowCapabilities(Collections.singletonList(defaultWindowCapability));
		return Collections.singletonList(displayCapability);
	}

	private DisplayCapabilities createDeprecatedDisplayCapabilities(String displayName, WindowCapability defaultMainWindow) {
		DisplayCapabilities convertedCapabilities = new DisplayCapabilities();
		convertedCapabilities.setDisplayType(DisplayType.SDL_GENERIC); //deprecated but it is mandatory...
		convertedCapabilities.setDisplayName(displayName);
		convertedCapabilities.setTextFields(defaultMainWindow.getTextFields());
		convertedCapabilities.setImageFields(defaultMainWindow.getImageFields());
		convertedCapabilities.setTemplatesAvailable(defaultMainWindow.getTemplatesAvailable());
		convertedCapabilities.setNumCustomPresetsAvailable(defaultMainWindow.getNumCustomPresetsAvailable());
		convertedCapabilities.setMediaClockFormats(new ArrayList<MediaClockFormat>()); // mandatory field but allows empty array
		convertedCapabilities.setGraphicSupported(defaultMainWindow.getImageTypeSupported().contains(ImageType.DYNAMIC));

		return convertedCapabilities;
	}

	private void updateDeprecatedDisplayCapabilities() {
		WindowCapability defaultMainWindowCapabilities = getDefaultMainWindowCapability();
		List<DisplayCapability> displayCapabilityList = (List<DisplayCapability>)getCapability(SystemCapabilityType.DISPLAYS);

		if (defaultMainWindowCapabilities == null || displayCapabilityList == null || displayCapabilityList.size() == 0) {
			return;
		}

		// cover the deprecated capabilities for backward compatibility
		setCapability(SystemCapabilityType.DISPLAY, createDeprecatedDisplayCapabilities(displayCapabilityList.get(0).getDisplayName(), defaultMainWindowCapabilities));
		setCapability(SystemCapabilityType.BUTTON, defaultMainWindowCapabilities.getButtonCapabilities());
		setCapability(SystemCapabilityType.SOFTBUTTON, defaultMainWindowCapabilities.getSoftButtonCapabilities());
	}

	private void updateCachedDisplayCapabilityList(List<DisplayCapability> newCapabilities) {
		if (newCapabilities == null || newCapabilities.isEmpty()) {
			DebugTool.logWarning("Received invalid display capability list");
			return;
		}

		List<DisplayCapability> oldCapabilities = (List<DisplayCapability>) getCapability(SystemCapabilityType.DISPLAYS);

		if (oldCapabilities == null || oldCapabilities.isEmpty()) {
			setCapability(SystemCapabilityType.DISPLAYS, newCapabilities);
			updateDeprecatedDisplayCapabilities();
			return;
		}

		DisplayCapability oldDefaultDisplayCapabilities = oldCapabilities.get(0);
		ArrayList<WindowCapability> copyWindowCapabilities = new ArrayList<>(oldDefaultDisplayCapabilities.getWindowCapabilities());

		DisplayCapability newDefaultDisplayCapabilities = newCapabilities.get(0);
		List<WindowCapability> newWindowCapabilities = newDefaultDisplayCapabilities.getWindowCapabilities();

		for (WindowCapability newWindow : newWindowCapabilities) {
			ListIterator<WindowCapability> iterator = copyWindowCapabilities.listIterator();
			boolean oldFound = false;
			final int newWindowID = newWindow.getWindowID() != null ? newWindow.getWindowID() : PredefinedWindows.DEFAULT_WINDOW.getValue();
			while (iterator.hasNext()) {
				WindowCapability oldWindow = iterator.next();
				final int oldWindowID = oldWindow.getWindowID() != null ? oldWindow.getWindowID() : PredefinedWindows.DEFAULT_WINDOW.getValue();
				if (newWindowID == oldWindowID) {
					iterator.set(newWindow); // replace the old window caps with new ones
					oldFound = true;
					break;
				}
			}

			if (!oldFound) {
				copyWindowCapabilities.add(newWindow); // this is a new unknown window
			}
		}

		// replace the window capabilities array with the merged one.
		newDefaultDisplayCapabilities.setWindowCapabilities(copyWindowCapabilities);
		setCapability(SystemCapabilityType.DISPLAYS, Collections.singletonList(newDefaultDisplayCapabilities));
		updateDeprecatedDisplayCapabilities();
	}

	public WindowCapability getWindowCapability(int windowID) {
		List<DisplayCapability> capabilities = (List<DisplayCapability>) getCapability(SystemCapabilityType.DISPLAYS);
		if (capabilities == null || capabilities.size() == 0) {
			return null;
		}
		DisplayCapability display = capabilities.get(0);
		for (WindowCapability windowCapability : display.getWindowCapabilities()) {
		    int currentWindowID = windowCapability.getWindowID() != null ? windowCapability.getWindowID() : PredefinedWindows.DEFAULT_WINDOW.getValue();
			if (currentWindowID == windowID) {
				return windowCapability;
			}
		}
		return null;
	}

	public WindowCapability getDefaultMainWindowCapability() {
		return getWindowCapability(PredefinedWindows.DEFAULT_WINDOW.getValue());
	}

	public void parseRAIResponse(RegisterAppInterfaceResponse response){
		if(response!=null && response.getSuccess()) {
			this.shouldConvertDeprecatedDisplayCapabilities = true; // reset the flag
			setCapability(SystemCapabilityType.DISPLAYS, createDisplayCapabilityList(response));
			setCapability(SystemCapabilityType.HMI, response.getHmiCapabilities());
			setCapability(SystemCapabilityType.DISPLAY, response.getDisplayCapabilities());
			setCapability(SystemCapabilityType.AUDIO_PASSTHROUGH, response.getAudioPassThruCapabilities());
			setCapability(SystemCapabilityType.PCM_STREAMING, response.getPcmStreamingCapabilities());
			setCapability(SystemCapabilityType.BUTTON, response.getButtonCapabilities());
			setCapability(SystemCapabilityType.HMI_ZONE, response.getHmiZoneCapabilities());
			setCapability(SystemCapabilityType.PRESET_BANK, response.getPresetBankCapabilities());
			setCapability(SystemCapabilityType.SOFTBUTTON, response.getSoftButtonCapabilities());
			setCapability(SystemCapabilityType.SPEECH, response.getSpeechCapabilities());
			setCapability(SystemCapabilityType.VOICE_RECOGNITION, response.getVrCapabilities());
			setCapability(SystemCapabilityType.PRERECORDED_SPEECH, response.getPrerecordedSpeech());
		}
	}

    private void setupRpcListeners() {
		rpcListener = new OnRPCListener() {
			@Override
			public void onReceived(RPCMessage message) {
				if (message != null) {
					if (RPCMessage.KEY_RESPONSE.equals(message.getMessageType())) {
						switch (message.getFunctionID()) {
							case SET_DISPLAY_LAYOUT:
								SetDisplayLayoutResponse response = (SetDisplayLayoutResponse) message;
								setCapability(SystemCapabilityType.DISPLAY, response.getDisplayCapabilities());
								setCapability(SystemCapabilityType.BUTTON, response.getButtonCapabilities());
								setCapability(SystemCapabilityType.PRESET_BANK, response.getPresetBankCapabilities());
								setCapability(SystemCapabilityType.SOFTBUTTON, response.getSoftButtonCapabilities());
								if (shouldConvertDeprecatedDisplayCapabilities) {
									setCapability(SystemCapabilityType.DISPLAYS, createDisplayCapabilityList(response));
								}
								break;
							case GET_SYSTEM_CAPABILITY:
								GetSystemCapabilityResponse systemCapabilityResponse = (GetSystemCapabilityResponse) message;
								SystemCapability systemCapability = systemCapabilityResponse.getSystemCapability();
								if (systemCapabilityResponse.getSuccess() && SystemCapabilityType.DISPLAYS.equals(systemCapability.getSystemCapabilityType())) {
									shouldConvertDeprecatedDisplayCapabilities = false; // Successfully got DISPLAYS data. No conversion needed anymore
									List<DisplayCapability> newCapabilities = (List<DisplayCapability>) systemCapability.getCapabilityForType(SystemCapabilityType.DISPLAYS);
									updateCachedDisplayCapabilityList(newCapabilities);
								}
								break;
						}
					} else if (RPCMessage.KEY_NOTIFICATION.equals(message.getMessageType())) {
						switch (message.getFunctionID()) {
							case ON_SYSTEM_CAPABILITY_UPDATED:
								OnSystemCapabilityUpdated onSystemCapabilityUpdated = (OnSystemCapabilityUpdated) message;
								if (onSystemCapabilityUpdated.getSystemCapability() != null) {
									SystemCapability systemCapability = onSystemCapabilityUpdated.getSystemCapability();
									SystemCapabilityType systemCapabilityType = systemCapability.getSystemCapabilityType();
									Object capability = systemCapability.getCapabilityForType(systemCapabilityType);
									if (cachedSystemCapabilities.containsKey(systemCapabilityType)) { //The capability already exists
										switch (systemCapabilityType) {
											case APP_SERVICES:
												// App services only updates what was changed so we need
												// to update the capability rather than override it
												AppServicesCapabilities appServicesCapabilities = (AppServicesCapabilities) capability;
												if (capability != null) {
													List<AppServiceCapability> appServicesCapabilitiesList = appServicesCapabilities.getAppServices();
													AppServicesCapabilities cachedAppServicesCapabilities = (AppServicesCapabilities) cachedSystemCapabilities.get(systemCapabilityType);
													//Update the cached app services
													cachedAppServicesCapabilities.updateAppServices(appServicesCapabilitiesList);
													//Set the new capability object to the updated cached capabilities
													capability = cachedAppServicesCapabilities;
												}
												break;
											case DISPLAYS:
												shouldConvertDeprecatedDisplayCapabilities = false; // Successfully got DISPLAYS data. No conversion needed anymore
												// this notification can return only affected windows (hence not all windows)
												List<DisplayCapability> newCapabilities = (List<DisplayCapability>) capability;
												updateCachedDisplayCapabilityList(newCapabilities);
										}
									}
									if (capability != null) {
										setCapability(systemCapabilityType, capability);
									}
								}
						}
					}
				}
			}
		};

		if (callback != null) {
			callback.addOnRPCListener(FunctionID.GET_SYSTEM_CAPABILITY, rpcListener);
			callback.addOnRPCListener(FunctionID.SET_DISPLAY_LAYOUT, rpcListener);
			callback.addOnRPCListener(FunctionID.ON_SYSTEM_CAPABILITY_UPDATED, rpcListener);
		}
	}

	/**
	 * Sets a capability in the cached map. This should only be done when an RPC is received and contains updates to the capability
	 * that is being cached in the SystemCapabilityManager.
	 * @param systemCapabilityType the system capability type that will be set
	 * @param capability the value of the capability that will be set
	 */
	public synchronized void setCapability(SystemCapabilityType systemCapabilityType, Object capability){
			cachedSystemCapabilities.put(systemCapabilityType, capability);
			notifyListeners(systemCapabilityType, capability);
	}

	/**
	 * Notify listners in the list about the new retrieved capability
	 * @param systemCapabilityType the system capability type that was retrieved
	 * @param capability the system capability value that was retrieved
	 */
	private void notifyListeners(SystemCapabilityType systemCapabilityType, Object capability) {
		synchronized(LISTENER_LOCK){
			CopyOnWriteArrayList<OnSystemCapabilityListener> listeners = onSystemCapabilityListeners.get(systemCapabilityType);
			if(listeners != null && listeners.size() > 0) {
				for (OnSystemCapabilityListener listener : listeners) {
					listener.onCapabilityRetrieved(capability);
				}
			}
		}
	}

	/**
	 * Ability to see if the connected module supports the given capability. Useful to check before
	 * attempting to query for capabilities that require asynchronous calls to initialize.
	 * @param type the SystemCapabilityType that is to be checked
	 * @return if that capability is supported with the current, connected module
	 */
	public boolean isCapabilitySupported(SystemCapabilityType type){
		if(cachedSystemCapabilities.get(type) != null){
			//The capability exists in the map and is not null
			return true;
		}else if(cachedSystemCapabilities.containsKey(SystemCapabilityType.HMI)){
			HMICapabilities hmiCapabilities = ((HMICapabilities)cachedSystemCapabilities.get(SystemCapabilityType.HMI));
			switch (type) {
				case NAVIGATION:
					return hmiCapabilities.isNavigationAvailable();
				case PHONE_CALL:
					return hmiCapabilities.isPhoneCallAvailable();
				case VIDEO_STREAMING:
					return hmiCapabilities.isVideoStreamingAvailable();
				case REMOTE_CONTROL:
					return hmiCapabilities.isRemoteControlAvailable();
				default:
					return false;
			}
		}else{
			return false;
		}
	}

	/**
	 * @param systemCapabilityType Type of capability desired
	 * @param scListener callback to execute upon retrieving capability
	 */
	public void getCapability(final SystemCapabilityType systemCapabilityType, final OnSystemCapabilityListener scListener){
		Object capability = cachedSystemCapabilities.get(systemCapabilityType);
		if(capability != null){
			scListener.onCapabilityRetrieved(capability);
			return;
		}else if(scListener == null){
			return;
		}

		retrieveCapability(systemCapabilityType, scListener);
	}

	/**
	 * @param systemCapabilityType Type of capability desired
	 * @return Desired capability if it is cached in the manager, otherwise returns a null object
	 * and works in the background to retrieve the capability for the next call
	 */
	public Object getCapability(final SystemCapabilityType systemCapabilityType){
		Object capability = cachedSystemCapabilities.get(systemCapabilityType);
		if(capability != null){
			return capability;
		}

		retrieveCapability(systemCapabilityType, null);
		return null;
	}

	/**
	 * Add a listener to be called whenever a new capability is retrieved
	 * @param systemCapabilityType Type of capability desired
	 * @param listener callback to execute upon retrieving capability
	 */
	public void addOnSystemCapabilityListener(final SystemCapabilityType systemCapabilityType, final OnSystemCapabilityListener listener){
		getCapability(systemCapabilityType, listener);
		synchronized(LISTENER_LOCK){
			if (systemCapabilityType != null && listener != null) {
				if (onSystemCapabilityListeners.get(systemCapabilityType) == null) {
					onSystemCapabilityListeners.put(systemCapabilityType, new CopyOnWriteArrayList<OnSystemCapabilityListener>());
				}
				onSystemCapabilityListeners.get(systemCapabilityType).add(listener);
			}
		}
	}

	/**
	 * Remove an OnSystemCapabilityListener that was previously added
	 * @param systemCapabilityType Type of capability
	 * @param listener the listener that should be removed
	 */
	public boolean removeOnSystemCapabilityListener(final SystemCapabilityType systemCapabilityType, final OnSystemCapabilityListener listener){
		synchronized(LISTENER_LOCK){
			if(onSystemCapabilityListeners!= null
					&& systemCapabilityType != null
					&& listener != null
					&& onSystemCapabilityListeners.get(systemCapabilityType) != null){
				return onSystemCapabilityListeners.get(systemCapabilityType).remove(listener);
			}
		}
		return false;
	}

	/**
	 * @param systemCapabilityType Type of capability desired
	 * passes GetSystemCapabilityType request to `callback` to be sent by proxy.
	 * this method will send RPC and call the listener's callback only if the systemCapabilityType is queryable
	 */
	private void retrieveCapability(final SystemCapabilityType systemCapabilityType, final OnSystemCapabilityListener scListener){
		if (!systemCapabilityType.isQueryable()){
			String message = "This systemCapabilityType cannot be queried for";
			DebugTool.logError(message);
			if( scListener != null){
				scListener.onError(message);
			}
			return;
		}
		final GetSystemCapability request = new GetSystemCapability();
		request.setSystemCapabilityType(systemCapabilityType);
		request.setOnRPCResponseListener(new OnRPCResponseListener() {
			@Override
			public void onResponse(int correlationId, RPCResponse response) {
				if(response.getSuccess()){
					Object retrievedCapability = ((GetSystemCapabilityResponse) response).getSystemCapability().getCapabilityForType(systemCapabilityType);
					cachedSystemCapabilities.put(systemCapabilityType, retrievedCapability);
					if(scListener!=null){scListener.onCapabilityRetrieved(retrievedCapability);	}
				}else{
					if(scListener!=null){scListener.onError(response.getInfo());}
				}
			}

			@Override
			public void onError(int correlationId, Result resultCode, String info) {
				if(scListener!=null){scListener.onError(info);}
			}
		});
		request.setCorrelationID(CorrelationIdGenerator.generateId());

		if(callback!=null){
			callback.sendRPCRequest(request);
		}
	}

	/**
	 * Converts a capability object into a list.
	 * @param object the capability that needs to be converted
	 * @param classType The class type of that should be contained in the list
	 * @return a List of capabilities if object is instance of List, otherwise it will return null.
	 */
	@SuppressWarnings({"unchecked"})
	public static <T> List<T> convertToList(Object object, Class<T> classType){
		if(null == classType || null == object || !(object instanceof List)) {
			return null;
		}

		List list = (List)object;
		if (list.isEmpty()) {
			//We return a new list of type T instead of null because while we don't know if
			//the original list was of type T we want to ensure that we don't throw a cast class exception
			//but still
			return new ArrayList<T>();
		}

		if(!classType.isInstance(list.get(0))){
			//The list is not of the correct list type
			return null;
		}

		return (List<T>)object;
	}
}
