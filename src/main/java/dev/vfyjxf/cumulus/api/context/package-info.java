/**
 * 各 pass 的上下文对象。
 *
 * <p>上下文提供统一能力：世界只读访问、跨实例只读访问、预算（budget）、以及向引擎回传
 * {@link dev.vfyjxf.cumulus.api.request.DataRequest} / {@link dev.vfyjxf.cumulus.api.diagnostic.Diagnostic} /
 * {@link dev.vfyjxf.cumulus.api.interest.InterestUpdate}。
 *
 * <p>重要：pass 不应直接“强制加载区块”，而应通过 request 语义表达缺数据。
 */
@NotNullByDefault
package dev.vfyjxf.cumulus.api.context;

import dev.vfyjxf.cloudlib.api.annotation.NotNullByDefault;
