package dev.vfyjxf.cumulus.api.request;

/**
 * 请求优先级（建议）。
 *
 * <p>该优先级仅作为“建议”。宿主系统可以忽略或映射到自身的加载队列/票据系统。
 */
public enum RequestPriority {
    /** 低优先级（背景加载/预取）。 */
    LOW,
    /** 默认优先级。 */
    NORMAL,
    /** 高优先级（阻塞关键流程）。 */
    HIGH
}
