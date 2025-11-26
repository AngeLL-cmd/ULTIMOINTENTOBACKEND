package com.elecciones.controller;

import com.elecciones.dto.LoginRequest;
import com.elecciones.service.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {
    
    private final AdminService adminService;
    
    /**
     * Endpoint para login de administrador
     * POST /api/admin/login
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest loginRequest) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            log.info("Intento de login admin con email: {}", loginRequest.getEmail());
            
            String token = adminService.authenticate(loginRequest.getEmail(), loginRequest.getPassword());
            
            if (token != null) {
                response.put("success", true);
                response.put("token", token);
                response.put("message", "Login exitoso");
                log.info("Login exitoso para: {}", loginRequest.getEmail());
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "Credenciales inválidas");
                log.warn("Login fallido para: {}", loginRequest.getEmail());
                return ResponseEntity.status(401).body(response);
            }
        } catch (Exception e) {
            log.error("Error en login admin: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Error al procesar el login: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * Endpoint para verificar si el token es válido
     * GET /api/admin/verify
     */
    @GetMapping("/verify")
    public ResponseEntity<Map<String, Object>> verify(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                response.put("success", false);
                response.put("message", "Token no proporcionado");
                return ResponseEntity.status(401).body(response);
            }
            
            String token = authHeader.substring(7);
            boolean isValid = adminService.validateToken(token);
            
            response.put("success", isValid);
            response.put("valid", isValid);
            response.put("message", isValid ? "Token válido" : "Token inválido");
            
            return isValid ? ResponseEntity.ok(response) : ResponseEntity.status(401).body(response);
        } catch (Exception e) {
            log.error("Error al verificar token: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("valid", false);
            response.put("message", "Error al verificar token");
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * Endpoint para eliminar valores nulos
     * POST /api/admin/clean/null-values
     */
    @PostMapping("/clean/null-values")
    public ResponseEntity<Map<String, Object>> deleteNullValues(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            if (!validateAuth(authHeader, response)) {
                return ResponseEntity.status(401).body(response);
            }
            
            int deletedCount = adminService.deleteNullValues();
            response.put("success", true);
            response.put("deletedCount", deletedCount);
            response.put("message", "Se eliminaron " + deletedCount + " registros con valores nulos");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error al eliminar valores nulos: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Error al eliminar valores nulos: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * Endpoint para eliminar duplicados
     * POST /api/admin/clean/duplicates
     */
    @PostMapping("/clean/duplicates")
    public ResponseEntity<Map<String, Object>> deleteDuplicates(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            if (!validateAuth(authHeader, response)) {
                return ResponseEntity.status(401).body(response);
            }
            
            int deletedCount = adminService.deleteDuplicateVotes();
            response.put("success", true);
            response.put("deletedCount", deletedCount);
            response.put("message", "Se eliminaron " + deletedCount + " votos duplicados");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error al eliminar duplicados: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Error al eliminar duplicados: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * Endpoint para validar DNIs
     * GET /api/admin/clean/validate-dnis
     */
    @GetMapping("/clean/validate-dnis")
    public ResponseEntity<Map<String, Object>> validateDNIs(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            if (!validateAuth(authHeader, response)) {
                return ResponseEntity.status(401).body(response);
            }
            
            java.util.List<String> invalidDNIs = adminService.validateDNIs();
            response.put("success", true);
            response.put("invalidDNIs", invalidDNIs);
            response.put("count", invalidDNIs.size());
            response.put("message", invalidDNIs.isEmpty() ? 
                "Todos los DNIs tienen el formato correcto" : 
                "Se encontraron " + invalidDNIs.size() + " DNIs con formato inválido");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error al validar DNIs: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Error al validar DNIs: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * Endpoint para normalizar datos
     * POST /api/admin/clean/normalize
     */
    @PostMapping("/clean/normalize")
    public ResponseEntity<Map<String, Object>> normalizeData(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            if (!validateAuth(authHeader, response)) {
                return ResponseEntity.status(401).body(response);
            }
            
            int normalizedCount = adminService.normalizeData();
            response.put("success", true);
            response.put("normalizedCount", normalizedCount);
            response.put("message", "Se normalizaron " + normalizedCount + " registros");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error al normalizar datos: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Error al normalizar datos: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * Endpoint para análisis de tendencias
     * POST /api/admin/training/trends
     */
    @PostMapping("/training/trends")
    public ResponseEntity<Map<String, Object>> analyzeTrends(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            if (!validateAuth(authHeader, response)) {
                return ResponseEntity.status(401).body(response);
            }
            
            Map<String, Object> trendsData = adminService.analyzeTrends();
            response.put("success", true);
            response.put("data", trendsData);
            response.put("message", "Análisis de tendencias completado");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error al analizar tendencias: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Error al analizar tendencias: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * Endpoint para detección de anomalías
     * POST /api/admin/training/anomalies
     */
    @PostMapping("/training/anomalies")
    public ResponseEntity<Map<String, Object>> detectAnomalies(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            if (!validateAuth(authHeader, response)) {
                return ResponseEntity.status(401).body(response);
            }
            
            Map<String, Object> anomaliesData = adminService.detectAnomalies();
            response.put("success", true);
            response.put("data", anomaliesData);
            response.put("message", "Detección de anomalías completada");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error al detectar anomalías: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Error al detectar anomalías: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * Endpoint para análisis de participación
     * POST /api/admin/training/participation
     */
    @PostMapping("/training/participation")
    public ResponseEntity<Map<String, Object>> analyzeParticipation(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            if (!validateAuth(authHeader, response)) {
                return ResponseEntity.status(401).body(response);
            }
            
            Map<String, Object> participationData = adminService.analyzeParticipation();
            response.put("success", true);
            response.put("data", participationData);
            response.put("message", "Análisis de participación completado");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error al analizar participación: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Error al analizar participación: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * Valida el token de autenticación
     */
    private boolean validateAuth(String authHeader, Map<String, Object> response) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.put("success", false);
            response.put("message", "Token no proporcionado");
            return false;
        }
        
        String token = authHeader.substring(7);
        if (!adminService.validateToken(token)) {
            response.put("success", false);
            response.put("message", "Token inválido o expirado");
            return false;
        }
        
        return true;
    }
}

