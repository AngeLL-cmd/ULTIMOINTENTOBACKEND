package com.elecciones.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsDTO {
    private Long totalVotes;
    private Long totalVoters;
    private Double participationRate;
    private Long presidentialVotes;
    private Long distritalVotes;
    private Long regionalVotes;
    private List<CandidateDTO> candidates;
}

