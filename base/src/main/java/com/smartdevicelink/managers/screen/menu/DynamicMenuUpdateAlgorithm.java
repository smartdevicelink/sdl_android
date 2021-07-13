package com.smartdevicelink.managers.screen.menu;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Bilal Alsharifi on 1/25/21.
 */
class DynamicMenuUpdateAlgorithm {
    // Cell state that tells the menu manager what it should do with a given SDLMenuCell
    enum MenuCellState {
        DELETE, // Marks the cell to be deleted
        ADD, // Marks the cell to be added
        KEEP // Marks the cell to be kept
    }

    static DynamicMenuUpdateRunScore compareOldMenuCells(List<MenuCell> oldMenuCells, List<MenuCell> updatedMenuCells) {
        if (!oldMenuCells.isEmpty() && updatedMenuCells.isEmpty()) {
            return new DynamicMenuUpdateRunScore(buildAllDeleteStatusesForMenu(oldMenuCells), new ArrayList<MenuCellState>(), 0);
        } else if (oldMenuCells.isEmpty() && !updatedMenuCells.isEmpty()) {
            return new DynamicMenuUpdateRunScore(new ArrayList<MenuCellState>(), buildAllAddStatusesForMenu(updatedMenuCells), updatedMenuCells.size());
        } else if (oldMenuCells.isEmpty()) {
            return null;
        }
        return startCompareAtRun(0, oldMenuCells, updatedMenuCells);
    }

    private static DynamicMenuUpdateRunScore startCompareAtRun(int startRun, List<MenuCell> oldMenuCells, List<MenuCell> updatedMenuCells) {
        DynamicMenuUpdateRunScore bestScore = null;

        for (int run = startRun; run < oldMenuCells.size(); run++) {
            // Set the menu status as a 1-1 array, start off will oldMenus = all Deletes, newMenu = all Adds
            List<MenuCellState> oldMenuStatus = buildAllDeleteStatusesForMenu(oldMenuCells);
            List<MenuCellState> newMenuStatus = buildAllAddStatusesForMenu(updatedMenuCells);

            int startIndex = 0;
            for (int oldCellIndex = run; oldCellIndex < oldMenuCells.size(); oldCellIndex++) {
                // For each old item, create inner loop to compare old cells to new cells to find a match
                // if a match if found we mark the index at match for both the old and the new status to
                // keep since we do not want to send RPCs for those cases
                for (int newCellIndex = startIndex; newCellIndex < updatedMenuCells.size(); newCellIndex++) {
                    if (oldMenuCells.get(oldCellIndex).equals(updatedMenuCells.get(newCellIndex))) {
                        oldMenuStatus.set(oldCellIndex, MenuCellState.KEEP);
                        newMenuStatus.set(newCellIndex, MenuCellState.KEEP);
                        startIndex = newCellIndex + 1;
                        break;
                    }
                }
            }

            // // Add RPC are the biggest operation so we need to find the run with the least amount of Adds.
            // We will reset the run we use each time a runScore is less than the current score.
            int numberOfAdds = 0;
            for (int status = 0; status < newMenuStatus.size(); status++) {
                if (newMenuStatus.get(status).equals(MenuCellState.ADD)) {
                    numberOfAdds++;
                }
            }

            // As soon as we a run that requires 0 Adds we will use it since we cant do better then 0
            if (numberOfAdds == 0) {
                bestScore = new DynamicMenuUpdateRunScore(oldMenuStatus, newMenuStatus, numberOfAdds);
                return bestScore;
            }

            // if we haven't create the bestScore object or if the current score beats the old score then we will create a new bestScore
            if (bestScore == null || numberOfAdds < bestScore.getScore()) {
                bestScore = new DynamicMenuUpdateRunScore(oldMenuStatus, newMenuStatus, numberOfAdds);
            }

        }
        return bestScore;
    }

    /**
     * Builds a 1-1 array of Deletes for every element in the array
     * @param oldMenu The old menu array
     */
    static List<MenuCellState> buildAllDeleteStatusesForMenu (List<MenuCell> oldMenu){
        List<MenuCellState> oldMenuStatus = new ArrayList<>(oldMenu.size());
        for (int index = 0; index < oldMenu.size(); index++) {
            oldMenuStatus.add(MenuCellState.DELETE);
        }
        return oldMenuStatus;
    }

    /**
     * Builds a 1-1 array of Adds for every element in the array
     * @param newMenu The new menu array
     */
    static List<MenuCellState> buildAllAddStatusesForMenu (List<MenuCell> newMenu){
        List<MenuCellState> newMenuStatus = new ArrayList<>(newMenu.size());
        for (int index = 0; index < newMenu.size(); index++) {
            newMenuStatus.add(MenuCellState.ADD);
        }
        return newMenuStatus;
    }
}
