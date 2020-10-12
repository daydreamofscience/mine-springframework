package com.yankaizhang.springframework.aop.support;

import com.yankaizhang.springframework.annotation.aopanno.*;
import com.yankaizhang.springframework.aop.AopConfig;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AopAnnotationReader主要完成对AOP切面类的解析
 */
public class AopAnnotationReader {

    public AopAnnotationReader() {}

    private Map<Class<?>, String> pointCutMap;  // 切面类和切点表达式的map

    public void setPointCutMap(Map<Class<?>, String> pointCutMap) {
        this.pointCutMap = pointCutMap;
    }

    /**
     * 解析切面类的注解，返回 *多个* aopConfig
     */
    public List<AdvisedSupport> parseAspect(){

        if (pointCutMap.isEmpty()) return null;
        ArrayList<AdvisedSupport> aopConfigList = new ArrayList<>(pointCutMap.size());

        for (Map.Entry<Class<?>, String> entry : pointCutMap.entrySet()) {
            String pointCutNow = entry.getValue();  // 当前切点表达式
            Class<?> aspectClazz = entry.getKey();  // 当前切面类

            AopConfig aopConfig = new AopConfig();
            aopConfig.setPointCut(pointCutNow);
            aopConfig.setAspectClass(aspectClazz.getTypeName());
            Method[] aspectMethods = aspectClazz.getDeclaredMethods();
            // 处理当前切面类的其他注解
            for (Method method : aspectMethods) {
                adviceTypes adviceType = getAdvice(method);
                if (adviceType == null) continue;
                switch (adviceType){
                    case BEFORE:
                        aopConfig.setAspectBefore(method.getName());break;
                    case AFTER_RETURN:
                        aopConfig.setAspectAfter(method.getName());break;
                    case AFTER_THROW:
                        String exception = method.getAnnotation(AfterThrowing.class).exception();
                        aopConfig.setAspectAfterThrow(method.getName());
                        aopConfig.setAspectAfterThrowingName(exception);
                        break;
                    default:
                        break;
                }
            }

            aopConfigList.add(new AdvisedSupport(aopConfig));
        }

        return aopConfigList;
    }

    private adviceTypes getAdvice(Method method){
        for (Annotation annotation : method.getAnnotations()) {
            if (annotation instanceof Before){
                return adviceTypes.BEFORE;
            }else if (annotation instanceof AfterReturning){
                return adviceTypes.AFTER_RETURN;
            }else if (annotation instanceof AfterThrowing){
                return adviceTypes.AFTER_THROW;
            }
        }
        return null;
    }

    /**
     * 通知类型
     */
    private enum adviceTypes{
        BEFORE,
        AFTER_RETURN,
        AFTER_THROW
    }
}