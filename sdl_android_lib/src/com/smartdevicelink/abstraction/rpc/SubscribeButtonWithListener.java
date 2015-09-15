package com.smartdevicelink.abstraction.rpc;

import com.smartdevicelink.abstraction.listener.ButtonListener;
import com.smartdevicelink.proxy.rpc.SubscribeButton;

public class SubscribeButtonWithListener extends SubscribeButton {
	
	private ButtonListener mListener;

	public ButtonListener getListener() {
		return mListener;
	}

	public void setListener(ButtonListener mListener) {
		this.mListener = mListener;
	}

}
