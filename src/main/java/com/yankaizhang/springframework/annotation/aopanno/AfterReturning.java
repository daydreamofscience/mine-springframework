package com.yankaizhang.springframework.annotation.aopanno;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AfterReturning {
    String value() default "";
}