package com.dio.budgeting.infrastructure.config;

import org.springframework.ai.audio.transcription.TranscriptionModel;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.audio.transcription.AudioTranscriptionResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Configuration
public class ElevenLabsTranscriptionConfig {

    @Value("${spring.ai.elevenlabs.api-key}")
    private String apiKey;

    @Bean
    public TranscriptionModel transcriptionModel() {
        return new TranscriptionModel() {

            @Override
            public String transcribe(Resource audioResource) {
                RestTemplate restTemplate = new RestTemplate();
                String url = "https://api.elevenlabs.io/v1/speech-to-text";

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.MULTIPART_FORM_DATA);
                headers.set("xi-api-key", apiKey);

                MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
                body.add("file", audioResource);
                body.add("model_id", "scribe_v1");

                HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

                try {
                    Map<String, Object> response = restTemplate.postForObject(url, requestEntity, Map.class);

                    if (response != null && response.containsKey("text")) {
                        return (String) response.get("text");
                    }
                    throw new RuntimeException("Não foi possível extrair o texto do ElevenLabs Scribe");

                } catch (Exception e) {
                    throw new RuntimeException("Erro ao transcrever áudio no ElevenLabs: " + e.getMessage(), e);
                }
            }

            // Assinatura com os tipos exatos do Spring AI
            @Override
            public AudioTranscriptionResponse call(AudioTranscriptionPrompt request) {
                // Metodo exigido pela interface, retornando null
                // já que o Controller usa o atalho .transcribe()
                return null;
            }
        };
    }
}