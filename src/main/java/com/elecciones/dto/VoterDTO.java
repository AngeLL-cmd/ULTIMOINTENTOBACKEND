package com.elecciones.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VoterDTO {
    private String dni;
    private String fullName;
    private String address;
    private String district;
    private String province;
    private String department;
    private LocalDate birthDate;
    private String photoUrl;
    private Boolean hasVoted;
}

