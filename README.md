# JSON API Analyzer Filter

Repo that contains specialized filter to use on Jackson `JsonParser` as well as scaffolding for using
it for extracting textual content from within JSON content, indicated by simple "inclusion paths" notation.

The idea is that for content like:

```json
{
  "_id": 124,
  "name": "Bob Burger",
  "phone": {
    "home": "555-123-4567",
    "work": "555-111-2222"
    
  }
}  
```

we can extract contents by specifying inclusion paths like:

```
   name, phone.home
```

which results in constructing indexable text blob of:

```
Bob Burger 555-123-4567
```

and for this specific case, we could do that by:

```java
JsonFieldExtractorFactory f = JsonFieldExtractorFactory.construct(new ObjectMapper());
JsonFieldExtractor extr = f.buildExtractor("name, phone.home");
String json = "..."; // get JSON from somewhere
String toIndex = extr.extractAsString(json).get(); // Optional.empty() if not JSON

assertThat(toIndex).isEqualTo("Bob Burger 555-123-4567 "); // note trailing space
```

## Caching

Instances of `JsonFeidlExtractorFactory` and `JsonFieldExtractor` are thread-safe and can be shared between threads.
They should be cached as much as possible: for former a Singleton is enough, and for latter, a size-bound
cache (like `Caffeine`) keyed by field definition `String` is recommended; this avoids processing to build token filter
(which should not be particularly expensive but is not free either).

## Implementation

Internally the implementation is based on Jackson's `JsonParser` configured with a `JsonToken` constructed from
inclusion path definition.
As such read performance should be close to that of basic JSON decoding with little extra overhead.
Output aggregation is simple text aggregation using `StringWriter`, although if output is needed as `ByteBuffer`,
additional UTF-8 encoding overhead is incurred.

## Benchmarking

Project includes [JMH](https://github.com/openjdk/jmh) based micro-benchmarks for comparing performance of extraction
to that of basic JSON decoding.

Sample results below are run on my dev laptop (MacBoo Pro, 6-core 2.6 Ghz) and JDK 17.

### "Docs Api" (2Kb)

Benchmark that uses example JSON document of 2164 bytes (2.1Kb) and extracts contents as `String`
(for extraction cases):

```
Benchmark                                 Mode  Cnt       Score       Error  Units
BenchmarkDocsApi.jsonReadAndExtractMost  thrpt    6  135952.402 ± 24056.339  ops/s
BenchmarkDocsApi.jsonReadAndExtractTiny  thrpt    6  184029.745 ±  9156.137  ops/s
BenchmarkDocsApi.jsonReadTree            thrpt    6  125507.516 ±  2572.125  ops/s
BenchmarkDocsApi.jsonScanOnly            thrpt    6  229630.819 ± 13640.572  ops/s
```

in this case we get average throughput numbers as follows:

* 230,000 documents (460 MB) per second per core for basic JSON scanning (skipping through tokens, not accessing values)
* 185,000 documents (370 MB) per second per core when extracting small amounts (2 unrelated subtrees, 5 leaf values)
* 135,000 documents (270 MB) per second per core when extracting larger amounts (about half the document; dozens of leaf values)
* 125,000 documents (250 MB) per second per core when building (but not processing) in-memory Tree representation (access all leaf values)
