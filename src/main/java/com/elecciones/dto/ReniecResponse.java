package com.elecciones.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReniecResponse {
    private Boolean success;
    private ReniecData data;
    private String message;
    private String error;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReniecData {
        private String numero;
        private String nombres;
        private String apellidoPaterno;
        private String apellidoMaterno;
        private String codVerifica;
        private String fechaNacimiento;
        private String sexo;
        private String estadoCivil;
        private String direccion;
        private String ubigeo;
        private String distrito;
        private String provincia;
        private String departamento;
        private String foto;
    }
}

