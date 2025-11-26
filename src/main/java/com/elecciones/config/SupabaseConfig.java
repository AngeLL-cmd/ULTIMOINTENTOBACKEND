package com.elecciones.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class SupabaseConfig {
    
    @Value("${supabase.url}")
    private String supabaseUrl;
    
    @Value("${supabase.key}")
    private String supabaseKey;
    
    @Value("${supabase.service-key}")
    private String supabaseServiceKey;
    
    public String getApiUrl() {
        return supabaseUrl + "/rest/v1";
    }
    
    public String getAuthHeader() {
        return "Bearer " + supabaseKey;
    }
    
    public String getServiceAuthHeader() {
        return "Bearer " + supabaseServiceKey;
    }
    
    // Getters explícitos para Lombok (por si acaso)
    public String getSupabaseUrl() {
        return supabaseUrl;
    }
    
    public String getSupabaseKey() {
        return supabaseKey;
    }
    
    public String getSupabaseServiceKey() {
        return supabaseServiceKey;
    }
}
