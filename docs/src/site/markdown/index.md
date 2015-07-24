## About HAL Interface Documentation

Runtime support for interface documentation of HAL-based JAX-RS services.


### Maven Dependency

```xml
<dependency>
  <groupId>io.wcm.caravan</groupId>
  <artifactId>io.wcm.caravan.hal.docs</artifactId>
  <version>1.0.0</version>
</dependency>
```

### Documentation

* [Usage][usage]
* [Changelog][changelog]


### Overview

For HAL-based RESTful services the REST URLs are not important, because all consumers follow only the HAL link relations. Thus there is need for a documentation for RESTful APIs that is based on link relations and not on URLs.

This bundle implementation an documentation support that can be generated automatically from the maven projects containing the service implementations and domain models and integrations nicely with the [HAL Browser][hal-browser].

See [Usage][usage] for a detailed description.


[usage]: usage.html
[changelog]: changes-report.html
[hal-browser]: http://caravan.wcm.io/hal/browser/
