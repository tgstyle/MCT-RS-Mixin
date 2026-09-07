package mctmods.rsmixin.helper.rebornstorage;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class MultiblockReport {

    public static final int MAX_FAULTS = 512;

    private static final int MAX_LINES = 6;
    private static final int MAX_TRACKED = 64;
    private static final int MAX_SHOWN_POSITIONS = 4;

    private MultiblockReport() {
    }

    private record Group(MultiblockRegion region, Block block) {
    }

    private static final class Tally {
        private int count;
        private final List<BlockPos> positions = new ArrayList<>();
    }

    public static boolean incomplete(List<MultiblockFault> faults) {
        return !faults.isEmpty();
    }

    public static int issueCount(List<MultiblockFault> faults) {
        return faults.size();
    }

    public static List<Component> lines(List<MultiblockFault> faults, BlockPos min, BlockPos max, BlockPos origin,
                                        int cpuCount, int storageCount) {
        List<Component> out = new ArrayList<>();
        out.add(header(faults, min, max));
        out.addAll(advice(cpuCount, storageCount));
        if (faults.isEmpty()) {
            return out;
        }

        List<Map.Entry<Group, Tally>> groups = group(faults);
        int shown = Math.min(groups.size(), MAX_LINES);
        for (int i = 0; i < shown; i++) {
            Map.Entry<Group, Tally> entry = groups.get(i);
            Group group = entry.getKey();
            Tally tally = entry.getValue();
            Component region = Component.translatable(group.region().key());
            Component where = Component.literal(positions(tally, origin));
            MutableComponent line;
            if (group.block() == null) {
                line = Component.translatable("misc.rsmixin.rebornstorage.missing", region, tally.count, where);
            } else {
                line = Component.translatable("misc.rsmixin.rebornstorage.wrong",
                        region, tally.count, group.block().getName(), where);
            }
            out.add(Component.literal(" - ").append(line).withStyle(ChatFormatting.RED));
        }
        if (groups.size() > shown) {
            out.add(Component.translatable("misc.rsmixin.rebornstorage.more", groups.size() - shown)
                    .withStyle(ChatFormatting.DARK_GRAY));
        }

        boolean shell = false;
        boolean interior = false;
        for (Map.Entry<Group, Tally> entry : groups) {
            if (entry.getKey().region() == MultiblockRegion.INTERIOR) { interior = true; } else { shell = true; }
        }
        if (shell) {
            out.add(Component.translatable("misc.rsmixin.rebornstorage.allowed",
                            Component.translatable("misc.rsmixin.rebornstorage.region.shell"),
                            MultiblockRules.allowedList(MultiblockRegion.FACE))
                    .withStyle(ChatFormatting.GRAY));
        }
        if (interior) {
            out.add(Component.translatable("misc.rsmixin.rebornstorage.allowed",
                            Component.translatable(MultiblockRegion.INTERIOR.key()),
                            MultiblockRules.allowedList(MultiblockRegion.INTERIOR))
                    .withStyle(ChatFormatting.GRAY));
        }
        return out;
    }

    public static List<Component> advice(int cpuCount, int storageCount) {
        List<Component> out = new ArrayList<>();
        if (cpuCount == 0) {
            out.add(Component.translatable("misc.rsmixin.rebornstorage.no_cpu").withStyle(ChatFormatting.GOLD));
        }
        if (storageCount == 0) {
            out.add(Component.translatable("misc.rsmixin.rebornstorage.no_storage_hint").withStyle(ChatFormatting.GOLD));
        }
        return out;
    }

    public static List<Component> formed(BlockPos min, BlockPos max, int cpuCount, int storageCount) {
        List<Component> out = new ArrayList<>();
        out.add(Component.translatable("misc.rsmixin.rebornstorage.formed", size(min, max), pos(min), pos(max))
                .withStyle(ChatFormatting.GREEN));
        out.addAll(advice(cpuCount, storageCount));
        return out;
    }

    public static Component actionBar(List<MultiblockFault> faults) {
        return Component.translatable("misc.rsmixin.rebornstorage.actionbar", issueCount(faults))
                .withStyle(ChatFormatting.RED);
    }

    public static String plain(List<MultiblockFault> faults, BlockPos min, BlockPos max, BlockPos origin,
                               int cpuCount, int storageCount) {
        StringBuilder sb = new StringBuilder(header(faults, min, max).getString());
        for (Component line : advice(cpuCount, storageCount)) {
            sb.append("; ").append(line.getString());
        }
        for (Map.Entry<Group, Tally> entry : group(faults)) {
            Group group = entry.getKey();
            sb.append("; ").append(Component.translatable(group.region().key()).getString())
                    .append(": ").append(entry.getValue().count);
            if (group.block() == null) {
                sb.append(" missing at ");
            } else {
                sb.append("x ").append(group.block().getName().getString()).append(" does not belong at ");
            }
            sb.append(positions(entry.getValue(), origin));
        }
        return sb.toString();
    }

    private static MutableComponent header(List<MultiblockFault> faults, BlockPos min, BlockPos max) {
        if (min == null || max == null) {
            return Component.translatable("misc.rsmixin.rebornstorage.actionbar", issueCount(faults))
                    .withStyle(ChatFormatting.YELLOW);
        }
        return Component.translatable("misc.rsmixin.rebornstorage.header", size(min, max), pos(min), pos(max))
                .withStyle(ChatFormatting.YELLOW);
    }

    private static String size(BlockPos min, BlockPos max) {
        return (max.getX() - min.getX() + 1) + "x" + (max.getY() - min.getY() + 1) + "x"
                + (max.getZ() - min.getZ() + 1);
    }

    private static String positions(Tally tally, BlockPos origin) {
        List<BlockPos> sorted = new ArrayList<>(tally.positions);
        if (origin != null) {
            sorted.sort(Comparator.comparingDouble(p -> p.distSqr(origin)));
        }
        int shown = Math.min(sorted.size(), MAX_SHOWN_POSITIONS);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < shown; i++) {
            if (i > 0) {
                sb.append("; ");
            }
            sb.append(pos(sorted.get(i)));
        }
        int hidden = tally.count - shown;
        if (hidden > 0) {
            sb.append(" (+").append(hidden).append(" more)");
        }
        return sb.toString();
    }

    private static List<Map.Entry<Group, Tally>> group(List<MultiblockFault> faults) {
        Map<Group, Tally> grouped = new TreeMap<>(Comparator
                .comparing((Group g) -> g.region().ordinal())
                .thenComparing(g -> g.block() != null)
                .thenComparing(g -> g.block() == null ? "" : g.block().getName().getString()));

        for (MultiblockFault fault : faults) {
            Block block = fault.missing() ? null : fault.found().getBlock();
            Tally tally = grouped.computeIfAbsent(new Group(fault.region(), block), key -> new Tally());
            tally.count++;
            if (tally.positions.size() < MAX_TRACKED) {
                tally.positions.add(fault.pos());
            }
        }

        List<Map.Entry<Group, Tally>> entries = new ArrayList<>(grouped.entrySet());
        entries.sort(Comparator.comparingInt((Map.Entry<Group, Tally> e) -> e.getValue().count).reversed());
        return entries;
    }

    private static String pos(BlockPos pos) {
        return pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
    }
}
