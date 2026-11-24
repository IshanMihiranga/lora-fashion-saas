package lk.lorafashions.shared.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.servlet.ServletException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestIdFilterTest {

  private final RequestIdFilter filter = new RequestIdFilter();
  private final MockHttpServletResponse response = new MockHttpServletResponse();
  private final AtomicReference<String> idSeenByChain = new AtomicReference<>();
  private final MockFilterChain recordingChain =
      new MockFilterChain() {
        @Override
        public void doFilter(
            jakarta.servlet.ServletRequest request, jakarta.servlet.ServletResponse response) {
          idSeenByChain.set(MDC.get(RequestIdFilter.MDC_KEY));
        }
      };

  @AfterEach
  void clearMdc() {
    MDC.clear();
  }

  @Test
  void generatesIdWhenHeaderIsMissing() throws Exception {
    filter.doFilter(new MockHttpServletRequest(), response, recordingChain);

    String id = response.getHeader(RequestIdFilter.HEADER);
    assertThat(UUID.fromString(id)).isNotNull();
    assertThat(idSeenByChain.get()).isEqualTo(id);
  }

  @Test
  void reusesValidClientId() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(RequestIdFilter.HEADER, "abc-123-XYZ");

    filter.doFilter(request, response, recordingChain);

    assertThat(response.getHeader(RequestIdFilter.HEADER)).isEqualTo("abc-123-XYZ");
    assertThat(idSeenByChain.get()).isEqualTo("abc-123-XYZ");
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "",
        "has space",
        "line\nbreak",
        "<script>",
        "id;drop",
        "ünicode",
        "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
      })
  void replacesInvalidClientId(String invalid) throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(RequestIdFilter.HEADER, invalid);

    filter.doFilter(request, response, recordingChain);

    String id = response.getHeader(RequestIdFilter.HEADER);
    assertThat(id).isNotEqualTo(invalid);
    assertThat(UUID.fromString(id)).isNotNull();
  }

  @Test
  void acceptsIdOfExactlySixtyFourCharacters() {
    String max = "a".repeat(64);
    assertThat(RequestIdFilter.resolveRequestId(max)).isEqualTo(max);
  }

  @Test
  void clearsMdcAfterRequest() throws Exception {
    filter.doFilter(new MockHttpServletRequest(), response, recordingChain);

    assertThat(MDC.get(RequestIdFilter.MDC_KEY)).isNull();
  }

  @Test
  void clearsMdcWhenDownstreamFails() {
    MockFilterChain failingChain =
        new MockFilterChain() {
          @Override
          public void doFilter(
              jakarta.servlet.ServletRequest request, jakarta.servlet.ServletResponse response)
              throws ServletException {
            throw new ServletException("boom");
          }
        };

    assertThatThrownBy(() -> filter.doFilter(new MockHttpServletRequest(), response, failingChain))
        .isInstanceOf(ServletException.class);
    assertThat(MDC.get(RequestIdFilter.MDC_KEY)).isNull();
  }
}
