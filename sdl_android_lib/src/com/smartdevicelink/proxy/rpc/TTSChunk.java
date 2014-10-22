package com.smartdevicelink.proxy.rpc;

import java.util.Hashtable;

import com.smartdevicelink.proxy.RPCStruct;
import com.smartdevicelink.proxy.rpc.enums.SpeechCapabilities;
import com.smartdevicelink.util.DebugTool;

/**
 * Specifies what is to be spoken. This can be simply a text phrase, which SDL will speak according to its own rules.
 *  It can also be phonemes from either the Microsoft SAPI phoneme set, or from the LHPLUS phoneme set. 
 *  It can also be a pre-recorded sound in WAV format (either developer-defined, or provided by the SDL platform).
 *  
 *  <p>In SDL, words, and therefore sentences, can be built up from phonemes and are used to explicitly provide the proper pronounciation to the TTS engine.
 *   For example, to have SDL pronounce the word "read" as "red", rather than as when it is pronounced like "reed",
 *   the developer would use phonemes to express this desired pronounciation.
 *  <p>For more information about phonemes, see <a href="http://en.wikipedia.org/wiki/Phoneme">http://en.wikipedia.org/wiki/Phoneme</a>.
 *  <p><b> Parameter List
 * <table border="1" rules="all">
 * 		<tr>
 * 			<th>Name</th>
 * 			<th>Type</th>
 * 			<th>Description</th>
 * 			<th>SmartDeviceLink Ver. Available</th>
 * 		</tr>
 * 		<tr>
 * 			<td>text</td>
 * 			<td>String</td>
 * 			<td>Text to be spoken, or a phoneme specification, or the name of a pre-recorded sound. The contents of this field are indicated by the "type" field.</td>
 * 			<td>SmartDeviceLink 1.0</td>
 * 		</tr>
 * 		<tr>
 * 			<td>type</td>
 * 			<td>SpeechCapabilities</td>
 * 			<td>Indicates the type of information in the "text" field (e.g. phrase to be spoken, phoneme specification, name of pre-recorded sound).	</td>
 * 			<td>SmartDeviceLink 1.0</td>
 * 		</tr>
 *  </table>
 * @since SmartDeviceLink 1.0
 */
public class TTSChunk extends RPCStruct {
	public static final String text = "text";
	public static final String type = "type";
	/**
	 * Constructs a newly allocated TTSChunk object
	 */
    public TTSChunk() { }
    /**
     * Constructs a newly allocated TTSChunk object indicated by the Hashtable parameter
     * @param hash The Hashtable to use
     */    
    public TTSChunk(Hashtable hash) {
        super(hash);
    }
    /**
     * Get text to be spoken, or a phoneme specification, or the name of a pre-recorded sound. The contents of this field are indicated by the "type" field.
     * @return text to be spoken, or a phoneme specification, or the name of a pre-recorded sound
     */    
    public String getText() {
        return (String) store.get( TTSChunk.text );
    }
    /**
     * Set the text to be spoken, or a phoneme specification, or the name of a pre-recorded sound. The contents of this field are indicated by the "type" field.
     * @param text to be spoken, or a phoneme specification, or the name of a pre-recorded sound.
     */    
    public void setText( String text ) {
        if (text != null) {
            store.put(TTSChunk.text, text );
        } else {
        	store.remove(TTSChunk.text);
        }
    }
    /**
     * Get the type of information in the "text" field (e.g. phrase to be spoken, phoneme specification, name of pre-recorded sound).	
     * @return the type of information in the "text" field
     */    
    public SpeechCapabilities getType() {
        Object obj = store.get(TTSChunk.type);
        if (obj instanceof SpeechCapabilities) {
            return (SpeechCapabilities) obj;
        } else if (obj instanceof String) {
            SpeechCapabilities theCode = null;
            try {
                theCode = SpeechCapabilities.valueForString((String) obj);
            } catch (Exception e) {
            	DebugTool.logError("Failed to parse " + getClass().getSimpleName() + "." + TTSChunk.type, e);
            }
            return theCode;
        }
        return null;
    }
    /**
     * Set the type of information in the "text" field (e.g. phrase to be spoken, phoneme specification, name of pre-recorded sound).	
     * @param type the type of information in the "text" field
     */    
    public void setType( SpeechCapabilities type ) {
        if (type != null) {
            store.put(TTSChunk.type, type );
        } else {
        	store.remove(TTSChunk.type);
        }
    }
}
