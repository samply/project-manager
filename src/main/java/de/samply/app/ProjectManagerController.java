package de.samply.app;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import de.samply.annotations.Bridgehead;
import de.samply.annotations.ProjectName;
import de.samply.frontend.FrontendService;
import de.samply.project.event.ProjectEventService;
import de.samply.utils.ProjectVersion;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

@Controller
public class ProjectManagerController {

    private final String projectVersion = ProjectVersion.getProjectVersion();
    private final ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT)
            .registerModule(new JavaTimeModule()).configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    private final ProjectEventService projectEventService;
    private final FrontendService frontendService;

    public ProjectManagerController(ProjectEventService projectEventService, FrontendService frontendService) {
        this.projectEventService = projectEventService;
        this.frontendService = frontendService;
    }

    @GetMapping(value = ProjectManagerConst.INFO)
    public ResponseEntity<String> info() {
        return new ResponseEntity<>(projectVersion, HttpStatus.OK);
    }


    @GetMapping(value = ProjectManagerConst.ACTIONS, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> fetchActions(
            @ProjectName @RequestParam(name = ProjectManagerConst.PROJECT_NAME, required = false) String projectName,
            @Bridgehead @RequestParam(name = ProjectManagerConst.BRIDGEHEAD, required = false) String bridgehead,
            @RequestParam(name = ProjectManagerConst.SITE) String site
    ) {
        return convertToResponseEntity(
                this.frontendService.fetchModuleActionPackage(site, Optional.ofNullable(projectName), Optional.ofNullable(bridgehead)));
    }

    private <T, R> ResponseEntity convertToResponseEntity(T input, Function<T, R> function) {
        return convertToResponseEntity(() -> function.apply(input));
    }

    private <T> ResponseEntity convertToResponseEntity(Supplier<T> supplier) {
        return convertToResponseEntity(() -> supplier.get());
    }

    private <T> ResponseEntity convertToResponseEntity(T result) {
        try {
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
