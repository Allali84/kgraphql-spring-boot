[![Download](https://api.bintray.com/packages/allali84/com.github/kgraphql-spring-boot-starter/images/download.svg?version=0.0.3) ](https://bintray.com/allali84/com.github/kgraphql-spring-boot-starter/0.0.3/link)

# kgraphql-spring-boot
KGraphQL Spring Framework Boot Starter


# Gradle

```Kotlin
repositories {
    jcenter()
    mavenCentral()
}
dependencies {
    implementation("com.github:kgraphql-spring-boot-starter:0.0.3")
}
```

# Maven

```xml
<dependency>
  <groupId>com.github</groupId>
  <artifactId>kgraphql-spring-boot-starter</artifactId>
  <version>0.0.3</version>
  <type>pom</type>
</dependency>
```

# Configuration

```yaml
spring:
 kgraphql:
  subscription: "/subscription"
  host: "/graphql" 
  cors:
   mapping: "/**"
   origins: "*"
   methods: "GET", "POST", "PUT", "DELETE", "HEAD"
   maxAge: 3600
```

# Subscription

```kotlin
            val publisher = mutation("createUFOSighting") {
                description = "Adds a new UFO Sighting to the database"

                resolver {
                    input: CreateUFOSightingInput -> service.create(input.toUFOSighting())
                }

            }

            subscription("SubscriptionToCreateUFOSighting") {
                description = "Publish event when a new UFO Sighting is added to the database"

                resolver { subscription: String ->
                    subscribe(subscription, publisher, UFOSighting()) {
                        applicationEventPublisher.publishEvent(UFOSightingCreatedEvent(Event(subscription, it)))
                    }
                }
            }

            subscription("UnsubscriptionToCreateUFOSighting") {
                description = "Unsubscription from the event when a new UFO Sighting is added to the database"

                resolver { subscription: String ->
                    unsubscribe(subscription, publisher, UFOSighting())
                }
            }

```

To Subscibe :

```kotlin

          schema.execute(
            """
                subscription {
                  SubscriptionToCreateUFOSighting(subscription: "Mysubscription") {id}
                  }
            """
         )

```

So after each execution of a the mutation "createUFOSighting" an event will be sent to the subscriber of our subscription "Mysubscription".

Client Example : 
```javascript
      var socket = new WebSocket('ws://localhost:8080/subscription/Mysubscription');
      socket.onopen = function() { console.log('connection established') };
      socket.onclose = function() { console.log('connection closed') };
      socket.onerror = function(err) { console.log('error: ', err)};
      socket.onmessage = function(message) {
        if (message.data !== '') {
            window.alert('message from server: ' + message.data);
        }
      };
```

To unsubscibe :

```kotlin

          schema.execute(
            """
                subscription {
                  UnsubscriptionToCreateUFOSighting(subscription: "Mysubscription") {id}
                  }
            """
         )

```