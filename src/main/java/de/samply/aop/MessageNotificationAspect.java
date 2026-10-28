package de.samply.aop;

import de.samply.db.model.Project;
import de.samply.db.model.ProjectBridgehead;
import de.samply.notification.NotificationService;
import de.samply.notification.NotificationServiceException;
import de.samply.notification.OperationType;
import de.samply.security.SessionUser;
import de.samply.utils.AspectUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Optional;

// The message of an action (reject, request changes, ...) is stored as a notification of the request, with its author.
// Not part of EMAILS: the message is kept also when no email is sent.
@Component
@Aspect
public class MessageNotificationAspect {

    private final NotificationService notificationService;
    private final SessionUser sessionUser;

    public MessageNotificationAspect(NotificationService notificationService, SessionUser sessionUser) {
        this.notificationService = notificationService;
        this.sessionUser = sessionUser;
    }

    @SuppressWarnings("EmptyMethod")
    @Pointcut("execution(* *(.., @de.samply.annotations.Message (*), ..))")
    public void messagePointcut() {
    }

    @Around("messagePointcut()")
    public Object storeMessage(ProceedingJoinPoint joinPoint) throws Throwable {
        Object result = joinPoint.proceed();
        if (result instanceof ResponseEntity<?> responseEntity && responseEntity.getStatusCode().is2xxSuccessful()) {
            storeMessageIfPresent(joinPoint);
        }
        return result;
    }

    private void storeMessageIfPresent(ProceedingJoinPoint joinPoint) throws NotificationServiceException {
        Optional<Project> project = AspectUtils.fetchProject(joinPoint);
        Optional<String> message = AspectUtils.fetchMessage(joinPoint).filter(StringUtils::hasText);
        if (project.isPresent() && message.isPresent()) {
            notificationService.createNotification(project.get(),
                    AspectUtils.fetchBridgehead(joinPoint).map(ProjectBridgehead::getBridgehead).orElse(null),
                    sessionUser.getEmail(), OperationType.MESSAGE, message.get(), null, null);
        }
    }

}
