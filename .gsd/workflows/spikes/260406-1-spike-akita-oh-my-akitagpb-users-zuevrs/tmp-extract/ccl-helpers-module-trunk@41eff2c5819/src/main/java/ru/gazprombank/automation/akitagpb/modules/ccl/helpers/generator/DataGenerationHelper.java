package ru.gazprombank.automation.akitagpb.modules.ccl.helpers.generator;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.NoArgGenerator;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import ru.gazprombank.automation.akitagpb.modules.ccl.helpers.StringHelper;

/** Утилитный класс для генерации тестовых данных */
public class DataGenerationHelper {

  // соответствие шаблонов и методов, которые заменяют эти шаблоны на генерируемые значения
  private static final Map<String, Method> TEMPLATES_METHODS;
  private static ThreadLocalRandom random;
  static final NoArgGenerator UUID_GENERATOR = Generators.timeBasedEpochRandomGenerator();

  static {
    TEMPLATES_METHODS =
        Arrays.stream(PatternReplacer.class.getDeclaredMethods())
            .filter(e -> e.getDeclaredAnnotation(GeneratablePattern.class) != null)
            .collect(
                Collectors.toMap(
                    e -> e.getDeclaredAnnotation(GeneratablePattern.class).value(), e -> e));
  }

  /**
   * Метод определяет порядковый номер текущего месяца (1-12), и на его основе формирует значение
   * period для заявок ЭБГ.
   *
   * @return возвращает значение периода в виде строки.
   */
  public static String getCurrentEbgPeriod() {
    int month = LocalDate.now().getMonthValue();
    if (month == 5 || month == 6 || month == 7) {
      return "1";
    } else if (month == 8 || month == 9 || month == 10) {
      return "2";
    } else if (month == 4) {
      return "4";
    } else {
      return "3";
    }
  }

  /**
   * Метод для получения рандомного числа типа long в указанных пределах.
   *
   * @param from нижний предел (если null - будет равен 0)
   * @param to верхний предел (если null - будет равен {нижний предел} + 100000)
   * @return возвращает рандомное число в заданных пределах.
   */
  public static Long getRandomNumber(String from, String to) {
    if (random == null) {
      random = ThreadLocalRandom.current();
    }
    long fromLong = from == null ? 0 : Long.parseLong(from);
    long toLong = to == null ? fromLong + 100000 : Long.parseLong(to);
    return random.nextLong(fromLong, toLong);
  }

  /**
   * Метод ищет в переданной строке шаблоны из аннотаций {@link GeneratablePattern} методов класса
   * {@link PatternReplacer} и заменяет их на сгенерированные этими методами значения. Также метод
   * заменяет переменные вида ${name} на их значения, если они сохранены в сценарии.
   *
   * @param value строка для обработки/генерации
   * @return возвращает исходную строку, в которой заменены все шаблоны на сгенерированные значения
   */
  public static String generateVariable(String value) {
    AtomicReference<String> result = new AtomicReference<>(value);
    TEMPLATES_METHODS.forEach(
        (template, method) -> {
          String pattern = String.format(".*%s.*", template);
          if (result.get().matches(pattern) || result.get().contains(template)) {
            try {
              result.set(method.invoke(PatternReplacer.class, value).toString());
            } catch (IllegalAccessException | InvocationTargetException e) {
              throw new RuntimeException(
                  String.format(
                      "Ошибка при генерации переменной по шаблону '%s' методом '%s' для строки '%s'",
                      template, method.getName(), result.get()),
                  e);
            }
          }
        });
    return StringHelper.processValue(result.get());
  }
}
