package ru.gazprombank.automation.akitagpb.modules.ccl.helpers;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.awaitility.Awaitility;
import org.awaitility.pollinterval.IterativePollInterval;

public class AwaitHelper {

  public static void awaitFor(
      Callable<Boolean> function, Integer timeout, Integer pollInterval, Integer pause) {
    AtomicInteger repeatCount = new AtomicInteger(0);

    IterativePollInterval interval =
        new IterativePollInterval(
            duration -> {
              if (repeatCount.get() % 4 == 0) {
                repeatCount.set(0);
                return Duration.ofSeconds(pause);
              } else {
                repeatCount.incrementAndGet();
                return Duration.ofSeconds(pollInterval);
              }
            },
            Duration.ofSeconds(pollInterval));

    Awaitility.await()
        .pollInSameThread()
        .timeout(timeout, TimeUnit.SECONDS)
        .pollInterval(interval)
        .until(function);
  }
}
