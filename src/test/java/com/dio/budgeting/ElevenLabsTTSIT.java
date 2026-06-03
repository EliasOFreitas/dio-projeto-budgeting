package com.dio.budgeting;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.ai.elevenlabs.ElevenLabsTextToSpeechModel;
import org.springframework.ai.elevenlabs.ElevenLabsTextToSpeechOptions;
import org.springframework.ai.audio.tts.TextToSpeechPrompt;
import org.springframework.ai.audio.tts.TextToSpeechResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.FileOutputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
// O teste só vai rodar se essa variável de ambiente estiver configurada no seu sistema ou IDE
@EnabledIfEnvironmentVariable(named = "ELEVENLABS_API_KEY", matches = ".+")
public class ElevenLabsTTSIT {

    @Autowired
    ElevenLabsTextToSpeechModel speechModel;

    @Test
    public void should_generateAudioFile_when_textIsProvided() {
        // 1. Defina o texto do seu teste
        String text = "Texto de Teste usando API ElevenLabs para uso em Java IA";

        // 2. Força a configuração da voz gratuita
        ElevenLabsTextToSpeechOptions options = ElevenLabsTextToSpeechOptions.builder()
                .voiceId("ErXwobaYiN019PkySvjV")
                .build();

        // 3. Monta o "Prompt" juntando o texto e as opções
        TextToSpeechPrompt prompt = new TextToSpeechPrompt(text, options);

        // 4. Chama o modelo e extrai o array de bytes do áudio gerado
        TextToSpeechResponse response = speechModel.call(prompt);
        byte[] audioBytes = response.getResult().getOutput();

        // 5. Verifica se o retorno não é nulo e se tem conteúdo
        assertThat(audioBytes).isNotNull();
        assertThat(audioBytes.length).isGreaterThan(0);

        // 6. Salva o áudio no computador (pasta raiz do projeto)
        String caminhoDoArquivo = "teste-elevenlabs.mp3";
        try (FileOutputStream fos = new FileOutputStream(caminhoDoArquivo)) {
            fos.write(audioBytes);
            System.out.println("✅ Arquivo de áudio gerado com sucesso em: " + caminhoDoArquivo);
        } catch (IOException e) {
            System.err.println("❌ Erro ao salvar o arquivo: " + e.getMessage());
        }
    }
}
