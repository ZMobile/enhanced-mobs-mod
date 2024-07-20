package net.fabricmc.example.config;

public class  ModConfig {
    private boolean allowBreak;
    private boolean allowPlace;
    private boolean zombiesBreakAndPlaceBlocks;
    private boolean skeletonsBreakBlocks;
    private boolean raidersBreakBlocks;
    private boolean witchesBreakBlocks;
    private boolean creepersExplodeObstructions;
    private boolean bloodmoonEnabled;
    private boolean trueDarknessEnforced;
    private double mobBlockPlacementPenalty;
    private double mobBlockBreakAdditionalPenalty;
    private double mobJumpPenalty;
    private Boolean optimizedMobitone;
    private Boolean infiniteZombieBlocks;
    private Double mobBlockBreakSpeed;

    public ModConfig() {
        allowBreak = true;
        allowPlace = true;
        zombiesBreakAndPlaceBlocks = true;
        skeletonsBreakBlocks = true;
        raidersBreakBlocks = true;
        witchesBreakBlocks = true;
        creepersExplodeObstructions = true;
        bloodmoonEnabled = true;
        trueDarknessEnforced = false;
        mobBlockPlacementPenalty = 3.0;
        mobBlockBreakAdditionalPenalty = 2.0;
        mobJumpPenalty = 2.0;
        optimizedMobitone = true;
        infiniteZombieBlocks = true;
        mobBlockBreakSpeed = 1.0;
    }

    public boolean isAllowBreak() {
        return allowBreak;
    }

    public void setAllowBreak(boolean allowBreak) {
        this.allowBreak = allowBreak;
    }

    public boolean isAllowPlace() {
        return allowPlace;
    }

    public void setAllowPlace(boolean allowPlace) {
        this.allowPlace = allowPlace;
    }

    public boolean isZombiesBreakAndPlaceBlocks() {
        return zombiesBreakAndPlaceBlocks;
    }

    public void setZombiesBreakAndPlaceBlocks(boolean zombiesBreakAndPlaceBlocks) {
        this.zombiesBreakAndPlaceBlocks = zombiesBreakAndPlaceBlocks;
    }

    public boolean isSkeletonsBreakBlocks() {
        return skeletonsBreakBlocks;
    }

    public void setSkeletonsBreakBlocks(boolean skeletonsBreakBlocks) {
        this.skeletonsBreakBlocks = skeletonsBreakBlocks;
    }

    public boolean isRaidersBreakBlocks() {
        return raidersBreakBlocks;
    }

    public void setRaidersBreakBlocks(boolean raidersBreakBlocks) {
        this.raidersBreakBlocks = raidersBreakBlocks;
    }

    public boolean isWitchesBreakBlocks() {
        return witchesBreakBlocks;
    }

    public void setWitchesBreakBlocks(boolean witchesBreakBlocks) {
        this.witchesBreakBlocks = witchesBreakBlocks;
    }

    public boolean isCreepersExplodeObstructions() {
        return creepersExplodeObstructions;
    }

    public void setCreepersExplodeObstructions(boolean creepersExplodeObstructions) {
        this.creepersExplodeObstructions = creepersExplodeObstructions;
    }

    public boolean isBloodmoonEnabled() {
        return bloodmoonEnabled;
    }

    public void setBloodmoonEnabled(boolean bloodmoonEnabled) {
        this.bloodmoonEnabled = bloodmoonEnabled;
    }

    public boolean isTrueDarknessEnforced() {
        return trueDarknessEnforced;
    }

    public void setTrueDarknessEnforced(boolean trueDarknessEnforced) {
        this.trueDarknessEnforced = trueDarknessEnforced;
    }

    public double getMobBlockPlacementPenalty() {
        return mobBlockPlacementPenalty;
    }

    public void setMobBlockPlacementPenalty(double mobBlockPlacementPenalty) {
        this.mobBlockPlacementPenalty = mobBlockPlacementPenalty;
    }

    public double getMobBlockBreakAdditionalPenalty() {
        return mobBlockBreakAdditionalPenalty;
    }

    public void setMobBlockBreakAdditionalPenalty(double mobBlockBreakAdditionalPenalty) {
        this.mobBlockBreakAdditionalPenalty = mobBlockBreakAdditionalPenalty;
    }

    public double getMobJumpPenalty() {
        return mobJumpPenalty;
    }

    public void setMobJumpPenalty(double mobJumpPenalty) {
        this.mobJumpPenalty = mobJumpPenalty;
    }

    public Boolean isOptimizedMobitone() {
        return optimizedMobitone;
    }

    public void setOptimizedMobitone(Boolean optimizedMobitone) {
        this.optimizedMobitone = optimizedMobitone;
    }

    public Boolean isInfiniteZombieBlocks() {
        return infiniteZombieBlocks;
    }

    public void setInfiniteZombieBlocks(Boolean infiniteZombieBlocks) {
        this.infiniteZombieBlocks = infiniteZombieBlocks;
    }

    public Double getMobBlockBreakSpeed() {
        return mobBlockBreakSpeed;
    }

    public void setMobBlockBreakSpeed(Double mobBlockBreakSpeed) {
        this.mobBlockBreakSpeed = mobBlockBreakSpeed;
    }
}
