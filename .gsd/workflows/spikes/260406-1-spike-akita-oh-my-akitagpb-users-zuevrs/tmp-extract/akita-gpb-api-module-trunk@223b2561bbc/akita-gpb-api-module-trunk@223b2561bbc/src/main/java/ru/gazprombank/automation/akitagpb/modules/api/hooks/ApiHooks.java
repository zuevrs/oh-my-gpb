package ru.gazprombank.automation.akitagpb.modules.api.hooks;

import io.cucumber.java.DataTableType;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import ru.gazprombank.automation.akitagpb.modules.api.rest.RequestParam;
import ru.gazprombank.automation.akitagpb.modules.api.rest.RequestParamType;
import ru.gazprombank.automation.akitagpb.modules.api.steps.ApiBaseMethods;

@Slf4j
public class ApiHooks extends ApiBaseMethods {

  @DataTableType
  public RequestParam requestParamTransformer(Map<String, String> entry) {
    return new RequestParam(
        RequestParamType.valueOf(entry.get("type").toUpperCase()),
        entry.get("name"),
        entry.get("value"));
  }
}
