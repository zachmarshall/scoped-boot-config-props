# Binding `@ConfigurationProperties` Issues in Spring Boot 2

This is a sample project that demonstrates binding errors when creating proxies for Spring Boot 2.x
`@ConfigurationProperties` instances. This used to work in Spring Boot 1.x.

There are two tags, [`boot-2.x`](../../tree/boot-2.x) and [`boot-1.x`](../../tree/boot-1.x), that can be checked
out to observe the difference in behavior. To reproduce, simply run `./gradlew clean build` (note the advised
use of `clean` since when you change tags, you change spring versions. The `clean` may not be strictly necessary
but it is advised.) This branch is currently using spring boot 2.0.3 and `./gradlew clean build` should fail when
it runs the tests. A snippet of the relevant part of the traceback is:

    Caused by: java.lang.IllegalArgumentException: ExistingValue must be an instance of org.example.ScopedConfigPropsTest$TestProperties
        at org.springframework.util.Assert.isTrue(Assert.java:134)
        at org.springframework.boot.context.properties.bind.Bindable.withExistingValue(Bindable.java:162)
        at org.springframework.boot.context.properties.ConfigurationPropertiesBindingPostProcessor.bind(ConfigurationPropertiesBindingPostProcessor.java:104)
        at org.springframework.boot.context.properties.ConfigurationPropertiesBindingPostProcessor.postProcessBeforeInitialization(ConfigurationPropertiesBindingPostProcessor.java:93)
        at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.applyBeanPostProcessorsBeforeInitialization(AbstractAutowireCapableBeanFactory.java:424)
        at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.initializeBean(AbstractAutowireCapableBeanFactory.java:1700)
        at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.doCreateBean(AbstractAutowireCapableBeanFactory.java:581)
        ... 64 more

This project serves as an example for [this Stack Overflow question](https://stackoverflow.com/q/51505685/8720).
