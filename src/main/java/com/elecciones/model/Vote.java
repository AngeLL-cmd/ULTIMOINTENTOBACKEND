package com.elecciones.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Vote {
    
    private String id;
    private String voterDni;
    private Candidate candidate;
    private ElectoralCategory category;
    private LocalDateTime votedAt = LocalDateTime.now();
    
    public enum ElectoralCategory {
        PRESIDENCIAL,
        DISTRITAL,
        REGIONAL
    }
}
