package ru.gazprombank.automation.akitagpb.modules.ccl.helpers;

import static ru.gazprombank.automation.akitagpb.modules.ccl.helpers.generator.DataGenerationHelper.getRandomNumber;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ListHelper {

  /**
   * Получить рандомный элемент списка
   *
   * @param list список
   * @return рандомный элемент списка
   */
  public static <T> T getRandomListElement(List<T> list) {
    return list.get(getRandomNumber("0", String.valueOf(list.size())).intValue());
  }

  /**
   * Получить N рандомных элементов списка
   *
   * @param list список
   * @param number число рандомных элементов
   * @return список из number рандомных элементов
   */
  public static <T> List<T> getRandomListElements(List<T> list, int number) {
    if (number > list.size()) {
      throw new IllegalArgumentException(
          String.format(
              "Переданный список содержит меньше элементов (%d), чем ожидается получить (%d)",
              list.size(), number));
    }
    var listIndexes = IntStream.range(0, list.size()).boxed().collect(Collectors.toList());
    Collections.shuffle(listIndexes);
    return listIndexes.stream().limit(number).map(list::get).collect(Collectors.toList());
  }

  /**
   * Привести все строки в списке списков к нижнему регистру
   *
   * @param list исходный список списков строк
   * @return список списков со строками в нижнем регистре
   */
  public static List<List<String>> toLowerCase(List<List<String>> list) {
    return list.stream()
        .map(innerList -> innerList.stream().map(String::toLowerCase).collect(Collectors.toList()))
        .collect(Collectors.toList());
  }

  /**
   * Метод для получения объекта Comparator в зависимости от типов элементов
   *
   * @param list список
   * @param <T> тип элементов списка
   * @return объект Comparator необходимый для проверки сортировки
   */
  public static <T> Object getListComparator(List<T> list) {
    List<T> listNonNull = list.stream().filter(Objects::nonNull).collect(Collectors.toList());
    Class<?> classType = listNonNull.isEmpty() ? String.class : listNonNull.get(0).getClass();

    if (classType == String.class) {
      return String.CASE_INSENSITIVE_ORDER;
    }
    return Comparator.naturalOrder();
  }
}
