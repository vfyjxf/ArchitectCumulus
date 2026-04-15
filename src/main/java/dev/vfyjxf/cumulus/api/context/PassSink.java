package dev.vfyjxf.cumulus.api.context;

import dev.vfyjxf.cumulus.api.diagnostic.Diagnostic;
import dev.vfyjxf.cumulus.api.interest.InterestUpdate;
import dev.vfyjxf.cumulus.api.request.DataRequest;

/**
 * pass 输出汇聚接口。
 *
 * <p>引擎实现会提供一个 sink，供 pass 回传：
 * <ul>
 *     <li>缺数据请求（DataRequest）</li>
 *     <li>诊断（Diagnostic）</li>
 *     <li>兴趣集更新（InterestUpdate）</li>
 * </ul>
 */
public interface PassSink {
    void request(DataRequest request);

    void diag(Diagnostic diagnostic);

    void interest(InterestUpdate update);
}
