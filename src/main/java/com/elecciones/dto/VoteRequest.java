package com.elecciones.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VoteRequest {
    @NotBlank(message = "DNI es requerido")
    private String voterDni;
    
    @NotNull(message = "Selecciones son requeridas")
    private List<VoteSelection> selections;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VoteSelection {
        @NotBlank(message = "ID del candidato es requerido")
        private String candidateId;
        
        @NotBlank(message = "Nombre del candidato es requerido")
        private String candidateName;
        
        @NotBlank(message = "Categoría es requerida")
        private String category; // "presidencial", "distrital", "regional"
    }
}

