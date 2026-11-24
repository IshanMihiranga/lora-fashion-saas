package lk.lorafashions.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.mongodb.MongoDBContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Real PostgreSQL and MongoDB containers for integration tests. Image versions come from the Maven
 * build ({@code test.postgres.image}, {@code test.mongo.image}) and match {@code
 * infra/docker-compose.yml}. Containers live as long as the cached Spring context.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

  @Bean
  @ServiceConnection
  PostgreSQLContainer postgresContainer() {
    return new PostgreSQLContainer(
        DockerImageName.parse(System.getProperty("test.postgres.image", "postgres:17.12")));
  }

  @Bean
  @ServiceConnection
  MongoDBContainer mongoContainer() {
    return new MongoDBContainer(
        DockerImageName.parse(System.getProperty("test.mongo.image", "mongo:8.0.38")));
  }
}
