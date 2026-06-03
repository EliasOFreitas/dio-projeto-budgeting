package com.dio.budgeting;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.ai.chat.prompt.Prompt;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "GOOGLE_API_KEY", matches = ".+")
public class GoogleGenAiChatModelIT {

    @Autowired
    private ChatModel chatModel;

    @Test
    void should_receiveResponse_when_chatModelIsCalled() {
        // Configuração limpa usando String para o modelo, evitando erros de Enum
        var options = GoogleGenAiChatOptions.builder()
                .model("models/gemini-2.5-flash")
                .temperature(0.8)
                .build();

        var response = this.chatModel.call(
                new Prompt("Gere um registro de budgeting, com descrição de gasto, valor em reais e local", options)
        );

        assertThat(response.getResult().getOutput().getText()).isNotEmpty();

        System.out.println("\n--- Resposta do Gemini (Integração) ---");
        System.out.println(response.getResult().getOutput().getText());
        System.out.println("---------------------------------------");
    }
}