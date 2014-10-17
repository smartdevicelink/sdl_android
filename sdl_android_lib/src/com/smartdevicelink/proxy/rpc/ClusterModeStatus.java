package com.smartdevicelink.proxy.rpc;

import java.util.Hashtable;

import com.smartdevicelink.proxy.RPCStruct;
import com.smartdevicelink.proxy.rpc.enums.CarModeStatus;
import com.smartdevicelink.proxy.rpc.enums.PowerModeQualificationStatus;
import com.smartdevicelink.proxy.rpc.enums.PowerModeStatus;
import com.smartdevicelink.util.DebugTool;

public class ClusterModeStatus extends RPCStruct {
    public static final String powerModeActive = "powerModeActive";
    public static final String powerModeQualificationStatus = "powerModeQualificationStatus";
    public static final String carModeStatus = "carModeStatus";
    public static final String powerModeStatus = "powerModeStatus";

	    public ClusterModeStatus() { }
	    public ClusterModeStatus(Hashtable hash) {
	        super(hash);
	    }

	    public void setPowerModeActive(Boolean powerModeActive) {
	        if (powerModeActive != null) {
	        	store.put(ClusterModeStatus.powerModeActive, powerModeActive);
	        } else {
	        	store.remove(ClusterModeStatus.powerModeActive);
	        }
	    }
	    public Boolean getPowerModeActive() {
	        return (Boolean) store.get(ClusterModeStatus.powerModeActive);
	    }
	    public void setPowerModeQualificationStatus(PowerModeQualificationStatus powerModeQualificationStatus) {
	        if (powerModeQualificationStatus != null) {
	            store.put(ClusterModeStatus.powerModeQualificationStatus, powerModeQualificationStatus);
	        } else {
	        	store.remove(ClusterModeStatus.powerModeQualificationStatus);
	        }
	    }
	    public PowerModeQualificationStatus getPowerModeQualificationStatus() {
	        Object obj = store.get(ClusterModeStatus.powerModeQualificationStatus);
	        if (obj instanceof PowerModeQualificationStatus) {
	            return (PowerModeQualificationStatus) obj;
	        } else if (obj instanceof String) {
	        	PowerModeQualificationStatus theCode = null;
	            try {
	                theCode = PowerModeQualificationStatus.valueForString((String) obj);
	            } catch (Exception e) {
	                DebugTool.logError("Failed to parse " + getClass().getSimpleName() + "." + ClusterModeStatus.powerModeQualificationStatus, e);
	            }
	            return theCode;
	        }
	        return null;
	    }
	    public void setCarModeStatus(CarModeStatus carModeStatus) {
	        if (carModeStatus != null) {
	            store.put(ClusterModeStatus.carModeStatus, carModeStatus);
	        } else {
	        	store.remove(ClusterModeStatus.carModeStatus);
	        }
	    }
	    public CarModeStatus getCarModeStatus() {
	        Object obj = store.get(ClusterModeStatus.carModeStatus);
	        if (obj instanceof CarModeStatus) {
	            return (CarModeStatus) obj;
	        } else if (obj instanceof String) {
	        	CarModeStatus theCode = null;
	            try {
	                theCode = CarModeStatus.valueForString((String) obj);
	            } catch (Exception e) {
	                DebugTool.logError("Failed to parse " + getClass().getSimpleName() + "." + ClusterModeStatus.carModeStatus, e);
	            }
	            return theCode;
	        }
	        return null;
	    }
	    public void setPowerModeStatus(PowerModeStatus powerModeStatus) {
	        if (powerModeStatus != null) {
	            store.put(ClusterModeStatus.powerModeStatus, powerModeStatus);
	        } else {
	        	store.remove(ClusterModeStatus.powerModeStatus);
	        }
	    }
	    public PowerModeStatus getPowerModeStatus() {
	        Object obj = store.get(ClusterModeStatus.powerModeStatus);
	        if (obj instanceof PowerModeStatus) {
	            return (PowerModeStatus) obj;
	        } else if (obj instanceof String) {
	        	PowerModeStatus theCode = null;
	            try {
	                theCode = PowerModeStatus.valueForString((String) obj);
	            } catch (Exception e) {
	                DebugTool.logError("Failed to parse " + getClass().getSimpleName() + "." + ClusterModeStatus.powerModeStatus, e);
	            }
	            return theCode;
	        }
	        return null;
	    }
}
