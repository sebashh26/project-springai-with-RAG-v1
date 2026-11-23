package com.matecomp.ollama.rag.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
public class DocumentPdfIndexServiceImpl implements  DocumentIndexService {

    private final VectorStore vectorStore;

    public  DocumentPdfIndexServiceImpl(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Override
    public void indexDocument(MultipartFile file) throws IOException {
        Resource resource = new InputStreamResource(file.getInputStream());
        PagePdfDocumentReader reader = new PagePdfDocumentReader(resource);
        List<Document> documents= reader.get();
        //config para optimizar para q la ia lo pueda interpreta y buscar mejor
        TokenTextSplitter tokenTextSplitter = TokenTextSplitter.builder()
                .withChunkSize(800)
                .withMinChunkSizeChars(350)
                .withMinChunkLengthToEmbed(5)
                .withMaxNumChunks(10000)
                .withKeepSeparator(true)
                .build();

        List<Document> chunks = tokenTextSplitter.apply(documents);
        String filename = StringUtils.hasLength(file.getOriginalFilename())
                ? file.getOriginalFilename() : "upload-file.pdf";
        chunks.forEach(document -> document.getMetadata().put("sourcema", filename));

        vectorStore.add(chunks);
    }
}
