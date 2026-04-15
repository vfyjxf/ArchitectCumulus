package dev.vfyjxf.cumulus.api.pass;

import dev.vfyjxf.cumulus.api.diagnostic.Diagnostic;
import dev.vfyjxf.cumulus.api.diagnostic.InstanceStatus;
import dev.vfyjxf.cumulus.api.interest.InterestUpdate;
import dev.vfyjxf.cumulus.api.request.DataRequest;

import java.util.List;

/**
 * 所有 pass report 的公共字段。
 *
 * <p>约定：
 * <ul>
 *     <li>status=INCOMPLETE 时应包含 requests（缺数据）与必要诊断</li>
 *     <li>status=ERROR 时应包含 ERROR 级诊断</li>
 * </ul>
 */
public interface PassReport {
    InstanceStatus status();

    List<Diagnostic> diagnostics();

    List<DataRequest> requests();

    InterestUpdate interestUpdate();
}
