package com.elecciones.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Voter {
    
    private String dni;
    private String fullName;
    private String address;
    private String district;
    private String province;
    private String department;
    private LocalDate birthDate;
    private String photoUrl;
    private Boolean hasVoted = false;
    private LocalDateTime votedAt;
    private LocalDateTime createdAt = LocalDateTime.now();
}
