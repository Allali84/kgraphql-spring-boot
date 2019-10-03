# kgraphql-spring-boot
KGraphQL Spring Framework Boot Starters


# How-To

```kotlin
repositories {
    maven {
      url = uri("https://dl.bintray.com/allali84/com.github/")
    }
  }
  dependencies {
    classpath("com.github:kgraphql-spring-boot-starter:0.0.1")
  }
```
# Configuration Example

```yaml
spring:
 kgraphql:
  host: "/graphql"
  cors:
   mapping: "/**"
   origins: "*"
   methods: "GET", "POST", "PUT", "DELETE", "HEAD"
   maxAge: 3600
```