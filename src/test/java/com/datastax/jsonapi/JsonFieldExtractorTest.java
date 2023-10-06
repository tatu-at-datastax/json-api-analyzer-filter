package com.datastax.jsonapi;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;

public class JsonFieldExtractorTest {
    private final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    public void testSimpleObjectInclusion() throws Exception {
        verifyInclusion("{'a':1,'b':2,'c':3}", "a",
                "{'a':1}",
                "1 ");
        verifyInclusion("{'a':{'b':1,'c':true,'x':false},'d':'xyz'}", "a.c",
                        "{'a':{'c':true}}",
                "true ");
        verifyInclusion("{'a':{'b':1,'c':true,'x':false},'d':'xyz'}", "a.b, a.x",
                        "{'a':{'b':1,'x':false}}",
                                "1 false ");
        verifyInclusion("{'a':{'b':1,'c':true,'x':false},'d':'xyz'}", "d, a.b",
                        "{'a':{'b':1},'d':'xyz'}",
                "1 xyz ");
    }

    @Test
    public void testSimpleArrayInclusion() throws Exception {
        verifyInclusion("{'a':'123','arr': ['abc', 'def']}", "arr",
                "{'arr':['abc','def']}",
                "abc def ");
        verifyInclusion("{'a':'123','arr': ['abc', 'def']}", "a, ",
                "{'a':'123'}",
                "123 ");
        verifyInclusion("{'a':{'b':{'arr': ['abc', 'def']}}, 'z':3 }", "a.b ",
                "{'a':{'b':{'arr':['abc','def']}}}",
                "abc def ");
    }

    @Test
    public void testMissingInclusion() throws Exception {
        verifyInclusion("{'a':1,'b':2,'c':3}", " ",
                "",
                "");

        verifyInclusion("{'a':1,'b':2,'c':3}", ", ",
                "",
                "");
    }

    @Test
    public void testEmptyInclusion() throws Exception {
        verifyInclusion("{'a':1,'b':2,'c':3}", " x, y",
                "",
                "");
    }

    @Test
    public void testLongerPathLast() throws Exception {
        verifyInclusion("{'a':1,'b':{'x':1,'y':2},'c':true}", "b, b.y",
                "{'b':{'x':1,'y':2}}",
                "1 2 ");
    }

    @Test
    public void testLongerPathFirst() throws Exception {
        verifyInclusion("{'a':1,'b':{'x':1,'y':2},'c':true}", "b.x, b",
                "{'b':{'x':1,'y':2}}",
                "1 2 ");
    }

    private void verifyInclusion(String json, String paths, String expJson, String expText) throws Exception{
        assertThat(filterAsJson(json, paths)).isEqualTo(a2q(expJson));
        assertThat(filterAsText(json, paths)).isEqualTo(expText);
    }

    private String filterAsJson(String json, String paths) throws IOException {
        json = a2q(json);
        JsonFieldExtractor extr = JsonFieldExtractorFactory.construct(MAPPER)
                .buildExtractor(paths);
        StringWriter sw = new StringWriter();
        try (JsonParser p = extr.extractingParser(json)) {
            try (JsonGenerator g = MAPPER.createGenerator(sw)) {
                while (p.nextToken() != null) {
                    g.copyCurrentStructure(p);
                }
            }
        }
        return sw.toString();
    }

    private String filterAsText(String json, String paths) throws IOException {
        json = a2q(json); // just so tests can use single quotes
        JsonFieldExtractor extr = JsonFieldExtractorFactory.construct(MAPPER)
                .buildExtractor(paths);
        return extr.extractAsString(json);
    }

    protected static String a2q(String json) {
        return json.replace("'", "\"");
    }
}
