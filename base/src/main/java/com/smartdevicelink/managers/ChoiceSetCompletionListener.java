package com.smartdevicelink.managers;

import com.smartdevicelink.managers.screen.choiceset.ChoiceCell;

import java.util.HashSet;

public interface ChoiceSetCompletionListener {


    /**
     * Returns whether an PreloadChoices operation was successful or not along with which choice cells failed
     * @param success - Boolean that is True if Operation was a success, False otherwise.
     * @param failedChoiceCells - A set of the ChoiceCells that failed to upload.
     */
    void onComplete(boolean success, HashSet<ChoiceCell> failedChoiceCells);
}
