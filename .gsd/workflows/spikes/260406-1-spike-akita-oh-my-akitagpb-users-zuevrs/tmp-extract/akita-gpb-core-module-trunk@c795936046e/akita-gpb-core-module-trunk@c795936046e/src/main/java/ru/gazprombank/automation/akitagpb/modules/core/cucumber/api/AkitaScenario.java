package ru.gazprombank.automation.akitagpb.modules.core.cucumber.api;

import io.cucumber.java.Scenario;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import ru.gazprombank.automation.akitagpb.modules.core.cucumber.ScopedVariables;
import ru.gazprombank.automation.akitagpb.modules.core.helpers.ConfigLoader;

/** Главный класс, отвечающий за сопровождение тестовых шагов */
@Slf4j
public final class AkitaScenario {

  private static ThreadLocal<AkitaScenario> instance = ThreadLocal.withInitial(AkitaScenario::new);

  /**
   * Среда прогона тестов, хранит в себе: Cucumber.Scenario, переменные, объявленные пользователем в
   * сценарии и страницы, тестирование которых будет производиться
   */
  private static ThreadLocal<AkitaEnvironment> environment =
      ThreadLocal.withInitial(AkitaEnvironment::new);

  /**
   * Список имён переменных которые нельзя изменять из тестовго сценария.<br>
   * Загружается из файла system_variables.conf.<br>
   * Структура файла:<br>
   * names: [ <br>
   * &ensp;имя_переменной1,<br>
   * &ensp;имя_переменной2<br>
   * ]
   */
  private static final List<String> systemVariableNames =
      ConfigLoader.getConfigValue("system_variables.conf", "names");

  private AkitaScenario() {}

  public static AkitaScenario getInstance() {
    return instance.get();
  }

  public AkitaEnvironment getEnvironment() {
    return environment.get();
  }

  public void setEnvironment(AkitaEnvironment akitaEnvironment) {
    environment.set(akitaEnvironment);
  }

  public static void sleep(int seconds) throws InterruptedException {
    Thread.sleep(TimeUnit.MILLISECONDS.convert(seconds, TimeUnit.SECONDS));
  }

  /** Возвращает текущий сценарий (Cucumber.api) */
  public Scenario getScenario() {
    return this.getEnvironment().getScenario();
  }

  /** Выводит дополнительный информационный текст в отчет (уровень логирования INFO) */
  public void log(Object object) {
    this.getEnvironment().log(object);
  }

  /**
   * Получение переменной по имени, заданного пользователем, из пула переменных "variables" в
   * AkitaEnvironment
   *
   * @param name - имя переменной, для которй необходимо получить ранее сохраненное значение
   */
  public Object getVar(String name) {
    Object obj = this.getEnvironment().getVar(name);
    if (obj == null) {
      throw new IllegalArgumentException("Переменная " + name + " не найдена");
    }
    return obj;
  }

  /** Получение переменной без проверки на NULL */
  public Object tryGetVar(String name) {
    return this.getEnvironment().getVar(name);
  }

  /**
   * Заменяет в строке все ключи переменных из пула переменных "variables" в классе AkitaEnvironment
   * на их значения
   *
   * @param stringToReplaceIn строка, в которой необходимо выполнить замену (не модифицируется)
   */
  public String replaceVariables(String stringToReplaceIn) {
    return this.getEnvironment().replaceVariables(stringToReplaceIn);
  }

  /**
   * Добавление переменной в пул "variables" в классе AkitaEnvironment с проверкой на то, что имя
   * переменной не входит в список системных переменных
   *
   * @param name имя переменной заданное пользователем, для которого сохраняется значение. Является
   *     ключом в пуле variables в классе AkitaEnvironment
   * @param object значение, которое нужно сохранить в переменную
   */
  public void setVar(String name, Object object) {
    checkVarName(name);
    this.getEnvironment().setVar(name, object);
  }

  /**
   * Добавление переменной в пул "variables" в классе AkitaEnvironment. Используется для изменения
   * системных переменных.
   *
   * @param name имя переменной заданное пользователем, для которого сохраняется значение. Является
   *     ключом в пуле variables в классе AkitaEnvironment
   * @param object значение, которое нужно сохранить в переменную
   */
  public void setSystemVar(String name, Object object) {
    this.getEnvironment().setVar(name, object);
  }

  /** Получение всех переменных из пула "variables" в классе AkitaEnvironment */
  public ScopedVariables getVars() {
    return this.getEnvironment().getVars();
  }

  /**
   * Проверка имени переменной на вхождение в список системных переменных.
   *
   * @param name имя переменной.
   */
  private void checkVarName(String name) {
    if (systemVariableNames != null && systemVariableNames.size() > 0) {
      MatcherAssert.assertThat(
          String.format(
              "Переменная с именем %s является системной и не может быть изменена из тестового сценария.",
              name),
          systemVariableNames.contains(name),
          Matchers.is(false));
    }
  }
}
