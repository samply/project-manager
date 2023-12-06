package de.samply.app;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import de.samply.security.SessionUser;
import de.samply.utils.ProjectVersion;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.function.Function;
import java.util.function.Supplier;

@Controller
public class ProjectManagerController {

    private final String projectVersion = ProjectVersion.getProjectVersion();
    private ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT)
            .registerModule(new JavaTimeModule()).configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

    @Autowired
    private SessionUser sessionUser; // session scope user info

    @GetMapping(value = ProjectManagerConst.INFO)
    public ResponseEntity<String> info() {
        //Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return new ResponseEntity<>(projectVersion, HttpStatus.OK);
    }

    @GetMapping(value = ProjectManagerConst.ACTIONS, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> fetchActions(
            //        @RequestParam(name = ProjectManagerConst.QUERY_EXECUTION_ID) Long queryExecutionId
    ) {

        //TODO
        return ResponseEntity.ok("TODO: Return actions");
    }

    private <T, R> ResponseEntity convertToResponseEntity(T input, Function<T, R> function) {
        return convertToResponseEntity(() -> function.apply(input));
    }

    private <T> ResponseEntity convertToResponseEntity(Supplier<T> supplier) {
        try {
            T result = supplier.get();
            return (result != null) ? ResponseEntity.ok(objectMapper.writeValueAsString(result))
                    : ResponseEntity.notFound().build();
        } catch (JsonProcessingException e) {
            return createInternalServerError(e);
        }
    }

    private ResponseEntity createInternalServerError(Exception e) {
        return ResponseEntity.internalServerError().body(ExceptionUtils.getStackTrace(e));
    }


}
