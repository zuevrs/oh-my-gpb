package ru.gazprombank.automation.akitagpb.modules.ccl.helpers;

import static ru.gazprombank.automation.akitagpb.modules.ccl.helpers.StringHelper.getRegexpGroupValue;

import com.ibm.icu.text.DecimalFormatSymbols;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

public class NumberHelper {

  /**
   * Метод для преобразования Double значений из вида 0.00Е3 в обычную запись (как в Camunda
   * Cockpit).
   *
   * @param doubleString строковая запись числа.
   * @return String преобразованная запись числа.
   */
  public static String parseDouble(String doubleString) {
    if (doubleString.toLowerCase().contains("e")) {
      DecimalFormat decimalFormat = (DecimalFormat) NumberFormat.getNumberInstance(Locale.ENGLISH);
      decimalFormat.setGroupingUsed(false);
      decimalFormat.setMaximumFractionDigits(
          Integer.parseInt(doubleString.substring(doubleString.toLowerCase().indexOf("e") + 1)));
      return decimalFormat.format(Double.parseDouble(doubleString));
    } else {
      return doubleString;
    }
  }

  /**
   * Форматировать вид числа Примеры: - формат "#": число 12345 -> "12345"; число 123.1 -> "123";
   * число 0.5 -> "0" - формат "#00": число 12345 -> "12345"; число 12 -> "12"; число 1 -> "01";
   * число 123.1 -> "123" - формат "# ###": число 12345 -> "12 345"; число 123 -> "123" - формат
   * "#.##": число 123 -> "123"; число 1.234 -> "1.23"; число 1.235 -> "1.24"; число 0.55 -> "0.55"
   * - формат "#,00": число 123 -> "123,00"; число 0.23 -> ",23" (без нуля) - формат "# ##0,00":
   * число 12345 -> "12 345,00"; число 0.23 -> "0,23"
   *
   * @param number число для форматирования
   * @param format формат
   * @return отформатированное число
   */
  public static String formatNumber(Number number, String format) {
    com.ibm.icu.text.DecimalFormat decimalFormat =
        (com.ibm.icu.text.DecimalFormat)
            com.ibm.icu.text.DecimalFormat.getInstance(Locale.forLanguageTag("ru"));
    var decimalDelimiter = getRegexpGroupValue(format, "[#\\d\\s]+([^#\\d\\s])[#\\d]+", 1);
    if (decimalDelimiter != null) {
      format = format.replace(decimalDelimiter, ".");
      var symbols = DecimalFormatSymbols.getInstance(Locale.forLanguageTag("ru"));
      symbols.setDecimalSeparatorString(decimalDelimiter);
      decimalFormat.setDecimalFormatSymbols(symbols);
    }
    format = format.replace(" ", ",");
    decimalFormat.applyPattern(format);
    return decimalFormat.format(number).replace(" ", " ");
  }
}
