package com.matecomp.ollama.rag.controller;

import com.matecomp.ollama.rag.service.DocumentIndexService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
public class IndexController {

    private final DocumentIndexService documentIndexService;

    public IndexController(DocumentIndexService documentIndexService) {
        this.documentIndexService = documentIndexService;
    }

    @PostMapping("/index-upload/pdf")
    public Map<String, String> upLoadPdf(@RequestParam("file")MultipartFile file){
        if (file==null || file.isEmpty()){
            return Map.of("message","File is empty");
        }

        if (file.getContentType()==null || !file.getContentType().startsWith("application/pdf")){
            return  Map.of("message","File is not a PDF file");
        }

        try {
            documentIndexService.indexDocument(file);
            return   Map.of("message","Document has been indexed");
        }catch (Exception e){
            return Map.of("message","Error to index document: ".concat(e.getMessage()));
        }

    }
}
