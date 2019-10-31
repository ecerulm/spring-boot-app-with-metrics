# References

References: 
* [Spring Boot Reference Documentation](https://docs.spring.io/spring-boot/docs/2.2.0.RELEASE/reference/html/index.html)
* [Spring Boot Actuator: Production-ready features](https://docs.spring.io/spring-boot/docs/2.2.0.RELEASE/reference/html/production-ready-features.html#production-ready)
* [6. Metrics](https://docs.spring.io/spring-boot/docs/2.2.0.RELEASE/reference/html/production-ready-features.html#production-ready-metrics)
* [6.2.3 Datadog](https://docs.spring.io/spring-boot/docs/2.2.0.RELEASE/reference/html/production-ready-features.html#production-ready-metrics-export-datadog)


# Purpose 
Show how you can send metrics to Datadog from a Spring Boot Application. 

Specifically show how you can disable the core metrics and only send you custom metrics.

# Steps


##  Generate project 
Use [Spring Initializr](https://start.spring.io/) add: 
 
 * Spring Boot Actuator.
 * Spring Web 


This should generate a `build.gradle` with the following dependencies 

```

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    implementation 'org.springframework.boot:spring-boot-starter-web'
    ...
}

```

## Add the datadog metric registry

As mentioned in [the documentation](https://docs.spring.io/spring-boot/docs/2.2.0.RELEASE/reference/html/production-ready-features.html#production-ready-metrics-getting-started):

> Having a dependency on micrometer-registry-{system} in your runtime classpath is enough for Spring Boot to configure the registry.


So add the following dependency to `build.gradle` if you want to send the metrics to Datadog :

```
    implementation group: 'io.micrometer', name: 'micrometer-registry-datadog'

```


Also you need to provide the configuration properties so that the Datadog MeterRegistry can communicate with Datadog. 
As explained in [6.2.3 Datadog](https://docs.spring.io/spring-boot/docs/2.2.0.RELEASE/reference/html/production-ready-features.html#production-ready-metrics-export-datadog)
add following to your application.properties or `application-<profile>.properties`:

```
management.metrics.export.datadog.api-key=YOUR_KEY
management.metrics.export.datadog.step=30s

logging.level.root=INFO
logging.level.io.micrometer=DEBUG
```

If you don't want to put the datadog api key in the file you can always provide it as a environment variable 
or as a CLI parameters when you start the app

Using CLI parameter:
```
./gradlew bootJar
java -jar build/libs/spring-boot-app-with-metrics-0.0.1-SNAPSHOT.jar --management.metrics.export.datadog.api-key=YOUR_KEY
```


Using environment variable:
```
./gradlew bootJar
export MANAGEMENT_METRICS_EXPORT_DATADOG_API_KEY=OTHER_KEY
java -jar build/libs/spring-boot-app-with-metrics-0.0.1-SNAPSHOT.jar 
```

# Enable the metrics web endpoint (if you want to access the metrics via http)

This is useful for debugging and troubleshooting.

In `application.properties`
```
management.endpoint.health.show-details=always
management.endpoints.web.exposure.include=metrics,health,info
management.metrics.export.simple.enabled=true
```


# Disable the default builtin core metrics (jvm, cpu, file descriptors)

I didn't found anything about `management.metrics.enable.all` in the [official documentation](https://docs.spring.io/spring-boot/docs/2.2.0.RELEASE/reference/html/production-ready-features.html#production-ready-metrics)
, the only references I found are this [github issue](https://github.com/spring-projects/spring-boot/issues/13408)
and this [StackOverflow question](https://stackoverflow.com/questions/54422023/how-to-specify-a-whitelist-of-the-metrics-i-want-to-use-in-spring-boot-with-micr)

But to disable all the [core built-in metrics (jvm metric, cpu metric,s uptime metrics)](https://docs.spring.io/spring-boot/docs/2.2.0.RELEASE/reference/html/production-ready-features.html#production-ready-metrics-meter)
you need to add the following to `application.properties`:

```
management.metrics.enable.all=false
```

If you want to enable back just one of the core metrics like JVM metrics then add:


```
management.metrics.enable.all=false
management.metrics.enable.jvm=true
```

If you run your app you can go to the metrics endpoint and 
you will see only the `jvm.*` metrics.

```bash
curl -s localhost:8080/actuator/metrics | jq .
{
  "names": [
    "jvm.memory.max",
    "jvm.threads.states",
    "jvm.gc.memory.promoted",
    "jvm.memory.used",
    "jvm.gc.max.data.size",
    "jvm.memory.committed",
    "jvm.buffer.memory.used",
    "jvm.threads.daemon",
    "jvm.gc.memory.allocated",
    "jvm.threads.live",
    "jvm.threads.peak",
    "jvm.classes.loaded",
    "jvm.gc.pause",
    "jvm.classes.unloaded",
    "jvm.gc.live.data.size",
    "jvm.buffer.count",
    "jvm.buffer.total.capacity",
    "myapp.metric1"
  ]
}
```

# Now add your custom metrics

In your code 

```java
package com.rubenlaguna.springbootappwithmetrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.ConfigurableEnvironment;

import java.time.Instant;
import java.util.PrimitiveIterator;
import java.util.Random;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

@SpringBootApplication
public class SpringBootAppWithMetricsApplication implements CommandLineRunner {

    private static final Logger LOG = LoggerFactory.getLogger(SpringBootAppWithMetricsApplication.class);

    @Autowired
    private ConfigurableEnvironment env;

    @Autowired
    private MeterRegistry meterRegistry;

    public static void main(String[] args) {
        SpringApplication.run(SpringBootAppWithMetricsApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        LOG.info("App started");
        LOG.info("management.metrics.export.datadog.api-key={}", env.getProperty("management.metrics.export.datadog.api-key"));
        LOG.info("management.metrics.export.datadog.step={}", env.getProperty("management.metrics.export.datadog.step"));

        Instant end = Instant.now().plusSeconds(120); // run for 2 minutes
        final Counter counter = meterRegistry.counter("myapp.metric1");

        final PrimitiveIterator.OfLong waitTimes = new Random().longs(10, 100).iterator();
        final PrimitiveIterator.OfInt countValues = new Random().ints(1, 100).iterator();
        while(Instant.now().isBefore(end)) {
            counter.increment(countValues.nextInt());
            Thread.sleep(waitTimes.next());
        }
    }
}

```

Right now the custom metric `myapp.metric1` will not show up in the metrics endpoint
You need to enable the `myapp.*` explicitly in `application.properties`:

```properties
management.metrics.enable.myapp=true
```

Now if you run your app you will see the metric in the endpoint:

```
curl -s localhost:8080/actuator/metrics/myapp.metric1  | jq .
{
  "name": "myapp.metric1",
  "description": null,
  "baseUnit": null,
  "measurements": [
    {
      "statistic": "COUNT",
      "value": 407
    }
  ],
  "availableTags": []
}
```



