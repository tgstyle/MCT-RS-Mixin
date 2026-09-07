package mctmods.rsmixin.helper.rebornstorage;

import java.util.List;

public interface MultiblockFaultHolder {

    List<MultiblockFault> rsmixin$faults();

    int rsmixin$cpuCount();

    int rsmixin$storageCount();
}
