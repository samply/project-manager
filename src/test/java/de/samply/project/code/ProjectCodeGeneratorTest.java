package de.samply.project.code;

import de.samply.db.repository.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProjectCodeGeneratorTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-24T13:14:15Z"), ZoneOffset.UTC);

    private ProjectRepository projects;

    @BeforeEach
    void setUp() {
        projects = mock(ProjectRepository.class);
        when(projects.existsByCode(anyString())).thenReturn(false);
    }

    private ProjectCodeGenerator generator(String template) {
        return new ProjectCodeGenerator(template, projects, CLOCK, new SecureRandom());
    }

    @Test
    void expandsDateAndRandomTokens() {
        String result = generator("REQ-{{YEAR}}-{{MONTH}}{{DAY}}-{{HOUR}}{{MINUTE}}{{SECOND}}-{{NUMBER:5}}-{{STRING:8}}")
                .generate();

        assertThat(result).matches("REQ-2026-0924-131415-[0-9]{5}-[0-9a-f]{8}");
    }

    @Test
    void usesLegacyFormatForBlankTemplate() {
        assertThat(generator("").generate()).matches("[0-9a-f]{20}");
        assertThat(generator(null).generate()).matches("[0-9a-f]{20}");
    }

    @Test
    void padsSequenceAndKeepsGrowingBeyondWidth() {
        when(projects.nextProjectCodeSequenceValue()).thenReturn(42L, 123456L);
        ProjectCodeGenerator generator = generator("REQ-{{YEAR}}-{{SEQUENCE:5}}");

        assertThat(generator.generate()).isEqualTo("REQ-2026-00042");
        assertThat(generator.generate()).isEqualTo("REQ-2026-123456");
    }

    @Test
    void doesNotConsumeSequenceWhenTemplateHasNoSequenceToken() {
        generator("REQ-{{STRING:8}}").generate();

        verify(projects, never()).nextProjectCodeSequenceValue();
    }

    @Test
    void retriesWhenGeneratedCodeAlreadyExists() {
        when(projects.nextProjectCodeSequenceValue()).thenReturn(1L, 2L);
        when(projects.existsByCode("REQ-1")).thenReturn(true);

        assertThat(generator("REQ-{{SEQUENCE:1}}").generate()).isEqualTo("REQ-2");
    }

    @Test
    void appendsSequenceSuffixWhenEveryAttemptExists() {
        when(projects.existsByCode(anyString())).thenAnswer(call -> !call.getArgument(0, String.class).matches("REQ-[0-9]-8"));
        when(projects.nextProjectCodeSequenceValue()).thenReturn(7L, 8L);

        assertThat(generator("REQ-{{NUMBER:1}}").generate()).matches("REQ-[0-9]-8");
        verify(projects, times(ProjectCodeGenerator.MAX_ATTEMPTS + 2)).existsByCode(anyString());
    }

    @Test
    void rejectsUnknownAndMalformedTokens() {
        assertThatIllegalArgumentException().isThrownBy(() -> generator("REQ-{{UNKNOWN}}-{{STRING:8}}"));
        assertThatIllegalArgumentException().isThrownBy(() -> generator("REQ-{{NUMBER:x}}"));
        assertThatIllegalArgumentException().isThrownBy(() -> generator("REQ-{{NUMBER:0}}"));
        assertThatIllegalArgumentException().isThrownBy(() -> generator("REQ-{{NUMBER}}"));
        assertThatIllegalArgumentException().isThrownBy(() -> generator("REQ-{{STRING:65}}"));
        assertThatIllegalArgumentException().isThrownBy(() -> generator("REQ-{{STRING:99999999999}}"));
        assertThatIllegalArgumentException().isThrownBy(() -> generator("REQ-{{YEAR:4}}-{{STRING:8}}"));
    }

    @Test
    void rejectsTemplateWithoutDistinguishingToken() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> generator("REQ-{{YEAR}}{{MONTH}}{{DAY}}"))
                .withMessageContaining("at least one {{SEQUENCE:n}}, {{NUMBER:n}} or {{STRING:n}} token");
    }

    @Test
    void acceptsUrlSafeLiteralCharacters() {
        assertThat(generator("REQ_{{YEAR}}-{{STRING:8}}").generate()).matches("REQ_2026-[0-9a-f]{8}");
    }

    @Test
    void explainsUnsafeLiteralCharacters() {
        assertThatIllegalArgumentException().isThrownBy(() -> generator("REQ {{STRING:8}}"));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> generator("REQ/{{STRING:8}}"))
                .withMessageContaining("literal text 'REQ/' contains unsupported character(s) '/'")
                .withMessageContaining("because project codes are used as URL path segments");
    }
}
