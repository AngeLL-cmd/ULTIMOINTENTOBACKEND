package com.elecciones.service;

import com.elecciones.dto.ReniecResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class FastApiService {
    
    private final RestTemplate restTemplate;
    
    @Value("${fastapi.base-url}")
    private String fastApiBaseUrl;
    
    @Value("${fastapi.reniec-endpoint}")
    private String reniecEndpoint;
    
    @Value("${fastapi.api-key:}")
    private String apiKey;
    
    /**
     * Consulta los datos de un DNI en RENIEC a través de la API de Factiliza
     */
    public ReniecResponse consultarDNI(String dni) {
        try {
            if (dni == null || dni.length() != 8) {
                return new ReniecResponse(false, null, null, "El DNI debe tener 8 dígitos");
            }
            
            // La API de Factiliza usa el DNI en la URL: /v1/dni/info/{dni}
            // Construir la URL correctamente
            String endpoint = reniecEndpoint.endsWith("/") ? reniecEndpoint + dni : reniecEndpoint + "/" + dni;
            String url = fastApiBaseUrl + endpoint;
            log.info("=== Consultando DNI en Factiliza ===");
            log.info("URL: {}", url);
            log.info("API Key configurada: {}", apiKey != null && !apiKey.isEmpty() ? "Sí (" + apiKey.substring(0, Math.min(20, apiKey.length())) + "...)" : "No");
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Accept", "application/json");
            headers.set("Content-Type", "application/json");
            
            // Agregar API key si está configurada
            if (apiKey != null && !apiKey.isEmpty()) {
                headers.set("Authorization", "Bearer " + apiKey);
                headers.set("X-API-Key", apiKey);
            }
            
            HttpEntity<?> request = new HttpEntity<>(headers);
            
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                request,
                new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                log.info("=== Respuesta de Factiliza recibida para DNI: {} ===", dni);
                log.info("Estructura de la respuesta: {}", responseBody.keySet());
                ObjectMapper jsonMapper = new ObjectMapper();
                log.info("Respuesta completa (JSON): {}", jsonMapper.writeValueAsString(responseBody));
                
                // La respuesta puede venir directamente o dentro de un campo "data"
                Map<String, Object> dataToProcess = responseBody;
                if (responseBody != null && responseBody.containsKey("data") && responseBody.get("data") instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> dataMap = (Map<String, Object>) responseBody.get("data");
                    dataToProcess = dataMap;
                    log.info("Datos encontrados dentro de campo 'data'");
                }
                
                // Convertir la respuesta de Factiliza al formato ReniecResponse
                ReniecResponse.ReniecData reniecData = convertFactilizaToReniecData(dataToProcess);
                
                if (reniecData != null) {
                    log.info("✓ Datos convertidos exitosamente:");
                    log.info("  - Nombres: {}", reniecData.getNombres());
                    log.info("  - Apellido Paterno: {}", reniecData.getApellidoPaterno());
                    log.info("  - Apellido Materno: {}", reniecData.getApellidoMaterno());
                    log.info("  - Dirección: {}", reniecData.getDireccion());
                    log.info("  - Distrito: {}", reniecData.getDistrito());
                    log.info("  - Provincia: {}", reniecData.getProvincia());
                    log.info("  - Departamento: {}", reniecData.getDepartamento());
                    return new ReniecResponse(true, reniecData, null, null);
                } else {
                    log.error("✗ No se pudieron convertir los datos de Factiliza.");
                    log.error("Respuesta original: {}", responseBody);
                    return new ReniecResponse(false, null, null, "No se pudieron obtener los datos del DNI");
                }
            }
            log.warn("Respuesta no exitosa de Factiliza. Status: {}", response.getStatusCode());
            return new ReniecResponse(false, null, null, "Error al consultar el DNI en RENIEC");
            
        } catch (HttpClientErrorException e) {
            log.error("Error HTTP al consultar DNI: {} - Status: {} - Body: {}", 
                e.getMessage(), e.getStatusCode(), e.getResponseBodyAsString());
            return new ReniecResponse(false, null, null, "DNI no encontrado en RENIEC: " + e.getStatusCode());
        } catch (Exception e) {
            log.error("Error al consultar DNI: {}", e.getMessage(), e);
            return new ReniecResponse(false, null, null, "Error al conectar con el servicio de RENIEC: " + e.getMessage());
        }
    }
    
    /**
     * Convierte la respuesta de Factiliza al formato ReniecData
     */
    private ReniecResponse.ReniecData convertFactilizaToReniecData(Map<String, Object> factilizaData) {
        try {
            ReniecResponse.ReniecData reniecData = new ReniecResponse.ReniecData();
            
            // Función auxiliar para obtener valores con múltiples posibles nombres
            java.util.function.Function<String[], String> getValue = keys -> {
                for (String key : keys) {
                    Object value = factilizaData.get(key);
                    if (value != null && !value.toString().trim().isEmpty()) {
                        return value.toString().trim();
                    }
                }
                return null;
            };
            
            // Log de la estructura completa para debugging
            log.debug("Estructura completa de respuesta Factiliza: {}", factilizaData.keySet());
            
            // Mapear campos de Factiliza a ReniecData (intentando múltiples nombres posibles)
            // Nombres - puede venir como "nombres", "nombre", "first_name", etc.
            reniecData.setNombres(getValue.apply(new String[]{
                "nombres", "nombre", "nombres_completos", "primer_nombre", "nombresCompletos",
                "first_name", "firstName", "nombresCompletos", "nombre_completo"
            }));
            
            // Apellidos
            reniecData.setApellidoPaterno(getValue.apply(new String[]{
                "apellido_paterno", "apellidoPaterno", "ap_paterno", "primer_apellido",
                "first_last_name", "firstLastName", "apellido_p", "paterno"
            }));
            
            reniecData.setApellidoMaterno(getValue.apply(new String[]{
                "apellido_materno", "apellidoMaterno", "ap_materno", "segundo_apellido",
                "second_last_name", "secondLastName", "apellido_m", "materno"
            }));
            
            // Fecha de nacimiento
            reniecData.setFechaNacimiento(getValue.apply(new String[]{
                "fecha_nacimiento", "fechaNacimiento", "fecha_de_nacimiento", "fechaNac", "nacimiento"
            }));
            
            // Dirección
            reniecData.setDireccion(getValue.apply(new String[]{
                "direccion", "direccion_completa", "domicilio", "direccionCompleta", "domicilio_completo",
                "address", "residence_address", "direccion_actual", "domicilio_actual"
            }));
            
            // Ubicación
            reniecData.setDistrito(getValue.apply(new String[]{
                "distrito", "ubigeo_distrito", "distrito_nombre", "distritoNombre", "distrito_descripcion",
                "district", "district_name", "districtName", "distrito_nombre"
            }));
            
            reniecData.setProvincia(getValue.apply(new String[]{
                "provincia", "ubigeo_provincia", "provincia_nombre", "provinciaNombre", "provincia_descripcion",
                "province", "province_name", "provinceName", "provincia_nombre"
            }));
            
            reniecData.setDepartamento(getValue.apply(new String[]{
                "departamento", "ubigeo_departamento", "departamento_nombre", "departamentoNombre", "departamento_descripcion",
                "department", "department_name", "departmentName", "region", "departamento_nombre"
            }));
            
            // Foto
            reniecData.setFoto(getValue.apply(new String[]{
                "foto", "foto_url", "imagen", "fotoUrl", "foto_dni"
            }));
            
            // Log de los datos convertidos
            log.info("Datos convertidos de Factiliza:");
            log.info("  - Nombres: {}", reniecData.getNombres());
            log.info("  - Apellido Paterno: {}", reniecData.getApellidoPaterno());
            log.info("  - Apellido Materno: {}", reniecData.getApellidoMaterno());
            log.info("  - Dirección: {}", reniecData.getDireccion());
            log.info("  - Distrito: {}", reniecData.getDistrito());
            log.info("  - Provincia: {}", reniecData.getProvincia());
            log.info("  - Departamento: {}", reniecData.getDepartamento());
            
            // Validar que al menos tengamos nombres o apellidos
            if (reniecData.getNombres() == null && reniecData.getApellidoPaterno() == null) {
                log.warn("No se encontraron nombres ni apellidos en la respuesta de Factiliza");
                // Intentar buscar en campos alternativos o en un campo "nombre_completo"
                String nombreCompleto = getValue.apply(new String[]{
                    "nombre_completo", "nombreCompleto", "full_name", "fullName", "nombres_apellidos"
                });
                if (nombreCompleto != null) {
                    // Intentar dividir el nombre completo
                    String[] partes = nombreCompleto.split("\\s+");
                    if (partes.length >= 2) {
                        reniecData.setNombres(partes[0]);
                        reniecData.setApellidoPaterno(partes[1]);
                        if (partes.length >= 3) {
                            reniecData.setApellidoMaterno(partes[2]);
                        }
                    }
                }
            }
            
            return reniecData;
        } catch (Exception e) {
            log.error("Error al convertir datos de Factiliza: {}", e.getMessage(), e);
            log.error("Datos recibidos de Factiliza: {}", factilizaData);
            return null;
        }
    }
    
    /**
     * Convierte los datos de RENIEC al formato de Voter
     */
    public Map<String, Object> convertirDatosReniec(String dni, ReniecResponse.ReniecData reniecData) {
        Map<String, Object> voterData = new HashMap<>();
        
        // Construir nombre completo
        String fullName = "";
        if (reniecData.getNombres() != null && reniecData.getApellidoPaterno() != null) {
            fullName = String.format("%s %s %s", 
                reniecData.getNombres(),
                reniecData.getApellidoPaterno(),
                reniecData.getApellidoMaterno() != null ? reniecData.getApellidoMaterno() : ""
            ).trim();
        } else {
            fullName = reniecData.getNombres() != null ? reniecData.getNombres() : "Nombre no disponible";
        }
        
        // Convertir fecha de nacimiento
        String birthDate = "1990-01-01";
        if (reniecData.getFechaNacimiento() != null) {
            try {
                String dateStr = reniecData.getFechaNacimiento();
                if (dateStr.contains("/")) {
                    String[] parts = dateStr.split("/");
                    if (parts.length == 3) {
                        birthDate = String.format("%s-%s-%s", parts[2], parts[1], parts[0]);
                    }
                } else {
                    birthDate = dateStr;
                }
            } catch (Exception e) {
                log.warn("Error al parsear fecha de nacimiento: {}", e.getMessage());
            }
        }
        
        voterData.put("dni", dni);
        voterData.put("fullName", fullName);
        voterData.put("address", reniecData.getDireccion() != null ? reniecData.getDireccion() : "No especificada");
        voterData.put("district", reniecData.getDistrito() != null ? reniecData.getDistrito() : "No especificado");
        voterData.put("province", reniecData.getProvincia() != null ? reniecData.getProvincia() : "No especificado");
        voterData.put("department", reniecData.getDepartamento() != null ? reniecData.getDepartamento() : "No especificado");
        voterData.put("birthDate", birthDate);
        voterData.put("photoUrl", reniecData.getFoto());
        
        return voterData;
    }
}

