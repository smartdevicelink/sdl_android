package com.smartdevicelink.proxy.interfaces;

public interface IProxyListenerALM extends IProxyListenerBase {
	// Adds Advanced Life-cycle Management call-backs to the IProxyListenerAbstract interface
	
	/**
	 * **MOVED TO IProxyListenerBase** - onOnHMIStatus() being called indicates that the proxy has entered a state in which the 
	 * application may create SDL related resources (addCommands, ChoiceSets). 
	 */
	//void onOnHMIStatus(OnHMIStatus notification);
	
	/**
	 * **MOVED TO IProxyListenerBase** - onProxyClosed() being called indicates that the app is no longer registered with SDL
	 * All resources on SDL (addCommands and ChoiceSets) have been deleted and will have to be
	 * recreated upon the next onReadyForInitialization() call-back. 
	 */
	//void onProxyClosed(String info, Exception e);
	
	/**
	 * **MOVED TO IProxyListenerBase** - onError() being called indicates that the proxy has experienced an unrecoverable error.
	 * A new proxy object must be initiated to reestablish connection with SDL.
	 * 
	 * @param info - Any info present about the error that occurred.
	 * @param e - Any exception thrown by the error.
	 */
	//void onError(String info, Exception e);
	
	/**
	 * **Deprecated** - onSdlInterfaceAvailable() being called indicates that the proxy now has access to SDL's HMI. 
	 * Monitor the onFocusChange call-back to determine which level of HMI is available to the proxy.
	 * 
	 * @param isFirstAvailability - Indicates this is the first onSdlInterfaceAvailable in this lifecycle.
	 */
	// HMI (Background, Limited, Full) from Unavailable  = onSdlInterfaceAvailable(Boolean isFirstAvailability);

	/**
	 * **Deprecated** - onSdlInterfaceUnavailable() being called indicates that the proxy does NOT have access to SDL's HIM.
	 */
	// HMI None onSdlInterfaceUnavailable();
	
	/**
	 * **Deprecated** - ALM HMI states converted back to HMI Levels
	 * 
	 * HMI Full = onSdlInFocus(Boolean isFirstSdlInFocus);
	 * HMI Limited = onSdlInFocusLimited();
	 * HMI Background = onSdlLostFocus();
	 */
}
