package com.matecomp.ollama.rag.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RagServiceIml implements  RagService {

    private final VectorStore vectorStore;

    private final ChatClient chatClient;

    public  RagServiceIml(VectorStore vectorStore, ChatClient.Builder chatClient) {
        this.vectorStore = vectorStore;
        this.chatClient = chatClient.build();
    }

    @Override
    public String ask (String question) {

        List<Document> results = vectorStore.similaritySearch(
                SearchRequest.builder()//busca los textos mas relevantes en la bd vectorial y los une para usarlos como un unico contexto para la ai
                        .query(question)//el texto os convierte en embeddings y los compara con lo que tenemos en la BD pero en chunk
                        .topK(4)//le decimos dame los 4 fragmentos mas parecidos a esta pregunta los chunk
                        .build()
        );

        String  context = results.stream().map(Document::getText)// CHUNK-> chunk.getText()
                .collect(Collectors.joining("\n\n---\n\n"));

        String prompt= """
                Responde solo con la informacion del contexto.
                Si no esta en el contexto di:"No encontre esa informacion en los documentos."
                CONTEXTO:
                %s
                
                PREGUNTA:
                %s
               """.formatted(context,question);// reemplazar esas % se llama interpolacion de strings
// asi se fomatea con lo q se hizo
//                Spring Boot es...
//
//        ---
//
//                Docker sirve para...
//
//        ---
//
//                Postgres guarda...

        return  chatClient
                .prompt()
                .user(prompt)
                .call()
                .content();
    }
}
