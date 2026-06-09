package dev.vfyjxf.cumulus.impl.codec;

public record RuntimeAssetJsonSource(
        String resourcePath,
        String content) {
    public RuntimeAssetJsonSource {
        if (resourcePath == null || resourcePath.isBlank()) {
            throw new IllegalArgumentException("Runtime asset JSON path must not be blank");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Runtime asset JSON content must not be blank");
        }
    }
}
