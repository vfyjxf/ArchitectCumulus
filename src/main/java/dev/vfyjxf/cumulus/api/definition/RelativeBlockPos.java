package dev.vfyjxf.cumulus.api.definition;

public record RelativeBlockPos(int x, int y, int z) {
    public static RelativeBlockPos of(int x, int y, int z) {
        return new RelativeBlockPos(x, y, z);
    }

    public String asString() {
        return x + "," + y + "," + z;
    }
}
