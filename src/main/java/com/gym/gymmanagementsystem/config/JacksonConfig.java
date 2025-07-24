package com.gym.gymmanagementsystem.config;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.datatype.hibernate6.Hibernate6Module;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    @Bean
    public Module hibernateModule() {
        Hibernate6Module module = new Hibernate6Module();
        // This ensures that uninitialized lazy-loaded objects are not serialized,
        // preventing the "No serializer found for class ...ByteBuddyInterceptor" error.
        module.disable(Hibernate6Module.Feature.FORCE_LAZY_LOADING);
        // Removed: module.enable(Hibernate6Module.Feature.SERIALIZE_IDENTIFIER_FOR_LAZY_LOAD_PROXY);
        return module;
    }
}