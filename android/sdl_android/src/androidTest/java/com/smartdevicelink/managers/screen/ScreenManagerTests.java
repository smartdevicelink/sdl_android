package com.smartdevicelink.managers.screen;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.livio.taskmaster.Taskmaster;
import com.smartdevicelink.managers.BaseSubManager;
import com.smartdevicelink.managers.ISdl;
import com.smartdevicelink.managers.file.FileManager;
import com.smartdevicelink.managers.file.filetypes.SdlArtwork;
import com.smartdevicelink.managers.permission.PermissionManager;
import com.smartdevicelink.managers.screen.menu.DynamicMenuUpdatesMode;
import com.smartdevicelink.proxy.rpc.enums.FileType;
import com.smartdevicelink.proxy.rpc.enums.MetadataType;
import com.smartdevicelink.proxy.rpc.enums.TextAlignment;
import com.smartdevicelink.test.TestValues;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * This is a unit test class for the SmartDeviceLink library manager class :
 * {@link ScreenManager}
 */
@RunWith(AndroidJUnit4.class)
public class ScreenManagerTests {
    private ScreenManager screenManager;
    private SdlArtwork testArtwork;

    @Before
    public void setUp() throws Exception {


        ISdl internalInterface = mock(ISdl.class);
        when(internalInterface.getTaskmaster()).thenReturn(new Taskmaster.Builder().build());
        PermissionManager permissionManager = mock(PermissionManager.class);

        when(internalInterface.getPermissionManager()).thenReturn(permissionManager);
        FileManager fileManager = mock(FileManager.class);
        screenManager = new ScreenManager(internalInterface, fileManager);
        screenManager.start(null);
        testArtwork = new SdlArtwork("testFile", FileType.GRAPHIC_PNG, 1, false);
    }

    @Test
    public void testInstantiation() {
        assertNull(screenManager.getTextField1());
        assertNull(screenManager.getTextField2());
        assertNull(screenManager.getTextField3());
        assertNull(screenManager.getTextField4());
        assertNull(screenManager.getTitle());
        assertNull(screenManager.getMediaTrackTextField());
        assertNull(screenManager.getPrimaryGraphic());
        assertNull(screenManager.getSecondaryGraphic());
        assertEquals(screenManager.getTextAlignment(), TextAlignment.CENTERED);
        assertNull(screenManager.getTextField1Type());
        assertNull(screenManager.getTextField2Type());
        assertNull(screenManager.getTextField3Type());
        assertNull(screenManager.getTextField4Type());
        assertTrue(screenManager.getMenu().isEmpty());
        assertNull(screenManager.getVoiceCommands());
        assertTrue(screenManager.getSoftButtonObjects().isEmpty());
        assertNull(screenManager.getSoftButtonObjectByName("test"));
        assertNull(screenManager.getSoftButtonObjectById(1));
        assertEquals(screenManager.getDynamicMenuUpdatesMode(), DynamicMenuUpdatesMode.ON_WITH_COMPAT_MODE);
        assertEquals(screenManager.getState(), BaseSubManager.READY);
        assertNull(screenManager.getMenuConfiguration().getMenuLayout());
        assertNull(screenManager.getMenuConfiguration().getSubMenuLayout());
    }

    @Test
    public void testSetTextField() {
        screenManager.setTextField1("It is");
        screenManager.setTextField2("Wednesday");
        screenManager.setTextField3("My");
        screenManager.setTextField4("Dudes");
        screenManager.setTitle("title");
        assertEquals(screenManager.getTextField1(), "It is");
        assertEquals(screenManager.getTextField2(), "Wednesday");
        assertEquals(screenManager.getTextField3(), "My");
        assertEquals(screenManager.getTextField4(), "Dudes");
        assertEquals(screenManager.getTitle(), "title");
    }

    @Test
    public void testMediaTrackTextFields() {
        String songTitle = "Wild For The Night";
        screenManager.setMediaTrackTextField(songTitle);
        assertEquals(screenManager.getMediaTrackTextField(), songTitle);
    }

    @Test
    public void testSetPrimaryGraphic() {
        screenManager.setPrimaryGraphic(testArtwork);
        assertEquals(screenManager.getPrimaryGraphic(), testArtwork);
    }

    @Test
    public void testSetPrimaryGraphicWithBlankImage() {
        screenManager.setPrimaryGraphic(null);
        assertNull(screenManager.getPrimaryGraphic());
    }

    @Test
    public void testSetSecondaryGraphic() {
        screenManager.setSecondaryGraphic(testArtwork);
        assertEquals(screenManager.getSecondaryGraphic(), testArtwork);
    }

    @Test
    public void testSetSecondaryGraphicWithBlankImage() {
        screenManager.setSecondaryGraphic(null);
        assertNull(screenManager.getSecondaryGraphic());
    }

    @Test
    public void testAlignment() {
        screenManager.setTextAlignment(TextAlignment.LEFT_ALIGNED);
        assertEquals(screenManager.getTextAlignment(), TextAlignment.LEFT_ALIGNED);
    }

    @Test
    public void testSetTextFieldTypes() {
        screenManager.setTextField1Type(MetadataType.MEDIA_TITLE);
        screenManager.setTextField2Type(MetadataType.MEDIA_ALBUM);
        screenManager.setTextField3Type(MetadataType.MEDIA_ARTIST);
        screenManager.setTextField4Type(MetadataType.MEDIA_GENRE);
        assertEquals(screenManager.getTextField1Type(), MetadataType.MEDIA_TITLE);
        assertEquals(screenManager.getTextField2Type(), MetadataType.MEDIA_ALBUM);
        assertEquals(screenManager.getTextField3Type(), MetadataType.MEDIA_ARTIST);
        assertEquals(screenManager.getTextField4Type(), MetadataType.MEDIA_GENRE);
    }

    @Test
    public void testSetMenuManagerFields() {
        screenManager.setDynamicMenuUpdatesMode(DynamicMenuUpdatesMode.FORCE_ON);
        screenManager.setMenu(TestValues.GENERAL_MENUCELL_LIST);
        screenManager.setMenuConfiguration(TestValues.GENERAL_MENU_CONFIGURATION);

        assertEquals(screenManager.getDynamicMenuUpdatesMode(), DynamicMenuUpdatesMode.FORCE_ON);
        assertEquals(screenManager.getMenu(), TestValues.GENERAL_MENUCELL_LIST);
        assertEquals(screenManager.getMenuConfiguration(), TestValues.GENERAL_MENU_CONFIGURATION);
    }

    @Test
    public void testSetVoiceCommands() {
        screenManager.setVoiceCommands(TestValues.GENERAL_VOICE_COMMAND_LIST);
        assertEquals(screenManager.getVoiceCommands(), TestValues.GENERAL_VOICE_COMMAND_LIST);
    }

    @Test
    public void testSetSoftButtonObjects() {
        // Create softButtonObject1
        SoftButtonState softButtonState1 = new SoftButtonState("object1-state1", "it is", testArtwork);
        SoftButtonState softButtonState2 = new SoftButtonState("object1-state2", "Wed", testArtwork);
        SoftButtonObject softButtonObject1 = new SoftButtonObject("object1", Arrays.asList(softButtonState1, softButtonState2), softButtonState1.getName(), null);
        softButtonObject1.setButtonId(100);

        // Create softButtonObject2
        SoftButtonState softButtonState3 = new SoftButtonState("object2-state1", "my", testArtwork);
        SoftButtonState softButtonState4 = new SoftButtonState("object2-state2", "dudes!", null);
        SoftButtonObject softButtonObject2 = new SoftButtonObject("object2", Arrays.asList(softButtonState3, softButtonState4), softButtonState3.getName(), null);
        softButtonObject2.setButtonId(200);

        List<SoftButtonObject> softButtonObjects = Arrays.asList(softButtonObject1, softButtonObject2);
        screenManager.setSoftButtonObjects(Arrays.asList(softButtonObject1, softButtonObject2));
        assertEquals(screenManager.getSoftButtonObjects(), softButtonObjects);
        assertEquals(screenManager.getSoftButtonObjectByName("object2"), softButtonObject2);
        assertEquals(screenManager.getSoftButtonObjectById(200), softButtonObject2);
    }

    @Test
    public void testSettingSoftButtonId() {
        // Create softButtonObject1
        SoftButtonState softButtonState1 = new SoftButtonState("object1-state1", "it is", testArtwork);
        SoftButtonState softButtonState2 = new SoftButtonState("object1-state2", "Wed", testArtwork);
        SoftButtonObject softButtonObject1 = new SoftButtonObject("object1", Arrays.asList(softButtonState1, softButtonState2), softButtonState1.getName(), null);
        softButtonObject1.setButtonId(100);

        // Create softButtonObject2
        SoftButtonState softButtonState3 = new SoftButtonState("object2-state1", "my", testArtwork);
        SoftButtonState softButtonState4 = new SoftButtonState("object2-state2", "dudes!", null);
        SoftButtonObject softButtonObject2 = new SoftButtonObject("object2", Arrays.asList(softButtonState3, softButtonState4), softButtonState3.getName(), null);
        softButtonObject2.setButtonId(200);

        List<SoftButtonObject> softButtonObjects = Arrays.asList(softButtonObject1, softButtonObject2);
        assertTrue(screenManager.checkAndAssignButtonIds(softButtonObjects, BaseScreenManager.ManagerLocation.SOFTBUTTON_MANAGER));
        screenManager.softButtonIDBySoftButtonManager.add(200);
        assertFalse(screenManager.checkAndAssignButtonIds(softButtonObjects, BaseScreenManager.ManagerLocation.ALERT_MANAGER));
        screenManager.softButtonIDByAlertManager.add(100);
        assertFalse(screenManager.checkAndAssignButtonIds(softButtonObjects, BaseScreenManager.ManagerLocation.SOFTBUTTON_MANAGER));
        screenManager.softButtonIDByAlertManager.clear();
        screenManager.softButtonIDBySoftButtonManager.clear();
        assertTrue(screenManager.checkAndAssignButtonIds(softButtonObjects, BaseScreenManager.ManagerLocation.ALERT_MANAGER));
        softButtonObject1.setButtonId(400);
        softButtonObject2.setButtonId(500);
        assertTrue(screenManager.checkAndAssignButtonIds(softButtonObjects, BaseScreenManager.ManagerLocation.SOFTBUTTON_MANAGER));
        SoftButtonObject softButtonObject3 = new SoftButtonObject("object1", Arrays.asList(softButtonState1, softButtonState2), softButtonState1.getName(), null);
        SoftButtonObject softButtonObject4 = new SoftButtonObject("object2", Arrays.asList(softButtonState3, softButtonState4), softButtonState3.getName(), null);
        assertTrue(screenManager.checkAndAssignButtonIds(softButtonObjects, BaseScreenManager.ManagerLocation.SOFTBUTTON_MANAGER));





    }
    @Test
    public void testAssigningIdsToSoftButtonObjects() {
        SoftButtonObject sbo1, sbo2, sbo3, sbo4, sbo5;

        // Case 1 - don't set id for any button (Manager should set ids automatically starting from 1 and up)
        sbo1 = new SoftButtonObject(null, Collections.EMPTY_LIST, null, null);
        sbo2 = new SoftButtonObject(null, Collections.EMPTY_LIST, null, null);
        sbo3 = new SoftButtonObject(null, Collections.EMPTY_LIST, null, null);
        sbo4 = new SoftButtonObject(null, Collections.EMPTY_LIST, null, null);
        sbo5 = new SoftButtonObject(null, Collections.EMPTY_LIST, null, null);
        screenManager.checkAndAssignButtonIds(Arrays.asList(sbo1, sbo2, sbo3, sbo4, sbo5), BaseScreenManager.ManagerLocation.SOFTBUTTON_MANAGER);
        assertEquals("SoftButtonObject id doesn't match the expected value", 1, sbo1.getButtonId());
        assertEquals("SoftButtonObject id doesn't match the expected value", 2, sbo2.getButtonId());
        assertEquals("SoftButtonObject id doesn't match the expected value", 3, sbo3.getButtonId());
        assertEquals("SoftButtonObject id doesn't match the expected value", 4, sbo4.getButtonId());
        assertEquals("SoftButtonObject id doesn't match the expected value", 5, sbo5.getButtonId());


        // Case 2 - Set ids for all buttons (Manager shouldn't alter the ids set by developer)
        sbo1 = new SoftButtonObject(null, Collections.EMPTY_LIST, null, null);
        sbo1.setButtonId(100);
        sbo2 = new SoftButtonObject(null, Collections.EMPTY_LIST, null, null);
        sbo2.setButtonId(200);
        sbo3 = new SoftButtonObject(null, Collections.EMPTY_LIST, null, null);
        sbo3.setButtonId(300);
        sbo4 = new SoftButtonObject(null, Collections.EMPTY_LIST, null, null);
        sbo4.setButtonId(400);
        sbo5 = new SoftButtonObject(null, Collections.EMPTY_LIST, null, null);
        sbo5.setButtonId(500);
        screenManager.checkAndAssignButtonIds(Arrays.asList(sbo1, sbo2, sbo3, sbo4, sbo5), BaseScreenManager.ManagerLocation.SOFTBUTTON_MANAGER);
        assertEquals("SoftButtonObject id doesn't match the expected value", 100, sbo1.getButtonId());
        assertEquals("SoftButtonObject id doesn't match the expected value", 200, sbo2.getButtonId());
        assertEquals("SoftButtonObject id doesn't match the expected value", 300, sbo3.getButtonId());
        assertEquals("SoftButtonObject id doesn't match the expected value", 400, sbo4.getButtonId());
        assertEquals("SoftButtonObject id doesn't match the expected value", 500, sbo5.getButtonId());


        // Case 3 - Set ids for some buttons (Manager shouldn't alter the ids set by developer. And it should assign ids for the ones that don't have id)
        sbo1 = new SoftButtonObject(null, Collections.EMPTY_LIST, null, null);
        sbo1.setButtonId(50);
        sbo2 = new SoftButtonObject(null, Collections.EMPTY_LIST, null, null);
        sbo3 = new SoftButtonObject(null, Collections.EMPTY_LIST, null, null);
        sbo4 = new SoftButtonObject(null, Collections.EMPTY_LIST, null, null);
        sbo4.setButtonId(100);
        sbo5 = new SoftButtonObject(null, Collections.EMPTY_LIST, null, null);
        screenManager.checkAndAssignButtonIds(Arrays.asList(sbo1, sbo2, sbo3, sbo4, sbo5), BaseScreenManager.ManagerLocation.SOFTBUTTON_MANAGER);
        assertEquals("SoftButtonObject id doesn't match the expected value", 50, sbo1.getButtonId());
        assertEquals("SoftButtonObject id doesn't match the expected value", 101, sbo2.getButtonId());
        assertEquals("SoftButtonObject id doesn't match the expected value", 102, sbo3.getButtonId());
        assertEquals("SoftButtonObject id doesn't match the expected value", 100, sbo4.getButtonId());
        assertEquals("SoftButtonObject id doesn't match the expected value", 103, sbo5.getButtonId());
    }
}
