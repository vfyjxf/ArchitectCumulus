package dev.vfyjxf.cumulus.api.binding;

import java.util.List;
import java.util.Set;

public interface BindingsView {
    Set<String> roles();

    List<PartRef> parts(String role);
}
