# Proxy server

![Kotlin](https://shields.io/badge/Kotlin-2.0-purple)

Proxy server transmitting messages between clients.

## Requirements

- JDK 20
- Kotlin 2.0
- Gradle 8.4
- Jackson 2.17

## Execution

1. Run server
```bash
./gradlew :server:run
```
2. Run clients

```bash
./gradlew :client:run
```

## Modules

- client - client source code
- server - server source code
- commons - common DTO, objects and configuration