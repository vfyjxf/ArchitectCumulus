package dev.vfyjxf.cumulus.api.engine;

public interface Budget {
    boolean tryConsume(long cost);

    long remaining();

    static Budget unlimited() {
        return new Budget() {
            @Override
            public boolean tryConsume(long cost) {
                return true;
            }

            @Override
            public long remaining() {
                return Long.MAX_VALUE;
            }
        };
    }
}
