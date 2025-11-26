package com.elecciones.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CandidateDTO {
    private String id;
    private String name;
    private String photoUrl;
    private String description;
    private String partyName;
    private String partyLogoUrl;
    private String partyDescription;
    private String category;
    private String academicFormation;
    private String professionalExperience;
    private String campaignProposal;
    private Integer voteCount;
}

