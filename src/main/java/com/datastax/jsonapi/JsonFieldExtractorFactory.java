package com.datastax.jsonapi;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Factory for constructing reusable {@link JsonFieldExtractor} instances.
 */
public class JsonFieldExtractorFactory {
    private final JsonFactory jsonFactory;

    private JsonFieldExtractorFactory(JsonFactory jf) {
        jsonFactory = jf;
    }

    public static JsonFieldExtractorFactory construct(JsonFactory jf) {
        return new JsonFieldExtractorFactory(jf);
    }

    public static JsonFieldExtractorFactory construct(ObjectMapper mapper) {
        return JsonFieldExtractorFactory.construct(mapper.getFactory());
    }

    public JsonFieldExtractor buildExtractor(String commaSeparatedPaths) {
        return JsonFieldExtractor.construct(jsonFactory, commaSeparatedPaths);
    }
}
