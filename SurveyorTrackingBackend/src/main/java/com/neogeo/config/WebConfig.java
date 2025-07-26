package com.neogeo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.lang.NonNull;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
public void addCorsMappings(@NonNull CorsRegistry registry) {
    registry.addMapping("/**")
        .allowedOrigins(
            // Local development
            "http://localhost:9898", 
            "http://localhost:3000",
            "http://localhost:6060",
            "http://localhost:6565",
            "http://127.0.0.1:9898",
            // Production servers
            "http://183.82.114.29:9898",
            "http://183.82.114.29:6868", 
            "http://183.82.114.29:6060",
            "http://183.82.114.29:6565",
            "http://183.82.114.29:3000",
            "http://183.82.114.29:3001",
            "http://183.82.114.29:9511",
            "http://183.82.114.29:9503",

            // Allow file:// protocol for testing
            "file://"
        )
        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
        .allowedHeaders("*")
        .exposedHeaders("Authorization")
        .allowCredentials(false)  // Set to false for simpler CORS handling
        .maxAge(3600);

    // Uncomment the interceptor method below if this configuration doesn't work
    System.out.println("*************CORS configuration updated with all required origins");
}

/* 
    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) {
                // Explicitly set HTTP 1.1 protocol version headers
                response.setHeader("Content-Type", "application/json; charset=utf-8");
                response.setHeader("Server", "Spring Boot");
                // Additional headers for remote access
                response.setHeader("Connection", "keep-alive");
                response.setHeader("X-Content-Type-Options", "nosniff");
                response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
                response.setHeader("Access-Control-Allow-Headers", "*");
                // Set cache control to prevent caching issues
                response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
                // Explicitly set HTTP/1.1 status line
                response.setStatus(HttpServletResponse.SC_OK);
                return true;
            }
        });
    } */
}
