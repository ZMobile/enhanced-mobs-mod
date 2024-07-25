package net.fabricmc.example.mobai;

import java.util.HashMap;
import java.util.Map;

public class BreakPlaceAndChaseGoalTracker {
    private static Map<Integer, BreakPlaceAndChaseGoal> mobIdToGoalMap = new HashMap<>();

    public BreakPlaceAndChaseGoalTracker() {
    }

    public static void addGoal(int mobId, BreakPlaceAndChaseGoal goal) {
        mobIdToGoalMap.put(mobId, goal);
    }

    public static BreakPlaceAndChaseGoal getGoal(int mobId) {
        return mobIdToGoalMap.get(mobId);
    }

    public static void removeGoal(int mobId) {
        mobIdToGoalMap.remove(mobId);
    }
}
