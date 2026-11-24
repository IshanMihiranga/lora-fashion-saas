package lk.lorafashions.shared.config;

import java.util.List;
import java.util.Map;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.StringUtils;

/**
 * Stops start-up with a clear message when a required setting for the active profile is missing.
 *
 * <p>Runs after configuration files (and the local {@code .env}) are loaded. Only setting
 * <em>names</em> are reported, never values.
 */
public final class RequiredSettingsCheck implements EnvironmentPostProcessor, Ordered {

  private static final List<String> CLOUD_SETTINGS =
      List.of("APP_DB_URL", "APP_DB_USERNAME", "APP_DB_PASSWORD", "APP_MONGO_URI");

  private static final Map<String, List<String>> REQUIRED_BY_PROFILE =
      Map.of(
          "local",
          List.of(
              "LOCAL_POSTGRES_PORT",
              "LOCAL_POSTGRES_DB",
              "LOCAL_POSTGRES_USER",
              "LOCAL_POSTGRES_PASSWORD",
              "LOCAL_MONGO_PORT",
              "LOCAL_MONGO_DB",
              "LOCAL_MONGO_ROOT_USER",
              "LOCAL_MONGO_ROOT_PASSWORD"),
          "dev",
          CLOUD_SETTINGS,
          "prod",
          CLOUD_SETTINGS);

  private static final Map<String, String> HINT_BY_PROFILE =
      Map.of(
          "local", "Create the repository .env from .env.example (cp .env.example .env).",
          "dev", "Set them as environment variables.",
          "prod", "Set them as environment variables.");

  @Override
  public void postProcessEnvironment(
      ConfigurableEnvironment environment, SpringApplication application) {
    for (String profile : environment.getActiveProfiles()) {
      List<String> required = REQUIRED_BY_PROFILE.get(profile);
      if (required == null) {
        continue;
      }
      List<String> missing =
          required.stream()
              .filter(key -> !StringUtils.hasText(environment.getProperty(key)))
              .toList();
      if (!missing.isEmpty()) {
        throw new IllegalStateException(
            "Missing required settings for profile '%s': %s. %s"
                .formatted(profile, String.join(", ", missing), HINT_BY_PROFILE.get(profile)));
      }
    }
  }

  @Override
  public int getOrder() {
    return Ordered.LOWEST_PRECEDENCE;
  }
}
