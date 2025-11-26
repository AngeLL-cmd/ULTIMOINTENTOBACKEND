package com.elecciones.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Candidate {
    
    private String id;
    private String name;
    private String photoUrl;
    private String description;
    private String partyName;
    private String partyLogoUrl;
    private String partyDescription;
    private ElectoralCategory category;
    private String academicFormation;
    private String professionalExperience;
    private String campaignProposal;
    private Integer voteCount = 0;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    public enum ElectoralCategory {
        PRESIDENCIAL,
        DISTRITAL,
        REGIONAL
    }
}
