package org.example;

import org.springframework.aop.scope.ScopedProxyUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.context.properties.ConfigurationBeanFactoryMetaData;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.annotation.AnnotationUtils;

/**
 * Detects {@link ConfigurationProperties} beans in the Spring context and
 * creates a scoped proxy for them.
 */
public class ConfigPropertiesScopePostProcessor implements BeanFactoryPostProcessor {
    ConfigurationBeanFactoryMetaData metaData;

    public ConfigPropertiesScopePostProcessor(ConfigurationBeanFactoryMetaData metaData) {
        this.metaData = metaData;
    }

    /**
     * Checks whether the supplied class and bean is a
     * {@link ConfigurationProperties} bean and also not registered to be skipped by
     * this processor.
     *
     * @param beanClass
     *            The class of the bean.
     * @param beanName
     *            The name of the bean. This is used to find annotation metadata in
     *            the context.
     * @return true if the bean is a {@link ConfigurationProperties} bean and not
     *         skipped.
     */
    private boolean isConfigPropBeanAndProxyable(Class<?> beanClass, String beanName) {
        boolean isConfigProps = false;
        ConfigurationProperties annotation = AnnotationUtils.findAnnotation(beanClass, ConfigurationProperties.class);
        if (annotation != null) {
            isConfigProps = true;
        } else if (this.metaData != null) {
            annotation = this.metaData.findFactoryAnnotation(beanName, ConfigurationProperties.class);
            if (annotation != null) {
                isConfigProps = true;
            }
        }
        return isConfigProps;
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

        // Register dummy custom scope. Real code is more interesting.
        beanFactory.registerScope(CustomScope.NAME, new CustomScope());

        BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;
        for (String beanName : beanFactory.getBeanDefinitionNames()) {
            Class<?> clazz = beanFactory.getType(beanName);
            if (isConfigPropBeanAndProxyable(clazz, beanName)) {
                BeanDefinition bd = beanFactory.getBeanDefinition(beanName);
                BeanDefinitionHolder bdh = new BeanDefinitionHolder(bd, beanName);
                bd.setScope(CustomScope.NAME);
                bdh = ScopedProxyUtils.createScopedProxy(bdh, registry, true);
                registry.registerBeanDefinition(beanName, bdh.getBeanDefinition());
            }
        }
        // We just added some new bean definitions to the registry (e.g., named
        // scopedTarget.<orignalName>). They are used when
        // org.springframework.boot.context.properties.ConfigurationPropertiesBindingPostProcessor.postProcessBeforeInitialization(Object,String)
        // post processes the new aop proxy target bean instance. However, without the
        // line below, the metaData used by ConfigurationPropertiesBindingPostProcessor
        // won't have the scopedTarget bean name in its list and therefore, if the
        // @ConfigurationProperties annotation was on a factory method, then
        // ConfigurationPropertiesBindingPostProcessor wouldn't find it. This makes sure
        // that these additional new bean method names are found after they have been
        // added to the bean definition registry.
        metaData.postProcessBeanFactory(beanFactory);
    }
}
