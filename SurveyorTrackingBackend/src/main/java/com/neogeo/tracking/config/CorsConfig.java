package com.neogeo.tracking.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {
    
    @Value("${frontend.port:9898}")
    private String frontendPort;
    
    @Override
    public void addCorsMappings(@NonNull CorsRegistry registry) {
        // Maintain exact same production origins
        String[] allowedOrigins = getProductionOrigins();
        
        // Apply identical CORS mappings as in production
        applyProductionCorsMappings(registry, allowedOrigins);
        
        System.out.println("*************CORS configuration updated in CorsConfig with all required origins");
    }
    
    // Keep production origins exactly as they were
    private String[] getProductionOrigins() {
        return new String[] {
            // Local development
            "http://localhost:9898", 
            "http://localhost:3000",
            "http://localhost:3001",
            "http://localhost:6060",
            "http://localhost:6565",
            "http://127.0.0.1:9898",
            "http://127.0.0.1:3000",
            "http://127.0.0.1:3001",
            // Production servers
            "http://183.82.114.29:9898",
            "http://183.82.114.29:6868", 
            "http://183.82.114.29:6060",
            "http://183.82.114.29:6565",
            "http://183.82.114.29:3000",
            "http://183.82.114.29:3001",
            // File protocol
            "file://"
        };
    }
    
    // Replicate the exact production CORS mappings
    private void applyProductionCorsMappings(CorsRegistry registry, String[] allowedOrigins) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Authorization")
                .allowCredentials(false)
                .maxAge(3600);

        registry.addMapping("/ws/location/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Authorization")
                .allowCredentials(false)
                .maxAge(3600);

        registry.addMapping("/ws/location")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Authorization")
                .allowCredentials(false)
                .maxAge(3600);

        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Authorization")
                .allowCredentials(false)
                .maxAge(3600);
    }
    
    // Optional: Add method to check if we're in production
    private boolean isProductionEnvironment() {
        // Implement your environment detection logic here
        return true; // Default to production for safety
    }
}