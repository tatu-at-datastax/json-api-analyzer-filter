package com.datastax.jsonapi;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonTokenId;
import com.fasterxml.jackson.core.filter.FilteringParserDelegate;
import com.fasterxml.jackson.core.filter.TokenFilter;
import com.fasterxml.jackson.databind.util.ByteBufferBackedInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Main class that handles extraction of JSON field contents from JSON documents,
 * to be used for textual analysis. Instances are created via {@link JsonFieldExtractorFactory};
 * uses {@link PathBasedFilterFactory} for constructing {@link PathBasedFilter} used
 * for actual filtering of streaming JSON content.
 *<p>
 * Content passed as possible JSON is checked to see whether it starts with expected start marker
 * (START-OBJECT, {@code { }}, or START-ARRAY, {@code [ ]}), and if not, extraction is not attempted
 * and {@link Optional#empty()} is returned. Otherwise {@link Optional} of extract result is
 * returned.
 *</p>
 */
public class JsonFieldExtractor {
    private final JsonFactory jsonFactory;
    private final TokenFilter filter;

    /*
    /**********************************************************
    /* Construction
    /**********************************************************
     */

    private JsonFieldExtractor(JsonFactory jsonFactory, TokenFilter filter) {
        this.jsonFactory = jsonFactory;
        this.filter = filter;
    }

    public static JsonFieldExtractor construct(JsonFactory jsonFactory,
                                               String commaSeparatedPaths) {
        return new JsonFieldExtractor(jsonFactory,
                PathBasedFilterFactory.filterForPaths(commaSeparatedPaths));
    }

    /*
    /**********************************************************
    /* Public API
    /**********************************************************
     */

    public Optional<String> extractAsString(String json) throws IOException {
        if (!_hasJson(json)) {
            return Optional.empty();
        }
        try (JsonParser p = jsonFactory.createParser(json)) {
            return Optional.of(_extractAsString(p, json.length()));
        }
    }

    public Optional<String> extractAsString(byte[] json) throws IOException {
        if (!_hasJson(json)) {
            return Optional.empty();
        }
        try (JsonParser p = jsonFactory.createParser(json)) {
            return Optional.of(_extractAsString(p, json.length));
        }
    }

    public Optional<String> extractAsString(ByteBuffer json) throws IOException {
        if (!_hasJson(json)) {
            return Optional.empty();
        }
        try (InputStream in = new ByteBufferBackedInputStream(json)) {
            try (JsonParser p = jsonFactory.createParser(in)) {
                return Optional.of(_extractAsString(p, json.remaining()));
            }
        }
    }

    public Optional<byte[]> extractAsBytes(String json) throws IOException {
        if (!_hasJson(json)) {
            return Optional.empty();
        }
        try (JsonParser p = jsonFactory.createParser(json)) {
            return Optional.of(_extractAsBytes(p, json.length()));
        }
    }

    public Optional<byte[]> extractAsBytes(byte[] json) throws IOException {
        if (!_hasJson(json)) {
            return Optional.empty();
        }
        try (JsonParser p = jsonFactory.createParser(json)) {
            return Optional.of(_extractAsBytes(p, json.length));
        }
    }

    public Optional<byte[]> extractAsBytes(ByteBuffer json) throws IOException {
        if (!_hasJson(json)) {
            return Optional.empty();
        }
        try (InputStream in = new ByteBufferBackedInputStream(json)) {
            try (JsonParser p = jsonFactory.createParser(in)) {
                return Optional.of(_extractAsBytes(p, json.remaining()));
            }
        }
    }

    // Method mostly useful for testing purposes
    public Optional<JsonParser> extractingParser(String json) throws IOException {
        if (!_hasJson(json)) {
            return Optional.empty();
        }
        JsonParser p = jsonFactory.createParser(json);
        JsonParser fp = new FilteringParserDelegate(p, filter,
                TokenFilter.Inclusion.INCLUDE_ALL_AND_PATH, true);
        return Optional.of(fp);
    }

    /*
    /**********************************************************
    /* Internal helper methods
    /**********************************************************
     */

    private boolean _hasJson(String json) {
        if (json.isEmpty()) {
            return false;
        }
        char c = json.charAt(0);
        return (c == '{') || (c == '[');
    }

    private boolean _hasJson(byte[] json) {
        if (json.length == 0) {
            return false;
        }
        char c = (char) json[0];
        return (c == '{') || (c == '[');
    }

    private boolean _hasJson(ByteBuffer bb) {
        if (!bb.hasRemaining()) {
            return false;
        }
        char c = (char) bb.get(bb.position());
        return (c == '{') || (c == '[');
    }

    private String _extractAsString(JsonParser p, int jsonLength) throws IOException {
        StringWriter sw = new StringWriter(estimateResultLength(jsonLength));
        try (JsonParser fp = new FilteringParserDelegate(p, filter,
                TokenFilter.Inclusion.ONLY_INCLUDE_ALL, true)) {
            while (fp.nextToken() != null) {
                if (includeToken(fp.currentTokenId())) {
                    sw.append(fp.getText()).append(' ');
                }
            }
        }
        return sw.toString();
    }

    private byte[] _extractAsBytes(JsonParser p, int jsonLength) throws IOException {
        // !!! TODO: implement more efficiently
        return _extractAsString(p, jsonLength).getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Helper method for estimating rough size of output buffer we need, to reduce
     * need for resizing, but ideally avoiding overallocation.
     */
    private int estimateResultLength(int jsonLength) {
        // Estimate that we'll need output buffer that's 1/4 the size of the input
        int estimate = jsonLength >> 2;
        // But avoid tiny buffers
        if (estimate < 100) {
            return 100;
        }
        if (estimate > 50_000) {
            return 50_000;
        }
        return estimate;
    }

    private boolean includeToken(int id) {
        switch (id) {
        case JsonTokenId.ID_STRING:
        case JsonTokenId.ID_NUMBER_FLOAT:
        case JsonTokenId.ID_NUMBER_INT:
        case JsonTokenId.ID_FALSE:
        case JsonTokenId.ID_TRUE:
            return true;
        default:
            return false;
        }
    }
}
