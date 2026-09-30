package de.samply.frontend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import de.samply.notification.OperationType;
import de.samply.project.state.ProjectState;
import org.springframework.http.HttpStatus;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record Notification(
        Long id,
        String email,
        // Full name of the user behind the email, if known
        String userName,
        Instant timestamp,
        String projectCode,
        // Current phase of the request, not the phase at the time of the notification
        ProjectState projectState,
        String bridgehead,
        String humanReadableBridgehead,
        OperationType operationType,
        String details,
        String error,
        HttpStatus httpStatus,
        Boolean read
) {
}
