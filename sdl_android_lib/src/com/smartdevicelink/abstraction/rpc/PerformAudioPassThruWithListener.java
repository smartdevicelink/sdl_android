package com.smartdevicelink.abstraction.rpc;

import com.smartdevicelink.abstraction.listener.AudioPassThruListener;
import com.smartdevicelink.proxy.rpc.PerformAudioPassThru;

public class PerformAudioPassThruWithListener extends PerformAudioPassThru {
	
	private AudioPassThruListener mListener;

	public AudioPassThruListener getListener() {
		return mListener;
	}

	public void setListener(AudioPassThruListener mListener) {
		this.mListener = mListener;
	}

}
