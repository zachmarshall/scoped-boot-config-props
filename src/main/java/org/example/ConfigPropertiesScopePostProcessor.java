package org.example;

import org.springframework.aop.scope.ScopedProxyUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Detects {@link ConfigurationProperties} beans in the Spring context and
 * creates a scoped proxy for them.
 *
 * Part of the code used from <a href="https://github.com/spring-projects/spring-boot/blob/main/spring-boot-project/spring-boot/src/main/java/org/springframework/boot/context/properties/ConfigurationPropertiesBean.java">ConfigurationPropertiesBean</a>
 */
public class ConfigPropertiesScopePostProcessor implements BeanFactoryPostProcessor {

    public ConfigPropertiesScopePostProcessor() {
    }

    /**
     * Checks whether the supplied class and bean is a
     * {@link ConfigurationProperties} bean and also not registered to be skipped by
     * this processor.
     *
     * @param beanFactory
     *            The bean factory.
     * @param beanName
     *            The name of the bean. This is used to find annotation metadata in
     *            the context.
     * @return true if the bean is a {@link ConfigurationProperties} bean and not
     *         skipped.
     */
    private boolean isConfigPropBeanAndProxyable(ConfigurableListableBeanFactory beanFactory, String beanName) {
        try {
            if (beanFactory.getBeanDefinition(beanName).isAbstract()) {
                return false;
            }
            if (beanFactory.findAnnotationOnBean(beanName, ConfigurationProperties.class) != null) {
                return true;
            }
            Method factoryMethod = findFactoryMethod(beanFactory, beanName);
            return findMergedAnnotation(factoryMethod, ConfigurationProperties.class).isPresent();
        }
        catch (NoSuchBeanDefinitionException ex) {
            return false;
        }
    }

    /**
     * Iterates through all the bean definitions in the registry and finds
     * {@link ConfigurationProperties} beans that should be proxied for
     * custom scope. Creates a scoped proxy for those beans and registers them in
     * the context. The Spring {@link ScopedProxyUtils} handles this for us,
     * including marking the original bean as not eligible for autowiring which
     * handles most bean conflicts.
     *
     * <p>(However, certain Spring cloud/boot components
     * actually do not completely honor this registration method (errarntly
     * detecting both beans but not realizing that one is not an autowire candidate
     * - see
     * <tt>org.springframework.boot.autoconfigure.web.ServerPropertiesAutoConfiguration.DuplicateServerPropertiesDetector.customize(ConfigurableEmbeddedServletContainer)</tt>.)
     */
    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        if (!(beanFactory instanceof BeanDefinitionRegistry)) {
            throw new IllegalArgumentException("Can only process BeanDefinitionRegistry instances");
        }
        boolean oldAllowBeanDefinitionOverriding = false;
        if (beanFactory instanceof DefaultListableBeanFactory){
            oldAllowBeanDefinitionOverriding = ((DefaultListableBeanFactory)beanFactory).isAllowBeanDefinitionOverriding();
            ((DefaultListableBeanFactory)beanFactory).setAllowBeanDefinitionOverriding(true);
        }

        // Register dummy custom scope. Real code is more interesting.
        beanFactory.registerScope(CustomScope.NAME, new CustomScope());

        BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;
        for (String beanName : beanFactory.getBeanDefinitionNames()) {
            if (isConfigPropBeanAndProxyable(beanFactory, beanName)) {
                BeanDefinition bd = beanFactory.getBeanDefinition(beanName);
                BeanDefinitionHolder bdh = new BeanDefinitionHolder(bd, beanName);
                bd.setScope(CustomScope.NAME);
                BeanDefinitionHolder scopedBdh = ScopedProxyUtils.createScopedProxy(bdh, registry, true);
                registry.registerBeanDefinition(beanName, scopedBdh.getBeanDefinition());
            }
        }

        if (beanFactory instanceof DefaultListableBeanFactory){
            ((DefaultListableBeanFactory)beanFactory).setAllowBeanDefinitionOverriding(oldAllowBeanDefinitionOverriding);
        }
    }

    private static <A extends Annotation> MergedAnnotation<A> findMergedAnnotation(AnnotatedElement element,
                                                                                   Class<A> annotationType) {
        return (element != null) ? MergedAnnotations.from(element, MergedAnnotations.SearchStrategy.TYPE_HIERARCHY).get(annotationType)
                : MergedAnnotation.missing();
    }

    private static Method findFactoryMethod(ConfigurableListableBeanFactory beanFactory, String beanName) {
        if (beanFactory.containsBeanDefinition(beanName)) {
            BeanDefinition beanDefinition = beanFactory.getMergedBeanDefinition(beanName);
            if (beanDefinition instanceof RootBeanDefinition) {
                RootBeanDefinition rootBeanDefinition = (RootBeanDefinition)beanDefinition;
                Method resolvedFactoryMethod = rootBeanDefinition.getResolvedFactoryMethod();
                if (resolvedFactoryMethod != null) {
                    return resolvedFactoryMethod;
                }
            }
            return findFactoryMethodUsingReflection(beanFactory, beanDefinition);
        }
        return null;
    }

    private static Method findFactoryMethodUsingReflection(ConfigurableListableBeanFactory beanFactory,
                                                           BeanDefinition beanDefinition) {
        String factoryMethodName = beanDefinition.getFactoryMethodName();
        String factoryBeanName = beanDefinition.getFactoryBeanName();
        if (factoryMethodName == null || factoryBeanName == null) {
            return null;
        }
        Class<?> factoryType = beanFactory.getType(factoryBeanName);
        if (factoryType.getName().contains(ClassUtils.CGLIB_CLASS_SEPARATOR)) {
            factoryType = factoryType.getSuperclass();
        }
        AtomicReference<Method> factoryMethod = new AtomicReference<>();
        ReflectionUtils.doWithMethods(factoryType, (method) -> {
            if (method.getName().equals(factoryMethodName)) {
                factoryMethod.set(method);
            }
        });
        return factoryMethod.get();
    }

}
