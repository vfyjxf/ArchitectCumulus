package dev.vfyjxf.cumulus.impl.minecraft;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;

public interface EntityBuildWriter {
    boolean canWrite(ResourceLocation entityId);

    boolean hasEntity(BlockPos pos, ResourceLocation entityId, String nbt);

    boolean spawnEntity(BlockPos pos, ResourceLocation entityId, String nbt);

    static boolean containsAuthoredNbt(String actual, String authored) {
        return containsAuthoredNbt(parse(actual), parse(authored));
    }

    static boolean containsAuthoredNbt(CompoundTag actual, CompoundTag authored) {
        for (var key : authored.getAllKeys()) {
            if (!actual.contains(key)) {
                return false;
            }
            var expected = authored.get(key);
            var current = actual.get(key);
            if (expected instanceof CompoundTag expectedCompound && current instanceof CompoundTag currentCompound) {
                if (!containsAuthoredNbt(currentCompound, expectedCompound)) {
                    return false;
                }
            } else if (!expected.equals(current)) {
                return false;
            }
        }
        return true;
    }

    private static CompoundTag parse(String value) {
        try {
            return TagParser.parseTag(value);
        } catch (Exception ex) {
            return new CompoundTag();
        }
    }
}
