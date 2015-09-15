package com.smartdevicelink.abstraction.listener;

import com.smartdevicelink.proxy.rpc.OnButtonEvent;
import com.smartdevicelink.proxy.rpc.OnButtonPress;

public interface ButtonListener extends NotificationListener{
	
	public void handleButtonPress(OnButtonPress onButtonPress);
	public void handleButtonEvent(OnButtonEvent onButtonEvent);

}
