package com.elecciones.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {
    
    private final SupabaseService supabaseService;
    
    @Value("${admin.email:admin@elecciones.pe}")
    private String adminEmail;
    
    @Value("${admin.password:admin123}")
    private String adminPassword;
    
    @Value("${jwt.secret}")
    private String jwtSecret;
    
    // Almacenar tokens activos (en producción usar Redis o base de datos)
    private final Map<String, Long> activeTokens = new HashMap<>();
    private static final long TOKEN_EXPIRATION = 24 * 60 * 60 * 1000; // 24 horas
    
    /**
     * Autentica un administrador y genera un token JWT simple
     */
    public String authenticate(String email, String password) {
        log.debug("Autenticando admin - Email: {}, Password configurada: {}", email, adminPassword != null ? "***" : "null");
        
        if (email == null || password == null) {
            log.warn("Email o password nulos");
            return null;
        }
        
        // Verificar credenciales
        if (email.equals(adminEmail) && password.equals(adminPassword)) {
            // Generar token simple
            String token = generateToken(email);
            activeTokens.put(token, System.currentTimeMillis() + TOKEN_EXPIRATION);
            log.info("Token generado para admin: {}", email);
            return token;
        }
        
        log.warn("Credenciales inválidas para: {}", email);
        return null;
    }
    
    /**
     * Valida un token
     */
    public boolean validateToken(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }
        
        // Verificar si el token está en la lista de tokens activos
        Long expiration = activeTokens.get(token);
        if (expiration == null) {
            log.debug("Token no encontrado en tokens activos");
            return false;
        }
        
        // Verificar si el token no ha expirado
        if (System.currentTimeMillis() > expiration) {
            log.debug("Token expirado");
            activeTokens.remove(token);
            return false;
        }
        
        return true;
    }
    
    /**
     * Genera un token JWT simple
     */
    private String generateToken(String email) {
        try {
            long expiration = System.currentTimeMillis() + TOKEN_EXPIRATION;
            
            // Crear payload
            String header = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"typ\":\"JWT\",\"alg\":\"HS256\"}".getBytes(StandardCharsets.UTF_8));
            
            String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(String.format("{\"email\":\"%s\",\"exp\":%d}", email, expiration)
                    .getBytes(StandardCharsets.UTF_8));
            
            // Crear signature
            String data = header + "." + payload;
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] signatureBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            String signature = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(signatureBytes);
            
            return data + "." + signature;
        } catch (Exception e) {
            log.error("Error al generar token: {}", e.getMessage(), e);
            // Fallback: token simple con hash
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] hash = digest.digest((email + System.currentTimeMillis() + jwtSecret).getBytes(StandardCharsets.UTF_8));
                return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
            } catch (Exception ex) {
                log.error("Error al generar token fallback: {}", ex.getMessage(), ex);
                return email + "_" + System.currentTimeMillis();
            }
        }
    }
    
    /**
     * Limpia tokens expirados (debería ejecutarse periódicamente)
     */
    public void cleanExpiredTokens() {
        long now = System.currentTimeMillis();
        activeTokens.entrySet().removeIf(entry -> entry.getValue() < now);
        log.debug("Tokens limpiados. Tokens activos: {}", activeTokens.size());
    }
    
    // ========== CLEANING METHODS ==========
    
    /**
     * Elimina registros con valores nulos
     */
    public int deleteNullValues() {
        return supabaseService.deleteNullValues();
    }
    
    /**
     * Elimina votos duplicados
     */
    public int deleteDuplicateVotes() {
        return supabaseService.deleteDuplicateVotes();
    }
    
    /**
     * Valida DNIs y retorna lista de inválidos
     */
    public List<String> validateDNIs() {
        return supabaseService.validateDNIs();
    }
    
    /**
     * Normaliza datos (nombres, direcciones, etc.)
     */
    public int normalizeData() {
        return supabaseService.normalizeData();
    }
    
    /**
     * Analiza tendencias electorales basadas en datos históricos
     */
    public Map<String, Object> analyzeTrends() {
        List<Map<String, Object>> votesByDate = supabaseService.getVotesByDate();
        
        Map<String, Object> result = new HashMap<>();
        
        if (votesByDate.isEmpty()) {
            result.put("hasData", false);
            result.put("message", "No hay datos suficientes para analizar tendencias");
            return result;
        }
        
        // Preparar datos para el gráfico (últimos días disponibles)
        List<Map<String, Object>> trendPredictions = new java.util.ArrayList<>();
        int daysToShow = Math.min(14, votesByDate.size());
        int startIndex = Math.max(0, votesByDate.size() - daysToShow);
        
        for (int i = startIndex; i < votesByDate.size(); i++) {
            Map<String, Object> voteData = votesByDate.get(i);
            Map<String, Object> point = new HashMap<>();
            String dateStr = (String) voteData.get("date");
            try {
                java.time.LocalDate date = java.time.LocalDate.parse(dateStr);
                point.put("date", date.format(java.time.format.DateTimeFormatter.ofPattern("MMM dd", java.util.Locale.forLanguageTag("es-PE"))));
            } catch (Exception e) {
                point.put("date", dateStr);
            }
            point.put("historical", voteData.get("count"));
            point.put("predicted", null);
            point.put("isFuture", false);
            trendPredictions.add(point);
        }
        
        // Calcular tendencia (comparar últimos días)
        int upward = 0, downward = 0, stable = 0;
        if (votesByDate.size() >= 2) {
            for (int i = 1; i < votesByDate.size(); i++) {
                int prev = (Integer) votesByDate.get(i-1).get("count");
                int curr = (Integer) votesByDate.get(i).get("count");
                if (curr > prev) upward++;
                else if (curr < prev) downward++;
                else stable++;
            }
            int total = upward + downward + stable;
            if (total > 0) {
                upward = (upward * 100) / total;
                downward = (downward * 100) / total;
                stable = (stable * 100) / total;
            }
        }
        
        result.put("hasData", true);
        result.put("trendPredictions", trendPredictions);
        result.put("trendAnalysis", Map.of(
            "upward", upward,
            "downward", downward,
            "stable", stable
        ));
        result.put("totalDataPoints", votesByDate.size());
        // Incluir datos completos para entrenamiento ML
        result.put("rawVotesByDate", votesByDate);
        
        return result;
    }
    
    /**
     * Detecta anomalías en los datos de votación
     */
    public Map<String, Object> detectAnomalies() {
        Map<String, Object> anomaliesData = supabaseService.detectAnomalies();
        
        Map<String, Object> result = new HashMap<>();
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> anomalies = (List<Map<String, Object>>) anomaliesData.get("anomalies");
        
        if (anomalies == null || anomalies.isEmpty()) {
            result.put("hasData", false);
            result.put("message", "No se detectaron anomalías en los datos");
            return result;
        }
        
        result.put("hasData", true);
        result.put("anomalies", anomalies);
        result.put("anomalyPatterns", anomaliesData.get("anomalyPatterns"));
        result.put("totalVotes", anomaliesData.get("totalVotes"));
        // Incluir datos completos de votos para entrenamiento ML
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rawVotes = (List<Map<String, Object>>) anomaliesData.get("rawVotes");
        if (rawVotes != null) {
            result.put("rawVotes", rawVotes);
        }
        
        return result;
    }
    
    /**
     * Analiza la participación por región y demografía
     */
    public Map<String, Object> analyzeParticipation() {
        Map<String, Object> participationData = supabaseService.getParticipationData();
        
        Map<String, Object> result = new HashMap<>();
        
        @SuppressWarnings("unchecked")
        Map<String, Map<String, Object>> participationByRegion = 
            (Map<String, Map<String, Object>>) participationData.get("participationByRegion");
        
        if (participationByRegion == null || participationByRegion.isEmpty()) {
            result.put("hasData", false);
            result.put("message", "No hay datos suficientes para analizar participación");
            return result;
        }
        
        result.put("hasData", true);
        result.put("participationByRegion", participationByRegion);
        result.put("participationByDemographic", participationData.get("participationByDemographic"));
        result.put("totalVoters", participationData.get("totalVoters"));
        result.put("totalVoted", participationData.get("totalVoted"));
        
        return result;
    }
}

