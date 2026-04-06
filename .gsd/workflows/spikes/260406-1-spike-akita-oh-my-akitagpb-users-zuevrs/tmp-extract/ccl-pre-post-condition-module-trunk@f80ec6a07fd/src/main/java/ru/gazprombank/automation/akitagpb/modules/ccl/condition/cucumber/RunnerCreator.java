package ru.gazprombank.automation.akitagpb.modules.ccl.condition.cucumber;

import io.cucumber.core.eventbus.EventBus;
import io.cucumber.core.options.CommandlineOptionsParser;
import io.cucumber.core.options.CucumberProperties;
import io.cucumber.core.options.CucumberPropertiesParser;
import io.cucumber.core.options.RuntimeOptions;
import io.cucumber.core.runner.Runner;
import io.cucumber.core.runtime.BackendServiceLoader;
import io.cucumber.core.runtime.BackendSupplier;
import io.cucumber.core.runtime.ObjectFactoryServiceLoader;
import io.cucumber.core.runtime.ObjectFactorySupplier;
import io.cucumber.core.runtime.RunnerSupplier;
import io.cucumber.core.runtime.SingletonObjectFactorySupplier;
import io.cucumber.core.runtime.SingletonRunnerSupplier;
import io.cucumber.core.runtime.TimeServiceEventBus;
import java.time.Clock;
import java.util.UUID;
import java.util.function.Supplier;

/** Класс для создания раннера, запускающего шаги из кукумбер-фичи */
public class RunnerCreator {

  private static Runner runner;

  public static Runner getRunner() {
    if (runner == null) {
      initRunner();
    }
    return runner;
  }

  /** Инициализировать раннер */
  public static void initRunner() {
    Supplier<ClassLoader> classLoader = () -> Thread.currentThread().getContextClassLoader();
    EventBus eventBus = new TimeServiceEventBus(Clock.systemUTC(), UUID::randomUUID);
    RuntimeOptions runtimeOptions = getRuntimeOptions();
    ObjectFactoryServiceLoader objectFactoryServiceLoader =
        new ObjectFactoryServiceLoader(classLoader, runtimeOptions);
    ObjectFactorySupplier objectFactorySupplier =
        new SingletonObjectFactorySupplier(objectFactoryServiceLoader);
    BackendSupplier backendSupplier = new BackendServiceLoader(classLoader, objectFactorySupplier);
    RunnerSupplier runnerSupplier =
        new SingletonRunnerSupplier(
            runtimeOptions, eventBus, backendSupplier, objectFactorySupplier);
    runner = runnerSupplier.get();
  }

  /**
   * Получить опции запуска фич/шагов
   *
   * @return опции запуска RuntimeOptions
   */
  private static RuntimeOptions getRuntimeOptions() {
    String[] argv =
        new String[] {
          "--plugin", "org.jetbrains.plugins.cucumber.java.run.CucumberJvm5SMFormatter",
          "--glue", "ru.gazprombank.automation.akitagpb"
        };
    RuntimeOptions propertiesFileOptions =
        new CucumberPropertiesParser().parse(CucumberProperties.fromPropertiesFile()).build();
    RuntimeOptions environmentOptions =
        new CucumberPropertiesParser()
            .parse(CucumberProperties.fromEnvironment())
            .build(propertiesFileOptions);
    RuntimeOptions systemOptions =
        new CucumberPropertiesParser()
            .parse(CucumberProperties.fromSystemProperties())
            .build(environmentOptions);
    CommandlineOptionsParser commandlineOptionsParser = new CommandlineOptionsParser(System.out);
    return commandlineOptionsParser
        .parse(argv)
        .addDefaultGlueIfAbsent()
        .addDefaultFeaturePathIfAbsent()
        .addDefaultSummaryPrinterIfNotDisabled()
        .enablePublishPlugin()
        .build(systemOptions);
  }
}
