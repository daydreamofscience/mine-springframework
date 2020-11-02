package com.yankaizhang.springframework.context.annotation;

import java.lang.annotation.*;

/**
 * Bean懒加载
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Lazy {
}
