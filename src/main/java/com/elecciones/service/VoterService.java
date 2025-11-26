package com.elecciones.service;

import com.elecciones.dto.ReniecResponse;
import com.elecciones.dto.VoterDTO;
import com.elecciones.model.Voter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class VoterService {
    
    private final SupabaseService supabaseService;
    private final FastApiService fastApiService;
    
    /**
     * Verifica y registra/actualiza un votante
     * SOLO permite el acceso si la API de Factiliza valida el DNI exitosamente.
     * Si el votante existe en Supabase, lo devuelve (pero solo si fue validado previamente por la API).
     */
    public VoterDTO verificarYRegistrarVotante(String dni) {
        // Validar DNI
        if (dni == null || dni.length() != 8) {
            throw new RuntimeException("El DNI debe tener 8 dígitos");
        }
        
        // Verificar si el servicio de Factiliza está disponible
        if (fastApiService == null) {
            log.error("FastApiService es NULL - El servicio de validación de DNI no está disponible");
            throw new RuntimeException("El servicio de validación de DNI no está disponible. Por favor, contacte al administrador.");
        }
        
        // SIEMPRE validar el DNI con la API de Factiliza ANTES de permitir cualquier acceso
        // Esto asegura que solo DNIs válidos y registrados en RENIEC puedan acceder
        Map<String, Object> voterDataMap = null;
        ReniecResponse reniecResponse = null;
        
        try {
            log.info("=== Validando DNI con Factiliza para: {} ===", dni);
            reniecResponse = fastApiService.consultarDNI(dni);
            log.info("Respuesta de Factiliza - Success: {}, Error: {}", 
                reniecResponse.getSuccess(), reniecResponse.getError());
            
            if (reniecResponse.getSuccess() && reniecResponse.getData() != null) {
                // Validar que los datos sean válidos (tenga al menos nombre o apellido)
                ReniecResponse.ReniecData data = reniecResponse.getData();
                boolean tieneNombre = data.getNombres() != null && !data.getNombres().trim().isEmpty();
                boolean tieneApellido = data.getApellidoPaterno() != null && !data.getApellidoPaterno().trim().isEmpty();
                
                if (tieneNombre || tieneApellido) {
                    // Convertir datos de RENIEC
                    voterDataMap = fastApiService.convertirDatosReniec(dni, reniecResponse.getData());
                    log.info("✓ DNI validado exitosamente por Factiliza - Nombre completo: {}", voterDataMap.get("fullName"));
                    log.info("  - Dirección: {}", voterDataMap.get("address"));
                    log.info("  - Distrito: {}", voterDataMap.get("district"));
                    log.info("  - Provincia: {}", voterDataMap.get("province"));
                    log.info("  - Departamento: {}", voterDataMap.get("department"));
                } else {
                    log.warn("✗ DNI rechazado: La API no devolvió datos suficientes (sin nombre ni apellido) para DNI: {}", dni);
                    throw new RuntimeException("El DNI no es válido o no se encuentra registrado en RENIEC. Por favor, verifique que el DNI sea correcto.");
                }
            } else {
                log.warn("✗ DNI rechazado: La API de Factiliza no pudo validar el DNI: {}", dni);
                String errorMsg = reniecResponse.getError() != null ? reniecResponse.getError() : "DNI no encontrado en RENIEC";
                throw new RuntimeException("El DNI no es válido: " + errorMsg + ". Por favor, verifique que el DNI sea correcto.");
            }
        } catch (RuntimeException e) {
            // Re-lanzar excepciones de validación directamente
            log.error("✗ Acceso denegado para DNI {}: {}", dni, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("✗ Error al consultar RENIEC para DNI {}: {}", dni, e.getMessage(), e);
            throw new RuntimeException("Error al validar el DNI con RENIEC. Por favor, intente nuevamente o contacte al administrador si el problema persiste.");
        }
        
        // Si llegamos aquí, el DNI fue validado exitosamente por la API de Factiliza
        // Ahora buscar si el votante ya existe en Supabase
        Optional<Voter> existingVoter = supabaseService.findVoterByDni(dni);
        
        if (existingVoter.isPresent()) {
            // Si existe, actualizar con los datos más recientes de RENIEC y devolverlo
            log.info("Votante encontrado en Supabase: {} - Actualizando con datos de RENIEC", dni);
            Voter voter = existingVoter.get();
            
            // Actualizar datos con información más reciente de RENIEC
            if (voterDataMap != null) {
                voter.setFullName((String) voterDataMap.get("fullName"));
                voter.setAddress((String) voterDataMap.get("address"));
                voter.setDistrict((String) voterDataMap.get("district"));
                voter.setProvince((String) voterDataMap.get("province"));
                voter.setDepartment((String) voterDataMap.get("department"));
                
                try {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                    String birthDateStr = (String) voterDataMap.get("birthDate");
                    if (birthDateStr != null) {
                        voter.setBirthDate(LocalDate.parse(birthDateStr, formatter));
                    }
                } catch (Exception e) {
                    log.warn("Error al parsear fecha de nacimiento: {}", e.getMessage());
                }
                
                // Actualizar en Supabase
                try {
                    voter = supabaseService.saveVoter(voter);
                } catch (Exception e) {
                    log.warn("Error al actualizar votante, usando datos existentes: {}", e.getMessage());
                }
            }
            
            return convertToDTO(voter);
        }
        
        // Crear nuevo votante con datos de RENIEC (ya validados)
        Voter voter = new Voter();
        voter.setDni(dni);
        
        if (voterDataMap != null) {
            voter.setFullName((String) voterDataMap.get("fullName"));
            voter.setAddress((String) voterDataMap.get("address"));
            voter.setDistrict((String) voterDataMap.get("district"));
            voter.setProvince((String) voterDataMap.get("province"));
            voter.setDepartment((String) voterDataMap.get("department"));
            
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                String birthDateStr = (String) voterDataMap.get("birthDate");
                if (birthDateStr != null) {
                    voter.setBirthDate(LocalDate.parse(birthDateStr, formatter));
                } else {
                    voter.setBirthDate(LocalDate.of(1990, 1, 1));
                }
            } catch (Exception e) {
                log.warn("Error al parsear fecha de nacimiento: {}", e.getMessage());
                voter.setBirthDate(LocalDate.of(1990, 1, 1));
            }
        }
        
        voter.setHasVoted(false);
        
        // Guardar votante en Supabase
        try {
            voter = supabaseService.saveVoter(voter);
            log.info("Votante registrado exitosamente: {}", dni);
        } catch (RuntimeException e) {
            // Si hay un error, intentar buscar el votante una vez más
            log.warn("Error al guardar votante, intentando buscar existente: {}", e.getMessage());
            Optional<Voter> retryVoter = supabaseService.findVoterByDni(dni);
            if (retryVoter.isPresent()) {
                log.info("Votante encontrado después del error: {}", dni);
                return convertToDTO(retryVoter.get());
            }
            // Si no se encuentra, lanzar el error original
            throw e;
        }
        
        return convertToDTO(voter);
    }
    
    /**
     * Obtiene un votante por DNI
     */
    public Optional<VoterDTO> obtenerVotantePorDni(String dni) {
        return supabaseService.findVoterByDni(dni)
            .map(this::convertToDTO);
    }
    
    /**
     * Obtiene listado de votantes (solo para administradores)
     */
    public List<VoterDTO> obtenerListadoVotantes(String dniFilter) {
        List<Voter> voters = supabaseService.findAllVoters(dniFilter);
        return voters.stream()
            .map(this::convertToDTO)
            .collect(java.util.stream.Collectors.toList());
    }
    
    private VoterDTO convertToDTO(Voter voter) {
        VoterDTO dto = new VoterDTO();
        dto.setDni(voter.getDni());
        dto.setFullName(voter.getFullName());
        dto.setAddress(voter.getAddress());
        dto.setDistrict(voter.getDistrict());
        dto.setProvince(voter.getProvince());
        dto.setDepartment(voter.getDepartment());
        dto.setBirthDate(voter.getBirthDate());
        dto.setPhotoUrl(null); // photo_url no existe en la tabla voters
        dto.setHasVoted(voter.getHasVoted());
        return dto;
    }
}
