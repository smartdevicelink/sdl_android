package com.smartdevicelink.proxy.rpc;

import java.util.Hashtable;
import java.util.Vector;

import com.smartdevicelink.proxy.RPCStruct;
import com.smartdevicelink.proxy.rpc.enums.HMILevel;
import com.smartdevicelink.util.DebugTool;
/**
 * Defining sets of HMI levels, which are permitted or prohibited for a given RPC.
 * <p><b>Parameter List
 * <table border="1" rules="all">
 * 		<tr>
 * 			<th>Name</th>
 * 			<th>Type</th>
 * 			<th>Description</th>
 * 			<th>SmartDeviceLink Ver. Available</th>
 * 		</tr>
 * 		<tr>
 * 			<td>allowed</td>
 * 			<td>HMILevel</td>
 * 			<td>A set of all HMI levels that are permitted for this given RPC.
 * 					<ul>
 *					<li>Min: 0</li>
 *					<li>Max: 100</li>
 *					</ul>
 * 			</td>
 * 			<td>SmartDeviceLink 2.0</td>
 * 		</tr>
 * 		<tr>
 * 			<td>userDisallowed</td>
 * 			<td>HMILevel</td>
 * 			<td>A set of all HMI levels that are prohibated for this given RPC.
 * 					<ul>
 *					<li>Min: 0</li>
 *					<li>Max: 100</li>
 *					</ul>
 * 			</td>
 * 			<td>SmartDeviceLink 2.0</td>
 * 		</tr>
 *  </table>
 * @since SmartDeviceLink 2.0
 */
public class HMIPermissions extends RPCStruct {
	public static final String allowed = "allowed";
	public static final String userDisallowed = "userDisallowed";
	/**
	 * Constructs a newly allocated HMIPermissions object
	 */
    public HMIPermissions() { }
    
    /**
     * Constructs a newly allocated HMIPermissions object indicated by the Hashtable parameter
     * @param hash The Hashtable to use
     */
    public HMIPermissions(Hashtable hash) {
        super(hash);
    }
    
    /**
     * get a set of all HMI levels that are permitted for this given RPC.
     * @return   a set of all HMI levels that are permitted for this given RPC
     */
    public Vector<HMILevel> getAllowed() {
        if (store.get(HMIPermissions.allowed) instanceof Vector<?>) {
	    	Vector<?> list = (Vector<?>)store.get(HMIPermissions.allowed);
	        if (list != null && list.size() > 0) {
	            Object obj = list.get(0);
	            if (obj instanceof HMILevel) {
	                return (Vector<HMILevel>) list;
	            } else if (obj instanceof String) {
	                Vector<HMILevel> newList = new Vector<HMILevel>();
	                for (Object hashObj : list) {
	                    String strFormat = (String)hashObj;
	                    HMILevel toAdd = null;
	                    try {
	                        toAdd = HMILevel.valueForString(strFormat);
	                    } catch (Exception e) {
	                    	DebugTool.logError("Failed to parse " + getClass().getSimpleName() + "." + HMIPermissions.allowed, e);
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
     * set  HMI level that is permitted for this given RPC.
     * @param allowed HMI level that is permitted for this given RPC
     */
    public void setAllowed(Vector<HMILevel> allowed) {
        if (allowed != null) {
            store.put(HMIPermissions.allowed, allowed);
        } else {
    		store.remove(HMIPermissions.allowed);
    	}
    }
    
    /**
     * get a set of all HMI levels that are prohibited for this given RPC
     * @return a set of all HMI levels that are prohibited for this given RPC
     */
    public Vector<HMILevel> getUserDisallowed() {
        if (store.get(HMIPermissions.userDisallowed) instanceof Vector<?>) {
	    	Vector<?> list = (Vector<?>)store.get(HMIPermissions.userDisallowed);
	        if (list != null && list.size() > 0) {
	            Object obj = list.get(0);
	            if (obj instanceof HMILevel) {
	                return (Vector<HMILevel>) list;
	            } else if (obj instanceof String) {
	                Vector<HMILevel> newList = new Vector<HMILevel>();
	                for (Object hashObj : list) {
	                    String strFormat = (String)hashObj;
	                    HMILevel toAdd = null;
	                    try {
	                        toAdd = HMILevel.valueForString(strFormat);
	                    } catch (Exception e) {
	                    	DebugTool.logError("Failed to parse " + getClass().getSimpleName() + "." + HMIPermissions.userDisallowed, e);
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
     * set a set of all HMI levels that are prohibited for this given RPC
     * @param userDisallowed  HMI level that is prohibited for this given RPC
     */
    public void setUserDisallowed(Vector<HMILevel> userDisallowed) {
        if (userDisallowed != null) {
            store.put(HMIPermissions.userDisallowed, userDisallowed);
        } else {
    		store.remove(HMIPermissions.userDisallowed);
    	}
    }
}
