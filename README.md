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

would be extract by specifying inclusion paths like:

```
   name, phone.home
```

to result in indexable text blob of:

```
Bob Burger 555-123-4567
```

and for this specific case, we could do that by:

```java
JsonFieldExtractorFactory f = JsonFieldExtractorFactory.construct(new ObjectMapper());
JsonFieldExtractor extr = f.buildExtractor("name, phone.home");
String json = "..."; // get JSON from somewhere
String toIndex = extr.extractAsString(json);

assertThat(toIndex).isEqualTo("Bob Burger 555-123-4567 "); // note trailing space
```
