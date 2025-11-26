package com.elecciones.controller;

import com.elecciones.service.SupabaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
@Slf4j
public class HealthController {
    
    private final SupabaseService supabaseService;
    
    /**
     * Verifica la conexión con Supabase
     * GET /api/health/supabase
     */
    @GetMapping("/supabase")
    public ResponseEntity<Map<String, Object>> checkSupabaseConnection() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Intentar contar votantes (operación simple que verifica la conexión)
            long voterCount = supabaseService.countVoters();
            
            response.put("success", true);
            response.put("connected", true);
            response.put("message", "Conexión a Supabase exitosa");
            response.put("voterCount", voterCount);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error al verificar conexión con Supabase: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("connected", false);
            response.put("message", "Error al conectar con Supabase: " + e.getMessage());
            response.put("error", e.getClass().getSimpleName());
            
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * Verifica el estado general del sistema
     * GET /api/health
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "Sistema Electoral Backend");
        return ResponseEntity.ok(response);
    }
}

