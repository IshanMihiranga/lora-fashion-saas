package lk.lorafashions;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.modulith.Modulithic;

/**
 * Lora Fashions modular monolith.
 *
 * <p>Each top-level package under {@code lk.lorafashions} is an application module. Module
 * boundaries are verified by {@code ModularityTest}; see {@code docs/architecture.md}.
 */
@SpringBootApplication
@Modulithic(systemName = "Lora Fashions", sharedModules = "shared")
public class Application {

  /**
   * Starts the application.
   *
   * @param args command-line arguments
   */
  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }
}
