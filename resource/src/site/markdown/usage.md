## HAL Resource Usage

### Creating a HAL resource


To augment a Jackson ObjectNode it just needs to create a new HalResource and pass the JSON to the constructor. Now it is possible to add links and embedded resources, where embedded resources are HalResources too.

```java
ObjectNode json = ...
ObjectNode childJson = ...
HalResource resource = new HalResource(json)
  .addLinks("self", HalResourceFactory.createLink(uri))
  .addEmbedded("children", new HalResource(childJson));
```

### HalResourceFactory and ResourceMapper

Creating HAL resources can be very struggling. Thats why the HalResourceFactory can work with any kind of input objects and a ResourceMapper to convert the input objects into another representation and create the corresponding link for them.

```java
Iterable<A> children = ...
ResourceMapper<A> mapper = ...
List<HalResource> embeddedChildren = HalResourceFactory.createEmbeddedResources(children, mapper);
```

### HalBuilder

The HalBuilder class offers a short and easy way to create/manipulate HAL resources.

```java
Object state = ...
HalResource resource = new HalBuilder(state, uri)
  .link("more", moreUri)
  .embed("parent", parent, mapper)
  .build();

```
