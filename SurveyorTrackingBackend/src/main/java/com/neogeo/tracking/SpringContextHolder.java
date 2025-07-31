
package com.neogeo.tracking;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Utility class to access the Spring ApplicationContext from non-Spring managed classes.
 * This is particularly useful for accessing Spring beans from utility classes.
 */
@Component
public class SpringContextHolder implements ApplicationContextAware {

    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        SpringContextHolder.applicationContext = applicationContext;
    }

    /**
     * Get the Spring ApplicationContext
     * @return the ApplicationContext
     */
    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    /**
     * Get a Spring bean by name
     * @param beanName the name of the bean
     * @return the bean instance
     */
    public static Object getBean(String beanName) {
        return applicationContext.getBean(beanName);
    }

    /**
     * Get a Spring bean by type
     * @param beanClass the class of the bean
     * @return the bean instance
     */
    public static <T> T getBean(Class<T> beanClass) {
        return applicationContext.getBean(beanClass);
    }
}
