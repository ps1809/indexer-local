package com.projectiq.indexerlocal.service.ai;

/**
 * Abstraction for pluggable embedding providers.
 * Implementations can use Ollama, OpenAI, local models, or any other provider.
 */
public interface EmbeddingProvider {

    /**
     * Generate an embedding vector for the given text.
     *
     * @param text the text to embed
     * @return embedding vector as double array
     */
    double[] generateEmbedding(String text);

    /**
     * Get the dimension size of embeddings produced by this provider.
     */
    int getDimensionSize();

    /**
     * Get the provider name.
     */
    String getProviderName();
}