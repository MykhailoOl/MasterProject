package com.example.masterproject.llm;

import com.example.masterproject.model.enums.LlmProvider;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class LlmClientRegistry {

    private final Map<LlmProvider, LlmClient> clients = new EnumMap<>(LlmProvider.class);

    public LlmClientRegistry(List<LlmClient> llmClients) {
        for (LlmClient client : llmClients) {
            clients.put(client.provider(), client);
        }
    }

    public LlmClient require(LlmProvider provider) {
        LlmClient client = clients.get(provider);
        if (client == null) {
            throw new IllegalArgumentException("Unsupported LLM provider: " + provider);
        }
        return client;
    }
}
