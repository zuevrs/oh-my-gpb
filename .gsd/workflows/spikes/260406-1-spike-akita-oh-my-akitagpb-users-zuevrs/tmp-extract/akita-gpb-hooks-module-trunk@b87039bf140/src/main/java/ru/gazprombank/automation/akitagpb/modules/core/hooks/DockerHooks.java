package ru.gazprombank.automation.akitagpb.modules.core.hooks;

import static ru.gazprombank.automation.akitagpb.modules.core.helpers.DockerHelper.setEnv;

import io.cucumber.java.AfterAll;
import io.cucumber.java.BeforeAll;
import java.util.HashMap;
import java.util.Map;
import ru.gazprombank.automation.akitagpb.modules.core.helpers.DockerHelper;

public class DockerHooks {

  @BeforeAll
  public static void startDockerCompose() throws Exception {
    if (System.getProperty("dockerCompose") != null) {
      Map<String, String> map = new HashMap<>();
      map.put("TESTCONTAINERS_RYUK_DISABLED", "true");
      setEnv(map);
      DockerHelper.env.start();
    }
  }

  @AfterAll
  public static void stopDockerCompose() {
    if (System.getProperty("dockerCompose") != null) {
      DockerHelper.env.stop();
    }
  }
}
