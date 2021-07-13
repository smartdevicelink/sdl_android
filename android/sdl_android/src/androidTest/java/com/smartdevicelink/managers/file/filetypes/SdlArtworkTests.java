package com.smartdevicelink.managers.file.filetypes;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.smartdevicelink.proxy.rpc.enums.StaticIconName;
import com.smartdevicelink.test.TestValues;

import org.junit.Test;
import org.junit.runner.RunWith;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertNotSame;
import static junit.framework.TestCase.assertTrue;

@RunWith(AndroidJUnit4.class)
public class SdlArtworkTests {

    @Test
    public void testClone() {
        SdlArtwork original = TestValues.GENERAL_ARTWORK;
        SdlArtwork clone = original.clone();

        equalTest(original, clone);


        SdlArtwork artwork = new SdlArtwork(StaticIconName.ALBUM);
        assertNotNull(artwork);

        SdlArtwork staticIconClone = artwork.clone();
        assertNotNull(staticIconClone);


        assertTrue(clone instanceof Cloneable);
        assertTrue(artwork instanceof Cloneable);

    }

    public static boolean equalTest(SdlArtwork original, SdlArtwork clone) {
        if (original == null && clone == null) {
            return true;
        }

        assertNotNull(original);
        assertNotNull(clone);
        assertNotSame(original, clone);


        assertEquals(original.getResourceId(), clone.getResourceId());

        assertEquals(original.getFileData(), clone.getFileData());

        assertNotNull(original.getImageRPC());
        assertNotNull(clone.getImageRPC());

        assertNotSame(original.getImageRPC(), clone.getImageRPC());
        assertEquals(original.getImageRPC().getIsTemplate(), clone.getImageRPC().getIsTemplate());
        assertEquals(original.getImageRPC().getValue(), clone.getImageRPC().getValue());
        assertEquals(original.getImageRPC().getImageType(), clone.getImageRPC().getImageType());

        return true;
    }
}
