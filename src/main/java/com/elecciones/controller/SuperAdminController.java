package com.elecciones.controller;

import com.elecciones.dto.LoginRequest;
import com.elecciones.service.SuperAdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/superadmin")
@RequiredArgsConstructor
@Slf4j
public class SuperAdminController {
    
    private final SuperAdminService superAdminService;
    
    /**
     * Endpoint para login de super administrador
     * POST /api/superadmin/login
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest loginRequest) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            log.info("=== SUPER ADMIN LOGIN INTENT ===");
            log.info("Intento de login super admin con email: {}", loginRequest.getEmail());
            log.info("Endpoint: /api/superadmin/login");
            
            String token = superAdminService.authenticate(loginRequest.getEmail(), loginRequest.getPassword());
            
            if (token != null) {
                response.put("success", true);
                response.put("token", token);
                response.put("message", "Login exitoso");
                log.info("Login exitoso para super admin: {}", loginRequest.getEmail());
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "Credenciales inválidas");
                log.warn("Login fallido para super admin: {}", loginRequest.getEmail());
                return ResponseEntity.status(401).body(response);
            }
        } catch (Exception e) {
            log.error("Error en login super admin: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Error al procesar el login: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * Endpoint para verificar si el token es válido
     * GET /api/superadmin/verify
     */
    @GetMapping("/verify")
    public ResponseEntity<Map<String, Object>> verify(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                response.put("success", false);
                response.put("valid", false);
                response.put("message", "Token no proporcionado");
                return ResponseEntity.status(401).body(response);
            }
            
            String token = authHeader.substring(7);
            boolean isValid = superAdminService.validateToken(token);
            
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
     * Endpoint para exportar todos los datos
     * GET /api/superadmin/migration/export
     */
    @GetMapping("/migration/export")
    public ResponseEntity<Map<String, Object>> exportData(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            if (!validateAuth(authHeader, response)) {
                return ResponseEntity.status(401).body(response);
            }
            
            Map<String, Object> exportData = superAdminService.exportAllData();
            response.put("success", true);
            response.put("data", exportData);
            response.put("message", "Datos exportados exitosamente");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error al exportar datos: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Error al exportar datos: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * Endpoint para importar datos
     * POST /api/superadmin/migration/import
     */
    @PostMapping("/migration/import")
    public ResponseEntity<Map<String, Object>> importData(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody Map<String, Object> data) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            if (!validateAuth(authHeader, response)) {
                return ResponseEntity.status(401).body(response);
            }
            
            superAdminService.importData(data);
            response.put("success", true);
            response.put("message", "Datos importados exitosamente");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error al importar datos: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Error al importar datos: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * Endpoint para obtener logs de auditoría de seguridad
     * GET /api/superadmin/audit/security
     */
    @GetMapping("/audit/security")
    public ResponseEntity<Map<String, Object>> getSecurityAudit(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            if (!validateAuth(authHeader, response)) {
                return ResponseEntity.status(401).body(response);
            }
            
            Map<String, Object> auditLogs = superAdminService.getSecurityAuditLogs();
            response.put("success", true);
            response.put("data", auditLogs);
            response.put("message", "Logs de auditoría obtenidos exitosamente");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error al obtener logs de auditoría: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Error al obtener logs de auditoría: " + e.getMessage());
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
        if (!superAdminService.validateToken(token)) {
            response.put("success", false);
            response.put("message", "Token inválido o expirado");
            return false;
        }
        
        return true;
    }
}

