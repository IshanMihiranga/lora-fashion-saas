/**
 * Shared kernel: building blocks every module may use (CLAUDE.md §5).
 *
 * <p>Declared as an open module so its sub-packages are usable by all modules. It must never depend
 * on any other module.
 */
@ApplicationModule(displayName = "Shared kernel", type = ApplicationModule.Type.OPEN)
package lk.lorafashions.shared;

import org.springframework.modulith.ApplicationModule;
