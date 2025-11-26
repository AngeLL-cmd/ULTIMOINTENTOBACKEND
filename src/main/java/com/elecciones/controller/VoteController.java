package com.elecciones.controller;

import com.elecciones.dto.VoteRequest;
import com.elecciones.service.VoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/votes")
@RequiredArgsConstructor
@Slf4j
public class VoteController {
    
    private final VoteService voteService;
    
    /**
     * Registra los votos de un votante
     * POST /api/votes
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> registrarVotos(@Valid @RequestBody VoteRequest voteRequest) {
        try {
            voteService.registrarVotos(voteRequest);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Votos registrados exitosamente");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error al registrar votos: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    /**
     * Obtiene las categorías ya votadas por un votante
     * GET /api/votes/voter/{dni}/categories
     */
    @GetMapping("/voter/{dni}/categories")
    public ResponseEntity<List<String>> obtenerCategoriasVotadas(@PathVariable String dni) {
        return ResponseEntity.ok(voteService.obtenerCategoriasVotadas(dni));
    }
    
    /**
     * Invalida los votos de un votante (marca candidate_id como NULL)
     * POST /api/votes/invalidate/{dni}
     */
    @PostMapping("/invalidate/{dni}")
    public ResponseEntity<Map<String, Object>> invalidarVotos(@PathVariable String dni) {
        try {
            int invalidatedCount = voteService.invalidarVotos(dni);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Votos invalidados exitosamente");
            response.put("invalidatedCount", invalidatedCount);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error al invalidar votos: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}

