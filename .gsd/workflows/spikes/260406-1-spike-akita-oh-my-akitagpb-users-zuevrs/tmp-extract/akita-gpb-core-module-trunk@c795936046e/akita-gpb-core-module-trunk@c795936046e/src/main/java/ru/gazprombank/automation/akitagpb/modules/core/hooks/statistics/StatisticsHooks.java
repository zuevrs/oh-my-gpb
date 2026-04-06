package ru.gazprombank.automation.akitagpb.modules.core.hooks.statistics;

import static ru.gazprombank.automation.akitagpb.modules.core.cucumber.ScopedVariables.resolveVars;

import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import io.cucumber.java.AfterAll;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import ru.gazprombank.automation.akitagpb.modules.core.helpers.ConfigLoader;
import ru.gazprombank.automation.akitagpb.modules.core.hooks.statistics.dto.MetricDto;
import ru.gazprombank.automation.akitagpb.modules.core.hooks.statistics.dto.StatisticsDto;
import ru.gazprombank.automation.akitagpb.modules.core.steps.BaseMethods;

public class StatisticsHooks {

  private static StatisticsHooks instance;
  private static int teamcityCount = 0;
  private static int bitbucketCount = 0;
  private static int e2eUiTagsCount = 0;
  private static int e2eMixTagsCount = 0;
  private static int e2eApiTagsCount = 0;
  private static int componentApiIntegrationTagsCount = 0;
  private static int componentApiAutonomicTagsCount = 0;
  private static String APP_NAME = "";
  private static final TreeSet<String> endpoint = new TreeSet<>();
  private static final Boolean turnOff =
      ConfigLoader.getConfigValueOrDefault("statistics.off", false);

  private StatisticsHooks() {}

  public static StatisticsHooks getInstance() {
    if (instance == null) {
      instance = new StatisticsHooks();
    }
    return instance;
  }

  public void addEndpoint(String url) {
    URI uri;
    try {
      uri = new URI(url);
      endpoint.add(StringUtils.removeEnd(uri.getPath(), "/"));
    } catch (URISyntaxException e) {
      System.out.println(e.getMessage());
    }
  }

  @Before
  public static void addTags(Scenario scenario) {
    if (turnOff) return;
    teamcityCount++;
    List<String> tagList =
        scenario.getSourceTagNames().stream().map(String::toLowerCase).collect(Collectors.toList());
    Arrays.stream(Tags.values())
        .forEach(
            tag -> {
              if (tagList.contains(tag.getName().toLowerCase())) {
                tag.increaseTagCount();
              }
            });
    APP_NAME =
        "@" + BaseMethods.getInstance().getPropertyOrStringVariableOrValue(resolveVars("app_name"));
  }

  @AfterAll
  public static void generateStatistic() throws IOException, InterruptedException {
    if (turnOff) return;
    StatisticsDto statisticsDto = new StatisticsDto();
    List<MetricDto> metricDtoList = new ArrayList<>();
    DefaultPrettyPrinter prettyPrinter = new DefaultPrettyPrinter();
    prettyPrinter.indentArraysWith(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE);
    ObjectWriter ow = new ObjectMapper().findAndRegisterModules().writer(prettyPrinter);

    statisticsDto.setReportDate(
        LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH.mm.ss.SSSSSS")));
    statisticsDto.setAutoSystem(getStatProp("auto_system"));
    statisticsDto.setAppName(getStatProp("app_name"));
    statisticsDto.setAppVersion(getStatProp("app_version"));
    statisticsDto.setEnvName(getStatProp("env_name"));
    statisticsDto.setReportType("e2e");
    //    gitGrep(Paths.get(System.getProperty("user.dir")));
    metricDtoList.add(new MetricDto("count", String.valueOf(teamcityCount)));
    //    if (bitbucketCount > 0)
    //      metricDtoList.add(new MetricDto("bitbucketCount", String.valueOf(bitbucketCount)));
    metricDtoList.add(new MetricDto("e2eUi", String.valueOf(e2eUiTagsCount)));
    metricDtoList.add(new MetricDto("e2eMix", String.valueOf(e2eMixTagsCount)));
    metricDtoList.add(new MetricDto("e2eApi", String.valueOf(e2eApiTagsCount)));
    metricDtoList.add(
        new MetricDto("componentApiIntegration", String.valueOf(componentApiIntegrationTagsCount)));
    metricDtoList.add(
        new MetricDto("componentApiAutonomic", String.valueOf(componentApiAutonomicTagsCount)));

    endpoint.forEach(tag -> metricDtoList.add(new MetricDto(tag)));
    statisticsDto.setMetricsList(metricDtoList);

    try {
      ow.writeValue(
          new File(
              FilenameUtils.normalize(
                  System.getProperty("user.dir") + "/src/main/resources/statistic.json")),
          statisticsDto);
    } catch (IOException e) {
      System.out.println(e.getMessage());
    }
  }

  private static String getStatProp(String propName) {
    return FilenameUtils.normalize(
        System.getProperty(propName) == null ? "" : System.getProperty(propName));
  }

  public static void gitGrep(Path directory) throws IOException, InterruptedException {
    runCommand(directory, "git", "grep", APP_NAME);
  }

  public static void runCommand(Path directory, String... command)
      throws IOException, InterruptedException {
    Objects.requireNonNull(directory, "directory");
    if (!Files.exists(directory)) {
      throw new RuntimeException("can't run command in non-existing directory '" + directory + "'");
    }
    ProcessBuilder pb = new ProcessBuilder().command(command).directory(directory.toFile());
    Process p = pb.start();
    StreamGobbler outputGobbler = new StreamGobbler(p.getInputStream(), "OUTPUT");
    outputGobbler.start();
    int exit = p.waitFor();
    outputGobbler.join();
  }

  private static class StreamGobbler extends Thread {

    private final InputStream is;
    private final String type;

    private StreamGobbler(InputStream is, String type) {
      this.is = is;
      this.type = type;
    }

    @Override
    public void run() {
      try (BufferedReader br = new BufferedReader(new InputStreamReader(is)); ) {
        String line;

        while ((line = br.readLine()) != null) {
          bitbucketCount++;
        }
      } catch (IOException ioe) {
        ioe.printStackTrace();
      }
    }
  }

  private enum Tags {
    UI("@e2eUi") {
      public void increaseTagCount() {
        e2eUiTagsCount++;
      }
    },
    MIX("@e2eMix") {
      public void increaseTagCount() {
        e2eMixTagsCount++;
      }
    },
    API("@e2eApi") {
      public void increaseTagCount() {
        e2eApiTagsCount++;
      }
    },
    COMP_INT("@componentApiIntegration") {
      public void increaseTagCount() {
        componentApiIntegrationTagsCount++;
      }
    },
    COMP_SC("@componentApiAutonomic") {
      public void increaseTagCount() {
        componentApiAutonomicTagsCount++;
      }
    };

    private final String name;

    Tags(String name) {
      this.name = name;
    }

    private String getName() {
      return this.name;
    }

    public abstract void increaseTagCount();
  }
}
