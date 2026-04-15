/**
 * 代码侧 builder/DSL。
 *
 * <p>用于在 Java 代码中构建 {@link dev.vfyjxf.cumulus.api.ArchitectSchema} 与 {@link dev.vfyjxf.cumulus.api.binding.plan.BindingPlan}，
 * 目标是让“代码驱动 schema”和“数据驱动 schema”保持同构，便于后续迁移到 JSON/datagen。
 */
@NotNullByDefault
package dev.vfyjxf.cumulus.api.dsl;

import dev.vfyjxf.cloudlib.api.annotation.NotNullByDefault;
