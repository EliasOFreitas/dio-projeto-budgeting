package com.dio.budgeting;

import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.MimeTypeUtils;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "GOOGLE_API_KEY", matches = ".+")
public class GoogleGenAiTranscriptionModelIT {

    private final ChatClient chatClient;

    @Autowired
    public GoogleGenAiTranscriptionModelIT(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @ParameterizedTest
    @CsvSource({
            "recording-1.m4a, 80 reais",
            "recording-2.m4a, 40 reais",
            "recording-3.m4a, 120 reais",
            "recording-4.m4a, 90 reais",
            "recording-5.m4a, 200 reais",
            "recording-6.m4a, 60 reais",
    })
    public void should_containExpectedKeywords_when_audioFilesAreProcessed(String fileName, String expectedKeyword) {
        // 1. Carregamos o ficheiro de áudio
        var recording = new ClassPathResource("audio/" + fileName);

        String instrucaoDeTranscricao = """
        Transcreva o conteúdo falado neste áudio em formato de texto simples.
        
        REGRAS OBRIGATÓRIAS DE FORMATAÇÃO:
        1. NUNCA utilize o símbolo monetário "R$".
        2. Sempre transcreva os valores da moeda explicitamente usando a palavra "reais" (formato: [número] + "reais"). 
        3. Exemplo: se ouvir sessenta reais, escreva OBRIGATORIAMENTE "60 reais" e JAMAIS "R$ 60".
        
        Contexto do Áudio:
        - Idioma: Português brasileiro.
        - Assunto: Descrição de gastos financeiros.
        - Padrão das frases: uma ação (gastei, paguei, comprei) + valor + um local ou estabelecimento.
        """;

        // 2. Executamos a chamada sem usar a classe Media!
        // Passamos o MimeType e o recording (Resource) de forma direta no método .media()
        String response = chatClient.prompt()
                .user(userSpec -> userSpec
                        .text(instrucaoDeTranscricao)
                        .media(MimeTypeUtils.parseMimeType("audio/mp4"), recording))
                .call()
                .content();

        // 3. Validação
        assertThat(response).containsIgnoringCase(expectedKeyword);
        System.out.println("Transcrição do Gemini: " + response);
    }
}