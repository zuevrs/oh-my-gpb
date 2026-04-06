package ru.gazprombank.automation.akitagpb.modules.ccl.helpers.generator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Аннотация для методов, которые в переданной строке заменяют шаблон value() на соответствующее
 * этому шаблону генерируемое значение. Это нужно для метода {@link
 * DataGenerationHelper#generateVariable(String)}
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface GeneratablePattern {

  String value();
}
