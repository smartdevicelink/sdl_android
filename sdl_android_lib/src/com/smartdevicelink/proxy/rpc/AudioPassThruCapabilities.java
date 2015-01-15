package com.smartdevicelink.proxy.rpc;

import java.util.Hashtable;

import com.smartdevicelink.proxy.RPCStruct;
import com.smartdevicelink.proxy.rpc.enums.AudioType;
import com.smartdevicelink.proxy.rpc.enums.BitsPerSample;
import com.smartdevicelink.proxy.rpc.enums.SamplingRate;
import com.smartdevicelink.util.DebugTool;

/**
 * Describes different audio type configurations for PerformAudioPassThru, e.g. {8kHz,8-bit,PCM}<br>
 * Specifies the capabilities of audio capturing: sampling rate, bits per sample, audio type.<br>
 * 
 * <p><b>Parameter List</b>
 * <table border="1" rules="all">
 * 		<tr>
 * 			<th>Name</th>
 * 			<th>Type</th>
 * 			<th>Description</th>
 * 			<th>SmartDeviceLink Ver. Available</th>
 * 		</tr>
 * 		<tr>
 * 			<td>samplingRate</td>
 * 			<td>SamplingRate</td>
 * 			<td>Describes the sampling rate for AudioPassThru
 * 			</td>
 * 			<td>SmartDeviceLink 2.0</td>
 * 		</tr>
 * 		<tr>
 * 			<td>bitsPerSample</td>
 * 			<td>BitsPerSample</td>
 * 			<td>Describes the sample depth in bit for AudioPassThru
 * 			</td>
 * 			<td>SmartDeviceLink 2.0</td>
 * 		</tr>
 * 		<tr>
 * 			<td>audioType</td>
 * 			<td>AudioType</td>
 * 			<td>Describes the audiotype for AudioPassThru
 * 			</td>
 * 			<td>SmartDeviceLink 2.0</td>
 * 		</tr>
 *  </table>
 * @since SmartDeviceLink 2.0
 */
public class AudioPassThruCapabilities extends RPCStruct {
	public static final String KEY_SAMPLING_RATE = "samplingRate";
	public static final String KEY_AUDIO_TYPE = "audioType";
	public static final String KEY_BITS_PER_SAMPLE = "bitsPerSample";
	
	/**
	 * Constructs a newly allocated AudioPassThruCapabilities object
	 */
    public AudioPassThruCapabilities() {}
    
    /**
     * Constructs a newly allocated AudioPassThruCapabilities object indicated by the Hashtable parameter
     * @param hash The Hashtable to use
     */
    public AudioPassThruCapabilities(Hashtable<String, Object> hash) {
        super(hash);
    }
    
    /**
     * set the sampling rate for AudioPassThru
     * @param samplingRate the sampling rate for AudioPassThru
     */
    public void setSamplingRate(SamplingRate samplingRate) {
    	if (samplingRate != null) {
    		store.put(KEY_SAMPLING_RATE, samplingRate);
    	} else {
    		store.remove(KEY_SAMPLING_RATE);
    	}
    }
    
    /**
     * get the sampling rate for AudioPassThru
     * @return  the sampling rate for AudioPassThru
     */
    public SamplingRate getSamplingRate() {
        Object obj = store.get(KEY_SAMPLING_RATE);
        if (obj instanceof SamplingRate) {
            return (SamplingRate) obj;
        } else if (obj instanceof String) {
        	SamplingRate theCode = null;
            try {
                theCode = SamplingRate.valueForString((String) obj);
            } catch (Exception e) {
            	DebugTool.logError("Failed to parse " + getClass().getSimpleName() + "." + KEY_SAMPLING_RATE, e);
            }
            return theCode;
        }
        return null;
    }
    
    /**
     * set the sample depth in bit for AudioPassThru
     * @param bitsPerSample the sample depth in bit for AudioPassThru
     */
    public void setBitsPerSample(BitsPerSample bitsPerSample) {
    	if (bitsPerSample != null) {
    		store.put(KEY_BITS_PER_SAMPLE, bitsPerSample);
    	} else {
    		store.remove(KEY_BITS_PER_SAMPLE);
    	}
    }
    
    /**
     * get  the sample depth in bit for AudioPassThru
     * @return the sample depth in bit for AudioPassThru
     */
    public BitsPerSample getBitsPerSample() {
        Object obj = store.get(KEY_BITS_PER_SAMPLE);
        if (obj instanceof BitsPerSample) {
            return (BitsPerSample) obj;
        } else if (obj instanceof String) {
        	BitsPerSample theCode = null;
            try {
                theCode = BitsPerSample.valueForString((String) obj);
            } catch (Exception e) {
            	DebugTool.logError("Failed to parse " + getClass().getSimpleName() + "." + KEY_BITS_PER_SAMPLE, e);
            }
            return theCode;
        }
        return null;
    }
    
    /**
     * set the audiotype for AudioPassThru
     * @param audioType the audiotype for AudioPassThru
     */
    public void setAudioType(AudioType audioType) {
    	if (audioType != null) {
    		store.put(KEY_AUDIO_TYPE, audioType);
    	} else {
    		store.remove(KEY_AUDIO_TYPE);
    	}
    }
    
    /**
     * get the audiotype for AudioPassThru
     * @return the audiotype for AudioPassThru
     */
    public AudioType getAudioType() {
        Object obj = store.get(KEY_AUDIO_TYPE);
        if (obj instanceof AudioType) {
            return (AudioType) obj;
        } else if (obj instanceof String) {
        	AudioType theCode = null;
            try {
                theCode = AudioType.valueForString((String) obj);
            } catch (Exception e) {
            	DebugTool.logError("Failed to parse " + getClass().getSimpleName() + "." + KEY_AUDIO_TYPE, e);
            }
            return theCode;
        }
        return null;
    }
}
