package ru.gazprombank.automation.akitagpb.modules.core.steps.isteps;

import ru.gazprombank.automation.akitagpb.modules.core.cucumber.api.AkitaScenario;
import ru.gazprombank.automation.akitagpb.modules.core.steps.BaseMethods;

/** Общие методы, используемые в различных шагах */
public interface IBaseMethods {

  AkitaScenario akitaScenario = AkitaScenario.getInstance();
  BaseMethods baseMethod = BaseMethods.getInstance();
}
