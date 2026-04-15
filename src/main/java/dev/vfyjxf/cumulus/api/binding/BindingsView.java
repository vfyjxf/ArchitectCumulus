package dev.vfyjxf.cumulus.api.binding;

import java.util.List;
import java.util.Set;

/**
 * bindings 的只读视图：role -> parts。
 *
 * <p>bindings 由 binder 产生，后续 pass（checker/compiler/...）应尽量只读 bindings，
 * 以避免重复扫描世界。
 */
public interface BindingsView {
    /**
     * 所有 role 名称。
     */
    Set<String> roles();

    /**
     * 获取某个 role 的部件列表（如果不存在则返回空列表）。
     */
    List<PartRef> parts(String role);
}
