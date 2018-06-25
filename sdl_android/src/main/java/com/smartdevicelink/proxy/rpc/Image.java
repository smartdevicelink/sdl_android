package com.smartdevicelink.proxy.rpc;

import android.support.annotation.NonNull;

import com.smartdevicelink.proxy.RPCStruct;
import com.smartdevicelink.proxy.rpc.enums.ImageType;

import java.util.Hashtable;

/**
 *Specifies, which image shall be used, e.g. in Alerts or on Softbuttons provided the display supports it.
 *<p><b>Parameter List</b></p>
 * <table border="1" rules="all">
 * 		<tr>
 * 			<th>Name</th>
 * 			<th>Type</th>
 * 			<th>Description</th>
 * 			<th>SmartDeviceLink Ver. Available</th>
 * 		</tr>
 * 		<tr>
 * 			<td>value</td>
 * 			<td>String</td>
 * 			<td>Either the static hex icon value or the binary image file name identifier (sent by PutFile).
 * 					<ul>
 *					<li>Min: 0</li>
 *					<li>Max: 65535</li>
 *					</ul>
 * 			</td>
 * 			<td>SmartDeviceLink 2.0</td>
 * 		</tr>
 * 		<tr>
 * 			<td>imageType</td>
 * 			<td>ImageType</td>
 * 			<td>Describes, whether it is a static or dynamic image.</td>
 * 			<td>SmartDeviceLink 2.0</td>
 * 		</tr>
 *  </table>
 * @since SmartDeviceLink 2.0
 */
public class Image extends RPCStruct {
	public static final String KEY_VALUE = "value";
	public static final String KEY_IMAGE_TYPE = "imageType";
	public static final String KEY_IS_TEMPLATE = "isTemplate";

	/**
	 * Constructs a newly allocated Image object
	 */
    public Image() { }
    
    /**
     * Constructs a newly allocated Image object indicated by the Hashtable parameter
     * @param hash The Hashtable to use
     */      
    public Image(Hashtable<String, Object> hash) {
        super(hash);
    }

    /**
     * Constructs a newly allocated Image object
     * @param value either the static hex icon value or the binary image file name identifier (sent by PutFile)
     * @param imageType whether it is a static or dynamic image
     */
    public Image(@NonNull String value, @NonNull ImageType imageType) {
        this();
        setValue(value);
        setImageType(imageType);
    }

    /**
     * set either the static hex icon value or the binary image file name identifier (sent by PutFile)
     * @param value either the static hex icon value or the binary image file name identifier (sent by PutFile)
     */
    public void setValue(@NonNull String value) {
        setValue(KEY_VALUE, value);
    }
    
    /**
     * get either the static hex icon value or the binary image file name identifier (sent by PutFile)
     * @return  either the static hex icon value or the binary image file name identifier (sent by PutFile)
     */
    public String getValue() {
        return getString(KEY_VALUE);
    }
    
    /**
     * set the image type
     * @param imageType whether it is a static or dynamic image
     */
    public void setImageType(@NonNull ImageType imageType) {
        setValue(KEY_IMAGE_TYPE, imageType);
    }
    
    /**
     * get image type
     * @return the image type
     */
    public ImageType getImageType() {
        return (ImageType) getObject(ImageType.class, KEY_IMAGE_TYPE);
    }

    /**
     * set whether the image is a template
     * @param isTemplate boolean that tells whether it is a template image
     */
    public void setIsTemplate(Boolean isTemplate){
        setValue(KEY_IS_TEMPLATE, isTemplate);
    }

    /**
     * get whether the image is a template
     * @return boolean that tells whether it is a template image
     */
    public Boolean getIsTemplate(){
        return getBoolean(KEY_IS_TEMPLATE);
    }
}
