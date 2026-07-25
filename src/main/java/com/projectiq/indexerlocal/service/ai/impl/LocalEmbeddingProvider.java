package com.projectiq.indexerlocal.service.ai.impl;

import com.projectiq.indexerlocal.service.ai.EmbeddingProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Default local embedding provider that generates deterministic embedding vectors
 * using hash-based feature extraction. This is a lightweight, zero-dependency
 * implementation suitable for development and testing.
 *
 * For production use, replace with Ollama, OpenAI, or other embedding providers.
 */
@Service
public class LocalEmbeddingProvider implements EmbeddingProvider {

    private static final Logger log = LoggerFactory.getLogger(LocalEmbeddingProvider.class);
    private static final int DIMENSION_SIZE = 128;

    @Override
    public double[] generateEmbedding(String text) {
        if (text == null || text.isEmpty()) {
            return new double[DIMENSION_SIZE];
        }

        double[] embedding = new double[DIMENSION_SIZE];
        String normalized = text.toLowerCase().trim();

        // Generate deterministic features from the text
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(normalized.getBytes(StandardCharsets.UTF_8));

            // Seed the embedding with hash values
            for (int i = 0; i < DIMENSION_SIZE && i < hash.length; i++) {
                embedding[i] = (hash[i] & 0xFF) / 256.0;
            }

            // Add n-gram based features for semantic richness
            addNGramFeatures(embedding, normalized, 2);
            addNGramFeatures(embedding, normalized, 3);
            addTokenFrequencyFeatures(embedding, normalized);

            // Normalize the embedding vector
            normalizeVector(embedding);

        } catch (NoSuchAlgorithmException e) {
            log.warn("SHA-256 not available, using fallback embedding generation");
            generateFallbackEmbedding(embedding, normalized);
        }

        return embedding;
    }

    @Override
    public int getDimensionSize() {
        return DIMENSION_SIZE;
    }

    @Override
    public String getProviderName() {
        return "local-hash-embedding";
    }

    private void addNGramFeatures(double[] embedding, String text, int n) {
        if (text.length() < n) return;

        int offset = (n - 2) * 32;
        for (int i = 0; i < text.length() - n + 1; i++) {
            String ngram = text.substring(i, i + n);
            int index = Math.abs(ngram.hashCode()) % (DIMENSION_SIZE - offset);
            embedding[offset + index] += 0.1;
        }

        // Normalize n-gram contributions
        double sum = 0;
        for (int i = offset; i < offset + 32 && i < DIMENSION_SIZE; i++) {
            sum += embedding[i] * embedding[i];
        }
        double norm = Math.sqrt(sum);
        if (norm > 0) {
            for (int i = offset; i < offset + 32 && i < DIMENSION_SIZE; i++) {
                embedding[i] /= norm;
            }
        }
    }

    private void addTokenFrequencyFeatures(double[] embedding, String text) {
        String[] tokens = text.split("\\s+");
        int offset = 96; // Use remaining dimensions for token features

        for (String token : tokens) {
            if (token.isEmpty()) continue;
            int index = Math.abs(token.hashCode()) % (DIMENSION_SIZE - offset);
            embedding[offset + index] += 1.0 / tokens.length;
        }
    }

    private void normalizeVector(double[] vector) {
        double sum = 0;
        for (double v : vector) {
            sum += v * v;
        }
        double norm = Math.sqrt(sum);
        if (norm > 0) {
            for (int i = 0; i < vector.length; i++) {
                vector[i] /= norm;
            }
        }
    }

    private void generateFallbackEmbedding(double[] embedding, String text) {
        // Simple character-code based embedding
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        for (int i = 0; i < DIMENSION_SIZE && i < bytes.length; i++) {
            embedding[i] = (bytes[i] & 0xFF) / 256.0;
        }
        normalizeVector(embedding);
    }
}