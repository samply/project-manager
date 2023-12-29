package de.samply.aop;

import de.samply.annotations.EmailSender;
import de.samply.annotations.EmailSenders;
import de.samply.db.model.Project;
import de.samply.db.model.ProjectBridgehead;
import de.samply.db.model.ProjectBridgeheadUser;
import de.samply.db.repository.*;
import de.samply.notification.smtp.EmailRecipient;
import de.samply.notification.smtp.EmailService;
import de.samply.notification.smtp.EmailServiceException;
import de.samply.security.SessionUser;
import de.samply.user.roles.OrganisationRoleToProjectRoleMapper;
import de.samply.user.roles.ProjectRole;
import de.samply.user.roles.UserProjectRoles;
import de.samply.utils.AspectUtils;
import de.samply.utils.ProjectRolesUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@Aspect
public class EmailSenderAspect {

    private final EmailService emailService;
    private final SessionUser sessionUser;
    private final BridgeheadAdminUserRepository bridgeheadAdminUserRepository;
    private final ProjectBridgeheadUserRepository projectBridgeheadUserRepository;
    private final ProjectManagerAdminUserRepository projectManagerAdminUserRepository;
    private final OrganisationRoleToProjectRoleMapper organisationRoleToProjectRoleMapper;
    private final ProjectBridgeheadRepository projectBridgeheadRepository;
    private final ProjectRepository projectRepository;

    public EmailSenderAspect(EmailService emailService,
                             SessionUser sessionUser,
                             BridgeheadAdminUserRepository bridgeheadAdminUserRepository,
                             ProjectBridgeheadUserRepository projectBridgeheadUserRepository,
                             ProjectManagerAdminUserRepository projectManagerAdminUserRepository,
                             OrganisationRoleToProjectRoleMapper organisationRoleToProjectRoleMapper,
                             ProjectBridgeheadRepository projectBridgeheadRepository,
                             ProjectRepository projectRepository) {
        this.emailService = emailService;
        this.sessionUser = sessionUser;
        this.bridgeheadAdminUserRepository = bridgeheadAdminUserRepository;
        this.projectBridgeheadUserRepository = projectBridgeheadUserRepository;
        this.projectManagerAdminUserRepository = projectManagerAdminUserRepository;
        this.organisationRoleToProjectRoleMapper = organisationRoleToProjectRoleMapper;
        this.projectBridgeheadRepository = projectBridgeheadRepository;
        this.projectRepository = projectRepository;
    }

    @Pointcut("@annotation(de.samply.annotations.EmailSenders)")
    public void emailSenderPointcut() {
    }

    @Around("emailSenderPointcut()")
    public Object aroundEmailSender(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            Optional<ResponseEntity> responseEntity = fetchResult(joinPoint);
            if (responseEntity.isPresent() && responseEntity.get().getStatusCode().is2xxSuccessful()) {
                sendEmail(joinPoint);
            }
            return responseEntity.get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Optional<ResponseEntity> fetchResult(ProceedingJoinPoint joinPoint) throws Throwable {
        Object result = joinPoint.proceed();
        if (result instanceof Optional) {
            Object result2 = ((Optional) result).get();
            if (result2 instanceof ResponseEntity) {
                return (Optional<ResponseEntity>) result;
            }
        } else if (result instanceof ResponseEntity) {
            return Optional.ofNullable((ResponseEntity) result);
        }
        return Optional.empty();
    }

    private void sendEmail(ProceedingJoinPoint joinPoint) {
        fetchEmailSenders(joinPoint).ifPresent(emailSenders ->
                Arrays.stream(emailSenders.value()).forEach(emailSender ->
                        fetchEmailRecipients(emailSender, joinPoint).forEach(emailRecipient ->
                                sendEmail(emailRecipient, emailSender))));
    }

    private void sendEmail(EmailRecipient emailRecipient, EmailSender emailSender) {
        try {
            emailService.sendEmail(emailRecipient.email(), emailRecipient.bridgehead(), emailRecipient.role(), emailSender.templateType());
        } catch (EmailServiceException e) {
            throw new RuntimeException(e);
        }
    }

    private Optional<EmailSenders> fetchEmailSenders(JoinPoint joinPoint) {
        return AspectUtils.fetchT(joinPoint, EmailSenders.class);
    }

    private Set<EmailRecipient> fetchEmailRecipients(EmailSender emailSender, ProceedingJoinPoint joinPoint) {
        Set<EmailRecipient> result = new HashSet<>();
        Optional<String> projectCode = AspectUtils.fetchProjectCode(joinPoint);
        Optional<String> bridgehead = AspectUtils.fetchBridghead(joinPoint);
        Optional<String> email = AspectUtils.fetchEmail(joinPoint);
        Arrays.stream(emailSender.recipients()).forEach(emailRecipientType ->
                result.addAll(switch (emailRecipientType) {
                    case SESSION_USER -> fetchEmailRecipientsForSessionUser(projectCode, bridgehead);
                    case EMAIL_ANNOTATION -> fetchEmailRecipientsForEmailAnnotation(projectCode, bridgehead, email);
                    case BRIDGEHEAD_ADMIN -> fetchEmailRecipientsForBridgeheadAdmin(projectCode, bridgehead);
                    case PROJECT_MANAGER_ADMIN -> fetchEmailRecipientsForProjectManagerAdmin();
                    case PROJECT_ALL -> fetchEmailRecipientsForAllProjectUsers(projectCode, bridgehead);
                }));
        return result;
    }

    private Set<EmailRecipient> fetchEmailRecipientsForSessionUser(Optional<String> projectCode, Optional<String> bridgehead) {
        Set<EmailRecipient> result = new HashSet<>();
        Optional<UserProjectRoles> userProjectRolesOptional = fetchSessionUserProjectRoles(projectCode);
        userProjectRolesOptional.ifPresent(userProjectRoles -> {
            ProjectRole projectRole = null;
            if (bridgehead.isPresent()) {
                List<ProjectRole> bridgeheadRolesOrderedInTimeDescendent = userProjectRoles.getBridgeheadRolesOrderedInDescendentTime(bridgehead.get());
                if (!bridgeheadRolesOrderedInTimeDescendent.isEmpty()) {
                    projectRole = bridgeheadRolesOrderedInTimeDescendent.get(0);
                }
            }
            if (projectRole == null) {
                Set<ProjectRole> rolesNotDependentOnBridgeheads = userProjectRoles.getRolesNotDependentOnBridgeheads();
                if (!rolesNotDependentOnBridgeheads.isEmpty()) {
                    projectRole = rolesNotDependentOnBridgeheads.stream().toList().get(0);
                }
            }
            if (projectRole == null) {
                projectRole = ProjectRole.DEFAULT;
            }
            result.add(new EmailRecipient(sessionUser.getEmail(), bridgehead, projectRole));
        });
        return result;
    }

    private Optional<UserProjectRoles> fetchSessionUserProjectRoles(Optional<String> projectCode) {
        return (projectCode.isPresent()) ? organisationRoleToProjectRoleMapper.map(projectCode.get()) : Optional.empty();
    }

    private Set<EmailRecipient> fetchEmailRecipientsForEmailAnnotation(Optional<String> projectCode, Optional<String> bridgehead, Optional<String> email) {
        Set<EmailRecipient> result = new HashSet<>();
        if (email.isPresent()) {
            ProjectRole projectRole = ProjectRole.DEFAULT;
            if (projectCode.isPresent() && bridgehead.isPresent()) {
                Optional<ProjectBridgeheadUser> projectBridgeheadUser = fetchProjectBridgeheadUser(projectCode.get(), bridgehead.get(), email.get());
                if (projectBridgeheadUser.isPresent()) {
                    projectRole = projectBridgeheadUser.get().getProjectRole();
                }
            }
            result.add(new EmailRecipient(email.get(), bridgehead, projectRole));
        }
        return result;
    }

    private Optional<ProjectBridgeheadUser> fetchProjectBridgeheadUser(String projectCode, String bridgehead, String email) {
        Optional<Project> project = projectRepository.findByCode(projectCode);
        if (project.isPresent()) {
            Optional<ProjectBridgehead> projectBridgehead = projectBridgeheadRepository.findFirstByBridgeheadAndProject(bridgehead, project.get());
            if (projectBridgehead.isPresent()) {
                List<ProjectBridgeheadUser> projectBridgeheadUserList =
                        ProjectRolesUtils.orderCollectionInDescendentTime(
                                projectBridgeheadUserRepository.getByEmailAndProjectBridgehead(email, projectBridgehead.get()),
                                projectBridgeheadUser -> projectBridgeheadUser.getProjectRole());
                if (!projectBridgeheadUserList.isEmpty()) {
                    return Optional.of(projectBridgeheadUserList.get(0));
                }
            }
        }
        return Optional.empty();
    }

    private Set<EmailRecipient> fetchEmailRecipientsForBridgeheadAdmin(Optional<String> projectCode, Optional<String> bridgehead) {
        Set<EmailRecipient> result = new HashSet<>();
        fetchProjectBridgeheads(projectCode, bridgehead).forEach(projectBridgehead ->
                bridgeheadAdminUserRepository.findByBridgehead(projectBridgehead.getBridgehead()).forEach(bridgeheadAdminUser ->
                        result.add(new EmailRecipient(bridgeheadAdminUser.getEmail(), Optional.of(projectBridgehead.getBridgehead()), ProjectRole.BRIDGEHEAD_ADMIN))));
        return result;
    }

    private Set<ProjectBridgehead> fetchProjectBridgeheads(Optional<String> projectCode, Optional<String> bridgehead) {
        if (projectCode.isPresent()) {
            Optional<Project> project = projectRepository.findByCode(projectCode.get());
            if (project.isPresent()) {
                if (bridgehead.isPresent()) {
                    Optional<ProjectBridgehead> projectBridgehead = projectBridgeheadRepository.findFirstByBridgeheadAndProject(bridgehead.get(), project.get());
                    if (projectBridgehead.isPresent()) {
                        return Set.of(projectBridgehead.get());
                    }
                } else {
                    return projectBridgeheadRepository.findByProject(project.get());
                }
            }
        }
        return new HashSet<>();
    }

    private Set<EmailRecipient> fetchEmailRecipientsForProjectManagerAdmin() {
        Set<EmailRecipient> result = new HashSet<>();
        projectManagerAdminUserRepository.findAll().forEach(projectManagerAdminUser -> {
            result.add(new EmailRecipient(projectManagerAdminUser.getEmail(), Optional.empty(), ProjectRole.PROJECT_MANAGER_ADMIN));
        });
        return result;
    }

    private Set<EmailRecipient> fetchEmailRecipientsForAllProjectUsers(Optional<String> projectCode, Optional<String> bridgehead) {
        Map<String, EmailRecipient> userEmailRecipientMap = new HashMap<>();
        fetchProjectBridgeheads(projectCode, bridgehead).forEach(projectBridgehead -> {
            projectBridgeheadUserRepository.getByProjectBridgehead(projectBridgehead).forEach(projectBridgeheadUser -> {
                boolean addUser;
                EmailRecipient emailRecipient = userEmailRecipientMap.get(projectBridgeheadUser.getEmail());
                if (emailRecipient != null) {
                    addUser = ProjectRolesUtils.compare(emailRecipient.role(), projectBridgeheadUser.getProjectRole()) > 0;
                } else {
                    addUser = true;
                }
                if (addUser) {
                    userEmailRecipientMap.put(projectBridgeheadUser.getEmail(),
                            new EmailRecipient(projectBridgeheadUser.getEmail(), Optional.of(projectBridgehead.getBridgehead()), projectBridgeheadUser.getProjectRole()));
                }
            });
            bridgeheadAdminUserRepository.findByBridgehead(projectBridgehead.getBridgehead()).forEach(bridgeheadAdminUser -> {
                if (!userEmailRecipientMap.containsKey(bridgeheadAdminUser.getEmail())) {
                    userEmailRecipientMap.put(bridgeheadAdminUser.getEmail(), new EmailRecipient(bridgeheadAdminUser.getEmail(), Optional.of(bridgeheadAdminUser.getBridgehead()), ProjectRole.BRIDGEHEAD_ADMIN));
                }
            });
        });
        projectManagerAdminUserRepository.findAll().forEach(projectManagerAdminUser -> {
            if (!userEmailRecipientMap.containsKey(projectManagerAdminUser.getEmail())) {
                userEmailRecipientMap.put(projectManagerAdminUser.getEmail(), new EmailRecipient(projectManagerAdminUser.getEmail(), Optional.empty(), ProjectRole.PROJECT_MANAGER_ADMIN));
            }
        });
        return new HashSet<>(userEmailRecipientMap.values());
    }


}
