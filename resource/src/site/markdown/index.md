## About HAL Resource

Library for building and reading JSON HAL resources.


### Maven Dependency

```xml
<dependency>
  <groupId>io.wcm.caravan</groupId>
  <artifactId>io.wcm.caravan.hal.resource</artifactId>
  <version>1.0.2</version>
</dependency>
```

### Documentation

* [Usage][usage]
* [API documentation][apidocs]
* [Changelog][changelog]


[usage]: usage.html
[apidocs]: apidocs/
[changelog]: changes-report.html


### Overview

A JSON HAL (Hypertext Application Language) library to document JSON output. Further information is available on the [HAL specification](http://stateless.co/hal_specification.html).

Central component is the HalResource class which wraps HAL operations around a Jackson ObjectNode. It offers methods to manipulate links and embedded resources. These work directly on the passed ObjectNode, hence no serializer is necessary anymore.
