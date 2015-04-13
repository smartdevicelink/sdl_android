package com.smartdevicelink.test.rpc.datatypes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import junit.framework.TestCase;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.smartdevicelink.marshal.JsonRPCMarshaller;
import com.smartdevicelink.proxy.rpc.DisplayCapabilities;
import com.smartdevicelink.proxy.rpc.ImageField;
import com.smartdevicelink.proxy.rpc.ImageResolution;
import com.smartdevicelink.proxy.rpc.ScreenParams;
import com.smartdevicelink.proxy.rpc.TextField;
import com.smartdevicelink.proxy.rpc.TouchEventCapabilities;
import com.smartdevicelink.proxy.rpc.enums.CharacterSet;
import com.smartdevicelink.proxy.rpc.enums.DisplayType;
import com.smartdevicelink.proxy.rpc.enums.FileType;
import com.smartdevicelink.proxy.rpc.enums.ImageFieldName;
import com.smartdevicelink.proxy.rpc.enums.MediaClockFormat;
import com.smartdevicelink.proxy.rpc.enums.TextFieldName;
import com.smartdevicelink.test.utils.JsonUtils;
import com.smartdevicelink.test.utils.Validator;

public class DisplayCapabilitiesTests extends TestCase{

    private static final boolean                GRAPHIC_SUPPORTED              = true;
    private static final int                    CUSTOM_PRESETS_AVAILABLE       = 5;
    private static final DisplayType            DISPLAY_TYPE                   = DisplayType.NGN;

    // screen params constants
    private static final int                    SCREEN_IMAGE_RESOLUTION_WIDTH  = 1250;
    private static final int                    SCREEN_IMAGE_RESOLUTION_HEIGHT = 1080;
    private static final boolean                TOUCH_AVAILABLE                = true;
    private static final boolean                MULTI_TOUCH_AVAILABLE          = false;
    private static final boolean                DOUBLE_PRESS_AVAILABLE         = false;
    
    // textFields constants
    private static final TextFieldName[]        TEXT_FIELD_NAMES               = new TextFieldName[] {
            TextFieldName.alertText1, TextFieldName.alertText2, TextFieldName.mainField1 };
    private static final int[]                  TEXT_FIELD_WIDTHS              = new int[] { 25, 25, 30 };
    private static final int                    TEXT_FIELD_ROWS                = 1;
    private static final CharacterSet           CHARACTER_SET                  = CharacterSet.TYPE2SET;
    
    
    // imageFields constants
    private static final ImageFieldName[]       IMAGE_FIELD_NAMES              = new ImageFieldName[] {
            ImageFieldName.appIcon, ImageFieldName.graphic, ImageFieldName.softButtonImage };
    private static final FileType[]             IMAGE_FILE_TYPES               = new FileType[] { FileType.GRAPHIC_PNG,
            FileType.GRAPHIC_JPEG, FileType.GRAPHIC_BMP                       };
    private static final ImageResolution[]      IMAGE_RESOLUTIONS              = new ImageResolution[3];
    private static final Integer				IMAGE_RESOLUTION_ITEM_1_WIDTH  = 20;
    private static final Integer				IMAGE_RESOLUTION_ITEM_1_HEIGHT = 20;
    private static final Integer				IMAGE_RESOLUTION_ITEM_2_WIDTH  = 30;
    private static final Integer				IMAGE_RESOLUTION_ITEM_2_HEIGHT = 30;
    private static final Integer				IMAGE_RESOLUTION_ITEM_3_WIDTH  = 40;
    private static final Integer				IMAGE_RESOLUTION_ITEM_3_HEIGHT = 40;
    
    
    private final List<String>           TEMPLATES_AVAILABLE            = Arrays.asList(new String[] { "Media",
            "Navigation", "Productivity"                                      });
    
    private final List<MediaClockFormat> MEDIA_CLOCK_FORMATS            = Arrays.asList(new MediaClockFormat[] { MediaClockFormat.CLOCK1 });
    
    private DisplayCapabilities                 msg;

    private ScreenParams                        screenParams;
    private List<TextField>                     textFields;
    private List<ImageField>                    imageFields;
    private ImageResolution                     imageResolution;
    private TouchEventCapabilities              touchEventCapabilities;

    @Override
    public void setUp(){    	
    	msg = new DisplayCapabilities();

        createCustomObjects();

        msg.setGraphicSupported(GRAPHIC_SUPPORTED);
        msg.setNumCustomPresetsAvailable(CUSTOM_PRESETS_AVAILABLE);
        msg.setDisplayType(DISPLAY_TYPE);
        msg.setImageFields(imageFields);
        msg.setTextFields(textFields);
        msg.setMediaClockFormats(MEDIA_CLOCK_FORMATS);
        msg.setScreenParams(screenParams);
        msg.setTemplatesAvailable(TEMPLATES_AVAILABLE);
    }

    private void createCustomObjects(){
    	IMAGE_RESOLUTIONS[0] = new ImageResolution();
    	IMAGE_RESOLUTIONS[0].setResolutionWidth(IMAGE_RESOLUTION_ITEM_1_WIDTH);
    	IMAGE_RESOLUTIONS[0].setResolutionHeight(IMAGE_RESOLUTION_ITEM_1_HEIGHT);
    	
    	IMAGE_RESOLUTIONS[1] = new ImageResolution();
    	IMAGE_RESOLUTIONS[1].setResolutionWidth(IMAGE_RESOLUTION_ITEM_2_WIDTH);
    	IMAGE_RESOLUTIONS[1].setResolutionHeight(IMAGE_RESOLUTION_ITEM_2_HEIGHT);
    	
    	IMAGE_RESOLUTIONS[2] = new ImageResolution();
    	IMAGE_RESOLUTIONS[2].setResolutionWidth(IMAGE_RESOLUTION_ITEM_3_WIDTH);
    	IMAGE_RESOLUTIONS[2].setResolutionHeight(IMAGE_RESOLUTION_ITEM_3_HEIGHT);
    	
        imageResolution = new ImageResolution();
        imageResolution.setResolutionWidth(SCREEN_IMAGE_RESOLUTION_WIDTH);
        imageResolution.setResolutionHeight(SCREEN_IMAGE_RESOLUTION_HEIGHT);
        
        touchEventCapabilities = new TouchEventCapabilities();
        touchEventCapabilities.setPressAvailable(TOUCH_AVAILABLE);
        touchEventCapabilities.setMultiTouchAvailable(MULTI_TOUCH_AVAILABLE);
        touchEventCapabilities.setDoublePressAvailable(DOUBLE_PRESS_AVAILABLE);
        
        screenParams = new ScreenParams();
        screenParams.setImageResolution(imageResolution);
        screenParams.setTouchEventAvailable(touchEventCapabilities);

        textFields = new ArrayList<TextField>(TEXT_FIELD_NAMES.length);
        for(int i = 0; i < TEXT_FIELD_NAMES.length; i++){
        	TextField textField = new TextField();
        	textField.setName(TEXT_FIELD_NAMES[i]);
        	textField.setCharacterSet(CHARACTER_SET);
        	textField.setWidth(TEXT_FIELD_WIDTHS[i]);
        	textField.setRows(TEXT_FIELD_ROWS);
            textFields.add(textField);
        }

        imageFields = new ArrayList<ImageField>(IMAGE_FIELD_NAMES.length);
        for(int i = 0; i < IMAGE_FIELD_NAMES.length; i++){
        	ImageField imageField = new ImageField();
        	imageField.setName(IMAGE_FIELD_NAMES[i]);

        	List<FileType> fileList = new ArrayList<FileType>();
        	for(int j = 0; j < IMAGE_FILE_TYPES.length; j++){
        		fileList.add(IMAGE_FILE_TYPES[j]);
        	}
        	imageField.setImageTypeSupported(fileList);
        	imageField.setImageResolution(IMAGE_RESOLUTIONS[i]);
        	
            imageFields.add(imageField);
        }
    }

    public void testGraphicSupported(){
        boolean copy = msg.getGraphicSupported();
        assertEquals("Input value didn't match expected value.", GRAPHIC_SUPPORTED, copy);
    }

    public void testNumPresetsAvailable(){
        int copy = msg.getNumCustomPresetsAvailable();
        assertEquals("Input value didn't match expected value.", CUSTOM_PRESETS_AVAILABLE, copy);
    }

    public void testDisplayType(){
        DisplayType copy = msg.getDisplayType();
        assertEquals("Input value didn't match expected value.", DISPLAY_TYPE, copy);
    }

    public void testScreenParams(){
        ScreenParams copy = msg.getScreenParams();

        assertTrue("Input value didn't match expected value.", Validator.validateScreenParams(screenParams, copy));
    }
    
    public void testTemplatesAvailable(){
        List<String> copy = msg.getTemplatesAvailable();

        assertEquals("Templates available size didn't match expected size.", TEMPLATES_AVAILABLE.size(), copy.size());

        for(int i = 0; i < TEMPLATES_AVAILABLE.size(); i++){
            assertEquals("Template data at index " + i + " didn't match expected data.", TEMPLATES_AVAILABLE.get(i),
                    copy.get(i));
        }
    }

    public void testMediaClockFormats(){
        List<MediaClockFormat> copy = msg.getMediaClockFormats();

        assertEquals("Media clock formats size didn't match expected size.", MEDIA_CLOCK_FORMATS.size(), copy.size());

        for(int i = 0; i < MEDIA_CLOCK_FORMATS.size(); i++){
            assertEquals("Media clock format data at index " + i + " didn't match expected data.",
                    MEDIA_CLOCK_FORMATS.get(i), copy.get(i));
        }
    }

    public void testTextFields(){
        List<TextField> copy = msg.getTextFields();
        assertEquals("Text fields size didn't match expected size.", textFields.size(), copy.size());

        for(int i = 0; i < textFields.size(); i++){
            assertTrue("Text field data at index " + i + " didn't match expected data.",
                    Validator.validateTextFields(textFields.get(i), copy.get(i)));
        }
    }

    public void testImageFields(){
        List<ImageField> copy = msg.getImageFields();
        assertEquals("Text fields size didn't match expected size.", imageFields.size(), copy.size());
       
        for(int i = 0; i < imageFields.size(); i++){
            assertTrue("Text field data at index " + i + " didn't match expected data.",
                    Validator.validateImageFields(imageFields.get(i), copy.get(i)));
        }
    }
    
    public void testJson(){
        JSONObject reference = new JSONObject();
        JSONObject screenParams = new JSONObject(), imageResolution = new JSONObject(), touchEventCapabilities = new JSONObject();
        JSONArray mediaClockFormats = new JSONArray(), textFields = new JSONArray(), imageFields = new JSONArray();

        try{
            reference.put(DisplayCapabilities.KEY_NUM_CUSTOM_PRESETS_AVAILABLE, CUSTOM_PRESETS_AVAILABLE);
            reference.put(DisplayCapabilities.KEY_GRAPHIC_SUPPORTED, GRAPHIC_SUPPORTED);
            reference.put(DisplayCapabilities.KEY_DISPLAY_TYPE, DISPLAY_TYPE);
            reference.put(DisplayCapabilities.KEY_TEMPLATES_AVAILABLE,
                    JsonUtils.createJsonArray(TEMPLATES_AVAILABLE));

            for(MediaClockFormat format : MEDIA_CLOCK_FORMATS){
                mediaClockFormats.put(format);
            }
            reference.put(DisplayCapabilities.KEY_MEDIA_CLOCK_FORMATS, mediaClockFormats);

            // text fields
            for(TextField textField : this.textFields){
                JSONObject textFieldJson = new JSONObject();
                textFieldJson.put(TextField.KEY_CHARACTER_SET, textField.getCharacterSet());
                textFieldJson.put(TextField.KEY_NAME, textField.getName());
                textFieldJson.put(TextField.KEY_ROWS, textField.getRows());
                textFieldJson.put(TextField.KEY_WIDTH, textField.getWidth());
                textFields.put(textFieldJson);
            }
            reference.put(DisplayCapabilities.KEY_TEXT_FIELDS, textFields);

            // image fields
            for(ImageField imageField : this.imageFields){
                JSONObject imageFieldJson = new JSONObject();
                imageFieldJson.put(ImageField.KEY_IMAGE_TYPE_SUPPORTED, imageField.getImageTypeSupported());
                imageFieldJson.put(ImageField.KEY_NAME, imageField.getName());

                ImageResolution resolution = imageField.getImageResolution();
                JSONObject resolutionJson = new JSONObject();
                resolutionJson.put(ImageResolution.KEY_RESOLUTION_HEIGHT, resolution.getResolutionHeight());
                resolutionJson.put(ImageResolution.KEY_RESOLUTION_WIDTH, resolution.getResolutionWidth());
                imageFieldJson.put(ImageField.KEY_IMAGE_RESOLUTION, resolutionJson);

                imageFields.put(imageFieldJson);
            }
            reference.put(DisplayCapabilities.KEY_IMAGE_FIELDS, imageFields);

            // screen params
            ImageResolution resolution = this.screenParams.getImageResolution();
            imageResolution.put(ImageResolution.KEY_RESOLUTION_HEIGHT, resolution.getResolutionHeight());
            imageResolution.put(ImageResolution.KEY_RESOLUTION_WIDTH, resolution.getResolutionWidth());
            screenParams.put(ScreenParams.KEY_RESOLUTION, imageResolution);

            TouchEventCapabilities touchCapabilities = this.touchEventCapabilities;
            touchEventCapabilities.put(TouchEventCapabilities.KEY_PRESS_AVAILABLE,
                    touchCapabilities.getPressAvailable());
            touchEventCapabilities.put(TouchEventCapabilities.KEY_DOUBLE_PRESS_AVAILABLE,
                    touchCapabilities.getDoublePressAvailable());
            touchEventCapabilities.put(TouchEventCapabilities.KEY_MULTI_TOUCH_AVAILABLE,
                    touchCapabilities.getMultiTouchAvailable());
            screenParams.put(ScreenParams.KEY_TOUCH_EVENT_AVAILABLE, touchEventCapabilities);
            reference.put(DisplayCapabilities.KEY_SCREEN_PARAMS, screenParams);

            JSONObject underTest = msg.serializeJSON();

            assertEquals("JSON size didn't match expected size.", reference.length(), underTest.length());
            
            Iterator<?> iterator = reference.keys();
            while(iterator.hasNext()){
                String key = (String) iterator.next();
                if(key.equals(DisplayCapabilities.KEY_IMAGE_FIELDS)){
                    JSONArray referenceArray = JsonUtils.readJsonArrayFromJsonObject(reference, key);
                    JSONArray underTestArray = JsonUtils.readJsonArrayFromJsonObject(underTest, key);
                    assertEquals("Image field size didn't match expected size.", referenceArray.length(),
                    		underTestArray.length());

                    for(int i = 0; i < referenceArray.length(); i++){
                    	Hashtable<String, Object> hashReference = JsonRPCMarshaller.deserializeJSONObject(referenceArray.getJSONObject(i));
                    	Hashtable<String, Object> hashTest= JsonRPCMarshaller.deserializeJSONObject(underTestArray.getJSONObject(i));
                    	
                        assertTrue("Image field at index " + i + " didn't match expected value.",
                                Validator.validateImageFields(new ImageField(hashReference), new ImageField(hashTest)));
                    }
                }
                else if(key.equals(DisplayCapabilities.KEY_TEXT_FIELDS)){
                    JSONArray referenceArray = JsonUtils.readJsonArrayFromJsonObject(reference, key);
                    JSONArray underTestArray = JsonUtils.readJsonArrayFromJsonObject(underTest, key);
                    assertEquals("Text field size didn't match expected size.", referenceArray.length(),
                            underTestArray.length());

                    for(int i = 0; i < referenceArray.length(); i++){
                    	Hashtable<String, Object> hashReference = JsonRPCMarshaller.deserializeJSONObject(referenceArray.getJSONObject(i));
                    	Hashtable<String, Object> hashTest= JsonRPCMarshaller.deserializeJSONObject(underTestArray.getJSONObject(i));
                    	
                        assertTrue("Text field at index " + i + " didn't match expected value.",
                                Validator.validateTextFields(new TextField(hashReference), new TextField(hashTest)));
                    }
                }
                else if(key.equals(DisplayCapabilities.KEY_TEMPLATES_AVAILABLE)){
                    JSONArray referenceArray = JsonUtils.readJsonArrayFromJsonObject(reference, key);
                    JSONArray underTestArray = JsonUtils.readJsonArrayFromJsonObject(underTest, key);
                    assertEquals("Template list size didn't match expected size.", referenceArray.length(),
                            underTestArray.length());

                    for(int i = 0; i < referenceArray.length(); i++){
                        assertTrue("Template field at index " + i + " didn't match expected value.",
                                Validator.validateText(referenceArray.getString(i), underTestArray.getString(i)));
                    }
                }
                else if(key.equals(DisplayCapabilities.KEY_SCREEN_PARAMS)){
                    JSONObject referenceArray = JsonUtils.readJsonObjectFromJsonObject(reference, key);
                    JSONObject underTestArray = JsonUtils.readJsonObjectFromJsonObject(underTest, key);
                	Hashtable<String, Object> hashReference = JsonRPCMarshaller.deserializeJSONObject(referenceArray);
                	Hashtable<String, Object> hashTest= JsonRPCMarshaller.deserializeJSONObject(underTestArray);
                    
                    assertTrue("Screen params value didn't match expected value.", Validator.validateScreenParams(
                            new ScreenParams(hashReference), new ScreenParams(hashTest)));
                }
                else if(key.equals(DisplayCapabilities.KEY_MEDIA_CLOCK_FORMATS)){
                    JSONArray referenceArray = JsonUtils.readJsonArrayFromJsonObject(reference, key);
                    JSONArray underTestArray = JsonUtils.readJsonArrayFromJsonObject(underTest, key);
                    assertEquals("Media clock format list size didn't match expected size.", referenceArray.length(),
                            underTestArray.length());

                    for(int i = 0; i < referenceArray.length(); i++){
                        assertTrue("Media clock format at index " + i + " didn't match expected value.",
                                Validator.validateText(referenceArray.getString(i), underTestArray.getString(i)));
                    }
                }
                else{
                    assertEquals("JSON value didn't match expected value for key \"" + key + "\".",
                            JsonUtils.readObjectFromJsonObject(reference, key),
                            JsonUtils.readObjectFromJsonObject(underTest, key));
                }
            }
        }catch(JSONException e){
            /* do nothing */
        }
    }

    public void testNull(){
        DisplayCapabilities msg = new DisplayCapabilities();
        assertNotNull("Null object creation failed.", msg);

        assertNull("Display type wasn't set, but getter method returned an object.", msg.getDisplayType());
        assertNull("Graphic supported wasn't set, but getter method returned an object.", msg.getGraphicSupported());
        assertNull("Image field list wasn't set, but getter method returned an object.", msg.getImageFields());
        assertNull("Media clock format list wasn't set, but getter method returned an object.",
                msg.getMediaClockFormats());
        assertNull("Num of custom presets wasn't set, but getter method returned an object.",
                msg.getNumCustomPresetsAvailable());
        assertNull("Screen params wasn't set, but getter method returned an object.", msg.getScreenParams());
        assertNull("Templates available list wasn't set, but getter method returned an object.",
                msg.getTemplatesAvailable());
        assertNull("Text fields list wasn't set, but getter method returned an object.", msg.getTextFields());
    }
}
