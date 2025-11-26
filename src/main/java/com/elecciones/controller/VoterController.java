package com.elecciones.controller;

import com.elecciones.dto.VoterDTO;
import com.elecciones.service.VoterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/voters")
@RequiredArgsConstructor
@Slf4j
public class VoterController {
    
    private final VoterService voterService;
    
    /**
     * Verifica y registra un votante usando DNI
     * POST /api/voters/verify
     */
    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verificarVotante(@RequestBody Map<String, String> request) {
        try {
            String dni = request.get("dni");
            
            if (dni == null || dni.length() != 8) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("error", "El DNI debe tener 8 dígitos");
                return ResponseEntity.badRequest().body(error);
            }
            
            VoterDTO voter = voterService.verificarYRegistrarVotante(dni);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", voter);
            response.put("message", "Verificación exitosa");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error al verificar votante: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    /**
     * Obtiene un votante por DNI
     * GET /api/voters/{dni}
     */
    @GetMapping("/{dni}")
    public ResponseEntity<VoterDTO> obtenerVotante(@PathVariable String dni) {
        return voterService.obtenerVotantePorDni(dni)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Obtiene listado de votantes (solo para administradores)
     * GET /api/voters/list
     */
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> obtenerListadoVotantes(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(required = false) String dni) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Verificar autenticación de admin (simplificado, en producción usar mejor validación)
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                response.put("success", false);
                response.put("error", "No autorizado");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            
            java.util.List<VoterDTO> voters = voterService.obtenerListadoVotantes(dni);
            
            response.put("success", true);
            response.put("data", voters);
            response.put("count", voters.size());
            response.put("message", "Listado obtenido exitosamente");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error al obtener listado de votantes: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}

