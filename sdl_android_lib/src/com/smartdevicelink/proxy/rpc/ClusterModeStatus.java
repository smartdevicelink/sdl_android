package com.smartdevicelink.proxy.rpc;

import org.json.JSONObject;

import com.smartdevicelink.proxy.RPCObject;
import com.smartdevicelink.proxy.rpc.enums.CarModeStatus;
import com.smartdevicelink.proxy.rpc.enums.PowerModeQualificationStatus;
import com.smartdevicelink.proxy.rpc.enums.PowerModeStatus;
import com.smartdevicelink.util.JsonUtils;

public class ClusterModeStatus extends RPCObject {
    public static final String KEY_POWER_MODE_ACTIVE = "powerModeActive";
    public static final String KEY_POWER_MODE_QUALIFICATION_STATUS = "powerModeQualificationStatus";
    public static final String KEY_CAR_MODE_STATUS = "carModeStatus";
    public static final String KEY_POWER_MODE_STATUS = "powerModeStatus";

    private Boolean powerModeActive;
    private String powerModeStatus; // represents PowerModeStatus enum
    private String carModeStatus; // represents CarModeStatus enum
    private String powerModeQualificationStatus; // represents PowerModeQualificationStatus enum
    
    public ClusterModeStatus() { }
    
    /**
     * Creates a ClusterModeStatus object from a JSON object.
     * 
     * @param jsonObject The JSON object to read from
     */
    public ClusterModeStatus(JSONObject jsonObject) {
        switch(sdlVersion){
        default:
            this.powerModeActive = JsonUtils.readBooleanFromJsonObject(jsonObject, KEY_POWER_MODE_ACTIVE);
            this.powerModeStatus = JsonUtils.readStringFromJsonObject(jsonObject, KEY_POWER_MODE_STATUS);
            this.carModeStatus = JsonUtils.readStringFromJsonObject(jsonObject, KEY_CAR_MODE_STATUS);
            this.powerModeQualificationStatus = JsonUtils.readStringFromJsonObject(jsonObject, KEY_POWER_MODE_QUALIFICATION_STATUS);
            break;
        }
    }

    public void setPowerModeActive(Boolean powerModeActive) {
        this.powerModeActive = powerModeActive;
    }
    
    public Boolean getPowerModeActive() {
        return this.powerModeActive;
    }
    
    public void setPowerModeQualificationStatus(PowerModeQualificationStatus powerModeQualificationStatus) {
        this.powerModeQualificationStatus = (powerModeQualificationStatus == null) ? null : powerModeQualificationStatus.getJsonName(sdlVersion);
    }
    
    public PowerModeQualificationStatus getPowerModeQualificationStatus() {
        return PowerModeQualificationStatus.valueForJsonName(this.powerModeQualificationStatus, sdlVersion);
    }
    
    public void setCarModeStatus(CarModeStatus carModeStatus) {
        this.carModeStatus = (carModeStatus == null) ? null : carModeStatus.getJsonName(sdlVersion);
    }
    
    public CarModeStatus getCarModeStatus() {
        return CarModeStatus.valueForJsonName(this.carModeStatus, sdlVersion);
    }
    
    public void setPowerModeStatus(PowerModeStatus powerModeStatus) {
        this.powerModeStatus = (powerModeStatus == null) ? null : powerModeStatus.getJsonName(sdlVersion);
    }
    
    public PowerModeStatus getPowerModeStatus() {
        return PowerModeStatus.valueForJsonName(this.powerModeStatus, sdlVersion);
    }

    @Override
    public JSONObject getJsonParameters(int sdlVersion){
        JSONObject result = super.getJsonParameters(sdlVersion);

        switch(sdlVersion){
        default:
            JsonUtils.addToJsonObject(result, KEY_CAR_MODE_STATUS, this.carModeStatus);
            JsonUtils.addToJsonObject(result, KEY_POWER_MODE_ACTIVE, this.powerModeActive);
            JsonUtils.addToJsonObject(result, KEY_POWER_MODE_QUALIFICATION_STATUS, this.powerModeQualificationStatus);
            JsonUtils.addToJsonObject(result, KEY_POWER_MODE_STATUS, this.powerModeStatus);
            break;
        }
        
        return result;
    }

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((carModeStatus == null) ? 0 : carModeStatus.hashCode());
		result = prime * result + ((powerModeActive == null) ? 0 : powerModeActive.hashCode());
		result = prime * result + ((powerModeQualificationStatus == null) ? 0 : powerModeQualificationStatus.hashCode());
		result = prime * result + ((powerModeStatus == null) ? 0 : powerModeStatus.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) { 
			return true;
		}
		if (obj == null) { 
			return false;
		}
		if (getClass() != obj.getClass()) { 
			return false;
		}
		ClusterModeStatus other = (ClusterModeStatus) obj;
		if (carModeStatus == null) {
			if (other.carModeStatus != null) { 
				return false;
			}
		} else if (!carModeStatus.equals(other.carModeStatus)) { 
			return false;
		}
		if (powerModeActive == null) {
			if (other.powerModeActive != null) { 
				return false;
			}
		} else if (!powerModeActive.equals(other.powerModeActive)){ 
			return false;
		}
		if (powerModeQualificationStatus == null) {
			if (other.powerModeQualificationStatus != null) { 
				return false;
			}
		} else if (!powerModeQualificationStatus.equals(other.powerModeQualificationStatus)) { 
			return false;
		}
		if (powerModeStatus == null) {
			if (other.powerModeStatus != null) { 
				return false;
			}
		} else if (!powerModeStatus.equals(other.powerModeStatus)) { 
			return false;
		}
		return true;
	}
}
