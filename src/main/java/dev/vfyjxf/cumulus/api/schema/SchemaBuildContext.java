package dev.vfyjxf.cumulus.api.schema;

/**
 * schema 构建上下文（由宿主提供）。
 *
 * <p>用于给数据驱动组件提供外部依赖，例如：
 * <ul>
 *     <li>注册表访问（blocks/tags/entity types）</li>
 *     <li>配置开关/环境信息</li>
 *     <li>缓存/共享资源</li>
 * </ul>
 *
 * <p>v1 仅提供占位接口；common 中有 {@code EmptySchemaBuildContext} 供测试使用。
 */
public interface SchemaBuildContext {
}
