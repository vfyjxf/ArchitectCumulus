package dev.vfyjxf.cumulus.api.diagnostic;

import java.util.Map;
import java.util.Objects;

/**
 * 修复提示（纯数据，不执行）。
 *
 * <p>用于“自动构建/自动修复”类工具：checker/compiler 可附带若干 fix hint，
 * 由上层系统决定是否以及如何应用。
 */
public record FixHint(String kind, String summary, Map<String, String> params) {
    public FixHint {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(summary, "summary");
        params = Map.copyOf(Objects.requireNonNull(params, "params"));
    }
}
