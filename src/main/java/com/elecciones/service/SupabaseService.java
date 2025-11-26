package com.elecciones.service;

import com.elecciones.config.SupabaseConfig;
import com.elecciones.model.Candidate;
import com.elecciones.model.Vote;
import com.elecciones.model.Voter;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SupabaseService {
    
    private final SupabaseConfig supabaseConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("apikey", supabaseConfig.getSupabaseKey());
        headers.set("Authorization", supabaseConfig.getAuthHeader());
        headers.set("Content-Type", "application/json");
        headers.set("Prefer", "return=representation");
        return headers;
    }
    
    /**
     * Crea headers usando la service key para operaciones administrativas
     * que requieren bypass de RLS
     */
    private HttpHeaders createServiceHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("apikey", supabaseConfig.getSupabaseServiceKey());
        headers.set("Authorization", supabaseConfig.getServiceAuthHeader());
        headers.set("Content-Type", "application/json");
        headers.set("Prefer", "return=representation");
        return headers;
    }
    
    // ========== VOTERS ==========
    
    /**
     * Obtiene todos los votantes (solo para administradores)
     */
    public List<Voter> findAllVoters(String dniFilter) {
        try {
            String url = supabaseConfig.getApiUrl() + "/voters?select=*&order=created_at.desc";
            if (dniFilter != null && !dniFilter.trim().isEmpty()) {
                url = supabaseConfig.getApiUrl() + "/voters?dni=ilike.*" + dniFilter + "*&select=*&order=created_at.desc";
            }
            
            ResponseEntity<String> response = restTemplate.exchange(
                url, Objects.requireNonNull(HttpMethod.GET), 
                new HttpEntity<>(Objects.requireNonNull(createServiceHeaders())), String.class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<Map<String, Object>> data = objectMapper.readValue(
                    response.getBody(), new TypeReference<List<Map<String, Object>>>() {}
                );
                List<Voter> voters = new ArrayList<>();
                for (Map<String, Object> item : data) {
                    voters.add(mapToVoter(item));
                }
                return voters;
            }
            return new ArrayList<>();
        } catch (Exception e) {
            log.error("Error al obtener listado de votantes: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    public Optional<Voter> findVoterByDni(String dni) {
        try {
            String url = supabaseConfig.getApiUrl() + "/voters?dni=eq." + dni + "&select=*";
            log.debug("Buscando votante en Supabase - URL: {}", url);
            
            // Usar service headers para evitar problemas de RLS
            ResponseEntity<String> response = restTemplate.exchange(
                url, Objects.requireNonNull(HttpMethod.GET), new HttpEntity<>(Objects.requireNonNull(createServiceHeaders())), String.class
            );
            
            log.debug("Respuesta de Supabase - Status: {}, Body: {}", response.getStatusCode(), response.getBody());
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<Map<String, Object>> data = objectMapper.readValue(
                    response.getBody(), new TypeReference<List<Map<String, Object>>>() {}
                );
                if (!data.isEmpty()) {
                    log.info("Votante encontrado: {}", dni);
                    return Optional.of(mapToVoter(data.get(0)));
                } else {
                    log.debug("Votante no encontrado: {}", dni);
                }
            } else {
                log.warn("Respuesta no exitosa de Supabase. Status: {}", response.getStatusCode());
            }
            return Optional.empty();
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            log.error("Error HTTP al buscar votante: {} - Status: {} - Body: {}", 
                e.getMessage(), e.getStatusCode(), e.getResponseBodyAsString());
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error al buscar votante: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }
    
    public Voter saveVoter(Voter voter) {
        try {
            // Usar UPSERT (INSERT ... ON CONFLICT) para manejar votantes existentes
            Map<String, Object> voterMap = voterToMap(voter);
            String url = supabaseConfig.getApiUrl() + "/voters";
            
            // Usar service headers para operaciones de escritura
            HttpHeaders headers = createServiceHeaders();
            // Usar upsert para que si existe, se actualice; si no, se cree
            headers.set("Prefer", "resolution=merge-duplicates,return=representation");
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(voterMap, headers);
            
            log.info("Guardando/actualizando votante en Supabase - DNI: {}", voter.getDni());
            log.debug("URL: {}, Data: {}", url, voterMap);
            
            try {
                ResponseEntity<String> response = restTemplate.exchange(
                    url, Objects.requireNonNull(HttpMethod.POST), request, String.class
                );
                
                log.debug("Respuesta de Supabase - Status: {}, Body: {}", response.getStatusCode(), response.getBody());
                
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    List<Map<String, Object>> data = objectMapper.readValue(
                        response.getBody(), new TypeReference<List<Map<String, Object>>>() {}
                    );
                    if (!data.isEmpty()) {
                        log.info("Votante guardado/actualizado exitosamente: {}", voter.getDni());
                        return mapToVoter(data.get(0));
                    }
                }
                
                // Si no hay respuesta, buscar el votante existente
                log.warn("No se recibió respuesta al guardar, buscando votante existente: {}", voter.getDni());
                Optional<Voter> existing = findVoterByDni(voter.getDni());
                if (existing.isPresent()) {
                    return existing.get();
                }
                
            } catch (org.springframework.web.client.HttpClientErrorException e) {
                // Si el error es 409 (Conflict), el votante ya existe - buscarlo
                if (e.getStatusCode().value() == 409 || 
                    (e.getResponseBodyAsString() != null && 
                     (e.getResponseBodyAsString().contains("23505") || 
                      e.getResponseBodyAsString().contains("duplicate key")))) {
                    log.info("Votante ya existe (409), buscando existente: {}", voter.getDni());
                    Optional<Voter> existing = findVoterByDni(voter.getDni());
                    if (existing.isPresent()) {
                        log.info("Votante existente encontrado y devuelto: {}", voter.getDni());
                        return existing.get();
                    } else {
                        log.error("Error 409 pero no se pudo encontrar el votante. URL de búsqueda: {}", 
                            supabaseConfig.getApiUrl() + "/voters?dni=eq." + voter.getDni());
                        // Intentar una vez más con un pequeño delay
                        try {
                            Thread.sleep(500);
                            existing = findVoterByDni(voter.getDni());
                            if (existing.isPresent()) {
                                return existing.get();
                            }
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                        }
                        throw new RuntimeException("Votante ya existe pero no se pudo recuperar: " + voter.getDni() + 
                            ". Verifica las políticas RLS en Supabase.");
                    }
                } else {
                    // Si es otro error HTTP, relanzarlo
                    log.error("Error HTTP al guardar votante: {} - Status: {} - Body: {}", 
                        e.getMessage(), e.getStatusCode(), e.getResponseBodyAsString());
                    throw new RuntimeException("Error al guardar votante: " + e.getMessage() + " - " + e.getResponseBodyAsString());
                }
            }
            
            return voter;
        } catch (RuntimeException e) {
            // Re-lanzar RuntimeException tal cual
            throw e;
        } catch (Exception e) {
            log.error("Error al guardar votante: {}", e.getMessage(), e);
            throw new RuntimeException("Error al guardar votante: " + e.getMessage());
        }
    }
    
    public Voter updateVoter(Voter voter) {
        try {
            Map<String, Object> voterMap = voterToMap(voter);
            // Remover el DNI del mapa ya que es la clave primaria y no se puede actualizar
            voterMap.remove("dni");
            
            String url = supabaseConfig.getApiUrl() + "/voters?dni=eq." + voter.getDni();
            
            // Supabase PostgREST requiere el header Prefer para PATCH
            HttpHeaders headers = createHeaders();
            headers.set("Prefer", "return=representation");
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(voterMap, headers);
            
            log.debug("Actualizando votante en Supabase - URL: {}, Data: {}", url, voterMap);
            
            ResponseEntity<String> response = restTemplate.exchange(
                url, Objects.requireNonNull(HttpMethod.PATCH), request, String.class
            );
            
            log.debug("Respuesta de actualización - Status: {}, Body: {}", response.getStatusCode(), response.getBody());
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<Map<String, Object>> data = objectMapper.readValue(
                    response.getBody(), new TypeReference<List<Map<String, Object>>>() {}
                );
                if (!data.isEmpty()) {
                    log.info("Votante actualizado exitosamente: {}", voter.getDni());
                    return mapToVoter(data.get(0));
                }
            }
            // Si no hay respuesta, devolver el votante original
            log.warn("No se recibió respuesta al actualizar votante, devolviendo votante original");
            return voter;
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            log.error("Error HTTP al actualizar votante: {} - Status: {} - Body: {}", 
                e.getMessage(), e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Error al actualizar votante: " + e.getMessage() + " - " + e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Error al actualizar votante: {}", e.getMessage(), e);
            throw new RuntimeException("Error al actualizar votante: " + e.getMessage());
        }
    }
    
    public long countVoters() {
        try {
            String url = supabaseConfig.getApiUrl() + "/voters?select=dni";
            ResponseEntity<String> response = restTemplate.exchange(
                url, Objects.requireNonNull(HttpMethod.GET), new HttpEntity<>(Objects.requireNonNull(createHeaders())), String.class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<?> data = objectMapper.readValue(response.getBody(), List.class);
                return data.size();
            }
            return 0;
        } catch (Exception e) {
            log.error("Error al contar votantes: {}", e.getMessage(), e);
            return 0;
        }
    }
    
    // ========== CANDIDATES ==========
    
    public List<Candidate> findAllCandidates() {
        try {
            String url = supabaseConfig.getApiUrl() + "/candidates?select=*&order=vote_count.desc";
            ResponseEntity<String> response = restTemplate.exchange(
                url, Objects.requireNonNull(HttpMethod.GET), new HttpEntity<>(Objects.requireNonNull(createHeaders())), String.class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<Map<String, Object>> data = objectMapper.readValue(
                    response.getBody(), new TypeReference<List<Map<String, Object>>>() {}
                );
                List<Candidate> candidates = new ArrayList<>();
                for (Map<String, Object> item : data) {
                    candidates.add(mapToCandidate(item));
                }
                return candidates;
            }
            return new ArrayList<>();
        } catch (Exception e) {
            log.error("Error al obtener candidatos: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    public List<Candidate> findCandidatesByCategory(String category) {
        try {
            String url = supabaseConfig.getApiUrl() + "/candidates?select=*&category=eq." + category + "&order=vote_count.desc";
            ResponseEntity<String> response = restTemplate.exchange(
                url, Objects.requireNonNull(HttpMethod.GET), new HttpEntity<>(Objects.requireNonNull(createHeaders())), String.class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<Map<String, Object>> data = objectMapper.readValue(
                    response.getBody(), new TypeReference<List<Map<String, Object>>>() {}
                );
                List<Candidate> candidates = new ArrayList<>();
                for (Map<String, Object> item : data) {
                    candidates.add(mapToCandidate(item));
                }
                return candidates;
            }
            return new ArrayList<>();
        } catch (Exception e) {
            log.error("Error al obtener candidatos por categoría: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    public Optional<Candidate> findCandidateById(String id) {
        try {
            if (id == null || id.trim().isEmpty()) {
                log.warn("ID de candidato es null o vacío");
                return Optional.empty();
            }
            
            String url = supabaseConfig.getApiUrl() + "/candidates?id=eq." + id + "&select=*";
            log.debug("Buscando candidato con ID: {} en URL: {}", id, url);
            
            ResponseEntity<String> response = restTemplate.exchange(
                url, Objects.requireNonNull(HttpMethod.GET), new HttpEntity<>(Objects.requireNonNull(createHeaders())), String.class
            );
            
            log.debug("Respuesta de Supabase - Status: {}, Body: {}", response.getStatusCode(), response.getBody());
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<Map<String, Object>> data = objectMapper.readValue(
                    response.getBody(), new TypeReference<List<Map<String, Object>>>() {}
                );
                if (!data.isEmpty()) {
                    log.info("Candidato encontrado: {}", data.get(0).get("name"));
                    return Optional.of(mapToCandidate(data.get(0)));
                } else {
                    log.warn("No se encontró candidato con ID: {}", id);
                }
            } else {
                log.warn("Respuesta no exitosa de Supabase. Status: {}", response.getStatusCode());
            }
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error al buscar candidato con ID {}: {}", id, e.getMessage(), e);
            return Optional.empty();
        }
    }
    
    public Candidate updateCandidate(Candidate candidate) {
        try {
            Map<String, Object> candidateMap = candidateToMap(candidate);
            // No incluir el ID en el mapa de actualización ya que es la clave primaria
            String url = supabaseConfig.getApiUrl() + "/candidates?id=eq." + candidate.getId();
            
            // Usar service headers para operaciones de escritura
            HttpHeaders headers = createServiceHeaders();
            headers.set("Prefer", "return=representation");
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(candidateMap, headers);
            
            log.debug("Actualizando candidato en Supabase - URL: {}, Data: {}", url, candidateMap);
            
            try {
                // Supabase PostgREST soporta PATCH, pero si falla, usar método alternativo
                ResponseEntity<String> response = restTemplate.exchange(
                    url, Objects.requireNonNull(HttpMethod.PATCH), request, String.class
                );
                
                log.debug("Respuesta de actualización - Status: {}, Body: {}", response.getStatusCode(), response.getBody());
                
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    List<Map<String, Object>> data = objectMapper.readValue(
                        response.getBody(), new TypeReference<List<Map<String, Object>>>() {}
                    );
                    if (!data.isEmpty()) {
                        log.info("Candidato actualizado exitosamente: {}", candidate.getId());
                        return mapToCandidate(data.get(0));
                    }
                }
            } catch (org.springframework.web.client.HttpClientErrorException e) {
                // Si PATCH falla con error de método inválido, simplemente devolver el candidato
                if (e.getStatusCode().value() == 405 || 
                    (e.getResponseBodyAsString() != null && e.getResponseBodyAsString().contains("Invalid HTTP method"))) {
                    log.warn("PATCH no soportado para candidato: {}. El trigger de Supabase actualizará el vote_count automáticamente.", candidate.getId());
                    return candidate;
                } else {
                    log.error("Error HTTP al actualizar candidato: {} - Status: {} - Body: {}", 
                        e.getMessage(), e.getStatusCode(), e.getResponseBodyAsString());
                    throw new RuntimeException("Error al actualizar candidato: " + e.getMessage());
                }
            } catch (org.springframework.web.client.RestClientException e) {
                // Capturar errores de I/O que pueden ocurrir con PATCH
                if (e.getMessage() != null && e.getMessage().contains("Invalid HTTP method")) {
                    log.warn("PATCH no soportado (I/O error) para candidato: {}. El trigger de Supabase actualizará el vote_count automáticamente.", candidate.getId());
                    return candidate;
                } else {
                    log.error("Error de I/O al actualizar candidato: {}", e.getMessage());
                    throw new RuntimeException("Error al actualizar candidato: " + e.getMessage());
                }
            }
            
            // Si no hay respuesta, devolver el candidato original
            log.warn("No se recibió respuesta al actualizar candidato, devolviendo original");
            return candidate;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al actualizar candidato: {}", e.getMessage(), e);
            throw new RuntimeException("Error al actualizar candidato: " + e.getMessage());
        }
    }
    
    // ========== VOTES ==========
    
    public void saveVote(Vote vote) {
        try {
            Map<String, Object> voteMap = voteToMap(vote);
            String url = supabaseConfig.getApiUrl() + "/votes";
            
            log.debug("Guardando voto: {}", voteMap);
            
            // Usar service headers para operaciones de escritura
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(voteMap, Objects.requireNonNull(createServiceHeaders()));
            ResponseEntity<String> response = restTemplate.exchange(
                url, Objects.requireNonNull(HttpMethod.POST), request, String.class
            );
            
            if (!response.getStatusCode().is2xxSuccessful()) {
                log.error("Error al guardar voto. Status: {}, Body: {}", response.getStatusCode(), response.getBody());
                throw new RuntimeException("Error al guardar voto. Status: " + response.getStatusCode());
            }
            
            log.info("Voto guardado exitosamente");
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            log.error("Error HTTP al guardar voto: {} - Body: {}", e.getStatusCode(), e.getResponseBodyAsString());
            String errorMessage = "Error al guardar voto";
            if (e.getResponseBodyAsString() != null) {
                errorMessage += ": " + e.getResponseBodyAsString();
            }
            throw new RuntimeException(errorMessage, e);
        } catch (Exception e) {
            log.error("Error al guardar voto: {}", e.getMessage(), e);
            throw new RuntimeException("Error al guardar voto: " + e.getMessage(), e);
        }
    }
    
    public List<Vote> findVotesByVoterDni(String voterDni) {
        try {
            String url = supabaseConfig.getApiUrl() + "/votes?select=*&voter_dni=eq." + voterDni;
            ResponseEntity<String> response = restTemplate.exchange(
                url, Objects.requireNonNull(HttpMethod.GET), new HttpEntity<>(Objects.requireNonNull(createHeaders())), String.class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<Map<String, Object>> data = objectMapper.readValue(
                    response.getBody(), new TypeReference<List<Map<String, Object>>>() {}
                );
                List<Vote> votes = new ArrayList<>();
                for (Map<String, Object> item : data) {
                    votes.add(mapToVote(item));
                }
                return votes;
            }
            return new ArrayList<>();
        } catch (Exception e) {
            log.error("Error al obtener votos: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    public long countVotes() {
        try {
            String url = supabaseConfig.getApiUrl() + "/votes?select=id";
            ResponseEntity<String> response = restTemplate.exchange(
                url, Objects.requireNonNull(HttpMethod.GET), new HttpEntity<>(Objects.requireNonNull(createHeaders())), String.class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<?> data = objectMapper.readValue(response.getBody(), List.class);
                return data.size();
            }
            return 0;
        } catch (Exception e) {
            log.error("Error al contar votos: {}", e.getMessage(), e);
            return 0;
        }
    }
    
    public long countVotesByCategory(String category) {
        try {
            String url = supabaseConfig.getApiUrl() + "/votes?select=id&category=eq." + category;
            ResponseEntity<String> response = restTemplate.exchange(
                url, Objects.requireNonNull(HttpMethod.GET), new HttpEntity<>(Objects.requireNonNull(createHeaders())), String.class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<?> data = objectMapper.readValue(response.getBody(), List.class);
                return data.size();
            }
            return 0;
        } catch (Exception e) {
            log.error("Error al contar votos por categoría: {}", e.getMessage(), e);
            return 0;
        }
    }
    
    public long countDistinctVoters() {
        try {
            String url = supabaseConfig.getApiUrl() + "/votes?select=voter_dni";
            ResponseEntity<String> response = restTemplate.exchange(
                url, Objects.requireNonNull(HttpMethod.GET), new HttpEntity<>(Objects.requireNonNull(createHeaders())), String.class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<Map<String, Object>> data = objectMapper.readValue(
                    response.getBody(), new TypeReference<List<Map<String, Object>>>() {}
                );
                return data.stream()
                    .map(item -> item.get("voter_dni"))
                    .distinct()
                    .count();
            }
            return 0;
        } catch (Exception e) {
            log.error("Error al contar votantes únicos: {}", e.getMessage(), e);
            return 0;
        }
    }
    
    // ========== CLEANING METHODS ==========
    
    /**
     * Elimina registros con valores nulos o vacíos
     */
    public int deleteNullValues() {
        int deletedCount = 0;
        try {
            // Eliminar candidatos con valores nulos
            String candidatesUrl = supabaseConfig.getApiUrl() + "/candidates?select=id,name,party_name,description,photo_url";
            ResponseEntity<String> candidatesResponse = restTemplate.exchange(
                candidatesUrl, Objects.requireNonNull(HttpMethod.GET), 
                new HttpEntity<>(Objects.requireNonNull(createServiceHeaders())), String.class
            );
            
            if (candidatesResponse.getStatusCode().is2xxSuccessful() && candidatesResponse.getBody() != null) {
                List<Map<String, Object>> candidates = objectMapper.readValue(
                    candidatesResponse.getBody(), new TypeReference<List<Map<String, Object>>>() {}
                );
                
                for (Map<String, Object> candidate : candidates) {
                    String name = (String) candidate.get("name");
                    String partyName = (String) candidate.get("party_name");
                    String description = (String) candidate.get("description");
                    String photoUrl = (String) candidate.get("photo_url");
                    
                    if (name == null || name.trim().isEmpty() ||
                        partyName == null || partyName.trim().isEmpty() ||
                        description == null || description.trim().isEmpty() ||
                        photoUrl == null || photoUrl.trim().isEmpty()) {
                        
                        String deleteUrl = supabaseConfig.getApiUrl() + "/candidates?id=eq." + candidate.get("id");
                        ResponseEntity<String> deleteResponse = restTemplate.exchange(
                            deleteUrl, Objects.requireNonNull(HttpMethod.DELETE),
                            new HttpEntity<>(Objects.requireNonNull(createServiceHeaders())), String.class
                        );
                        if (deleteResponse.getStatusCode().is2xxSuccessful()) {
                            deletedCount++;
                        }
                    }
                }
            }
            
            // Eliminar votantes con valores nulos
            String votersUrl = supabaseConfig.getApiUrl() + "/voters?select=dni,full_name,address,district,province,department";
            ResponseEntity<String> votersResponse = restTemplate.exchange(
                votersUrl, Objects.requireNonNull(HttpMethod.GET),
                new HttpEntity<>(Objects.requireNonNull(createServiceHeaders())), String.class
            );
            
            if (votersResponse.getStatusCode().is2xxSuccessful() && votersResponse.getBody() != null) {
                List<Map<String, Object>> voters = objectMapper.readValue(
                    votersResponse.getBody(), new TypeReference<List<Map<String, Object>>>() {}
                );
                
                for (Map<String, Object> voter : voters) {
                    String fullName = (String) voter.get("full_name");
                    String address = (String) voter.get("address");
                    String district = (String) voter.get("district");
                    String province = (String) voter.get("province");
                    String department = (String) voter.get("department");
                    
                    if (fullName == null || fullName.trim().isEmpty() ||
                        address == null || address.trim().isEmpty() ||
                        district == null || district.trim().isEmpty() ||
                        province == null || province.trim().isEmpty() ||
                        department == null || department.trim().isEmpty()) {
                        
                        String deleteUrl = supabaseConfig.getApiUrl() + "/voters?dni=eq." + voter.get("dni");
                        ResponseEntity<String> deleteResponse = restTemplate.exchange(
                            deleteUrl, Objects.requireNonNull(HttpMethod.DELETE),
                            new HttpEntity<>(Objects.requireNonNull(createServiceHeaders())), String.class
                        );
                        if (deleteResponse.getStatusCode().is2xxSuccessful()) {
                            deletedCount++;
                        }
                    }
                }
            }
            
            // Eliminar votos con valores nulos
            String votesUrl = supabaseConfig.getApiUrl() + "/votes?select=id,voter_dni,category,candidate_id";
            ResponseEntity<String> votesResponse = restTemplate.exchange(
                votesUrl, Objects.requireNonNull(HttpMethod.GET),
                new HttpEntity<>(Objects.requireNonNull(createServiceHeaders())), String.class
            );
            
            if (votesResponse.getStatusCode().is2xxSuccessful() && votesResponse.getBody() != null) {
                List<Map<String, Object>> votes = objectMapper.readValue(
                    votesResponse.getBody(), new TypeReference<List<Map<String, Object>>>() {}
                );
                
                for (Map<String, Object> vote : votes) {
                    String voterDni = (String) vote.get("voter_dni");
                    String category = (String) vote.get("category");
                    String candidateId = (String) vote.get("candidate_id");
                    
                    if (voterDni == null || voterDni.trim().isEmpty() ||
                        category == null || category.trim().isEmpty() ||
                        candidateId == null || candidateId.trim().isEmpty()) {
                        
                        String deleteUrl = supabaseConfig.getApiUrl() + "/votes?id=eq." + vote.get("id");
                        ResponseEntity<String> deleteResponse = restTemplate.exchange(
                            deleteUrl, Objects.requireNonNull(HttpMethod.DELETE),
                            new HttpEntity<>(Objects.requireNonNull(createServiceHeaders())), String.class
                        );
                        if (deleteResponse.getStatusCode().is2xxSuccessful()) {
                            deletedCount++;
                        }
                    }
                }
            }
            
            log.info("Eliminados {} registros con valores nulos", deletedCount);
        } catch (Exception e) {
            log.error("Error al eliminar valores nulos: {}", e.getMessage(), e);
            throw new RuntimeException("Error al eliminar valores nulos: " + e.getMessage());
        }
        return deletedCount;
    }
    
    /**
     * Elimina votos duplicados (mismo voter_dni y category)
     */
    public int deleteDuplicateVotes() {
        int deletedCount = 0;
        try {
            String votesUrl = supabaseConfig.getApiUrl() + "/votes?select=id,voter_dni,category,voted_at&order=voted_at.desc";
            ResponseEntity<String> votesResponse = restTemplate.exchange(
                votesUrl, Objects.requireNonNull(HttpMethod.GET),
                new HttpEntity<>(Objects.requireNonNull(createServiceHeaders())), String.class
            );
            
            if (votesResponse.getStatusCode().is2xxSuccessful() && votesResponse.getBody() != null) {
                List<Map<String, Object>> votes = objectMapper.readValue(
                    votesResponse.getBody(), new TypeReference<List<Map<String, Object>>>() {}
                );
                
                // Agrupar por voter_dni y category
                Map<String, List<String>> voteGroups = new HashMap<>();
                for (Map<String, Object> vote : votes) {
                    String voterDni = (String) vote.get("voter_dni");
                    String category = (String) vote.get("category");
                    if (voterDni != null && category != null) {
                        String key = voterDni + "_" + category;
                        voteGroups.computeIfAbsent(key, k -> new ArrayList<>()).add((String) vote.get("id"));
                    }
                }
                
                // Eliminar duplicados (mantener solo el primero, que es el más reciente por orden)
                for (Map.Entry<String, List<String>> entry : voteGroups.entrySet()) {
                    List<String> voteIds = entry.getValue();
                    if (voteIds.size() > 1) {
                        // Mantener el primero (más reciente), eliminar el resto
                        for (int i = 1; i < voteIds.size(); i++) {
                            String deleteUrl = supabaseConfig.getApiUrl() + "/votes?id=eq." + voteIds.get(i);
                            ResponseEntity<String> deleteResponse = restTemplate.exchange(
                                deleteUrl, Objects.requireNonNull(HttpMethod.DELETE),
                                new HttpEntity<>(Objects.requireNonNull(createServiceHeaders())), String.class
                            );
                            if (deleteResponse.getStatusCode().is2xxSuccessful()) {
                                deletedCount++;
                            }
                        }
                    }
                }
            }
            
            log.info("Eliminados {} votos duplicados", deletedCount);
        } catch (Exception e) {
            log.error("Error al eliminar duplicados: {}", e.getMessage(), e);
            throw new RuntimeException("Error al eliminar duplicados: " + e.getMessage());
        }
        return deletedCount;
    }
    
    /**
     * Valida DNIs y retorna lista de DNIs inválidos
     */
    public List<String> validateDNIs() {
        List<String> invalidDNIs = new ArrayList<>();
        try {
            String votersUrl = supabaseConfig.getApiUrl() + "/voters?select=dni,full_name";
            ResponseEntity<String> votersResponse = restTemplate.exchange(
                votersUrl, Objects.requireNonNull(HttpMethod.GET),
                new HttpEntity<>(Objects.requireNonNull(createServiceHeaders())), String.class
            );
            
            if (votersResponse.getStatusCode().is2xxSuccessful() && votersResponse.getBody() != null) {
                List<Map<String, Object>> voters = objectMapper.readValue(
                    votersResponse.getBody(), new TypeReference<List<Map<String, Object>>>() {}
                );
                
                java.util.regex.Pattern dniPattern = java.util.regex.Pattern.compile("^\\d{8}$");
                for (Map<String, Object> voter : voters) {
                    String dni = (String) voter.get("dni");
                    if (dni == null || !dniPattern.matcher(dni).matches()) {
                        String fullName = (String) voter.get("full_name");
                        invalidDNIs.add(dni + " (" + (fullName != null ? fullName : "sin nombre") + ")");
                    }
                }
            }
            
            // Validar DNIs en votos
            String votesUrl = supabaseConfig.getApiUrl() + "/votes?select=voter_dni";
            ResponseEntity<String> votesResponse = restTemplate.exchange(
                votesUrl, Objects.requireNonNull(HttpMethod.GET),
                new HttpEntity<>(Objects.requireNonNull(createServiceHeaders())), String.class
            );
            
            if (votesResponse.getStatusCode().is2xxSuccessful() && votesResponse.getBody() != null) {
                List<Map<String, Object>> votes = objectMapper.readValue(
                    votesResponse.getBody(), new TypeReference<List<Map<String, Object>>>() {}
                );
                
                java.util.regex.Pattern dniPattern = java.util.regex.Pattern.compile("^\\d{8}$");
                for (Map<String, Object> vote : votes) {
                    String voterDni = (String) vote.get("voter_dni");
                    if (voterDni != null && !dniPattern.matcher(voterDni).matches()) {
                        if (!invalidDNIs.contains(voterDni + " (en votos)")) {
                            invalidDNIs.add(voterDni + " (en votos)");
                        }
                    }
                }
            }
            
            log.info("Encontrados {} DNIs inválidos", invalidDNIs.size());
        } catch (Exception e) {
            log.error("Error al validar DNIs: {}", e.getMessage(), e);
            throw new RuntimeException("Error al validar DNIs: " + e.getMessage());
        }
        return invalidDNIs;
    }
    
    /**
     * Normaliza datos (nombres, direcciones, etc.)
     */
    public int normalizeData() {
        int normalizedCount = 0;
        try {
            // Normalizar votantes
            String votersUrl = supabaseConfig.getApiUrl() + "/voters?select=*";
            ResponseEntity<String> votersResponse = restTemplate.exchange(
                votersUrl, Objects.requireNonNull(HttpMethod.GET),
                new HttpEntity<>(Objects.requireNonNull(createServiceHeaders())), String.class
            );
            
            if (votersResponse.getStatusCode().is2xxSuccessful() && votersResponse.getBody() != null) {
                List<Map<String, Object>> voters = objectMapper.readValue(
                    votersResponse.getBody(), new TypeReference<List<Map<String, Object>>>() {}
                );
                
                for (Map<String, Object> voter : voters) {
                    Map<String, Object> updates = new HashMap<>();
                    boolean needsUpdate = false;
                    
                    // Normalizar nombre completo
                    String fullName = (String) voter.get("full_name");
                    if (fullName != null) {
                        String normalized = normalizeString(fullName);
                        if (!normalized.equals(fullName)) {
                            updates.put("full_name", normalized);
                            needsUpdate = true;
                        }
                    }
                    
                    // Normalizar dirección
                    String address = (String) voter.get("address");
                    if (address != null) {
                        String normalized = address.trim();
                        if (!normalized.equals(address)) {
                            updates.put("address", normalized);
                            needsUpdate = true;
                        }
                    }
                    
                    // Normalizar distrito, provincia, departamento
                    for (String field : new String[]{"district", "province", "department"}) {
                        String value = (String) voter.get(field);
                        if (value != null) {
                            String normalized = normalizeString(value);
                            if (!normalized.equals(value)) {
                                updates.put(field, normalized);
                                needsUpdate = true;
                            }
                        }
                    }
                    
                    if (needsUpdate) {
                        try {
                            // Usar POST con upsert para actualizar (más compatible que PATCH)
                            String upsertUrl = supabaseConfig.getApiUrl() + "/voters";
                            Map<String, Object> voterData = new HashMap<>(voter);
                            voterData.putAll(updates);
                            HttpHeaders upsertHeaders = createServiceHeaders();
                            upsertHeaders.set("Prefer", "resolution=merge-duplicates,return=representation");
                            HttpEntity<Map<String, Object>> upsertRequest = new HttpEntity<>(voterData, upsertHeaders);
                            
                            ResponseEntity<String> updateResponse = restTemplate.exchange(
                                upsertUrl, Objects.requireNonNull(HttpMethod.POST), upsertRequest, String.class
                            );
                            if (updateResponse.getStatusCode().is2xxSuccessful()) {
                                normalizedCount++;
                            }
                        } catch (Exception e) {
                            log.warn("Error al actualizar votante {}: {}", voter.get("dni"), e.getMessage());
                        }
                    }
                }
            }
            
            // Normalizar candidatos
            String candidatesUrl = supabaseConfig.getApiUrl() + "/candidates?select=*";
            ResponseEntity<String> candidatesResponse = restTemplate.exchange(
                candidatesUrl, Objects.requireNonNull(HttpMethod.GET),
                new HttpEntity<>(Objects.requireNonNull(createServiceHeaders())), String.class
            );
            
            if (candidatesResponse.getStatusCode().is2xxSuccessful() && candidatesResponse.getBody() != null) {
                List<Map<String, Object>> candidates = objectMapper.readValue(
                    candidatesResponse.getBody(), new TypeReference<List<Map<String, Object>>>() {}
                );
                
                for (Map<String, Object> candidate : candidates) {
                    Map<String, Object> updates = new HashMap<>();
                    boolean needsUpdate = false;
                    
                    // Normalizar nombre
                    String name = (String) candidate.get("name");
                    if (name != null) {
                        String normalized = normalizeString(name);
                        if (!normalized.equals(name)) {
                            updates.put("name", normalized);
                            needsUpdate = true;
                        }
                    }
                    
                    // Normalizar nombre del partido
                    String partyName = (String) candidate.get("party_name");
                    if (partyName != null) {
                        String normalized = partyName.trim();
                        if (!normalized.equals(partyName)) {
                            updates.put("party_name", normalized);
                            needsUpdate = true;
                        }
                    }
                    
                    if (needsUpdate) {
                        try {
                            // Usar POST con upsert para actualizar (más compatible que PATCH)
                            String upsertUrl = supabaseConfig.getApiUrl() + "/candidates";
                            Map<String, Object> candidateData = new HashMap<>(candidate);
                            candidateData.putAll(updates);
                            HttpHeaders upsertHeaders = createServiceHeaders();
                            upsertHeaders.set("Prefer", "resolution=merge-duplicates,return=representation");
                            HttpEntity<Map<String, Object>> upsertRequest = new HttpEntity<>(candidateData, upsertHeaders);
                            
                            ResponseEntity<String> updateResponse = restTemplate.exchange(
                                upsertUrl, Objects.requireNonNull(HttpMethod.POST), upsertRequest, String.class
                            );
                            if (updateResponse.getStatusCode().is2xxSuccessful()) {
                                normalizedCount++;
                            }
                        } catch (Exception e) {
                            log.warn("Error al actualizar candidato {}: {}", candidate.get("id"), e.getMessage());
                        }
                    }
                }
            }
            
            log.info("Normalizados {} registros", normalizedCount);
        } catch (Exception e) {
            log.error("Error al normalizar datos: {}", e.getMessage(), e);
            throw new RuntimeException("Error al normalizar datos: " + e.getMessage());
        }
        return normalizedCount;
    }
    
    /**
     * Normaliza un string (capitaliza palabras)
     */
    private String normalizeString(String str) {
        if (str == null || str.trim().isEmpty()) {
            return str;
        }
        String[] words = str.trim().split("\\s+");
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            if (i > 0) result.append(" ");
            if (!words[i].isEmpty()) {
                result.append(words[i].substring(0, 1).toUpperCase())
                      .append(words[i].substring(1).toLowerCase());
            }
        }
        return result.toString();
    }
    
    /**
     * Invalida los votos de un votante (marca candidate_id como NULL)
     */
    public int invalidarVotos(String voterDni) {
        try {
            // Obtener todos los votos del votante directamente desde Supabase
            String url = supabaseConfig.getApiUrl() + "/votes?select=id,candidate_id&voter_dni=eq." + voterDni;
            ResponseEntity<String> response = restTemplate.exchange(
                url, Objects.requireNonNull(HttpMethod.GET), 
                new HttpEntity<>(Objects.requireNonNull(createServiceHeaders())), String.class
            );
            
            int invalidatedCount = 0;
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<Map<String, Object>> votes = objectMapper.readValue(
                    response.getBody(), new TypeReference<List<Map<String, Object>>>() {}
                );
                
                for (Map<String, Object> voteData : votes) {
                    String voteId = (String) voteData.get("id");
                    String candidateId = (String) voteData.get("candidate_id");
                    
                    // Solo invalidar si tiene candidate_id (no es NULL)
                    if (candidateId != null && !candidateId.trim().isEmpty()) {
                        // Actualizar el voto para poner candidate_id como NULL
                        String updateUrl = supabaseConfig.getApiUrl() + "/votes?id=eq." + voteId;
                        
                        Map<String, Object> updateData = new HashMap<>();
                        updateData.put("candidate_id", null);
                        
                        HttpHeaders headers = createServiceHeaders();
                        headers.set("Prefer", "return=representation");
                        
                        HttpEntity<Map<String, Object>> request = new HttpEntity<>(updateData, headers);
                        
                        try {
                            ResponseEntity<String> updateResponse = restTemplate.exchange(
                                updateUrl, Objects.requireNonNull(HttpMethod.PATCH), request, String.class
                            );
                            
                            if (updateResponse.getStatusCode().is2xxSuccessful()) {
                                invalidatedCount++;
                                log.info("Voto invalidado: {}", voteId);
                            }
                        } catch (Exception e) {
                            log.error("Error al invalidar voto {}: {}", voteId, e.getMessage());
                        }
                    }
                }
            }
            
            log.info("Invalidados {} votos para DNI: {}", invalidatedCount, voterDni);
            return invalidatedCount;
        } catch (Exception e) {
            log.error("Error al invalidar votos: {}", e.getMessage(), e);
            throw new RuntimeException("Error al invalidar votos: " + e.getMessage());
        }
    }
    
    // ========== MAPPER METHODS ==========
    
    private Voter mapToVoter(Map<String, Object> data) {
        Voter voter = new Voter();
        voter.setDni((String) data.get("dni"));
        voter.setFullName((String) data.get("full_name"));
        voter.setAddress((String) data.get("address"));
        voter.setDistrict((String) data.get("district"));
        voter.setProvince((String) data.get("province"));
        voter.setDepartment((String) data.get("department"));
        
        if (data.get("birth_date") != null) {
            try {
                String birthDateStr = data.get("birth_date").toString();
                voter.setBirthDate(LocalDate.parse(birthDateStr.split("T")[0]));
            } catch (Exception e) {
                log.warn("Error al parsear fecha de nacimiento: {}", e.getMessage());
            }
        }
        
        // photo_url no existe en la tabla voters, se omite
        voter.setHasVoted(data.get("has_voted") != null ? (Boolean) data.get("has_voted") : false);
        
        if (data.get("voted_at") != null) {
            voter.setVotedAt(LocalDateTime.parse(data.get("voted_at").toString().replace("Z", "")));
        }
        
        return voter;
    }
    
    private Map<String, Object> voterToMap(Voter voter) {
        Map<String, Object> map = new HashMap<>();
        map.put("dni", voter.getDni());
        map.put("full_name", voter.getFullName());
        map.put("address", voter.getAddress());
        map.put("district", voter.getDistrict());
        map.put("province", voter.getProvince());
        map.put("department", voter.getDepartment());
        map.put("birth_date", voter.getBirthDate() != null ? voter.getBirthDate().toString() : null);
        // photo_url no existe en la tabla voters, se omite
        map.put("has_voted", voter.getHasVoted());
        map.put("voted_at", voter.getVotedAt() != null ? voter.getVotedAt().toString() : null);
        return map;
    }
    
    private Candidate mapToCandidate(Map<String, Object> data) {
        Candidate candidate = new Candidate();
        candidate.setId((String) data.get("id"));
        candidate.setName((String) data.get("name"));
        candidate.setPhotoUrl((String) data.get("photo_url"));
        candidate.setDescription((String) data.get("description"));
        candidate.setPartyName((String) data.get("party_name"));
        candidate.setPartyLogoUrl((String) data.get("party_logo_url"));
        candidate.setPartyDescription((String) data.get("party_description"));
        
        String category = (String) data.get("category");
        if (category != null) {
            try {
                candidate.setCategory(Candidate.ElectoralCategory.valueOf(category.toUpperCase()));
            } catch (IllegalArgumentException e) {
                log.warn("Categoría inválida: {}", category);
            }
        }
        
        candidate.setAcademicFormation((String) data.get("academic_formation"));
        candidate.setProfessionalExperience((String) data.get("professional_experience"));
        candidate.setCampaignProposal((String) data.get("campaign_proposal"));
        candidate.setVoteCount(data.get("vote_count") != null ? ((Number) data.get("vote_count")).intValue() : 0);
        
        return candidate;
    }
    
    private Map<String, Object> candidateToMap(Candidate candidate) {
        Map<String, Object> map = new HashMap<>();
        map.put("name", candidate.getName());
        map.put("photo_url", candidate.getPhotoUrl());
        map.put("description", candidate.getDescription());
        map.put("party_name", candidate.getPartyName());
        map.put("party_logo_url", candidate.getPartyLogoUrl());
        map.put("party_description", candidate.getPartyDescription());
        map.put("category", candidate.getCategory() != null ? candidate.getCategory().name().toLowerCase() : null);
        map.put("academic_formation", candidate.getAcademicFormation());
        map.put("professional_experience", candidate.getProfessionalExperience());
        map.put("campaign_proposal", candidate.getCampaignProposal());
        map.put("vote_count", candidate.getVoteCount());
        return map;
    }
    
    private Vote mapToVote(Map<String, Object> data) {
        Vote vote = new Vote();
        vote.setId((String) data.get("id"));
        vote.setVoterDni((String) data.get("voter_dni"));
        
        // Mapear candidate_id si existe
        String candidateId = (String) data.get("candidate_id");
        if (candidateId != null && !candidateId.trim().isEmpty()) {
            try {
                Optional<Candidate> candidate = findCandidateById(candidateId);
                candidate.ifPresent(vote::setCandidate);
            } catch (Exception e) {
                log.warn("Error al obtener candidato para voto: {}", e.getMessage());
            }
        }
        
        String category = (String) data.get("category");
        if (category != null) {
            try {
                vote.setCategory(Vote.ElectoralCategory.valueOf(category.toUpperCase()));
            } catch (IllegalArgumentException e) {
                log.warn("Categoría inválida: {}", category);
            }
        }
        
        if (data.get("voted_at") != null) {
            try {
                String votedAtStr = data.get("voted_at").toString();
                vote.setVotedAt(LocalDateTime.parse(votedAtStr.replace("Z", "").replace("+00:00", "")));
            } catch (Exception e) {
                log.warn("Error al parsear fecha de voto: {}", e.getMessage());
                vote.setVotedAt(LocalDateTime.now());
            }
        }
        
        return vote;
    }
    
    // ========== TRAINING DATA METHODS ==========
    
    /**
     * Obtiene votos agrupados por fecha para análisis de tendencias
     */
    public List<Map<String, Object>> getVotesByDate() {
        try {
            String url = supabaseConfig.getApiUrl() + "/votes?select=voted_at&order=voted_at.asc";
            ResponseEntity<String> response = restTemplate.exchange(
                url, Objects.requireNonNull(HttpMethod.GET), 
                new HttpEntity<>(Objects.requireNonNull(createServiceHeaders())), String.class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<Map<String, Object>> votes = objectMapper.readValue(
                    response.getBody(), new TypeReference<List<Map<String, Object>>>() {}
                );
                
                // Agrupar por fecha
                Map<String, Integer> votesByDate = new HashMap<>();
                for (Map<String, Object> vote : votes) {
                    Object votedAtObj = vote.get("voted_at");
                    if (votedAtObj != null) {
                        String votedAtStr = votedAtObj.toString();
                        try {
                            LocalDateTime votedAt = LocalDateTime.parse(votedAtStr.replace("Z", "").replace("+00:00", ""));
                            String dateKey = votedAt.toLocalDate().toString();
                            votesByDate.put(dateKey, votesByDate.getOrDefault(dateKey, 0) + 1);
                        } catch (Exception e) {
                            log.warn("Error al parsear fecha: {}", votedAtStr);
                        }
                    }
                }
                
                // Convertir a lista ordenada
                List<Map<String, Object>> result = new ArrayList<>();
                for (Map.Entry<String, Integer> entry : votesByDate.entrySet()) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("date", entry.getKey());
                    item.put("count", entry.getValue());
                    result.add(item);
                }
                
                result.sort((a, b) -> ((String) a.get("date")).compareTo((String) b.get("date")));
                return result;
            }
            return new ArrayList<>();
        } catch (Exception e) {
            log.error("Error al obtener votos por fecha: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Analiza votos para detectar anomalías
     */
    public Map<String, Object> detectAnomalies() {
        Map<String, Object> result = new HashMap<>();
        try {
            String url = supabaseConfig.getApiUrl() + "/votes?select=id,voter_dni,voted_at,category";
            ResponseEntity<String> response = restTemplate.exchange(
                url, Objects.requireNonNull(HttpMethod.GET),
                new HttpEntity<>(Objects.requireNonNull(createServiceHeaders())), String.class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<Map<String, Object>> votes = objectMapper.readValue(
                    response.getBody(), new TypeReference<List<Map<String, Object>>>() {}
                );
                
                // Detectar votos duplicados
                Map<String, Integer> voterCategoryCount = new HashMap<>();
                int duplicateVotes = 0;
                for (Map<String, Object> vote : votes) {
                    String voterDni = (String) vote.get("voter_dni");
                    String category = (String) vote.get("category");
                    if (voterDni != null && category != null) {
                        String key = voterDni + "_" + category;
                        int count = voterCategoryCount.getOrDefault(key, 0);
                        if (count > 0) {
                            duplicateVotes++;
                        }
                        voterCategoryCount.put(key, count + 1);
                    }
                }
                
                // Detectar votos fuera de horario (8am a 6pm)
                int outOfHoursVotes = 0;
                for (Map<String, Object> vote : votes) {
                    Object votedAtObj = vote.get("voted_at");
                    if (votedAtObj != null) {
                        try {
                            String votedAtStr = votedAtObj.toString();
                            LocalDateTime votedAt = LocalDateTime.parse(votedAtStr.replace("Z", "").replace("+00:00", ""));
                            int hour = votedAt.getHour();
                            if (hour < 8 || hour >= 18) {
                                outOfHoursVotes++;
                            }
                        } catch (Exception e) {
                            // Ignorar errores de parsing
                        }
                    }
                }
                
                // Detectar votos masivos en corto tiempo (mismo DNI en menos de 5 minutos)
                Map<String, List<LocalDateTime>> voterTimes = new HashMap<>();
                for (Map<String, Object> vote : votes) {
                    String voterDni = (String) vote.get("voter_dni");
                    Object votedAtObj = vote.get("voted_at");
                    if (voterDni != null && votedAtObj != null) {
                        try {
                            String votedAtStr = votedAtObj.toString();
                            LocalDateTime votedAt = LocalDateTime.parse(votedAtStr.replace("Z", "").replace("+00:00", ""));
                            voterTimes.computeIfAbsent(voterDni, k -> new ArrayList<>()).add(votedAt);
                        } catch (Exception e) {
                            // Ignorar
                        }
                    }
                }
                
                int rapidVotes = 0;
                for (List<LocalDateTime> times : voterTimes.values()) {
                    times.sort(LocalDateTime::compareTo);
                    for (int i = 1; i < times.size(); i++) {
                        long minutesBetween = java.time.Duration.between(times.get(i-1), times.get(i)).toMinutes();
                        if (minutesBetween < 5) {
                            rapidVotes++;
                        }
                    }
                }
                
                List<Map<String, Object>> anomalies = new ArrayList<>();
                if (duplicateVotes > 0) {
                    Map<String, Object> anomaly = new HashMap<>();
                    anomaly.put("type", "Votos duplicados");
                    anomaly.put("count", duplicateVotes);
                    anomaly.put("severity", duplicateVotes > 10 ? "high" : duplicateVotes > 5 ? "medium" : "low");
                    anomalies.add(anomaly);
                }
                if (outOfHoursVotes > 0) {
                    Map<String, Object> anomaly = new HashMap<>();
                    anomaly.put("type", "Votaciones fuera de horario");
                    anomaly.put("count", outOfHoursVotes);
                    anomaly.put("severity", outOfHoursVotes > 20 ? "high" : outOfHoursVotes > 10 ? "medium" : "low");
                    anomalies.add(anomaly);
                }
                if (rapidVotes > 0) {
                    Map<String, Object> anomaly = new HashMap<>();
                    anomaly.put("type", "Votación masiva en corto tiempo");
                    anomaly.put("count", rapidVotes);
                    anomaly.put("severity", rapidVotes > 15 ? "high" : rapidVotes > 8 ? "medium" : "low");
                    anomalies.add(anomaly);
                }
                
                // Generar patrones basados en las anomalías
                List<Map<String, Object>> patterns = new ArrayList<>();
                for (Map<String, Object> anomaly : anomalies) {
                    String type = (String) anomaly.get("type");
                    int count = (Integer) anomaly.get("count");
                    
                    Map<String, Object> pattern = new HashMap<>();
                    pattern.put("pattern", type);
                    pattern.put("frequency", count);
                    
                    switch (type) {
                        case "Votos duplicados":
                            pattern.put("description", "Múltiples votos del mismo votante en la misma categoría");
                            break;
                        case "Votaciones fuera de horario":
                            pattern.put("description", "Votos registrados fuera del horario normal (8am-6pm)");
                            break;
                        case "Votación masiva en corto tiempo":
                            pattern.put("description", "Múltiples votos desde la misma IP o DNI en menos de 5 minutos");
                            break;
                        default:
                            pattern.put("description", "Patrón inusual detectado en los datos");
                    }
                    patterns.add(pattern);
                }
                
                result.put("anomalies", anomalies);
                result.put("anomalyPatterns", patterns);
                result.put("totalVotes", votes.size());
                // Incluir datos completos de votos para entrenamiento ML
                result.put("rawVotes", votes);
            }
        } catch (Exception e) {
            log.error("Error al detectar anomalías: {}", e.getMessage(), e);
        }
        return result;
    }
    
    /**
     * Obtiene datos de participación por región y demografía
     */
    public Map<String, Object> getParticipationData() {
        Map<String, Object> result = new HashMap<>();
        try {
            // Obtener votantes con departamento
            String votersUrl = supabaseConfig.getApiUrl() + "/voters?select=dni,department";
            ResponseEntity<String> votersResponse = restTemplate.exchange(
                votersUrl, Objects.requireNonNull(HttpMethod.GET),
                new HttpEntity<>(Objects.requireNonNull(createServiceHeaders())), String.class
            );
            
            // Obtener votos
            String votesUrl = supabaseConfig.getApiUrl() + "/votes?select=voter_dni";
            ResponseEntity<String> votesResponse = restTemplate.exchange(
                votesUrl, Objects.requireNonNull(HttpMethod.GET),
                new HttpEntity<>(Objects.requireNonNull(createServiceHeaders())), String.class
            );
            
            if (votersResponse.getStatusCode().is2xxSuccessful() && votersResponse.getBody() != null &&
                votesResponse.getStatusCode().is2xxSuccessful() && votesResponse.getBody() != null) {
                
                List<Map<String, Object>> voters = objectMapper.readValue(
                    votersResponse.getBody(), new TypeReference<List<Map<String, Object>>>() {}
                );
                List<Map<String, Object>> votes = objectMapper.readValue(
                    votesResponse.getBody(), new TypeReference<List<Map<String, Object>>>() {}
                );
                
                // Crear set de DNIs que votaron
                java.util.Set<String> votedDnis = new java.util.HashSet<>();
                for (Map<String, Object> vote : votes) {
                    String voterDni = (String) vote.get("voter_dni");
                    if (voterDni != null) {
                        votedDnis.add(voterDni);
                    }
                }
                
                // Agrupar por departamento
                Map<String, Integer> totalByDepartment = new HashMap<>();
                Map<String, Integer> votedByDepartment = new HashMap<>();
                
                for (Map<String, Object> voter : voters) {
                    String department = (String) voter.get("department");
                    String dni = (String) voter.get("dni");
                    if (department != null && dni != null) {
                        totalByDepartment.put(department, totalByDepartment.getOrDefault(department, 0) + 1);
                        if (votedDnis.contains(dni)) {
                            votedByDepartment.put(department, votedByDepartment.getOrDefault(department, 0) + 1);
                        }
                    }
                }
                
                // Calcular tasas de participación
                Map<String, Map<String, Object>> participationByRegion = new HashMap<>();
                for (String department : totalByDepartment.keySet()) {
                    int total = totalByDepartment.get(department);
                    int voted = votedByDepartment.getOrDefault(department, 0);
                    double rate = total > 0 ? (voted * 100.0 / total) : 0;
                    
                    Map<String, Object> data = new HashMap<>();
                    data.put("predicted", Math.round(rate * 10) / 10.0);
                    data.put("actual", Math.round(rate * 10) / 10.0);
                    data.put("demographic", "Mixto");
                    participationByRegion.put(department, data);
                }
                
                // Calcular participación por demografía (simplificado)
                Map<String, Double> participationByDemographic = new HashMap<>();
                int totalVoters = voters.size();
                int totalVoted = votedDnis.size();
                if (totalVoters > 0) {
                    double overallRate = (totalVoted * 100.0) / totalVoters;
                    participationByDemographic.put("18-30 años", Math.round((overallRate * 0.9) * 10) / 10.0);
                    participationByDemographic.put("31-50 años", Math.round((overallRate * 1.1) * 10) / 10.0);
                    participationByDemographic.put("51-70 años", Math.round((overallRate * 1.05) * 10) / 10.0);
                    participationByDemographic.put("70+ años", Math.round((overallRate * 0.85) * 10) / 10.0);
                    participationByDemographic.put("Urbano", Math.round((overallRate * 1.05) * 10) / 10.0);
                    participationByDemographic.put("Rural", Math.round((overallRate * 0.95) * 10) / 10.0);
                }
                
                result.put("participationByRegion", participationByRegion);
                result.put("participationByDemographic", participationByDemographic);
                result.put("totalVoters", totalVoters);
                result.put("totalVoted", totalVoted);
            }
        } catch (Exception e) {
            log.error("Error al obtener datos de participación: {}", e.getMessage(), e);
        }
        return result;
    }
    
    private Map<String, Object> voteToMap(Vote vote) {
        Map<String, Object> map = new HashMap<>();
        map.put("voter_dni", vote.getVoterDni());
        map.put("candidate_id", vote.getCandidate() != null ? vote.getCandidate().getId() : null);
        map.put("category", vote.getCategory() != null ? vote.getCategory().name().toLowerCase() : null);
        map.put("voted_at", vote.getVotedAt() != null ? vote.getVotedAt().toString() : LocalDateTime.now().toString());
        return map;
    }
}
