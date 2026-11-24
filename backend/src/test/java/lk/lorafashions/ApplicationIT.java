package lk.lorafashions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasLength;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import lk.lorafashions.shared.web.RequestIdFilter;
import lk.lorafashions.support.IntegrationTestBase;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/** Boots the whole application against real PostgreSQL and MongoDB. */
class ApplicationIT extends IntegrationTestBase {

  @Autowired private MockMvc mockMvc;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private MongoTemplate mongoTemplate;

  @Test
  void flywayAppliedTheEventPublicationMigration() {
    Integer applied =
        jdbcTemplate.queryForObject(
            "select count(*) from flyway_schema_history"
                + " where success and script = 'V202611241300__shared_event_publication.sql'",
            Integer.class);
    assertThat(applied).isEqualTo(1);

    Integer tables =
        jdbcTemplate.queryForObject(
            "select count(*) from information_schema.tables"
                + " where table_schema = 'public' and table_name = 'event_publication'",
            Integer.class);
    assertThat(tables).isEqualTo(1);
  }

  @Test
  void mongoDbIsReachable() {
    Document result = mongoTemplate.executeCommand("{ ping: 1 }");
    assertThat(result.get("ok")).isIn(1, 1.0);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {"/actuator/health", "/actuator/health/liveness", "/actuator/health/readiness"})
  void healthEndpointsReportUpWithoutDetails(String path) throws Exception {
    // The root endpoint also lists the probe group names; nothing else may be exposed.
    mockMvc
        .perform(get(path))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"))
        .andExpect(jsonPath("$.components").doesNotExist())
        .andExpect(jsonPath("$.details").doesNotExist())
        .andExpect(jsonPath("$.*", hasSize(lessThanOrEqualTo(2))));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "/actuator",
        "/actuator/env",
        "/actuator/beans",
        "/actuator/metrics",
        "/actuator/configprops",
        "/actuator/loggers",
        "/actuator/heapdump",
        "/actuator/info",
        "/actuator/modulith"
      })
  void otherActuatorEndpointsAreClosed(String path) throws Exception {
    mockMvc.perform(get(path)).andExpect(status().isNotFound());
  }

  @Test
  void responsesCarryARequestId() throws Exception {
    mockMvc
        .perform(get("/actuator/health").header(RequestIdFilter.HEADER, "client-id-123"))
        .andExpect(header().string(RequestIdFilter.HEADER, "client-id-123"));

    mockMvc
        .perform(get("/actuator/health"))
        .andExpect(header().string(RequestIdFilter.HEADER, hasLength(36)));
  }

  @Test
  void errorResponsesDoNotExposeInternals() throws Exception {
    String body =
        mockMvc
            .perform(get("/does-not-exist"))
            .andExpect(status().isNotFound())
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertThat(body).doesNotContain("trace", "exception", "Exception", "org.springframework");
  }
}
