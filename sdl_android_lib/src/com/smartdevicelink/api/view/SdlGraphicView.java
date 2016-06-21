package com.smartdevicelink.api.view;

import android.util.Log;

import com.smartdevicelink.api.file.SdlFile;
import com.smartdevicelink.api.file.SdlFileManager;
import com.smartdevicelink.api.file.SdlImage;
import com.smartdevicelink.api.interfaces.SdlContext;
import com.smartdevicelink.proxy.rpc.Image;
import com.smartdevicelink.proxy.rpc.Show;
import com.smartdevicelink.proxy.rpc.enums.ImageType;

public class SdlGraphicView extends SdlView {

    private static final String TAG = SdlGraphicView.class.getSimpleName();
    private static final String CLEAR_NAME = "";

    private SdlImage mSdlImage;
    private boolean isImagePresent = false;
    private boolean isWaitingToUpload = true;

    private boolean isSecondaryGraphic = false;

    public void setGraphic(SdlImage graphic){
        if(graphic != null) {
            mSdlImage = graphic;
            if(mSdlContext != null) {
                checkImagePresence();
            }
        }
    }

    @Override
    public void setSdlContext(SdlContext sdlContext) {
        super.setSdlContext(sdlContext);
        if(mSdlImage != null) {
            checkImagePresence();
        }
    }

    public SdlImage getGraphic(){
        return mSdlImage;
    }

    private void checkImagePresence() {
        SdlFileManager fileManager = mSdlContext.getSdlFileManager();
        isImagePresent = fileManager.isFileOnModule(mSdlImage.getSdlName());
        if (!isWaitingToUpload && !isImagePresent) {
            fileManager.uploadSdlImage(mSdlImage, mFileReadyListener);
        }
    }

    public void setSecondaryGraphic(boolean secondaryGraphic) {
        isSecondaryGraphic = secondaryGraphic;
    }

    @Override
    public void clear() {
        mSdlImage = null;
        redraw();
    }

    @Override
    void decorate(Show show) {
        if(mSdlImage == null){
            Image image = new Image();
            image.setImageType(ImageType.DYNAMIC);
            image.setValue(CLEAR_NAME);
            if(!isSecondaryGraphic) {
                show.setGraphic(image);
            } else {
                show.setSecondaryGraphic(image);
            }
        } else if(isImagePresent) {
            Image image = new Image();
            image.setImageType(ImageType.DYNAMIC);
            image.setValue(mSdlImage.getSdlName());
            if(!isSecondaryGraphic) {
                show.setGraphic(image);
            } else {
                show.setSecondaryGraphic(image);
            }
        }
    }

    @Override
    void uploadRequiredImages() {
        if(!isImagePresent && mSdlImage != null){
            SdlFileManager fileManager = mSdlContext.getSdlFileManager();
            fileManager.uploadSdlImage(mSdlImage, mFileReadyListener);
            isWaitingToUpload = false;
        }
    }

    private SdlFileManager.FileReadyListener mFileReadyListener = new SdlFileManager.FileReadyListener() {
        @Override
        public void onFileReady(SdlFile sdlFile) {
            isImagePresent = true;
            Log.d(TAG, "Graphic ready.");
            if(isVisible) {
                redraw();
            }
        }

        @Override
        public void onFileError(SdlFile sdlFile) {

        }
    };

}
