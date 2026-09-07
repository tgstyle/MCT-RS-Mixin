package mctmods.rsmixin.helper.rebornstorage;

public enum MultiblockRegion {
    FRAME("misc.rsmixin.rebornstorage.region.frame"),
    FACE("misc.rsmixin.rebornstorage.region.face"),
    INTERIOR("misc.rsmixin.rebornstorage.region.interior");

    private final String key;

    MultiblockRegion(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }
}
