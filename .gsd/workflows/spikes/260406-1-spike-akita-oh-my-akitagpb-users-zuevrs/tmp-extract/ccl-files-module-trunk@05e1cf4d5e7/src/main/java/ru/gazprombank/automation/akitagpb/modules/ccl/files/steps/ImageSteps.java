package ru.gazprombank.automation.akitagpb.modules.ccl.files.steps;

import static io.qameta.allure.Allure.label;
import static ru.gazprombank.automation.akitagpb.modules.ccl.helpers.StringHelper.processValue;
import static ru.yandex.qatools.ashot.util.ImageTool.toByteArray;

import com.epam.reportportal.listeners.ItemStatus;
import com.epam.reportportal.service.Launch;
import com.epam.reportportal.service.step.StepReporter;
import io.cucumber.java.ru.И;
import io.qameta.allure.Allure;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import javax.imageio.ImageIO;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import ru.gazprombank.automation.akitagpb.modules.core.steps.BaseMethods;
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.qatools.ashot.comparison.ImageDiff;
import ru.yandex.qatools.ashot.comparison.ImageDiffer;

public class ImageSteps extends BaseMethods {

  /**
   * Шаг сравнения изображений
   *
   * @param actualScreenName имя переменной текущего скриншота
   * @param expectedScreenName имя переменной эталонного скриншота
   */
  @SneakyThrows
  @И("^сравнить изображение \"(.*)\" с \"(.*)\"$")
  public void compareImage(String actualScreenName, String expectedScreenName) {
    BufferedImage actual = (BufferedImage) akitaScenario.getVar(actualScreenName);
    Screenshot actualScreen = new Screenshot(actual);

    BufferedImage expected = (BufferedImage) akitaScenario.getVar(expectedScreenName);
    Screenshot expectedScreen = new Screenshot(expected);

    ImageDiff diff =
        new ImageDiffer().withIgnoredColor(Color.magenta).makeDiff(expectedScreen, actualScreen);
    boolean hasDiff = diff.hasDiff();
    BufferedImage diffImage = diff.getMarkedImage();

    if (hasDiff) {
      label("testType", "screenshotDiff");
      // Прикладываем скриншоты в allure
      Allure.attachment("diff", new ByteArrayInputStream(toByteArray(diffImage)));
      Allure.attachment("actual", new ByteArrayInputStream(toByteArray(actual)));
      Allure.attachment("expected", new ByteArrayInputStream(toByteArray(expected)));

      // Прикладываем скриншоты в Report Portal
      Launch launch = Launch.currentLaunch();
      if (launch != null) {
        StepReporter stepReporter = launch.getStepReporter();
        File diffFile = new File("diff.png");
        File actualFile = new File("actual.png");
        File expectedFile = new File("expected.png");

        ImageIO.write(diffImage, "png", diffFile);
        ImageIO.write(actual, "png", actualFile);
        ImageIO.write(expected, "png", expectedFile);

        stepReporter.sendStep(ItemStatus.PASSED, "Текущий скриншот", actualFile);
        stepReporter.sendStep(ItemStatus.PASSED, "Эталонный скриншот", expectedFile);
        stepReporter.sendStep(ItemStatus.FAILED, "Разница скриншотов", diffFile);
      }
      Assertions.fail("Скриншоты отличаются");
    }
  }

  /**
   * Шаг сохранения изображения в хранилище
   *
   * @param filePath путь до файла изображения
   * @param imageName имя переменной сохраняемой в хранилище
   */
  @SneakyThrows
  @И("^получить изображение по пути \"(.*)\" и сохранить в переменную \"(.*)\"$")
  public void loadScreenShot(String filePath, String imageName) {
    filePath = processValue(filePath);
    File imageFile = new File(filePath);
    BufferedImage image = ImageIO.read(new File(filePath));

    Allure.attachment(imageFile.getName(), new ByteArrayInputStream(toByteArray(image)));
    akitaScenario.setVar(imageName, image);
  }
}
