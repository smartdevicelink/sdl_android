package com.smartdevicelink.api.choiceset;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.smartdevicelink.api.file.SdlImage;
import com.smartdevicelink.proxy.rpc.TTSChunk;
import com.smartdevicelink.proxy.rpc.enums.SpeechCapabilities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Created by mschwerz on 5/4/16.
 */
public class SdlChoice {
    private final String mChoiceName;
    private final String mMenuText;
    private String mSubText;
    private String mRightHandText;
    private OnSelectedListener mListener;
    private ArrayList<Integer> mIds= new ArrayList<>();
    private SdlImage mSdlImage;
    private final ArrayList<String> mVoiceCommands;

    public SdlChoice(@NonNull String choiceName, @NonNull String menuText, @NonNull ArrayList<String> manyVoiceCommands, @Nullable OnSelectedListener listener){
        mChoiceName= choiceName;
        mMenuText = menuText;
        mListener = listener;
        mVoiceCommands= manyVoiceCommands;
    }

    public SdlChoice(@NonNull String choiceName, @NonNull String menuText, @NonNull final String singleVoiceCommand, @Nullable OnSelectedListener listener){
        this(choiceName,menuText, new ArrayList<>(Collections.singletonList(singleVoiceCommand)),listener);
    }

    public String getChoiceName(){ return mChoiceName; }

    public SdlImage getSdlImage() {
        return mSdlImage;
    }

    public void setSdlImage(SdlImage sdlImage) {
        mSdlImage = sdlImage;
    }

    public String getMenuText() {
        return mMenuText;
    }


    OnSelectedListener getListener() {
        return mListener;
    }

    ArrayList<Integer> getId() {
        return mIds;
    }

    void addId(int id) {
        mIds.add(id);
    }
    void setIds(ArrayList<Integer> ids){ mIds= ids;}

    public void setRightHandText( String text ) {mRightHandText= text;}

    public String getRightHandText(){return mRightHandText;}

    public void setSubText( String text ){mSubText= text;}

    public String getSubText(){return mSubText;}

    SdlChoice getListenerLessDeepCopy(){
        SdlChoice copyChoice = new SdlChoice(mChoiceName, mMenuText,mVoiceCommands,null);
        copyChoice.mSubText =mSubText;
        copyChoice.mRightHandText= mRightHandText;
        copyChoice.mIds= mIds;
        copyChoice.mSdlImage= mSdlImage;
        return copyChoice;
    }

    void setOnSelectedListener(OnSelectedListener listener){ mListener= listener;}

    public interface OnSelectedListener {

        void onManualSelection();
        void onVoiceSelection();
    }

    public void addVoiceCommand(String tts){
        mVoiceCommands.add(tts);
    }

    public Collection<String> getVoiceCommands(){ return mVoiceCommands; }

}
