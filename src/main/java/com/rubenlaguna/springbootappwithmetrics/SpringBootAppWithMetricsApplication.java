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
