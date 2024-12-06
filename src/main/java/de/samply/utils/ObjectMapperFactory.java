package de.samply.utils;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import de.samply.app.ProjectManagerConst;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Component
public class ObjectMapperFactory {

    private final String instantPattern;
    private final String localDatePattern;

    public ObjectMapperFactory(
            @Value(ProjectManagerConst.FRONTEND_INSTANT_PATTERN_SV) String instantPattern,
            @Value(ProjectManagerConst.FRONTEND_LOCAL_DATE_PATTERN_SV) String localDatePattern) {
        this.instantPattern = instantPattern;
        this.localDatePattern = localDatePattern;
    }

    public ObjectMapper newInstance() {
        return new ObjectMapper()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .registerModule(customJavaTimeModule())
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false); // Avoid timestamps
    }

    private JavaTimeModule customJavaTimeModule() {
        JavaTimeModule module = new JavaTimeModule();

        DateTimeFormatter localDateFormatter = DateTimeFormatter.ofPattern(localDatePattern);
        module.addSerializer(LocalDate.class, new LocalDateSerializer(localDateFormatter));

        DateTimeFormatter instantFormatter = DateTimeFormatter.ofPattern(instantPattern).withZone(ZoneId.of("UTC"));
        module.addSerializer(Instant.class, new StdSerializer<>(Instant.class) {
            @Override
            public void serialize(Instant value, JsonGenerator gen, SerializerProvider provider) throws IOException {
                gen.writeString(instantFormatter.format(value));
            }
        });

        return module;
    }
}
