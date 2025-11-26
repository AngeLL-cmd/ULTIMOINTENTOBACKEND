package com.elecciones.service;

import com.elecciones.dto.CandidateDTO;
import com.elecciones.model.Candidate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CandidateService {
    
    private final SupabaseService supabaseService;
    
    /**
     * Obtiene todos los candidatos ordenados por votos
     */
    public List<CandidateDTO> obtenerTodosLosCandidatos() {
        return supabaseService.findAllCandidates().stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }
    
    /**
     * Obtiene candidatos por categoría
     */
    public List<CandidateDTO> obtenerCandidatosPorCategoria(String category) {
        return supabaseService.findCandidatesByCategory(category).stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }
    
    private CandidateDTO convertToDTO(Candidate candidate) {
        CandidateDTO dto = new CandidateDTO();
        dto.setId(candidate.getId());
        dto.setName(candidate.getName());
        dto.setPhotoUrl(candidate.getPhotoUrl());
        dto.setDescription(candidate.getDescription());
        dto.setPartyName(candidate.getPartyName());
        dto.setPartyLogoUrl(candidate.getPartyLogoUrl());
        dto.setPartyDescription(candidate.getPartyDescription());
        dto.setCategory(candidate.getCategory().name().toLowerCase());
        dto.setAcademicFormation(candidate.getAcademicFormation());
        dto.setProfessionalExperience(candidate.getProfessionalExperience());
        dto.setCampaignProposal(candidate.getCampaignProposal());
        dto.setVoteCount(candidate.getVoteCount());
        return dto;
    }
}
