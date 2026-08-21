package com.enterprise.ai.common.annotation;

import java.lang.annotation.*;

/**
 * 操作日志注解
 * 用于记录重要操作的日志
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLog {

    /**
     * 操作描述
     */
    String value() default "";

    /**
     * 操作模块
     */
    String module() default "";

    /**
     * 是否记录请求参数
     */
    boolean recordParams() default true;

    /**
     * 是否记录返回结果
     */
    boolean recordResult() default false;
}
