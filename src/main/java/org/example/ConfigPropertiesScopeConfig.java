package org.example;

import org.springframework.boot.context.properties.ConfigurationBeanFactoryMetadata;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConfigPropertiesScopeConfig {
    @Bean
    ConfigPropertiesScopePostProcessor configPropertiesScopePostProcessor(ConfigurationBeanFactoryMetadata metaData) {
        return new ConfigPropertiesScopePostProcessor(metaData);
    }
}
