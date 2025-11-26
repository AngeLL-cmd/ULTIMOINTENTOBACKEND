package com.elecciones.service;

import com.elecciones.dto.VoteRequest;
import com.elecciones.model.Candidate;
import com.elecciones.model.Vote;
import com.elecciones.model.Voter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class VoteService {
    
    private final SupabaseService supabaseService;
    
    /**
     * Registra los votos de un votante
     */
    public void registrarVotos(VoteRequest voteRequest) {
        String voterDni = voteRequest.getVoterDni();
        
        // Validar que hay selecciones
        if (voteRequest.getSelections() == null || voteRequest.getSelections().isEmpty()) {
            throw new RuntimeException("No hay votos para registrar");
        }
        
        // Verificar que el votante existe
        Voter voter = supabaseService.findVoterByDni(voterDni)
            .orElseThrow(() -> new RuntimeException("Votante no encontrado con DNI: " + voterDni));
        
        // Obtener categorías ya votadas
        List<String> categoriasVotadas = obtenerCategoriasVotadas(voterDni);
        
        // Procesar cada selección
        for (VoteRequest.VoteSelection selection : voteRequest.getSelections()) {
            String categoria = selection.getCategory().toLowerCase();
            
            // Verificar que no haya votado ya en esta categoría
            if (categoriasVotadas.contains(categoria)) {
                throw new RuntimeException("Ya has votado en la categoría: " + categoria);
            }
            
            // Obtener candidato
            log.info("Buscando candidato con ID: {}", selection.getCandidateId());
            Candidate candidate = supabaseService.findCandidateById(selection.getCandidateId())
                .orElseThrow(() -> new RuntimeException("Candidato no encontrado con ID: " + selection.getCandidateId()));
            
            log.info("Candidato encontrado: {} - Categoría: {}", candidate.getName(), candidate.getCategory());
            
            // Verificar que la categoría coincide
            Candidate.ElectoralCategory category = convertToCategory(selection.getCategory());
            if (candidate.getCategory() != category) {
                throw new RuntimeException(
                    String.format("La categoría del candidato (%s) no coincide con la selección (%s)", 
                        candidate.getCategory(), category)
                );
            }
            
            // Crear voto
            Vote vote = new Vote();
            vote.setVoterDni(voterDni);
            vote.setCandidate(candidate);
            vote.setCategory(convertToVoteCategory(category));
            
            log.info("Guardando voto para DNI: {}, Candidato: {}, Categoría: {}", 
                voterDni, candidate.getName(), category);
            
            try {
                supabaseService.saveVote(vote);
                log.info("Voto guardado exitosamente");
            } catch (Exception e) {
                log.error("Error al guardar voto: {}", e.getMessage(), e);
                // Verificar si es un error de restricción UNIQUE
                if (e.getMessage() != null && e.getMessage().contains("UNIQUE") || 
                    e.getMessage() != null && e.getMessage().contains("duplicate")) {
                    throw new RuntimeException("Ya has votado en esta categoría: " + categoria);
                }
                throw new RuntimeException("Error al guardar voto: " + e.getMessage());
            }
            
            // NOTA: El vote_count se actualiza automáticamente por el trigger en Supabase
            // No es necesario actualizarlo manualmente
            log.debug("Voto guardado. El trigger de Supabase actualizará automáticamente el vote_count del candidato.");
            
            // Agregar a la lista de categorías votadas para evitar duplicados en la misma transacción
            categoriasVotadas.add(categoria);
        }
        
        // Marcar votante como que ya votó
        voter.setHasVoted(true);
        supabaseService.saveVoter(voter);
    }
    
    /**
     * Obtiene las categorías ya votadas por un votante
     */
    public List<String> obtenerCategoriasVotadas(String voterDni) {
        return supabaseService.findVotesByVoterDni(voterDni).stream()
            .map(vote -> convertCategoryToString(vote.getCategory()))
            .collect(Collectors.toList());
    }
    
    private Candidate.ElectoralCategory convertToCategory(String category) {
        return switch (category.toLowerCase()) {
            case "presidencial" -> Candidate.ElectoralCategory.PRESIDENCIAL;
            case "distrital" -> Candidate.ElectoralCategory.DISTRITAL;
            case "regional" -> Candidate.ElectoralCategory.REGIONAL;
            default -> throw new IllegalArgumentException("Categoría inválida: " + category);
        };
    }
    
    private Vote.ElectoralCategory convertToVoteCategory(Candidate.ElectoralCategory category) {
        return switch (category) {
            case PRESIDENCIAL -> Vote.ElectoralCategory.PRESIDENCIAL;
            case DISTRITAL -> Vote.ElectoralCategory.DISTRITAL;
            case REGIONAL -> Vote.ElectoralCategory.REGIONAL;
        };
    }
    
    private String convertCategoryToString(Vote.ElectoralCategory category) {
        return switch (category) {
            case PRESIDENCIAL -> "presidencial";
            case DISTRITAL -> "distrital";
            case REGIONAL -> "regional";
        };
    }
    
    /**
     * Invalida los votos de un votante (marca candidate_id como NULL)
     */
    public int invalidarVotos(String voterDni) {
        log.info("Invalidando votos para DNI: {}", voterDni);
        return supabaseService.invalidarVotos(voterDni);
    }
}
