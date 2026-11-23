package com.matecomp.ollama.rag.controller;

import com.matecomp.ollama.rag.service.RagService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RagController {

    private final RagService ragService;

    public  RagController(RagService ragService) {
        this.ragService = ragService;
    }

    @PostMapping("/rag/ask")
    public String ask (@RequestBody String question){
        return ragService.ask(question);
    }
}
