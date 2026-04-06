package ru.gazprombank.automation.akitagpb.modules.ccl.helpers.async;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import ru.gazprombank.automation.akitagpb.modules.core.helpers.ConfigLoader;

public interface IAsyncExecutor {

  ExecutorService executor =
      Executors.newFixedThreadPool(
          ConfigLoader.getConfigValueOrDefault("async.executor.threads.poolSize", 8));

  static CompletableFuture<Void> runAsync(
      Class<? extends IAsyncExecutor> clazz, String methodName, Object... args) {
    return CompletableFuture.runAsync(
        () -> {
          Method method =
              Arrays.stream(clazz.getMethods())
                  .filter(m -> m.getName().equals(methodName))
                  .findFirst()
                  .orElseThrow(
                      () ->
                          new RuntimeException(
                              "Класс "
                                  + clazz.getName()
                                  + " не содержит метод с именем "
                                  + methodName));
          try {
            method.invoke(null, args);
          } catch (IllegalAccessException e) {
            e.printStackTrace();
            throw new RuntimeException(
                String.format(
                    "Невозможно вызвать метод %s - неправильный уровень доступа!\n%s",
                    methodName, e.getMessage()));
          } catch (InvocationTargetException e) {
            e.printStackTrace();
            throw new RuntimeException(
                String.format(
                    "Невозможно вызвать метод %s - ошибка при вызове метода: %s",
                    methodName, e.getMessage()));
          }
        },
        executor);
  }

  @SuppressWarnings("unchecked")
  static <T> CompletableFuture<T> runAsync(
      Class<? extends IAsyncExecutor> clazz,
      Class<T> returnedType,
      String methodName,
      Object... args) {
    return CompletableFuture.supplyAsync(
        () -> {
          Method method =
              Arrays.stream(clazz.getMethods())
                  .filter(
                      m -> m.getName().equals(methodName) && m.getReturnType().equals(returnedType))
                  .findFirst()
                  .orElseThrow(
                      () ->
                          new RuntimeException(
                              "Класс "
                                  + clazz.getName()
                                  + " не содержит метод с именем "
                                  + methodName));
          try {
            return (T) method.invoke(null, args);
          } catch (IllegalAccessException e) {
            e.printStackTrace();
            throw new RuntimeException(
                String.format(
                    "Невозможно вызвать метод %s - неправильный уровень доступа!\n%s",
                    methodName, e.getMessage()));
          } catch (InvocationTargetException e) {
            e.printStackTrace();
            throw new RuntimeException(
                String.format(
                    "Невозможно вызвать метод %s - ошибка при вызове метода: %s",
                    methodName, e.getMessage()));
          }
        },
        executor);
  }
}
