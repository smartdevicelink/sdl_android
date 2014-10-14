package com.smartdevicelink.proxy.rpc;

import java.util.Hashtable;
import java.util.Vector;

import com.smartdevicelink.proxy.RPCRequest;
import com.smartdevicelink.proxy.rpc.enums.TextAlignment;
import com.smartdevicelink.util.DebugTool;

/**
 * Updates the application's display text area, regardless of whether or not
 * this text area is visible to the user at the time of the request. The
 * application's display text area remains unchanged until updated by subsequent
 * calls to Show
 * <p>
 * The content of the application's display text area is visible to the user
 * when the application's {@linkplain com.smartdevicelink.proxy.rpc.enums.HMILevel}
 * is FULL or LIMITED, and the
 * {@linkplain com.smartdevicelink.proxy.rpc.enums.SystemContext}=MAIN and no
 * {@linkplain Alert} is in progress
 * <p>
 * The Show operation cannot be used to create an animated scrolling screen. To
 * avoid distracting the driver, Show commands cannot be issued more than once
 * every 4 seconds. Requests made more frequently than this will be rejected
 * <p>
 * <b>HMILevel needs to be FULL, LIMITED or BACKGROUND</b>
 * </p>
 * 
 * @since SmartDeviceLink 1.0
 * @see Alert
 * @see SetMediaClockTimer
 */
public class Show extends RPCRequest {
	public static final String graphic = "graphic";
	public static final String customPresets = "customPresets";
	public static final String mainField1 = "mainField1";
	public static final String mainField2 = "mainField2";
	public static final String mainField3 = "mainField3";
	public static final String mainField4 = "mainField4";
	public static final String statusBar = "statusBar";
	public static final String mediaClock = "mediaClock";
	public static final String alignment = "alignment";
	public static final String mediaTrack = "mediaTrack";
	public static final String secondaryGraphic = "secondaryGraphic";
	public static final String softButtons = "softButtons";
	/**
	 * Constructs a new Show object
	 */
	public Show() {
        super("Show");
    }
	/**
	 * Constructs a new Show object indicated by the Hashtable parameter
	 * <p>
	 * 
	 * @param hash
	 *            The Hashtable to use
	 */
    public Show(Hashtable hash) {
        super(hash);
    }
	/**
	 * Gets the text displayed in a single-line display, or in the upper display
	 * line in a two-line display
	 * 
	 * @return String -a String value representing the text displayed in a
	 *         single-line display, or in the upper display line in a two-line
	 *         display
	 */    
    public String getMainField1() {
        return (String) parameters.get(Show.mainField1);
    }
	/**
	 * Sets the text displayed in a single-line display, or in the upper display
	 * line in a two-line display
	 * 
	 * @param mainField1
	 *            the String value representing the text displayed in a
	 *            single-line display, or in the upper display line in a
	 *            two-line display
	 *            <p>
	 *            <b>Notes: </b>
	 *            <ul>
	 *            <li>If this parameter is omitted, the text of mainField1 does
	 *            not change</li>
	 *            <li>If this parameter is an empty string, the field will be
	 *            cleared</li>
	 *            </ul>
	 */    
    public void setMainField1(String mainField1) {
        if (mainField1 != null) {
            parameters.put(Show.mainField1, mainField1);
        } else {
        	parameters.remove(Show.mainField1);
        }
    }
	/**
	 * Gets the text displayed on the second display line of a two-line display
	 * 
	 * @return String -a String value representing the text displayed on the
	 *         second display line of a two-line display
	 */    
    public String getMainField2() {
        return (String) parameters.get(Show.mainField2);
    }
	/**
	 * Sets the text displayed on the second display line of a two-line display
	 * 
	 * @param mainField2
	 *            the String value representing the text displayed on the second
	 *            display line of a two-line display
	 *            <p>
	 *            <b>Notes: </b>
	 *            <ul>
	 *            <li>If this parameter is omitted, the text of mainField2 does
	 *            not change</li>
	 *            <li>If this parameter is an empty string, the field will be
	 *            cleared</li>
	 *            <li>If provided and the display is a single-line display, the
	 *            parameter is ignored</li>
	 *            <li>Maxlength = 500</li>
	 *            </ul>
	 */    
    public void setMainField2(String mainField2) {
        if (mainField2 != null) {
            parameters.put(Show.mainField2, mainField2);
        } else {
        	parameters.remove(Show.mainField2);
        }
    }

	/**
	 * Gets the text displayed on the first display line of the second page
	 * 
	 * @return String -a String value representing the text displayed on the
	 *         first display line of the second page
	 * @since SmartDeviceLink 2.0
	 */
    public String getMainField3() {
        return (String) parameters.get(Show.mainField3);
    }

	/**
	 * Sets the text displayed on the first display line of the second page
	 * 
	 * @param mainField3
	 *            the String value representing the text displayed on the first
	 *            display line of the second page
	 *            <p>
	 *            <b>Notes: </b>
	 *            <ul>
	 *            <li>If this parameter is omitted, the text of mainField3 does
	 *            not change</li>
	 *            <li>If this parameter is an empty string, the field will be
	 *            cleared</li>
	 *            <li>If provided and the display is a single-line display, the
	 *            parameter is ignored</li>
	 *            <li>Maxlength = 500</li>
	 *            </ul>
	 * @since SmartDeviceLink 2.0
	 */
    public void setMainField3(String mainField3) {
        if (mainField3 != null) {
            parameters.put(Show.mainField3, mainField3);
        } else {
        	parameters.remove(Show.mainField3);
        }
    }

	/**
	 * Gets the text displayed on the second display line of the second page
	 * 
	 * @return String -a String value representing the text displayed on the
	 *         first display line of the second page
	 * @since SmartDeviceLink 2.0
	 */
    public String getMainField4() {
        return (String) parameters.get(Show.mainField4);
    }

	/**
	 * Sets the text displayed on the second display line of the second page
	 * 
	 * @param mainField4
	 *            the String value representing the text displayed on the second
	 *            display line of the second page
	 *            <p>
	 *            <b>Notes: </b>
	 *            <ul>
	 *            <li>If this parameter is omitted, the text of mainField4 does
	 *            not change</li>
	 *            <li>If this parameter is an empty string, the field will be
	 *            cleared</li>
	 *            <li>If provided and the display is a single-line display, the
	 *            parameter is ignored</li>
	 *            <li>Maxlength = 500</li>
	 *            </ul>
	 * @since SmartDeviceLink 2.0
	 */
    public void setMainField4(String mainField4) {
        if (mainField4 != null) {
            parameters.put(Show.mainField4, mainField4);
        } else {
        	parameters.remove(Show.mainField4);
        }
    }
	/**
	 * Gets the alignment that Specifies how mainField1 and mainField2 text
	 * should be aligned on display
	 * 
	 * @return TextAlignment -an Enumeration value
	 */    
    public TextAlignment getAlignment() {
        Object obj = parameters.get(Show.alignment);
        if (obj instanceof TextAlignment) {
            return (TextAlignment) obj;
        } else if (obj instanceof String) {
            TextAlignment theCode = null;
            try {
                theCode = TextAlignment.valueForString((String) obj);
            } catch (Exception e) {
            	DebugTool.logError("Failed to parse " + getClass().getSimpleName() + "." + Show.alignment, e);
            }
            return theCode;
        }
        return null;
    }
	/**
	 * Sets the alignment that Specifies how mainField1 and mainField2 text
	 * should be aligned on display
	 * 
	 * @param alignment
	 *            an Enumeration value
	 *            <p>
	 *            <b>Notes: </b>
	 *            <ul>
	 *            <li>Applies only to mainField1 and mainField2 provided on this
	 *            call, not to what is already showing in display</li>
	 *            <li>If this parameter is omitted, text in both mainField1 and
	 *            mainField2 will be centered</li>
	 *            <li>Has no effect with navigation display</li>
	 *            </ul>
	 */    
    public void setAlignment(TextAlignment alignment) {
        if (alignment != null) {
            parameters.put(Show.alignment, alignment);
        } else {
        	parameters.remove(Show.alignment);
        }
    }
	/**
	 * Gets text in the Status Bar
	 * 
	 * @return String -the value in the Status Bar
	 */    
    public String getStatusBar() {
        return (String) parameters.get(Show.statusBar);
    }
	/**
	 * Sets text in the Status Bar
	 * 
	 * @param statusBar
	 *            a String representing the text you want to add in the Status
	 *            Bar
	 *            <p>
	 *            <b>Notes: </b><i>The status bar only exists on navigation
	 *            displays</i><br/>
	 *            <ul>
	 *            <li>If this parameter is omitted, the status bar text will
	 *            remain unchanged</li>
	 *            <li>If this parameter is an empty string, the field will be
	 *            cleared</li>
	 *            <li>If provided and the display has no status bar, this
	 *            parameter is ignored</li>
	 *            </ul>
	 */    
    public void setStatusBar(String statusBar) {
        if (statusBar != null) {
            parameters.put(Show.statusBar, statusBar);
        } else {
        	parameters.remove(Show.statusBar);
        }
    }
	/**
	 * Gets the String value of the MediaClock
	 * 
	 * @return String -a String value of the MediaClock
	 */ 
	@Deprecated	 
    public String getMediaClock() {
        return (String) parameters.get(Show.mediaClock);
    }
	/**
	 * Sets the value for the MediaClock field using a format described in the
	 * MediaClockFormat enumeration
	 * 
	 * @param mediaClock
	 *            a String value for the MdaiaClock
	 *            <p>
	 *            <b>Notes: </b><br/>
	 *            <ul>
	 *            <li>Must be properly formatted as described in the
	 *            MediaClockFormat enumeration</li>
	 *            <li>If a value of five spaces is provided, this will clear
	 *            that field on the display (i.e. the media clock timer field
	 *            will not display anything)</li>
	 *            </ul>
	 */
	@Deprecated
    public void setMediaClock(String mediaClock) {
        if (mediaClock != null) {
            parameters.put(Show.mediaClock, mediaClock);
        } else {
        	parameters.remove(Show.mediaClock);
        }
    }
	/**
	 * Gets the text in the track field
	 * 
	 * @return String -a String displayed in the track field
	 */    
    public String getMediaTrack() {
        return (String) parameters.get(Show.mediaTrack);
    }
	/**
	 * Sets the text in the track field
	 * 
	 * @param mediaTrack
	 *            a String value disaplayed in the track field
	 *            <p>
	 *            <b>Notes: </b><br/>
	 *            <ul>
	 *            <li>If parameter is omitted, the track field remains unchanged</li>
	 *            <li>If an empty string is provided, the field will be cleared</li>
	 *            <li>This field is only valid for media applications on navigation displays</li>
	 *            </ul>
	 */    
    public void setMediaTrack(String mediaTrack) {
        if (mediaTrack != null) {
            parameters.put(Show.mediaTrack, mediaTrack);
        } else {
        	parameters.remove(Show.mediaTrack);
        }
    }

	/**
	 * Sets an image to be shown on supported displays
	 * 
	 * @param graphic
	 *            the value representing the image shown on supported displays
	 *            <p>
	 *            <b>Notes: </b>If omitted on supported displays, the displayed
	 *            graphic shall not change<br/>
	 * @since SmartDeviceLink 2.0
	 */
    public void setGraphic(Image graphic) {
        if (graphic != null) {
            parameters.put(Show.graphic, graphic);
        } else {
        	parameters.remove(Show.graphic);
        }
    }

	/**
	 * Gets an image to be shown on supported displays
	 * 
	 * @return Image -the value representing the image shown on supported
	 *         displays
	 * @since SmartDeviceLink 2.0
	 */
    public Image getGraphic() {
    	Object obj = parameters.get(Show.graphic);
        if (obj instanceof Image) {
            return (Image) obj;
        } else if (obj instanceof Hashtable) {
        	try {
        		return new Image((Hashtable) obj);
            } catch (Exception e) {
            	DebugTool.logError("Failed to parse " + getClass().getSimpleName() + "." + Show.graphic, e);
            }
        }
        return null;
    }

    
    public void setSecondaryGraphic(Image secondaryGraphic) {
        if (secondaryGraphic != null) {
            parameters.put(Show.secondaryGraphic, secondaryGraphic);
        } else {
        	parameters.remove(Show.secondaryGraphic);
        }
    }


    public Image setSecondaryGraphic() {
    	Object obj = parameters.get(Show.secondaryGraphic);
        if (obj instanceof Image) {
            return (Image) obj;
        } else if (obj instanceof Hashtable) {
        	try {
        		return new Image((Hashtable) obj);
            } catch (Exception e) {
            	DebugTool.logError("Failed to parse " + getClass().getSimpleName() + "." + Show.secondaryGraphic, e);
            }
        }
        return null;
    }    
    
    
	/**
	 * Gets the Soft buttons defined by the App
	 * 
	 * @return Vector<SoftButton> -a Vector value representing the Soft buttons
	 *         defined by the App
	 * @since SmartDeviceLink 2.0
	 */
    public Vector<SoftButton> getSoftButtons() {
        if (parameters.get(Show.softButtons) instanceof Vector<?>) {
	    	Vector<?> list = (Vector<?>)parameters.get(Show.softButtons);
	        if (list != null && list.size() > 0) {
	            Object obj = list.get(0);
	            if (obj instanceof SoftButton) {
	                return (Vector<SoftButton>) list;
	            } else if (obj instanceof Hashtable) {
	                Vector<SoftButton> newList = new Vector<SoftButton>();
	                for (Object hashObj : list) {
	                    newList.add(new SoftButton((Hashtable)hashObj));
	                }
	                return newList;
	            }
	        }
        }
        return null;
    }

	/**
	 * Sets the the Soft buttons defined by the App
	 * 
	 * @param softButtons
	 *            a Vector value represemting the Soft buttons defined by the
	 *            App
	 *            <p>
	 *            <b>Notes: </b><br/>
	 *            <ul>
	 *            <li>If omitted on supported displays, the currently displayed
	 *            SoftButton values will not change</li>
	 *            <li>Array Minsize: 0</li>
	 *            <li>Array Maxsize: 8</li>
	 *            </ul>
	 * 
	 * @since SmartDeviceLink 2.0
	 */
    public void setSoftButtons(Vector<SoftButton> softButtons) {
        if (softButtons != null) {
            parameters.put(Show.softButtons, softButtons);
        } else {
        	parameters.remove(Show.softButtons);
        }
    }

	/**
	 * Gets the Custom Presets defined by the App
	 * 
	 * @return Vector<String> - a Vector value representing the Custom presets
	 *         defined by the App
	 * @since SmartDeviceLink 2.0
	 */
    public Vector<String> getCustomPresets() {
    	if (parameters.get(Show.customPresets) instanceof Vector<?>) {
    		Vector<?> list = (Vector<?>)parameters.get(Show.customPresets);
    		if (list != null && list.size()>0) {
    			Object obj = list.get(0);
    			if (obj instanceof String) {
    				return (Vector<String>) list;
    			}
    		}
    	}
        return null;
    }

	/**
	 * Sets the Custom Presets defined by the App
	 * 
	 * @param customPresets
	 *            a Vector value representing the Custom Presets defined by the
	 *            App
	 *            <p>
	 *            <ul>
	 *            <li>If omitted on supported displays, the presets will be shown as not defined</li>
	 *            <li>Array Minsize: 0</li>
	 *            <li>Array Maxsize: 6</li>
	 *            </ul>
	 * @since SmartDeviceLink 2.0
	 */
    public void setCustomPresets(Vector<String> customPresets) {
        if (customPresets != null) {
            parameters.put(Show.customPresets, customPresets);
        } else {
        	parameters.remove(Show.customPresets);
        }
    }
}
