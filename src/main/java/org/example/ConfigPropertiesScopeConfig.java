package org.example;

import org.springframework.boot.context.properties.ConfigurationBeanFactoryMetaData;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConfigPropertiesScopeConfig {
    @Bean
    ConfigPropertiesScopePostProcessor configPropertiesScopePostProcessor(ConfigurationBeanFactoryMetaData metaData) {
        return new ConfigPropertiesScopePostProcessor(metaData);
    }
}
