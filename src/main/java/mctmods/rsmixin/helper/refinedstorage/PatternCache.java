package mctmods.rsmixin.helper.refinedstorage;

import com.raoulvdberge.refinedstorage.apiimpl.autocrafting.CraftingPattern;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;

public final class PatternCache {
    private static final int MAX_ENTRIES = 500;
    private static final Map<ItemStack, CraftingPattern> IDENTITY = new WeakHashMap<>();
    private static final LinkedHashMap<Key, CraftingPattern> VALUES = new LinkedHashMap<Key, CraftingPattern>(64, 0.75F, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<Key, CraftingPattern> eldest) { return size() > MAX_ENTRIES; }
    };

    private PatternCache() {}

    public static CraftingPattern get(World world, ItemStack stack) {
        synchronized (VALUES) {
            CraftingPattern pattern = IDENTITY.get(stack);
            if (pattern != null) { return pattern; }

            Key key = new Key(stack);
            pattern = VALUES.get(key);
            if (pattern == null) {
                pattern = new CraftingPattern(world, null, stack.copy());
                VALUES.put(key, pattern);
            }

            IDENTITY.put(stack, pattern);
            return pattern;
        }
    }

    private static final class Key {
        private final Item item;
        private final int damage;
        private final NBTTagCompound tag;
        private final int hash;

        private Key(ItemStack stack) {
            this.item = stack.getItem();
            this.damage = stack.getItemDamage();
            this.tag = stack.getTagCompound() == null ? null : stack.getTagCompound().copy();
            this.hash = 31 * (31 * System.identityHashCode(item) + damage) + (tag == null ? 0 : tag.hashCode());
        }

        @Override public boolean equals(Object o) {
            if (this == o) { return true; }
            if (!(o instanceof Key)) { return false; }
            Key other = (Key) o;
            return item == other.item && damage == other.damage && Objects.equals(tag, other.tag);
        }

        @Override public int hashCode() { return hash; }
    }
}
