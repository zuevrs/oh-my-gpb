package ru.gazprombank.automation.akitagpb.modules.core.cucumber.api;

import static org.reflections.scanners.Scanners.MethodsAnnotated;
import static org.reflections.scanners.Scanners.TypesAnnotated;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Set;
import org.reflections.Reflections;

/**
 * Для поиска классов с заданной аннотацией среди всех классов в проекте на основе механизма
 * рефлексии
 */
public class AnnotationScanner {

  public Set<Class<?>> getClassesAnnotatedWith(Class<? extends Annotation> annotation) {
    return new Reflections("ru.gazprombank.automation.akitagpb", TypesAnnotated)
        .getTypesAnnotatedWith(annotation);
  }

  public Set<Method> getMethodsAnnotatedWith(Class<? extends Annotation> annotation) {
    return new Reflections("ru.gazprombank.automation.akitagpb", MethodsAnnotated)
        .getMethodsAnnotatedWith(annotation);
  }
}
