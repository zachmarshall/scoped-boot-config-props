package org.example;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConfigPropertiesScopeConfig {
    @Bean
    ConfigPropertiesScopePostProcessor configPropertiesScopePostProcessor() {
        return new ConfigPropertiesScopePostProcessor();
    }
}
