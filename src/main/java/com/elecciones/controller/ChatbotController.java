package com.elecciones.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
@Slf4j
public class ChatbotController {
    
    /**
     * Endpoint para procesar mensajes del chatbot
     * POST /api/chatbot/message
     */
    @PostMapping("/message")
    public ResponseEntity<Map<String, Object>> processMessage(@RequestBody Map<String, String> request) {
        try {
            String message = request.get("message");
            if (message == null || message.trim().isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("error", "El mensaje no puede estar vacío");
                return ResponseEntity.badRequest().body(error);
            }
            
            log.info("Mensaje recibido del chatbot: {}", message);
            
            // Procesar el mensaje y generar respuesta
            String response = generateResponse(message.toLowerCase().trim());
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("response", response);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error al procesar mensaje del chatbot: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Error al procesar el mensaje");
            return ResponseEntity.status(500).body(error);
        }
    }
    
    /**
     * Genera una respuesta basada en el mensaje del usuario
     */
    private String generateResponse(String message) {
        // Respuestas sobre cómo votar
        if (message.contains("votar") || message.contains("voto") || message.contains("votación")) {
            return "Para votar, sigue estos pasos:\n\n" +
                   "1. Ingresa tu DNI de 8 dígitos en la página principal\n" +
                   "2. El sistema verificará tu identidad\n" +
                   "3. Selecciona un candidato en cada categoría (Presidencial, Distrital, Regional)\n" +
                   "4. Haz clic en 'Confirmar Votos'\n" +
                   "5. ¡Listo! Tu voto será registrado\n\n" +
                   "Tienes 5 minutos para completar tu votación desde que ingresas tu DNI.";
        }
        
        // Respuestas sobre inicio de sesión
        if (message.contains("iniciar") || message.contains("sesión") || message.contains("login") || 
            message.contains("ingresar") || message.contains("entrar")) {
            return "Para iniciar sesión:\n\n" +
                   "1. Ve a la página principal\n" +
                   "2. Ingresa tu DNI (8 dígitos)\n" +
                   "3. El sistema verificará tu identidad automáticamente\n" +
                   "4. Serás redirigido a la página de votación\n\n" +
                   "No necesitas contraseña, solo tu DNI.";
        }
        
        // Respuestas sobre DNI
        if (message.contains("dni") || message.contains("documento") || message.contains("identidad")) {
            return "El DNI (Documento Nacional de Identidad) es tu número de identificación de 8 dígitos.\n\n" +
                   "Para votar necesitas:\n" +
                   "• Un DNI válido de 8 dígitos\n" +
                   "• Estar registrado en el sistema\n\n" +
                   "Si tu DNI no está registrado, el sistema lo registrará automáticamente al ingresarlo.";
        }
        
        // Respuestas sobre candidatos
        if (message.contains("candidato") || message.contains("candidatos") || message.contains("opciones")) {
            return "Puedes votar en tres categorías:\n\n" +
                   "1. **Presidencial**: Candidatos a la presidencia\n" +
                   "2. **Distrital**: Candidatos a nivel distrital\n" +
                   "3. **Regional**: Candidatos a nivel regional\n\n" +
                   "Debes seleccionar un candidato en cada categoría antes de confirmar tu voto.";
        }
        
        // Respuestas sobre tiempo
        if (message.contains("tiempo") || message.contains("minutos") || message.contains("duración") || 
            message.contains("cuánto") || message.contains("cuanto")) {
            return "Tienes **5 minutos** para completar tu votación desde que ingresas tu DNI.\n\n" +
                   "El contador aparece en la parte superior de la pantalla.\n" +
                   "Si se agota el tiempo, tu sesión se cerrará automáticamente y los votos no confirmados no serán válidos.";
        }
        
        // Respuestas sobre problemas técnicos
        if (message.contains("error") || message.contains("problema") || message.contains("no funciona") || 
            message.contains("no puedo") || message.contains("ayuda")) {
            return "Si tienes problemas:\n\n" +
                   "• Verifica que tu DNI tenga 8 dígitos\n" +
                   "• Asegúrate de tener conexión a internet\n" +
                   "• Intenta recargar la página\n" +
                   "• Si el problema persiste, contacta al administrador del sistema\n\n" +
                   "¿Hay algún error específico que estés viendo?";
        }
        
        // Respuestas sobre el sistema
        if (message.contains("sistema") || message.contains("electoral") || message.contains("qué es") || 
            message.contains("que es") || message.contains("información")) {
            return "El Sistema Electoral Perú 2025 es una plataforma digital para votaciones electrónicas.\n\n" +
                   "Características:\n" +
                   "• Verificación de identidad mediante DNI\n" +
                   "• Votación en tres categorías (Presidencial, Distrital, Regional)\n" +
                   "• Seguridad y transparencia en el proceso\n" +
                   "• Resultados en tiempo real\n\n" +
                   "¿Tienes alguna pregunta específica sobre el sistema?";
        }
        
        // Respuestas sobre resultados
        if (message.contains("resultado") || message.contains("resultados") || message.contains("estadística") || 
            message.contains("estadisticas") || message.contains("ganador")) {
            return "Los resultados están disponibles en el panel administrativo.\n\n" +
                   "Como votante, puedes ver:\n" +
                   "• El número de votos de cada candidato\n" +
                   "• Los porcentajes de votación\n" +
                   "• Las estadísticas generales\n\n" +
                   "Los resultados se actualizan en tiempo real mientras las personas votan.";
        }
        
        // Respuestas sobre seguridad
        if (message.contains("seguridad") || message.contains("seguro") || message.contains("privacidad") || 
            message.contains("datos")) {
            return "El sistema garantiza la seguridad de tus datos:\n\n" +
                   "• Tu DNI se verifica mediante RENIEC\n" +
                   "• Los votos son anónimos y confidenciales\n" +
                   "• Solo puedes votar una vez por categoría\n" +
                   "• Los datos están protegidos y encriptados\n\n" +
                   "Tu privacidad es nuestra prioridad.";
        }
        
        // Saludos
        if (message.contains("hola") || message.contains("buenos días") || message.contains("buenos dias") || 
            message.contains("buenas tardes") || message.contains("buenas noches") || message.contains("saludo")) {
            return "¡Hola! 👋\n\n" +
                   "Soy tu asistente virtual del Sistema Electoral Perú 2025.\n\n" +
                   "Puedo ayudarte con:\n" +
                   "• Cómo votar\n" +
                   "• Cómo iniciar sesión\n" +
                   "• Información sobre candidatos\n" +
                   "• Preguntas sobre el sistema\n\n" +
                   "¿En qué puedo ayudarte?";
        }
        
        // Despedidas
        if (message.contains("adiós") || message.contains("adios") || message.contains("chau") || 
            message.contains("gracias") || message.contains("hasta luego")) {
            return "¡Gracias por usar el Sistema Electoral Perú 2025! 🗳️\n\n" +
                   "Si tienes más preguntas, no dudes en consultarme.\n" +
                   "¡Que tengas un buen día!";
        }
        
        // Respuesta por defecto
        return "Entiendo tu pregunta. Te puedo ayudar con:\n\n" +
               "• **Cómo votar**: Pregúntame sobre el proceso de votación\n" +
               "• **Iniciar sesión**: Información sobre cómo ingresar al sistema\n" +
               "• **Candidatos**: Información sobre las opciones de voto\n" +
               "• **Tiempo**: Información sobre el tiempo disponible para votar\n" +
               "• **Problemas**: Ayuda con errores o problemas técnicos\n\n" +
               "¿Puedes ser más específico con tu pregunta?";
    }
}

