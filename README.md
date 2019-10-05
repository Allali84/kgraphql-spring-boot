[ ![Download](https://api.bintray.com/packages/allali84/com.github/kgraphql-spring-boot-starter/images/download.svg?version=0.0.1) ](https://bintray.com/allali84/com.github/kgraphql-spring-boot-starter/0.0.1/link)

# kgraphql-spring-boot
KGraphQL Spring Framework Boot Starter


# Gradle

```gradle
repositories {
    jcenter()
    mavenCentral()
}
dependencies {
    implementation("com.github:kgraphql-spring-boot-starter:0.0.1")
}
```

# Configuration

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