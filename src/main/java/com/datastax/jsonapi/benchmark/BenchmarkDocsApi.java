package com.datastax.jsonapi.benchmark;

import com.datastax.jsonapi.JsonFieldExtractor;
import com.datastax.jsonapi.JsonFieldExtractorFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.io.IOException;
import java.io.InputStream;

/**
 * Field extraction test using "example.json" from Docs API tests.
 */
@BenchmarkMode(Mode.Throughput)
@State(Scope.Benchmark)
@Fork(value = 3)
@Measurement(iterations = 2, time = 3)
@Warmup(iterations = 1, time = 3)
public class BenchmarkDocsApi
{
    private ObjectMapper MAPPER;

    private JsonFieldExtractor docsApiExtractorSmall;

    private JsonFieldExtractor docsApiExtractorBig;

    private byte[] exampleDocJson;

    @Setup(Level.Trial) // read once and for all
    public void prepare() throws IOException
    {
        MAPPER = new JsonMapper();
        try (InputStream in = getClass().getResourceAsStream("/jmh/docsapi-example.json")) {
            // Read as JSON to validate goodness
            JsonNode doc = MAPPER.readTree(in);
            // Input is indented, replicate:
            exampleDocJson = MAPPER.writerWithDefaultPrettyPrinter()
                    .writeValueAsBytes(doc);
            // and for fancies, re-parse, discard (ensures we have valid JSON as output)
            MAPPER.readTree(exampleDocJson);
        }
        JsonFieldExtractorFactory extractorFactory
                = JsonFieldExtractorFactory.construct(MAPPER);
        docsApiExtractorSmall = extractorFactory.buildExtractor(
                "products.electronics.Pixel_3a, quiz.sport.q1.answer");
        docsApiExtractorBig = extractorFactory.buildExtractor(
                "quiz.nests, products.food");

        // Verify that we can extract the fields we want
        String text = docsApiExtractorSmall.extractAsString(exampleDocJson).get().trim();
        final String EXP = "Huston Rocket Pixel Google 3a 600";
        if (!text.equals(EXP)) {
            throw new IllegalStateException("Invalid extracted text: expected '"+EXP+"', got '"
                    +text+"' (length "+text.length()+")");
        }
    }

    /**
     * No-operations case of simply scanning through JSON Document
     */
    @Benchmark
    public int jsonScanOnly(Blackhole bh) throws IOException {
        int count = 0;
        try (JsonParser p = MAPPER.createParser(exampleDocJson)) {
            while (p.nextToken() != null) {
                ++count;
            }
        }
        return _validate(bh, count);
    }

    @Benchmark
    public int jsonReadTree(Blackhole bh) throws IOException {
        JsonNode doc = MAPPER.readTree(exampleDocJson);
        return _validate(bh, doc.size());
    }

    @Benchmark
    public int jsonReadAndExtractTiny(Blackhole bh) throws IOException {
        String text = docsApiExtractorSmall.extractAsString(exampleDocJson).get();
        return _validate(bh, text.length());
    }

    @Benchmark
    public int jsonReadAndExtractMost(Blackhole bh) throws IOException {
        String text = docsApiExtractorBig.extractAsString(exampleDocJson).get();
        return _validate(bh, text.length());
    }

    // // // Helper methods

    private int _validate(Blackhole bh, int bogusResult) {
        if (bogusResult <= 0) {
            throw new IllegalStateException("Invalid (bogus) result "+bogusResult+": must be > 0");
        }
        bh.consume(bogusResult);
        return bogusResult;
    }
}
