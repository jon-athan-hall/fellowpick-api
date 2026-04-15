package com.fellowpick.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

// Binds the cors.allowed-origins property to a typed list.
@ConfigurationProperties(prefix = "cors")
public record CorsProperties(
        List<String> allowedOrigins
) {
}
