package com.smartdevicelink.proxy;

import com.smartdevicelink.proxy.interfaces.IProxyListenerBase;
import com.smartdevicelink.proxy.rpc.OnAppInterfaceUnregistered;
import com.smartdevicelink.proxy.rpc.RegisterAppInterfaceResponse;
import com.smartdevicelink.proxy.rpc.UnregisterAppInterfaceResponse;

public interface IProxyListener extends IProxyListenerBase{
	// Adds Legacy Life-cycle Management call-backs to the IProxyListenerAbstract interface
	
	void onProxyOpened();
	
	void onRegisterAppInterfaceResponse(RegisterAppInterfaceResponse response);

	void onOnAppInterfaceUnregistered(OnAppInterfaceUnregistered notification);
	
	void onUnregisterAppInterfaceResponse(UnregisterAppInterfaceResponse response);
	
}
