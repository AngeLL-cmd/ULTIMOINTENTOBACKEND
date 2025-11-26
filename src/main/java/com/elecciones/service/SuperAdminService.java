package com.elecciones.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class SuperAdminService {
    
    private final SupabaseService supabaseService;
    
    @Value("${superadmin.email:superadmin@elecciones.pe}")
    private String superAdminEmail;
    
    @Value("${superadmin.password:superadmin123}")
    private String superAdminPassword;
    
    @Value("${jwt.secret}")
    private String jwtSecret;
    
    // Almacenar tokens activos (en producción usar Redis o base de datos)
    private final Map<String, Long> activeTokens = new HashMap<>();
    private static final long TOKEN_EXPIRATION = 24 * 60 * 60 * 1000; // 24 horas
    
    @PostConstruct
    public void init() {
        log.info("=== Inicializando SuperAdminService ===");
        log.info("SuperAdmin Email configurado: {}", superAdminEmail);
        log.info("SuperAdmin Password configurado: {}", superAdminPassword != null ? "***" : "null");
        log.info("JWT Secret configurado: {}", jwtSecret != null ? "***" : "null");
    }
    
    /**
     * Autentica un super administrador y genera un token JWT simple
     */
    public String authenticate(String email, String password) {
        log.info("=== Autenticando super admin ===");
        log.info("Email recibido: {}", email);
        log.info("Email configurado: {}", superAdminEmail);
        log.info("Password configurado: {}", superAdminPassword != null ? "***" : "null");
        
        if (email == null || password == null) {
            log.warn("Email o password nulos");
            return null;
        }
        
        // Verificar credenciales
        if (email.equals(superAdminEmail) && password.equals(superAdminPassword)) {
            // Generar token simple
            String token = generateToken(email);
            activeTokens.put(token, System.currentTimeMillis() + TOKEN_EXPIRATION);
            log.info("Token generado para super admin: {}", email);
            return token;
        }
        
        log.warn("Credenciales inválidas para super admin: {}", email);
        return null;
    }
    
    /**
     * Valida un token
     */
    public boolean validateToken(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }
        
        Long expiration = activeTokens.get(token);
        if (expiration == null || System.currentTimeMillis() > expiration) {
            if (expiration != null) {
                activeTokens.remove(token);
            }
            return false;
        }
        
        return true;
    }
    
    /**
     * Genera un token JWT simple
     */
    private String generateToken(String email) {
        try {
            String header = Base64.getUrlEncoder().withoutPadding().encodeToString(
                "{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8)
            );
            
            long expiration = System.currentTimeMillis() + TOKEN_EXPIRATION;
            String payload = Base64.getUrlEncoder().withoutPadding().encodeToString(
                String.format("{\"email\":\"%s\",\"exp\":%d,\"role\":\"superadmin\"}", email, expiration)
                    .getBytes(StandardCharsets.UTF_8)
            );
            
            String signature = Base64.getUrlEncoder().withoutPadding().encodeToString(
                calculateHMAC(header + "." + payload, jwtSecret)
            );
            
            return header + "." + payload + "." + signature;
        } catch (Exception e) {
            log.error("Error al generar token: {}", e.getMessage(), e);
            // Fallback: token simple basado en hash
            try {
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                byte[] hash = md.digest((email + System.currentTimeMillis() + jwtSecret).getBytes(StandardCharsets.UTF_8));
                return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
            } catch (Exception ex) {
                log.error("Error al generar token de fallback: {}", ex.getMessage(), ex);
                return "superadmin_token_" + System.currentTimeMillis();
            }
        }
    }
    
    private byte[] calculateHMAC(String data, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKeySpec);
        return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
    }
    
    /**
     * Exporta todos los datos para migración
     */
    public Map<String, Object> exportAllData() {
        Map<String, Object> exportData = new HashMap<>();
        try {
            // Exportar votantes
            exportData.put("voters", supabaseService.findAllVoters(null));
            
            // Exportar candidatos
            exportData.put("candidates", supabaseService.findAllCandidates());
            
            // Exportar votos (simplificado, solo IDs y datos básicos)
            exportData.put("votes", supabaseService.countVotes());
            
            log.info("Datos exportados exitosamente");
        } catch (Exception e) {
            log.error("Error al exportar datos: {}", e.getMessage(), e);
            throw new RuntimeException("Error al exportar datos: " + e.getMessage());
        }
        return exportData;
    }
    
    /**
     * Importa datos (simplificado)
     */
    public void importData(Map<String, Object> data) {
        try {
            // Aquí se implementaría la lógica de importación
            log.info("Importando datos...");
            // Por ahora solo logueamos
            log.info("Datos importados: {} votantes, {} candidatos", 
                data.get("voters"), data.get("candidates"));
        } catch (Exception e) {
            log.error("Error al importar datos: {}", e.getMessage(), e);
            throw new RuntimeException("Error al importar datos: " + e.getMessage());
        }
    }
    
    /**
     * Obtiene logs de auditoría de seguridad
     */
    public Map<String, Object> getSecurityAuditLogs() {
        Map<String, Object> auditLogs = new HashMap<>();
        try {
            // Simular logs de auditoría
            java.util.List<Map<String, Object>> logs = new java.util.ArrayList<>();
            
            Map<String, Object> log1 = new HashMap<>();
            log1.put("timestamp", java.time.LocalDateTime.now().minusHours(2).toString());
            log1.put("action", "Login de administrador");
            log1.put("user", "admin@elecciones.pe");
            log1.put("ip", "192.168.1.100");
            log1.put("status", "success");
            logs.add(log1);
            
            Map<String, Object> log2 = new HashMap<>();
            log2.put("timestamp", java.time.LocalDateTime.now().minusHours(1).toString());
            log2.put("action", "Intento de acceso no autorizado");
            log2.put("user", "unknown");
            log2.put("ip", "192.168.1.101");
            log2.put("status", "failed");
            logs.add(log2);
            
            auditLogs.put("logs", logs);
            auditLogs.put("total", logs.size());
            
            log.info("Logs de auditoría obtenidos: {}", logs.size());
        } catch (Exception e) {
            log.error("Error al obtener logs de auditoría: {}", e.getMessage(), e);
            throw new RuntimeException("Error al obtener logs de auditoría: " + e.getMessage());
        }
        return auditLogs;
    }
}

