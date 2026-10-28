package de.samply.aop;

import de.samply.annotations.Bridgehead;
import de.samply.annotations.Message;
import de.samply.annotations.ProjectCode;
import de.samply.db.model.Project;
import de.samply.db.model.ProjectBridgehead;
import de.samply.notification.NotificationService;
import de.samply.notification.OperationType;
import de.samply.security.SessionUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.http.ResponseEntity;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// The message of an action is stored as a notification of the request, with the session user as its author
class MessageNotificationAspectTest {

    private final NotificationService notificationService = mock(NotificationService.class);
    private Endpoints endpoints;
    private Project project;

    @BeforeEach
    void setUp() {
        SessionUser sessionUser = mock(SessionUser.class);
        when(sessionUser.getEmail()).thenReturn("committee@example.org");
        AspectJProxyFactory factory = new AspectJProxyFactory(new Endpoints());
        factory.setProxyTargetClass(true);
        factory.addAspect(new MessageNotificationAspect(notificationService, sessionUser));
        endpoints = factory.getProxy();

        project = new Project();
        project.setCode("REQ-1");
    }

    @Test
    void storesTheMessageOfASuccessfulAction() throws Exception {
        endpoints.requestChanges(project, "Please describe the cohort");

        verify(notificationService).createNotification(project, null, "committee@example.org", OperationType.MESSAGE,
                "Please describe the cohort", null, null);
    }

    @Test
    void storesTheBridgeheadOfTheAction() throws Exception {
        ProjectBridgehead bridgehead = new ProjectBridgehead();
        bridgehead.setBridgehead("jurassic-park");

        endpoints.reject(project, bridgehead, "Not enough patients");

        verify(notificationService).createNotification(project, "jurassic-park", "committee@example.org",
                OperationType.MESSAGE, "Not enough patients", null, null);
    }

    @Test
    void storesNothingWithoutAMessage() throws Exception {
        endpoints.requestChanges(project, " ");
        endpoints.requestChanges(project, null);

        verify(notificationService, never()).createNotification(any(), any(), any(), any(), any(), isNull(), isNull());
    }

    @Test
    void storesNothingWhenTheActionFailed() throws Exception {
        endpoints.fail(project, "Please describe the cohort");

        verify(notificationService, never()).createNotification(any(), any(), any(), any(), any(), isNull(), isNull());
    }

    static class Endpoints {

        public ResponseEntity<Void> requestChanges(@ProjectCode Project project, @Message String message) {
            return ResponseEntity.ok().build();
        }

        public ResponseEntity<Void> reject(@ProjectCode Project project, @Bridgehead ProjectBridgehead bridgehead,
                                           @Message String message) {
            return ResponseEntity.ok().build();
        }

        public ResponseEntity<Void> fail(@ProjectCode Project project, @Message String message) {
            return ResponseEntity.badRequest().build();
        }

    }

}
