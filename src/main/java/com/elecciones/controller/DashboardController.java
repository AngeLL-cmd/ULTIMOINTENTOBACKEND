package com.elecciones.controller;

import com.elecciones.dto.CandidateDTO;
import com.elecciones.dto.DashboardStatsDTO;
import com.elecciones.service.CandidateService;
import com.elecciones.service.SupabaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Slf4j
public class DashboardController {
    
    private final SupabaseService supabaseService;
    private final CandidateService candidateService;
    
    /**
     * Obtiene las estadísticas del dashboard
     * GET /api/dashboard/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<DashboardStatsDTO> obtenerEstadisticas() {
        DashboardStatsDTO stats = new DashboardStatsDTO();
        
        // Total de votos
        long totalVotes = supabaseService.countVotes();
        stats.setTotalVotes(totalVotes);
        
        // Total de votantes
        long totalVoters = supabaseService.countVoters();
        stats.setTotalVoters(totalVoters);
        
        // Votantes únicos que han votado
        long uniqueVotersWhoVoted = supabaseService.countDistinctVoters();
        
        // Tasa de participación
        double participationRate = totalVoters > 0 ? 
            (double) uniqueVotersWhoVoted / totalVoters * 100 : 0;
        stats.setParticipationRate(participationRate);
        
        // Votos por categoría
        stats.setPresidentialVotes(supabaseService.countVotesByCategory("presidencial"));
        stats.setDistritalVotes(supabaseService.countVotesByCategory("distrital"));
        stats.setRegionalVotes(supabaseService.countVotesByCategory("regional"));
        
        // Candidatos
        List<CandidateDTO> candidates = candidateService.obtenerTodosLosCandidatos();
        stats.setCandidates(candidates);
        
        return ResponseEntity.ok(stats);
    }
}
