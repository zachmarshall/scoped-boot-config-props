# Binding `@ConfigurationProperties` Issues in Spring Boot 2

This is a sample project that demonstrates binding errors when creating proxies for Spring Boot 2.x
`@ConfigurationProperties` instances. This used to work in Spring Boot 1.x.

There are two tags, [`boot-2.x`](../../tree/boot-2.x) and [`boot-1.x`](../../tree/boot-1.x), that can be checked
out to observe the difference in behavior. To reproduce, simply run `./gradlew clean build` (note the advised
use of `clean` since when you change tags, you change spring versions. The `clean` may not be strictly necessary
but it is advised.) This branch is currently using spring boot 1.5.12 and `./gradlew clean build` should run
cleanly.

This project serves as an example for [this Stack Overflow question](https://stackoverflow.com/q/51505685/8720).
