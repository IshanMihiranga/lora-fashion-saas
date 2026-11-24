package lk.lorafashions.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;

/**
 * Base class for integration tests ({@code *IT}): full application context, MockMvc and real
 * PostgreSQL + MongoDB from Testcontainers. Subclasses share one cached context.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
public abstract class IntegrationTestBase {}
