package com.elecciones.controller;

import com.elecciones.dto.CandidateDTO;
import com.elecciones.service.CandidateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/candidates")
@RequiredArgsConstructor
@Slf4j
public class CandidateController {
    
    private final CandidateService candidateService;
    
    /**
     * Obtiene todos los candidatos
     * GET /api/candidates
     */
    @GetMapping
    public ResponseEntity<List<CandidateDTO>> obtenerTodosLosCandidatos() {
        return ResponseEntity.ok(candidateService.obtenerTodosLosCandidatos());
    }
    
    /**
     * Obtiene candidatos por categoría
     * GET /api/candidates/category/{category}
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<List<CandidateDTO>> obtenerCandidatosPorCategoria(
            @PathVariable String category) {
        return ResponseEntity.ok(candidateService.obtenerCandidatosPorCategoria(category));
    }
}

