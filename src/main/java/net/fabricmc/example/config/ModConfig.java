package net.fabricmc.example.config;

public class  ModConfig {
    private boolean allowBreak;
    private boolean allowPlace;
    private boolean zombiesBreakAndPlaceBlocks;
    private boolean skeletonsBreakBlocks;
    private Boolean skeletonsBreakBlocksDuringBloodmoon;
    private boolean raidersBreakBlocks;
    private boolean witchesBreakBlocks;
    private boolean creepersExplodeObstructions;
    private boolean bloodmoonEnabled;
    private boolean trueDarknessEnforced;
    private Boolean buildingMiningMobsDuringBloodmoonOnly;
    private double mobBlockPlacementPenalty;
    private double mobBlockBreakAdditionalPenalty;
    private double mobJumpPenalty;
    private Double bloodmoonSpawnPercentage;
    private Boolean optimizedMobitone;
    private Boolean infiniteZombieBlocks;
    private Double mobBlockBreakSpeed;
    private Integer daysBeforeBloodmoonPossibility;
    private Boolean creeperHiss;
    private Boolean spiderSpeed;
    private Boolean slowPath;
    private Long slowPathDelay;

    public ModConfig() {
        allowBreak = true;
        allowPlace = true;
        zombiesBreakAndPlaceBlocks = true;
        skeletonsBreakBlocks = true;
        skeletonsBreakBlocksDuringBloodmoon = true;
        raidersBreakBlocks = true;
        witchesBreakBlocks = true;
        creepersExplodeObstructions = true;
        bloodmoonEnabled = true;
        trueDarknessEnforced = false;
        buildingMiningMobsDuringBloodmoonOnly = false;
        mobBlockPlacementPenalty = 3.0;
        mobBlockBreakAdditionalPenalty = 2.0;
        mobJumpPenalty = 2.0;
        bloodmoonSpawnPercentage = 0.00001;
        optimizedMobitone = true;
        infiniteZombieBlocks = true;
        mobBlockBreakSpeed = 1.0;
        daysBeforeBloodmoonPossibility = 3;
        creeperHiss = false;
        spiderSpeed = true;
        slowPath = true;
        slowPathDelay = 4L;
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

    public Boolean isSkeletonsBreakBlocksDuringBloodmoon() {
        return skeletonsBreakBlocksDuringBloodmoon;
    }

    public void setSkeletonsBreakBlocksDuringBloodmoon(boolean skeletonsBreakBlocksDuringBloodmoon) {
        this.skeletonsBreakBlocksDuringBloodmoon = skeletonsBreakBlocksDuringBloodmoon;
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

    public Boolean isBuildingMiningMobsDuringBloodmoonOnly() {
        return buildingMiningMobsDuringBloodmoonOnly;
    }

    public void setBuildingMiningMobsDuringBloodmoonOnly(boolean buildingMiningMobsDuringBloodmoonOnly) {
        this.buildingMiningMobsDuringBloodmoonOnly = buildingMiningMobsDuringBloodmoonOnly;
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

    public Double getBloodmoonSpawnPercentage() {
        return bloodmoonSpawnPercentage;
    }

    public void setBloodmoonSpawnPercentage(double bloodmoonSpawnPercentage) {
        this.bloodmoonSpawnPercentage = bloodmoonSpawnPercentage;
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

    public Integer getDaysBeforeBloodmoonPossibility() {
        return daysBeforeBloodmoonPossibility;
    }

    public void setDaysBeforeBloodmoonPossibility(int daysBeforeBloodmoonPossibility) {
        this.daysBeforeBloodmoonPossibility = daysBeforeBloodmoonPossibility;
    }

    public Boolean isCreeperHiss() {
        return creeperHiss;
    }

    public void setCreeperHiss(boolean creeperHiss) {
        this.creeperHiss = creeperHiss;
    }

    public Boolean isSpiderSpeed() {
        return spiderSpeed;
    }

    public void setSpiderSpeed(Boolean spiderSpeed) {
        this.spiderSpeed = spiderSpeed;
    }

    public Boolean isSlowPath() {
        return slowPath;
    }

    public void setSlowPath(Boolean slowPath) {
        this.slowPath = slowPath;
    }

    public Long getSlowPathDelay() {
        return slowPathDelay;
    }

    public void setSlowPathDelay(Long slowPathDelay) {
        this.slowPathDelay = slowPathDelay;
    }
}
