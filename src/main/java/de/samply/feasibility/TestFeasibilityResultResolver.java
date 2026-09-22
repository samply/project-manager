package de.samply.feasibility;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.samply.form.DataType;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Random;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves a TEST_FEASIBILITY_RESULT template into a JsonNode by substituting
 * each {{DATA_TYPE}} placeholder (e.g. {{INTEGER}}) with an independently
 * random value of that de.samply.form.DataType, formatted as a valid JSON
 * literal (quoted for string-like types, unquoted otherwise) so the template
 * author never has to add quotes themselves.
 * <p>
 * Deliberately not ${...} or #{...}: environment files such as
 * development.env are loaded by tools (e.g. the IntelliJ EnvFile plugin, see
 * ROOT_DIRECTORY) that already perform their own ${VAR} substitution against
 * other environment variables, and some .env-style parsers treat a bare #
 * as a comment marker even mid-line - both would risk swallowing or
 * truncating the placeholder before it ever reaches this resolver.
 */
class TestFeasibilityResultResolver {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{(\\w+)}}");
    private static final int RANDOM_DAYS_RANGE = 365;

    private final ObjectMapper objectMapper;
    private final Random random;

    TestFeasibilityResultResolver(ObjectMapper objectMapper, Random random) {
        this.objectMapper = objectMapper;
        this.random = random;
    }

    JsonNode resolve(String template) {
        Matcher matcher = PLACEHOLDER.matcher(template);
        StringBuilder resolved = new StringBuilder();
        while (matcher.find()) {
            DataType dataType = DataType.fromString(matcher.group(1));
            matcher.appendReplacement(resolved, Matcher.quoteReplacement(randomLiteral(dataType)));
        }
        matcher.appendTail(resolved);

        try {
            return objectMapper.readTree(resolved.toString());
        } catch (Exception exception) {
            throw new FeasibilityServiceException(
                    "TEST_FEASIBILITY_RESULT did not resolve to valid JSON: " + resolved, exception);
        }
    }

    private String randomLiteral(DataType dataType) {
        return switch (dataType) {
            case INTEGER -> String.valueOf(random.nextInt(100_000));
            case DOUBLE -> String.valueOf(Math.round(random.nextDouble() * 100_000d) / 100d);
            case BOOLEAN -> String.valueOf(random.nextBoolean());
            case STRING, LONG_STRING, ENUM -> quoted("test-" + UUID.randomUUID());
            case DATE -> quoted(LocalDate.now().minusDays(random.nextInt(RANDOM_DAYS_RANGE)).toString());
            case TIMESTAMP -> quoted(Instant.now()
                    .minus(random.nextInt(RANDOM_DAYS_RANGE), ChronoUnit.DAYS).toString());
            case LOCAL_DATE_TIME -> quoted(LocalDateTime.now()
                    .minusDays(random.nextInt(RANDOM_DAYS_RANGE)).toString());
        };
    }

    private String quoted(String value) {
        return "\"" + value + "\"";
    }
}
