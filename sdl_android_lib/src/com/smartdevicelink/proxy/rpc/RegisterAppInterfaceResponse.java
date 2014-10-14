package com.smartdevicelink.proxy.rpc;

import java.util.Hashtable;
import java.util.Vector;

import com.smartdevicelink.proxy.RPCResponse;
import com.smartdevicelink.proxy.Version;
import com.smartdevicelink.proxy.rpc.enums.HmiZoneCapabilities;
import com.smartdevicelink.proxy.rpc.enums.Language;
import com.smartdevicelink.proxy.rpc.enums.PrerecordedSpeech;
import com.smartdevicelink.proxy.rpc.enums.SpeechCapabilities;
import com.smartdevicelink.proxy.rpc.enums.VrCapabilities;
import com.smartdevicelink.util.DebugTool;

/**
 * Register AppInterface Response is sent, when RegisterAppInterface has been called
 * 
 * @since SmartDeviceLink 1.0
 */
public class RegisterAppInterfaceResponse extends RPCResponse {
	public static final String vehicleType = "vehicleType";
	public static final String speechCapabilities = "speechCapabilities";
	public static final String vrCapabilities = "vrCapabilities";
	public static final String audioPassThruCapabilities = "audioPassThruCapabilities";
	public static final String hmiZoneCapabilities = "hmiZoneCapabilities";
    public static final String prerecordedSpeech = "prerecordedSpeech";
    public static final String supportedDiagModes = "supportedDiagModes";
    public static final String sdlMsgVersion = "syncMsgVersion";
    public static final String language = "language";
    public static final String buttonCapabilities = "buttonCapabilities";
    public static final String displayCapabilities = "displayCapabilities";
    public static final String hmiDisplayLanguage = "hmiDisplayLanguage";
    public static final String softButtonCapabilities = "softButtonCapabilities";
    public static final String presetBankCapabilities = "presetBankCapabilities";
	/**
	 * Constructs a new RegisterAppInterfaceResponse object
	 */
    public RegisterAppInterfaceResponse() {
        super("RegisterAppInterface");
    }

	/**
	 * Constructs a new RegisterAppInterfaceResponse object indicated by the Hashtable
	 * parameter
	 * <p>
	 * 
	 * @param hash
	 *            The Hashtable to use
	 */
    public RegisterAppInterfaceResponse(Hashtable hash) {
        super(hash);
    }

	/**
	 * Gets the version of the SDL&reg; SmartDeviceLink interface
	 * 
	 * @return SdlMsgVersion -a SdlMsgVersion object representing version of
	 *         the SDL&reg; SmartDeviceLink interface
	 */
    public SdlMsgVersion getSdlMsgVersion() {
        Object obj = parameters.get(RegisterAppInterfaceResponse.sdlMsgVersion);
        if (obj instanceof SdlMsgVersion) {
        	return (SdlMsgVersion)obj;
        } else if (obj instanceof Hashtable) {
        	return new SdlMsgVersion((Hashtable)obj);
        }
        return null;
    }

	/**
	 * Sets the version of the SDL&reg; SmartDeviceLink interface
	 * 
	 * @param sdlMsgVersion
	 *            a SdlMsgVersion object representing version of the SDL&reg;
	 *            SmartDeviceLink interface
	 *            <p>
	 *            <b>Notes: </b>To be compatible, app msg major version number
	 *            must be less than or equal to SDL&reg; major version number.
	 *            If msg versions are incompatible, app has 20 seconds to
	 *            attempt successful RegisterAppInterface (w.r.t. msg version)
	 *            on underlying protocol session, else will be terminated. Major
	 *            version number is a compatibility declaration. Minor version
	 *            number indicates minor functional variations (e.g. features,
	 *            capabilities, bug fixes) when sent from SDL&reg; to app (in
	 *            RegisterAppInterface response). However, the minor version
	 *            number sent from the app to SDL&reg; (in RegisterAppInterface
	 *            request) is ignored by SDL&reg;
	 */
    public void setSdlMsgVersion(SdlMsgVersion sdlMsgVersion) {
        if (sdlMsgVersion != null) {
            parameters.put(RegisterAppInterfaceResponse.sdlMsgVersion, sdlMsgVersion);
        }
    }

	/**
	 * Gets a Language enumeration indicating what language the application
	 * intends to use for user interaction (Display, TTS and VR)
	 * 
	 * @return Enumeration -a language enumeration
	 */
    public Language getLanguage() {
        Object obj = parameters.get(RegisterAppInterfaceResponse.language);
        if (obj instanceof Language) {
            return (Language) obj;
        } else if (obj instanceof String) {
            Language theCode = null;
            try {
                theCode = Language.valueForString((String) obj);
            } catch (Exception e) {
            	DebugTool.logError("Failed to parse " + getClass().getSimpleName() + "." + RegisterAppInterfaceResponse.language, e);
            }
            return theCode;
        }
        return null;
    }

	/**
	 * Sets an enumeration indicating what language the application intends to
	 * use for user interaction (Display, TTS and VR)
	 * 
	 * @param language
	 *            a Language Enumeration
	 *            <p>
	 * 
	 */
    public void setLanguage(Language language) {
        if (language != null) {
            parameters.put(RegisterAppInterfaceResponse.language, language);
        }
    }

	/**
	 * Gets an enumeration indicating what language the application intends to
	 * use for user interaction ( Display)
	 * 
	 * @return Language - a Language value representing an enumeration
	 *         indicating what language the application intends to use for user
	 *         interaction ( Display)
	 * @since SmartDeviceLink 2.0
	 */
    public Language getHmiDisplayLanguage() {
        Object obj = parameters.get(RegisterAppInterfaceResponse.hmiDisplayLanguage);
        if (obj instanceof Language) {
            return (Language) obj;
        } else if (obj instanceof String) {
            Language theCode = null;
            try {
                theCode = Language.valueForString((String) obj);
            } catch (Exception e) {
            	DebugTool.logError("Failed to parse " + getClass().getSimpleName() + "." + RegisterAppInterfaceResponse.hmiDisplayLanguage, e);
            }
            return theCode;
        }
        return null;
    }

	/**
	 * Sets an enumeration indicating what language the application intends to
	 * use for user interaction ( Display)
	 * 
	 * @param hmiDisplayLanguage
	 * @since SmartDeviceLink 2.0
	 */
    public void setHmiDisplayLanguage(Language hmiDisplayLanguage) {
        if (hmiDisplayLanguage != null) {
            parameters.put(RegisterAppInterfaceResponse.hmiDisplayLanguage, hmiDisplayLanguage);
        } else {
        	parameters.remove(RegisterAppInterfaceResponse.hmiDisplayLanguage);
        }
    }

	/**
	 * Gets getDisplayCapabilities set when application interface is registered.
	 * 
	 * @return DisplayCapabilities
	 */
    public DisplayCapabilities getDisplayCapabilities() {
        Object obj = parameters.get(RegisterAppInterfaceResponse.displayCapabilities);
        if (obj instanceof DisplayCapabilities) {
        	return (DisplayCapabilities)obj;
        } else if (obj instanceof Hashtable) {
        	return new DisplayCapabilities((Hashtable)obj);
        }
        return null;
    }
    /**
     * Sets Display Capabilities
     * @param displayCapabilities
     */
    public void setDisplayCapabilities(DisplayCapabilities displayCapabilities) {
        if (displayCapabilities != null) {
            parameters.put(RegisterAppInterfaceResponse.displayCapabilities, displayCapabilities);
        }
    }

	/**
	 * Gets buttonCapabilities set when application interface is registered.
	 * 
	 * @return buttonCapabilities
	 */
    public Vector<ButtonCapabilities> getButtonCapabilities() {
        if (parameters.get(RegisterAppInterfaceResponse.buttonCapabilities) instanceof Vector<?>) {
	    	Vector<?> list = (Vector<?>)parameters.get(RegisterAppInterfaceResponse.buttonCapabilities);
	        if (list != null && list.size() > 0) {
	            Object obj = list.get(0);
	            if (obj instanceof ButtonCapabilities) {
	                return (Vector<ButtonCapabilities>) list;
	            } else if (obj instanceof Hashtable) {
	                Vector<ButtonCapabilities> newList = new Vector<ButtonCapabilities>();
	                for (Object hashObj : list) {
	                    newList.add(new ButtonCapabilities((Hashtable)hashObj));
	                }
	                return newList;
	            }
	        }
        }
        return null;
    }
    /**
     * Sets Button Capabilities
     * @param buttonCapabilities
     */
    public void setButtonCapabilities(Vector<ButtonCapabilities> buttonCapabilities) {
        if (buttonCapabilities != null) {
            parameters.put(RegisterAppInterfaceResponse.buttonCapabilities, buttonCapabilities);
        }
    }
    /**
	 * Gets getSoftButtonCapabilities set when application interface is registered.
	 * 
	 * @return SoftButtonCapabilities 
	 */
    public Vector<SoftButtonCapabilities> getSoftButtonCapabilities() {
        if (parameters.get(RegisterAppInterfaceResponse.softButtonCapabilities) instanceof Vector<?>) {
	    	Vector<?> list = (Vector<?>)parameters.get(RegisterAppInterfaceResponse.softButtonCapabilities);
	        if (list != null && list.size() > 0) {
	            Object obj = list.get(0);
	            if (obj instanceof SoftButtonCapabilities) {
	                return (Vector<SoftButtonCapabilities>) list;
	            } else if (obj instanceof Hashtable) {
	                Vector<SoftButtonCapabilities> newList = new Vector<SoftButtonCapabilities>();
	                for (Object hashObj : list) {
	                    newList.add(new SoftButtonCapabilities((Hashtable)hashObj));
	                }
	                return newList;
	            }
	        }
        }
        return null;
    }
    /**
     * Sets softButtonCapabilities
     * @param softButtonCapabilities
     */
    public void setSoftButtonCapabilities(Vector<SoftButtonCapabilities> softButtonCapabilities) {
        if (softButtonCapabilities != null) {
            parameters.put(RegisterAppInterfaceResponse.softButtonCapabilities, softButtonCapabilities);
        }
    }

	/**
	 * Gets getPresetBankCapabilities set when application interface is registered.
	 * 
	 * @return PresetBankCapabilities 
	 */
    public PresetBankCapabilities getPresetBankCapabilities() {
        Object obj = parameters.get(RegisterAppInterfaceResponse.presetBankCapabilities);
        if (obj instanceof PresetBankCapabilities) {
        	return (PresetBankCapabilities)obj;
        } else if (obj instanceof Hashtable) {
        	return new PresetBankCapabilities((Hashtable)obj);
        }
        return null;
    }
    /**
     * Sets presetBankCapabilities
     * @param	presetBankCapabilities
     */
    public void setPresetBankCapabilities(PresetBankCapabilities presetBankCapabilities) {
        if (presetBankCapabilities != null) {
            parameters.put(RegisterAppInterfaceResponse.presetBankCapabilities, presetBankCapabilities);
        }
    }
	
	/**
	 * Gets hmiZoneCapabilities set when application interface is registered.
	 * 
	 * @return HmiZoneCapabilities
	 */
    public Vector<HmiZoneCapabilities> getHmiZoneCapabilities() {
        if (parameters.get(RegisterAppInterfaceResponse.hmiZoneCapabilities) instanceof Vector<?>) {
	    	Vector<?> list = (Vector<?>)parameters.get(RegisterAppInterfaceResponse.hmiZoneCapabilities);
	        if (list != null && list.size() > 0) {
	            Object obj = list.get(0);
	            if (obj instanceof HmiZoneCapabilities) {
	                return (Vector<HmiZoneCapabilities>) list;
	            } else if (obj instanceof String) {
	                Vector<HmiZoneCapabilities> newList = new Vector<HmiZoneCapabilities>();
	                for (Object hashObj : list) {
	                    String strFormat = (String)hashObj;
	                    HmiZoneCapabilities toAdd = null;
	                    try {
	                        toAdd = HmiZoneCapabilities.valueForString(strFormat);
	                    } catch (Exception e) {
	                    	DebugTool.logError("Failed to parse " + getClass().getSimpleName() + "." + RegisterAppInterfaceResponse.hmiZoneCapabilities, e);
	                    }
	                    if (toAdd != null) {
	                        newList.add(toAdd);
	                    }
	                }
	                return newList;
	            }
	        }
        }
        return null;
    }
    /**
     * Sets hmiZoneCapabilities
     * @param hmiZoneCapabilities
     */
    public void setHmiZoneCapabilities(Vector<HmiZoneCapabilities> hmiZoneCapabilities) {
        if (hmiZoneCapabilities != null) {
            parameters.put(RegisterAppInterfaceResponse.hmiZoneCapabilities, hmiZoneCapabilities);
        }
    }
	
	/**
	 * Gets speechCapabilities set when application interface is registered.
	 * 
	 * @return SpeechCapabilities
	 */
    public Vector<SpeechCapabilities> getSpeechCapabilities() {
        if (parameters.get(RegisterAppInterfaceResponse.speechCapabilities) instanceof Vector<?>) {
	    	Vector<?> list = (Vector<?>)parameters.get(RegisterAppInterfaceResponse.speechCapabilities);
	        if (list != null && list.size() > 0) {
	            Object obj = list.get(0);
	            if (obj instanceof SpeechCapabilities) {
	                return (Vector<SpeechCapabilities>) list;
	            } else if (obj instanceof String) {
	                Vector<SpeechCapabilities> newList = new Vector<SpeechCapabilities>();
	                for (Object hashObj : list) {
	                    String strFormat = (String)hashObj;
	                    SpeechCapabilities toAdd = null;
	                    try {
	                        toAdd = SpeechCapabilities.valueForString(strFormat);
	                    } catch (Exception e) {
	                    	DebugTool.logError("Failed to parse " + getClass().getSimpleName() + "." + RegisterAppInterfaceResponse.speechCapabilities, e);
	                    }
	                    if (toAdd != null) {
	                        newList.add(toAdd);
	                    }
	                }
	                return newList;
	            }
	        }
        }
        return null;
    }
    /**
     * Sets speechCapabilities
     * @param speechCapabilities
     */
    public void setSpeechCapabilities(Vector<SpeechCapabilities> speechCapabilities) {
        if (speechCapabilities != null) {
            parameters.put(RegisterAppInterfaceResponse.speechCapabilities, speechCapabilities);
        }
    }

    
    public Vector<PrerecordedSpeech> getPrerecordedSpeech() {
        if (parameters.get(RegisterAppInterfaceResponse.prerecordedSpeech) instanceof Vector<?>) {
	    	Vector<?> list = (Vector<?>)parameters.get(RegisterAppInterfaceResponse.prerecordedSpeech);
	        if (list != null && list.size() > 0) {
	            Object obj = list.get(0);
	            if (obj instanceof PrerecordedSpeech) {
	                return (Vector<PrerecordedSpeech>) list;
	            } else if (obj instanceof String) {
	                Vector<PrerecordedSpeech> newList = new Vector<PrerecordedSpeech>();
	                for (Object hashObj : list) {
	                    String strFormat = (String)hashObj;
	                    PrerecordedSpeech toAdd = null;
	                    try {
	                        toAdd = PrerecordedSpeech.valueForString(strFormat);
	                    } catch (Exception e) {
	                    	DebugTool.logError("Failed to parse " + getClass().getSimpleName() + "." + RegisterAppInterfaceResponse.prerecordedSpeech, e);
	                    }
	                    if (toAdd != null) {
	                        newList.add(toAdd);
	                    }
	                }
	                return newList;
	            }
	        }
        }
        return null;
    }

    public void setPrerecordedSpeech(Vector<PrerecordedSpeech> prerecordedSpeech) {
        if (prerecordedSpeech != null) {
            parameters.put(RegisterAppInterfaceResponse.prerecordedSpeech, prerecordedSpeech);
        }
    }
 
    
	/**
	 * Gets vrCapabilities set when application interface is registered.
	 * 
	 * @return VrCapabilities
	 */
    public Vector<VrCapabilities> getVrCapabilities() {
        if (parameters.get(RegisterAppInterfaceResponse.vrCapabilities) instanceof Vector<?>) {
	    	Vector<?> list = (Vector<?>)parameters.get(RegisterAppInterfaceResponse.vrCapabilities);
	        if (list != null && list.size() > 0) {
	            Object obj = list.get(0);
	            if (obj instanceof VrCapabilities) {
	                return (Vector<VrCapabilities>) list;
	            } else if (obj instanceof String) {
	                Vector<VrCapabilities> newList = new Vector<VrCapabilities>();
	                for (Object hashObj : list) {
	                    String strFormat = (String)hashObj;
	                    VrCapabilities toAdd = null;
	                    try {
	                        toAdd = VrCapabilities.valueForString(strFormat);
	                    } catch (Exception e) {
	                    	DebugTool.logError("Failed to parse " + getClass().getSimpleName() + "." + RegisterAppInterfaceResponse.vrCapabilities, e);
	                    }
	                    if (toAdd != null) {
	                        newList.add(toAdd);
	                    }
	                }
	                return newList;
	            }
	        }
        }
        return null;
    }
    /**
     * Sets VrCapabilities
     * @param vrCapabilities
     */
    public void setVrCapabilities(Vector<VrCapabilities> vrCapabilities) {
        if (vrCapabilities != null) {
            parameters.put(RegisterAppInterfaceResponse.vrCapabilities, vrCapabilities);
        }
    }
	
	/**
	 * Gets getVehicleType set when application interface is registered.
	 * 
	 * @return vehicleType 
	 */
    public VehicleType getVehicleType() {
        Object obj = parameters.get(RegisterAppInterfaceResponse.vehicleType);
        if (obj instanceof VehicleType) {
        	return (VehicleType)obj;
        } else if (obj instanceof Hashtable) {
        	return new VehicleType((Hashtable)obj);
        }
        return null;
    }
    /**
     * Sets vehicleType
     * @param vehicleType
     */
    public void setVehicleType(VehicleType vehicleType) {
        if (vehicleType != null) {
            parameters.put(RegisterAppInterfaceResponse.vehicleType, vehicleType);
        }
    }
	
	/**
	 * Gets AudioPassThruCapabilities set when application interface is registered.
	 * 
	 * @return AudioPassThruCapabilities 
	 */
    public Vector<AudioPassThruCapabilities> getAudioPassThruCapabilities() {
        if (parameters.get(RegisterAppInterfaceResponse.audioPassThruCapabilities) instanceof Vector<?>) {
	    	Vector<?> list = (Vector<?>)parameters.get(RegisterAppInterfaceResponse.audioPassThruCapabilities);
	        if (list != null && list.size() > 0) {
	            Object obj = list.get(0);
	            if (obj instanceof AudioPassThruCapabilities) {
	                return (Vector<AudioPassThruCapabilities>) list;
	            } else if (obj instanceof Hashtable) {
	                Vector<AudioPassThruCapabilities> newList = new Vector<AudioPassThruCapabilities>();
	                for (Object hashObj : list) {
	                    newList.add(new AudioPassThruCapabilities((Hashtable)hashObj));
	                }
	                return newList;
	            }
	        }
        }
        return null;
    }
    /**
     * Sets AudioPassThruCapabilities
     * @param audioPassThruCapabilities
     */
    public void setAudioPassThruCapabilities(Vector<AudioPassThruCapabilities> audioPassThruCapabilities) {
        if (audioPassThruCapabilities != null) {
            parameters.put(RegisterAppInterfaceResponse.audioPassThruCapabilities, audioPassThruCapabilities);
        }
    }
    public String getProxyVersionInfo() {
		if (Version.VERSION != null)
			return  Version.VERSION;
	
		return null;
    }
    public void setSupportedDiagModes(Vector<Integer> supportedDiagModes) {
        if (supportedDiagModes != null) {
        	parameters.put(RegisterAppInterfaceResponse.supportedDiagModes, supportedDiagModes);
        }
        else
        {
        	parameters.remove(RegisterAppInterfaceResponse.supportedDiagModes);
        }
    }

    public Vector<Integer> getSupportedDiagModes() {
        
    	if (parameters.get(RegisterAppInterfaceResponse.supportedDiagModes) instanceof Vector<?>) {
        	Vector<?> list = (Vector<?>)parameters.get( RegisterAppInterfaceResponse.supportedDiagModes);
        	if (list != null && list.size() > 0) {
        		Object obj = list.get(0);
        		if (obj instanceof Integer) {
                	return (Vector<Integer>) list;        			
        		}
        	}
        }
        return null;
    }          
}
