package com.smartdevicelink.proxy.rpc;

import com.smartdevicelink.proxy.RPCStruct;

import java.util.Hashtable;
/** The image resolution of this field.
 * 
 * <p><b>Parameter List</b>
 * <p>
 * <table border="1" rules="all">
 * 		<tr>
 * 			<th>Name</th>
 * 			<th>Type</th>
 * 			<th>Description</th>
 *                 <th>Reg.</th>
 *               <th>Notes</th>
 * 			<th>Version</th>
 * 		</tr>
 * 		<tr>
 * 			<td>resolutionWidth</td>
 * 			<td>Integer</td>
 * 			<td>The image resolution width.</td>
 *                 <td></td>
 *                 <td>minvalue:1<br> maxvalue: 10000</td>
 * 			<td>SmartDeviceLink 3.0 </td>
 * 		</tr>
 * 		<tr>
 * 			<td>resolutionHeight</td>
 * 			<td>Integer</td>
 * 			<td>The image resolution height.</td>
 *                 <td></td>
 *                 <td>minvalue:1<br> maxvalue: 10000</td>
 * 			<td>SmartDeviceLink 3.0 </td>
 * 		</tr>
 * 		</tr>
 *  </table>
 * @since SmartDeviceLink 3.0
 * @see DisplayType
 * @see MediaClockFormat
 * @see TextFieldName
 * @see ImageType 
 * @see ImageType GetCapabilities 

 *  
 */

public class ImageResolution extends RPCStruct {
	public static final String KEY_RESOLUTION_WIDTH = "resolutionWidth";
	public static final String KEY_RESOLUTION_HEIGHT = "resolutionHeight";
	
    public ImageResolution() {}
    
    public ImageResolution(Hashtable<String, Object> hash) {
        super(hash);
    }
    
    public void setResolutionWidth(Integer resolutionWidth) {
        if (resolutionWidth != null) {
            store.put(KEY_RESOLUTION_WIDTH, resolutionWidth);
        } else {
        	store.remove(KEY_RESOLUTION_WIDTH);
        }
    }
    
    public Integer getResolutionWidth() {
        return (Integer) store.get(KEY_RESOLUTION_WIDTH);
    }
    
    public void setResolutionHeight(Integer resolutionHeight) {
        if (resolutionHeight != null) {
            store.put(KEY_RESOLUTION_HEIGHT, resolutionHeight);
        } else {
        	store.remove(KEY_RESOLUTION_HEIGHT);
        }
    }
    
    public Integer getResolutionHeight() {
        return (Integer) store.get(KEY_RESOLUTION_HEIGHT);
    }
}
