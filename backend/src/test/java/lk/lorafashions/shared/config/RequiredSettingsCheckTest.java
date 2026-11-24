package lk.lorafashions.shared.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.SpringApplication;
import org.springframework.mock.env.MockEnvironment;

class RequiredSettingsCheckTest {

  private final RequiredSettingsCheck check = new RequiredSettingsCheck();
  private final SpringApplication application = new SpringApplication();

  @ParameterizedTest
  @ValueSource(strings = {"dev", "prod"})
  void cloudProfilesNameEveryMissingSetting(String profile) {
    MockEnvironment environment = new MockEnvironment().withProperty("APP_DB_URL", "jdbc:x");
    environment.setActiveProfiles(profile);

    assertThatThrownBy(() -> check.postProcessEnvironment(environment, application))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("'" + profile + "'")
        .hasMessageContaining("APP_DB_USERNAME, APP_DB_PASSWORD, APP_MONGO_URI")
        .hasMessageNotContaining("APP_DB_URL,")
        .hasMessageNotContaining("jdbc:x");
  }

  @Test
  void blankValuesCountAsMissing() {
    MockEnvironment environment =
        new MockEnvironment()
            .withProperty("APP_DB_URL", "jdbc:x")
            .withProperty("APP_DB_USERNAME", "u")
            .withProperty("APP_DB_PASSWORD", "  ")
            .withProperty("APP_MONGO_URI", "mongodb://x");
    environment.setActiveProfiles("prod");

    assertThatThrownBy(() -> check.postProcessEnvironment(environment, application))
        .hasMessageContaining("APP_DB_PASSWORD");
  }

  @Test
  void cloudProfilePassesWhenAllSettingsPresent() {
    MockEnvironment environment =
        new MockEnvironment()
            .withProperty("APP_DB_URL", "jdbc:x")
            .withProperty("APP_DB_USERNAME", "u")
            .withProperty("APP_DB_PASSWORD", "p")
            .withProperty("APP_MONGO_URI", "mongodb://x");
    environment.setActiveProfiles("dev");

    assertThatCode(() -> check.postProcessEnvironment(environment, application))
        .doesNotThrowAnyException();
  }

  @Test
  void localProfileWithoutEnvFileExplainsHowToCreateIt() {
    MockEnvironment environment = new MockEnvironment();
    environment.setActiveProfiles("local");

    assertThatThrownBy(() -> check.postProcessEnvironment(environment, application))
        .hasMessageContaining("LOCAL_POSTGRES_PASSWORD")
        .hasMessageContaining("cp .env.example .env");
  }

  @Test
  void profilesWithoutRequirementsAreIgnored() {
    MockEnvironment environment = new MockEnvironment();
    environment.setActiveProfiles("test");

    assertThatCode(() -> check.postProcessEnvironment(environment, application))
        .doesNotThrowAnyException();
  }
}
