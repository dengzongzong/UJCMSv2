package com.exam.common;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Spring 静态上下文,在无法注入的场景(如工具类/异步任务)中使用
 */
@Component
public class SpringContextHolder implements ApplicationContextAware {

    private static ApplicationContext context;

    @Override
    @SuppressWarnings("ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD")
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        context = applicationContext;
    }

    public static <T> T getBean(Class<T> clazz) {
        if (context == null) return null;
        try {
            return context.getBean(clazz);
        } catch (Exception e) {
            return null;
        }
    }

    public static Object getBean(String name) {
        if (context == null) return null;
        try {
            return context.getBean(name);
        } catch (Exception e) {
            return null;
        }
    }
}
