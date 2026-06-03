package com.dio.budgeting.infrastructure.http;

import com.dio.budgeting.application.ListTransactionsByCategoryUseCase;
import com.dio.budgeting.application.PersistTransactionUseCase;
import com.dio.budgeting.domain.Category;
import com.dio.budgeting.infrastructure.http.request.TransactionRequest;
import com.dio.budgeting.infrastructure.http.response.TransactionResponse;
import org.springframework.ai.audio.transcription.TranscriptionModel;
import org.springframework.ai.audio.tts.TextToSpeechModel;
import org.springframework.ai.audio.tts.TextToSpeechPrompt;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.elevenlabs.ElevenLabsTextToSpeechOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

@RestController
@RequestMapping("/transactions")
public class TransactionController {
    private final PersistTransactionUseCase persistTransactionUseCase;
    private final ListTransactionsByCategoryUseCase listTransactionsByCategoryUseCase;

    private final TranscriptionModel transcriptionModel;
    private final ChatClient chatClient;
    private final TextToSpeechModel textToSpeechModel;


    public TransactionController(PersistTransactionUseCase persistTransactionUseCase,
                                 ListTransactionsByCategoryUseCase listTransactionsByCategoryUseCase,
                                 TranscriptionModel transcriptionModel,
                                 @Value("classpath:prompts/system-message.st") Resource systemPrompt,
                                 ChatClient.Builder chatClientBuilder,
                                 TextToSpeechModel textToSpeechModel) throws IOException {
        this.persistTransactionUseCase = persistTransactionUseCase;
        this.listTransactionsByCategoryUseCase = listTransactionsByCategoryUseCase;
        this.transcriptionModel = transcriptionModel;
        this.chatClient = chatClientBuilder
                .defaultSystem(systemPrompt.getContentAsString(Charset.defaultCharset()))
                .defaultTools(persistTransactionUseCase, listTransactionsByCategoryUseCase)
                .build();
        this.textToSpeechModel = textToSpeechModel;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse createTransaction(@RequestBody TransactionRequest request) {
        var transaction = persistTransactionUseCase.execute(request.toInput());
        return TransactionResponse.from(transaction);
    }

    @GetMapping("/{category}")
    public List<TransactionResponse> readTransactions(@PathVariable Category category) {
        return listTransactionsByCategoryUseCase.execute(category).stream().map(TransactionResponse::from).toList();
    }

    @PostMapping(value = "/ai", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = "audio/mp3")
    public ResponseEntity<Resource> transcribe(@RequestParam("file") MultipartFile file) {
        // 1. O Gemini transcreve o áudio para texto
        var userMessage = transcriptionModel.transcribe(file.getResource());

        // 2. O Gemini processa o texto, decide se precisa acionar as ferramentas (Tools) e gera a resposta textual
        var result = chatClient.prompt().user(userMessage).call().content();

        // 3. Construção das opções do ElevenLabs explicitamente no metodo para blindar contra o erro 402
        ElevenLabsTextToSpeechOptions options = ElevenLabsTextToSpeechOptions.builder()
                .voiceId("ErXwobaYiN019PkySvjV")
                .modelId("eleven_multilingual_v2")
                .build();

        // 4. Empacotamento da resposta do Gemini junto com as opções explícitas
        TextToSpeechPrompt prompt = new TextToSpeechPrompt(result, options);

        // 5. Converte o texto final em um array de bytes contendo o áudio
        byte[] audio = textToSpeechModel.call(prompt).getResult().getOutput();
        var resource = new ByteArrayResource(audio);

        // 6. Retorna o arquivo MP3 montado com os Headers corretos para o cliente
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "audio/mpeg")
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename("audio.mp3")
                                .build()
                                .toString())
                .body(resource);
    }
}
