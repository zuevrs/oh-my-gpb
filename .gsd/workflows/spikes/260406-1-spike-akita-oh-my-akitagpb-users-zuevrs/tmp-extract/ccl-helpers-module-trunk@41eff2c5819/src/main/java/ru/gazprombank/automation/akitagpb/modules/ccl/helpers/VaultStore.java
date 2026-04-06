package ru.gazprombank.automation.akitagpb.modules.ccl.helpers;

import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.SneakyThrows;
import ru.gazprombank.automation.akitagpb.modules.core.helpers.ConfigLoader;

/** Класс для подключения к Vault */
public class VaultStore {

  private static VaultStore vaultStore;
  private Vault vault;
  private Map<String, String> vaultVars = new HashMap<>();

  private VaultStore() {}

  public static VaultStore getInstance() {
    if (vaultStore == null) {
      vaultStore = new VaultStore();
      vaultStore.init();
    }
    return vaultStore;
  }

  public Map<String, String> getVaultVars() {
    return vaultVars;
  }

  /** Метод для инициализации конфига подключения к Vault */
  @SneakyThrows
  private void init() {
    VaultConfig vaultConfig =
        new VaultConfig()
            .address(System.getenv("vault_url"))
            .token(System.getenv("vault_token"))
            .openTimeout(10)
            .readTimeout(10)
            .engineVersion(getEngineVersion())
            .build();

    vault = new Vault(vaultConfig);

    // Получаем переменные из указанных путей и сохраняем в хранилище
    List<String> vaultPathList =
        ConfigLoader.getConfigValueOrDefault("vault.path.list", new ArrayList<>());
    vaultPathList.forEach(
        path -> vaultVars.putAll(VaultStore.getInstance().getAllVarsFromPath(path)));
  }

  /**
   * Метод возвращает значение креды
   *
   * @param path путь до кредов в Vault
   * @param credName имя креды в Vault
   * @return значение креды
   */
  public String getVaultCertValue(String path, String credName) {
    try {
      return vault.logical().read(path).getData().get(credName);
    } catch (VaultException ex) {
      throw new RuntimeException(
          "Не смогли получить значение из Vault. Приччина: " + ex.getMessage());
    }
  }

  /**
   * Метод возвращает все значения из vault из указанного пути
   *
   * @param path путь до кредов в Vault
   * @return креды
   */
  public Map<String, String> getAllVarsFromPath(String path) {
    try {
      return vault.logical().read(path).getData();
    } catch (VaultException ex) {
      throw new RuntimeException(
          "Не смогли получить значения из Vault. Приччина: " + ex.getMessage());
    }
  }

  private int getEngineVersion() {
    return ConfigLoader.getConfigValueOrDefault("vault.path.version", 1);
  }
}
