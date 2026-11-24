package lk.lorafashions;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/** Verifies module boundaries and the absence of cycles (CLAUDE.md §5, rule 4). */
class ModularityTest {

  private final ApplicationModules modules = ApplicationModules.of(Application.class);

  @Test
  void moduleBoundariesAreRespected() {
    modules.verify();
  }

  @Test
  void sharedKernelIsDetectedAsModule() {
    assertThat(modules.getModuleByName("shared")).isPresent();
  }
}
