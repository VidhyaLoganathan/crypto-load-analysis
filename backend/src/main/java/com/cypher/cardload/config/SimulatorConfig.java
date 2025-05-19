package com.cypher.cardload.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SimulatorConfig {

    @Bean
    @ConfigurationProperties(prefix = "load-simulator")
    public SimulatorProperties simulatorProperties() {
        return new SimulatorProperties();
    }

    public static class SimulatorProperties {
        private boolean enabled = true; // Default to true for safety

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
