package com.datastax.jsonapi;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.filter.FilteringParserDelegate;
import com.fasterxml.jackson.core.filter.TokenFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;

public class PathBasedFilterTest {
    private final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    public void testSimpleInclusion() throws Exception {
        verifyInclusion("{\"a\":1,\"b\":2,\"c\":3}", "a",
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

    private void verifyInclusion(String json, String paths, String expJson, String expText) throws Exception{
        assertThat(filterAsJson(json, paths)).isEqualTo(a2q(expJson));
        assertThat(filterAsText(json, paths)).isEqualTo(expText);
    }

    private String filterAsJson(String json, String paths) throws IOException {
        json = a2q(json);
        TokenFilter filter = PathBasedFilterFactory.filterForPaths(paths);
        StringWriter sw = new StringWriter();
        try (JsonParser p = MAPPER.createParser(json)) {
            try (JsonParser fp = new FilteringParserDelegate(p, filter,
                    TokenFilter.Inclusion.INCLUDE_ALL_AND_PATH, true)) {
                try (JsonGenerator g = MAPPER.createGenerator(sw)) {
                    while (fp.nextToken() != null) {
                        g.copyCurrentStructure(fp);
                    }
                }
            }
        }
        return sw.toString();
    }

    private String filterAsText(String json, String paths) throws IOException {
        json = a2q(json);
        TokenFilter filter = PathBasedFilterFactory.filterForPaths(paths);
        StringWriter sw = new StringWriter();
        try (JsonParser p = MAPPER.createParser(json)) {
            try (JsonParser fp = new FilteringParserDelegate(p, filter,
                    TokenFilter.Inclusion.ONLY_INCLUDE_ALL, true)) {
                while (fp.nextToken() != null) {
                    switch (fp.currentToken()) {
                    case VALUE_STRING:
                    case VALUE_NUMBER_FLOAT:
                    case VALUE_NUMBER_INT:
                    case VALUE_FALSE:
                    case VALUE_TRUE:
                        sw.append(fp.getText()).append(' ');
                        break;
                    }
                }
            }
        }
        return sw.toString();
    }

    protected static String a2q(String json) {
        return json.replace("'", "\"");
    }
}
