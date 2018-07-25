package org.example;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Sets up a Spring Boot integration test.
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@TestPropertySource(properties = {"test.name=" + ScopedConfigPropsTest.INJECTED_VALUE})
public class ScopedConfigPropsTest {
    final static String DEFAULT_VALUE = "test";
    final static String INJECTED_VALUE = "injectedValue";

    static class TestProperties {
        String name = DEFAULT_VALUE;
        public String getName() {
            return name;
        }
        public void setName(String name) {
            this.name = name;
        }
    }

    @Configuration
    @EnableConfigurationProperties
    @EnableAutoConfiguration
    static class TestConfiguration {
        @Bean
        @ConfigurationProperties("test")
        TestProperties testProperties() {
            return new TestProperties();
        }
    }

    @Autowired
    TestProperties testProperties;

    @Test
    public void contextStarts() {
        assertNotNull(testProperties);
        assertEquals("Spring should have been able to bind an initial value.", INJECTED_VALUE, testProperties.getName());
    }
}
