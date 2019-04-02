/*
 * Copyright (c) 2019 Livio, Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided with the
 * distribution.
 *
 * Neither the name of the Livio Inc. nor the names of its contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package com.smartdevicelink.managers.file.filetypes;

import android.net.Uri;
import android.support.annotation.NonNull;

import com.smartdevicelink.proxy.rpc.Image;
import com.smartdevicelink.proxy.rpc.enums.FileType;
import com.smartdevicelink.proxy.rpc.enums.ImageType;
import com.smartdevicelink.proxy.rpc.enums.StaticIconName;

/**
 * A class that extends SdlFile, representing artwork (JPEG, PNG, or BMP) to be uploaded to core
 */
public class SdlArtwork extends SdlFile {
    private boolean isTemplate;
    private Image imageRPC;

    public SdlArtwork(){}

    public SdlArtwork(@NonNull String fileName, @NonNull FileType fileType, int id, boolean persistentFile) {
        super(fileName, fileType, id, persistentFile);
    }

    public SdlArtwork(@NonNull String fileName, @NonNull FileType fileType, Uri uri, boolean persistentFile) {
        super(fileName, fileType, uri, persistentFile);
    }

    public SdlArtwork(@NonNull String fileName, @NonNull FileType fileType, byte[] data, boolean persistentFile) {
        super(fileName, fileType, data, persistentFile);
    }

    public SdlArtwork(@NonNull StaticIconName staticIconName) {
        super(staticIconName);
    }

    /**
     * Set whether this SdlArtwork is a template image whose coloring should be decided by the HMI
     * @param isTemplate boolean that tells whether this SdlArtwork is a template image
     */
    public void setTemplateImage(boolean isTemplate){
        this.isTemplate = isTemplate;
    }

    /**
     * Get whether this SdlArtwork is a template image whose coloring should be decided by the HMI
     * @return boolean that tells whether this SdlArtwork is a template image
     */
    public boolean isTemplateImage(){
        return isTemplate;
    }

    @Override
    public void setType(@NonNull FileType fileType) {
        if(fileType.equals(FileType.GRAPHIC_JPEG) || fileType.equals(FileType.GRAPHIC_PNG)
                || fileType.equals(FileType.GRAPHIC_BMP)){
            super.setType(fileType);
        }else{
            throw new IllegalArgumentException("Only JPEG, PNG, and BMP image types are supported.");
        }
    }

    /**
     * Get the Image RPC representing this artwork. Generally for use internally, you should instead pass an artwork to a Screen Manager method.
     * @return The Image RPC representing this artwork.
     */
    public Image getImageRPC() {
        if (imageRPC == null) {
            if (isStaticIcon()) {
                imageRPC = new Image(getName(), ImageType.STATIC);
                imageRPC.setIsTemplate(true);
            } else {
                imageRPC = new Image(getName(), ImageType.DYNAMIC);
                imageRPC.setIsTemplate(isTemplate);
            }
        }
        return imageRPC;
    }
}