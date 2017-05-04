package com.smartdevicelink.proxy.rpc;

import com.smartdevicelink.protocol.enums.FunctionID;
import com.smartdevicelink.proxy.RPCNotification;
import com.smartdevicelink.proxy.rpc.enums.TriggerSource;

import java.util.Hashtable;
import java.util.List;

public class OnSdlChoiceChosen extends RPCNotification {
	public static final String KEY_SDL_CHOICE = "sdlChoice";
	public static final String KEY_TRIGGER_SOURCE = "triggerSource";
	
	public class SdlSubMenu {
		private Integer _menuID = null;
		@SuppressWarnings("unused")
        private Integer _position = null;
		private String _menuName = null;
		
		// Constructor
		SdlSubMenu(Integer menuID, Integer position, String menuName) {
			_menuID = menuID;
			_position = position;
			_menuName = menuName;
		}
		
		// Restrict no-arg constructor
		@SuppressWarnings("unused")
        private SdlSubMenu() {}
		
		// Public Getters
		public Integer getMenuID() {
			return _menuID;
		}
		
		public String getMenuName() {
			return _menuName;
		}
		
		public String toString() {
			return _menuName;
		}
	}
	
	public class SdlCommand {
		private Integer _commandID = null;
		private SdlSubMenu _parentSubMenu = null;
		@SuppressWarnings("unused")
        private Integer _position = null;
		private String _menuName = null;
		private List<String> _vrCommands = null;
		
		// Constructor
		SdlCommand(Integer commandID, SdlSubMenu parentSubMenu, Integer position, String menuName, List<String> vrCommands) {
			_commandID = commandID;
			_parentSubMenu = parentSubMenu;
			_position = position;
			_menuName = menuName;
			_vrCommands = vrCommands;
		}
		
		// Restrict no-arg constructor
		@SuppressWarnings("unused")
        private SdlCommand() {}
		
		// Public Getters
		public Integer getCommandID() {
			return _commandID;
		}
		
		public SdlSubMenu getParentSubMenu() {
			return _parentSubMenu;
		}
		
		public String getMenuName() {
			return _menuName;
		}
		
		public List<String> getVrCommands() {
			return _vrCommands;
		}
		
		public String toString() {
			return _menuName;
		}
	}
	
	public class SdlChoiceSet {
		private Integer _choiceSetID = null;
		private List<Choice> _choiceSet = null;
		
		// Constructor
		SdlChoiceSet(Integer choiceSetID, List<Choice> choiceSet) {
			_choiceSetID = choiceSetID;
			_choiceSet = choiceSet;
		}
		
		public Integer getChoiceSetID() {
			return _choiceSetID;
		}
		
		public List<Choice> getChoiceSet() {
			return _choiceSet;
		}
	}
	
	
	

	public OnSdlChoiceChosen() {
		super(FunctionID.ON_SDL_CHOICE_CHOSEN.toString());
	}
	public OnSdlChoiceChosen(Hashtable<String, Object> hash){
		super(hash);
	}
    public Choice getSdlChoice() {
    	return (Choice) parameters.get(KEY_SDL_CHOICE);
    }
    public void setSdlChoice(Choice sdlChoice) {
    	if (sdlChoice != null) {
    		parameters.put(KEY_SDL_CHOICE, sdlChoice);
    	} else {
            parameters.remove(KEY_SDL_CHOICE);
        }
    }
    public TriggerSource getTriggerSource() {
        Object obj = parameters.get(KEY_TRIGGER_SOURCE);
        if (obj instanceof TriggerSource) {
            return (TriggerSource) obj;
        } else if (obj instanceof String) {
            return TriggerSource.valueForString((String) obj);
        }
        return null;
    }
    public void setTriggerSource( TriggerSource triggerSource ) {
        if (triggerSource != null) {
            parameters.put(KEY_TRIGGER_SOURCE, triggerSource );
        } else {
        	parameters.remove(KEY_TRIGGER_SOURCE);
        }
    }
}
