package dev.vfyjxf.cumulus.common.checker;

import dev.vfyjxf.cumulus.api.context.CheckContext;
import dev.vfyjxf.cumulus.api.diagnostic.Diagnostic;
import dev.vfyjxf.cumulus.api.diagnostic.DiagnosticEvidence;
import dev.vfyjxf.cumulus.api.diagnostic.InstanceStatus;
import dev.vfyjxf.cumulus.api.diagnostic.Severity;
import dev.vfyjxf.cumulus.api.interest.InterestUpdate;
import dev.vfyjxf.cumulus.api.pass.CheckReport;
import dev.vfyjxf.cumulus.api.pass.Checker;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class RoleCountChecker implements Checker {
    public static final ResourceLocation DIAG_ROLE_COUNT = ResourceLocation.fromNamespaceAndPath("cumulus", "role_count");

    private final String role;
    private final int min;
    private final int max;

    public RoleCountChecker(String role, int min, int max) {
        this.role = Objects.requireNonNull(role, "role");
        if (min < 0) {
            throw new IllegalArgumentException("min must be >= 0");
        }
        if (max < min) {
            throw new IllegalArgumentException("max must be >= min");
        }
        this.min = min;
        this.max = max;
    }

    @Override
    public CheckReport check(CheckContext ctx) {
        Objects.requireNonNull(ctx, "ctx");
        int count = ctx.bindings().parts(role).size();
        if (count < min || count > max) {
            DiagnosticEvidence evidence = new DiagnosticEvidence(Set.of(ctx.anchorPos()), Set.of(), Set.of(role), Set.of());
            String summary = "Role '" + role + "' expected [" + min + "," + max + "], got " + count;
            Diagnostic diag = Diagnostic.of(DIAG_ROLE_COUNT, Severity.ERROR, summary, evidence);
            return new CheckReport(InstanceStatus.ERROR, List.of(diag), List.of(), InterestUpdate.empty());
        }
        return CheckReport.ok(InterestUpdate.empty());
    }
}

