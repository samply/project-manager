package de.samply.aop;

import de.samply.annotations.EmailSender;
import de.samply.annotations.EmailSenderIfError;
import de.samply.annotations.EmailSenders;
import de.samply.annotations.EmailSendersIfError;
import de.samply.app.ProjectManagerConst;
import de.samply.db.model.Project;
import de.samply.db.model.ProjectBridgehead;
import de.samply.db.model.ProjectBridgeheadUser;
import de.samply.email.*;
import de.samply.project.ProjectBridgeheadService;
import de.samply.project.ProjectBridgeheadUserService;
import de.samply.project.state.ProjectBridgeheadState;
import de.samply.security.SessionUser;
import de.samply.user.UserService;
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
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Supplier;

@Component
@Aspect
public class EmailSenderAspect {

    // Services
    private final EmailService emailService;
    private final SessionUser sessionUser;
    private final UserService userService;
    private final ProjectBridgeheadService projectBridgeheadService;
    private final ProjectBridgeheadUserService projectBridgeheadUserService;

    private final OrganisationRoleToProjectRoleMapper organisationRoleToProjectRoleMapper;
    private final EmailKeyValuesFactory emailKeyValuesFactory;

    public EmailSenderAspect(EmailService emailService,
                             SessionUser sessionUser,
                             UserService userService,
                             ProjectBridgeheadService projectBridgeheadService,
                             ProjectBridgeheadUserService projectBridgeheadUserService,
                             OrganisationRoleToProjectRoleMapper organisationRoleToProjectRoleMapper,
                             EmailKeyValuesFactory emailKeyValuesFactory) {
        this.emailService = emailService;
        this.sessionUser = sessionUser;
        this.userService = userService;
        this.projectBridgeheadService = projectBridgeheadService;
        this.projectBridgeheadUserService = projectBridgeheadUserService;
        this.organisationRoleToProjectRoleMapper = organisationRoleToProjectRoleMapper;
        this.emailKeyValuesFactory = emailKeyValuesFactory;
    }

    @SuppressWarnings("EmptyMethod")
    @Pointcut("@annotation(de.samply.annotations.EmailSender)")
    public void emailSenderPointcut() {
    }

    @SuppressWarnings("EmptyMethod")
    @Pointcut("@annotation(de.samply.annotations.EmailSenders)")
    public void emailSendersPointcut() {
    }

    @SuppressWarnings("EmptyMethod")
    @Pointcut("@annotation(de.samply.annotations.EmailSenderIfError)")
    public void emailSenderIfErrorPointcut() {
    }

    @SuppressWarnings("EmptyMethod")
    @Pointcut("@annotation(de.samply.annotations.EmailSendersIfError)")
    public void emailSendersIfErrorPointcut() {
    }


    @Around("emailSenderPointcut()")
    public Object aroundEmailSender(ProceedingJoinPoint joinPoint) throws Throwable {
        return aroundEmailSender(joinPoint, true, false);
    }

    @Around("emailSendersPointcut()")
    public Object aroundEmailSenders(ProceedingJoinPoint joinPoint) throws Throwable {
        return aroundEmailSender(joinPoint, false, false);
    }

    @Around("emailSenderIfErrorPointcut()")
    public Object aroundEmailSenderIfError(ProceedingJoinPoint joinPoint) throws Throwable {
        return aroundEmailSender(joinPoint, true, true);
    }

    @Around("emailSendersIfErrorPointcut()")
    public Object aroundEmailSendersIfError(ProceedingJoinPoint joinPoint) throws Throwable {
        return aroundEmailSender(joinPoint, false, true);
    }

    private Object aroundEmailSender(ProceedingJoinPoint joinPoint, boolean isSingleEmailSender, boolean ifError) throws Throwable {
        try {
            @SuppressWarnings("rawtypes") ResponseEntity responseEntity = (ResponseEntity) joinPoint.proceed();
            if (responseEntity.getStatusCode().is2xxSuccessful() ^ ifError) {
                sendEmail(joinPoint, isSingleEmailSender, ifError);
            }
            return responseEntity;
        } catch (Exception e) {
            if (ifError) {
                sendEmail(joinPoint, isSingleEmailSender, true);
            }
            throw new RuntimeException(e);
        }
    }

    private void sendEmail(ProceedingJoinPoint joinPoint, boolean isSingleEmailSender, boolean ifError) {
        if (isSingleEmailSender) {
            if (ifError) {
                sendEmailFromEmailSenderIfError(joinPoint, fetchEmailSenderIfError(joinPoint));
            } else {
                sendEmailFromEmailSender(joinPoint, fetchEmailSender(joinPoint));
            }
        } else {
            if (ifError) {
                sendEmailFromEmailSendersIfError(joinPoint, fetchEmailSendersIfError(joinPoint));
            } else {
                sendEmailFromEmailSenders(joinPoint, fetchEmailSenders(joinPoint));
            }
        }
    }

    private void sendEmailFromEmailSenders(ProceedingJoinPoint joinPoint, Optional<EmailSenders> emailSendersOptional) {
        emailSendersOptional.ifPresent(emailSenders -> Arrays.stream(emailSenders.value()).forEach(emailSender ->
                sendEmailFromEmailSender(joinPoint, Optional.of(emailSender))));
    }

    private void sendEmailFromEmailSender(ProceedingJoinPoint joinPoint, Optional<EmailSender> emailSenderOptional) {
        emailSenderOptional.ifPresent(emailSender -> fetchEmailRecipients(emailSender::recipients, joinPoint)
                .forEach(emailRecipient -> sendEmail(emailRecipient, emailSender::templateType)));
    }

    private void sendEmailFromEmailSendersIfError(ProceedingJoinPoint joinPoint, Optional<EmailSendersIfError> emailSendersIfErrorOptional) {
        emailSendersIfErrorOptional.ifPresent(emailSendersIfError -> Arrays.stream(emailSendersIfError.value()).forEach(emailSenderIfError ->
                sendEmailFromEmailSenderIfError(joinPoint, Optional.of(emailSenderIfError))));
    }

    private void sendEmailFromEmailSenderIfError(ProceedingJoinPoint joinPoint, Optional<EmailSenderIfError> emailSenderIfErrorOptional) {
        emailSenderIfErrorOptional.ifPresent(emailSenderIfError -> fetchEmailRecipients(emailSenderIfError::recipients, joinPoint)
                .forEach(emailRecipient -> sendEmail(emailRecipient, emailSenderIfError::templateType)));
    }

    @Async(ProjectManagerConst.ASYNC_EMAIL_SENDER_EXECUTOR)
    protected void sendEmail(EmailRecipient emailRecipient, Supplier<EmailTemplateType> emailTemplateTypeSupplier) {
        try {
            emailService.sendEmail(emailRecipient.getEmail(), emailRecipient.getProject(), emailRecipient.getBridgehead(),
                    emailRecipient.getRole(), emailTemplateTypeSupplier.get(), emailKeyValuesFactory.newInstance().add(emailRecipient));
        } catch (EmailServiceException e) {
            throw new RuntimeException(e);
        }
    }

    private Optional<EmailSender> fetchEmailSender(JoinPoint joinPoint) {
        return AspectUtils.fetchT(joinPoint, EmailSender.class);
    }

    private Optional<EmailSenders> fetchEmailSenders(JoinPoint joinPoint) {
        return AspectUtils.fetchT(joinPoint, EmailSenders.class);
    }

    private Optional<EmailSenderIfError> fetchEmailSenderIfError(JoinPoint joinPoint) {
        return AspectUtils.fetchT(joinPoint, EmailSenderIfError.class);
    }

    private Optional<EmailSendersIfError> fetchEmailSendersIfError(JoinPoint joinPoint) {
        return AspectUtils.fetchT(joinPoint, EmailSendersIfError.class);
    }

    private Set<EmailRecipient> fetchEmailRecipients(Supplier<EmailRecipientType[]> emailRecipientTypesSupplier, ProceedingJoinPoint joinPoint) {
        Set<EmailRecipient> result = new HashSet<>();
        Optional<Project> project = AspectUtils.fetchProject(joinPoint);
        Optional<ProjectBridgehead> projectBridgehead = AspectUtils.fetchBridgehead(joinPoint);
        Optional<String> email = AspectUtils.fetchEmail(joinPoint);
        Optional<String> message = AspectUtils.fetchMessage(joinPoint);
        Arrays.stream(emailRecipientTypesSupplier.get()).forEach(emailRecipientType ->
                result.addAll(switch (emailRecipientType) {
                    case SESSION_USER -> fetchEmailRecipientsForSessionUser(project, projectBridgehead);
                    case CREATOR -> fetchEmailRecipientsForCreator(project, projectBridgehead);
                    case EMAIL_ANNOTATION -> fetchEmailRecipientsForEmailAnnotation(project, projectBridgehead, email);
                    case ALL_BRIDGEHEAD_ADMINS -> fetchEmailRecipientsForAllBridgeheadAdminsOfTheProject(project);
                    case ALL_DEVELOPERS -> fetchEmailRecipientsForAllDeveloperUsersOfTheProject(project);
                    case ALL_PILOTS -> fetchEmailRecipientsForAllPilotUsersOfTheProject(project);
                    case ALL_FINALS -> fetchEmailRecipientsForAllFinalUsersOfTheProject(project);
                    case BRIDGEHEAD_ADMIN -> fetchEmailRecipientsForBridgeheadAdmin(project, projectBridgehead);
                    case BRIDGEHEAD_ADMINS_WHO_HAVE_NOT_ACCEPTED_NOR_REJECTED_THE_PROJECT ->
                            fetchEmailRecipientsForBridgeheadAdminsWhoHaveNotAcceptedNorRejectedTheProject(project);
                    case PROJECT_MANAGER_ADMIN ->
                            fetchEmailRecipientsForProjectManagerAdmin(project, projectBridgehead);
                    case PROJECT_ALL -> fetchEmailRecipientsForAllProjectUsers(project, projectBridgehead);
                }));
        if (message.isPresent()) {
            result.forEach(emailRecipient -> emailRecipient.setMessage(message));
        }
        return result;
    }

    private Set<EmailRecipient> fetchEmailRecipientsForCreator(Optional<Project> project, Optional<ProjectBridgehead> bridgehead) {
        Set<EmailRecipient> result = new HashSet<>();
        if (project.isPresent()) {
            project.ifPresent(value -> result.add(new EmailRecipient(value.getCreatorEmail(), project, bridgehead, ProjectRole.CREATOR)));
        }
        return result;
    }

    private Set<EmailRecipient> fetchEmailRecipientsForAllDeveloperUsersOfTheProject(Optional<Project> project) {
        return fetchEmailRecipientsForAllDeveloperUsersOfTheProject(project, ProjectRole.DEVELOPER);
    }

    private Set<EmailRecipient> fetchEmailRecipientsForAllPilotUsersOfTheProject(Optional<Project> project) {
        return fetchEmailRecipientsForAllDeveloperUsersOfTheProject(project, ProjectRole.PILOT);
    }

    private Set<EmailRecipient> fetchEmailRecipientsForAllFinalUsersOfTheProject(Optional<Project> project) {
        return fetchEmailRecipientsForAllDeveloperUsersOfTheProject(project, ProjectRole.FINAL);
    }

    private Set<EmailRecipient> fetchEmailRecipientsForAllDeveloperUsersOfTheProject(Optional<Project> project, ProjectRole projectRole) {
        Set<EmailRecipient> result = new HashSet<>();
        if (project.isPresent()) {
            projectBridgeheadUserService
                    .fetchUsers(projectRole, project.get())
                    .stream()
                    .filter(projectBridgeheadUser -> !projectBridgeheadUser.getEmail().equals(sessionUser.getEmail()))
                    .forEach(projectBridgeheadUser ->
                            result.add(
                                    new EmailRecipient(
                                            projectBridgeheadUser.getEmail(),
                                            project,
                                            Optional.of(projectBridgeheadUser.getProjectBridgehead()),
                                            projectRole)));
        }
        return result;
    }


    private Set<EmailRecipient> fetchEmailRecipientsForAllBridgeheadAdminsOfTheProject(Optional<Project> project) {
        Set<EmailRecipient> result = new HashSet<>();
        if (project.isPresent()) {
            project.ifPresent(value -> projectBridgeheadService
                    .fetchBridgeheads(value)
                    .forEach(projectBridgehead -> userService
                            .fetchBridgeheadAdmin(projectBridgehead)
                            .forEach(bridgeheadAdminUser -> result.add(
                                    new EmailRecipient(
                                            bridgeheadAdminUser.getEmail(),
                                            project,
                                            Optional.of(projectBridgehead),
                                            ProjectRole.BRIDGEHEAD_ADMIN)))));
        }
        return result;
    }

    private Set<EmailRecipient> fetchEmailRecipientsForSessionUser(Optional<Project> project, Optional<ProjectBridgehead> bridgehead) {
        Set<EmailRecipient> result = new HashSet<>();
        Optional<UserProjectRoles> userProjectRolesOptional = fetchSessionUserProjectRoles(project);
        userProjectRolesOptional.ifPresent(userProjectRoles -> {
            ProjectRole projectRole = null;
            if (bridgehead.isPresent()) {
                List<ProjectRole> bridgeheadRolesOrderedInTimeDescendent = userProjectRoles.getBridgeheadRolesOrderedInDescendentTime(bridgehead.get().getBridgehead());
                if (!bridgeheadRolesOrderedInTimeDescendent.isEmpty()) {
                    projectRole = bridgeheadRolesOrderedInTimeDescendent.getFirst();
                }
            }
            if (projectRole == null) {
                Set<ProjectRole> rolesNotDependentOnBridgeheads = userProjectRoles.getRolesNotDependentOnBridgeheads();
                if (!rolesNotDependentOnBridgeheads.isEmpty()) {
                    projectRole = rolesNotDependentOnBridgeheads.stream().toList().getFirst();
                }
            }
            if (projectRole == null) {
                projectRole = ProjectRole.DEFAULT;
            }
            result.add(new EmailRecipient(sessionUser.getEmail(), project, bridgehead, projectRole));
        });
        return result;
    }

    private Optional<UserProjectRoles> fetchSessionUserProjectRoles(Optional<Project> project) {
        return (project.isPresent()) ? organisationRoleToProjectRoleMapper.map(project.get()) : Optional.empty();
    }

    private Set<EmailRecipient> fetchEmailRecipientsForEmailAnnotation(Optional<Project> project, Optional<ProjectBridgehead> bridgehead, Optional<String> email) {
        Set<EmailRecipient> result = new HashSet<>();
        if (email.isPresent()) {
            ProjectRole projectRole = ProjectRole.DEFAULT;
            if (project.isPresent() && bridgehead.isPresent()) {
                Optional<ProjectBridgeheadUser> projectBridgeheadUser = fetchProjectBridgeheadUser(bridgehead.get(), email.get());
                if (projectBridgeheadUser.isPresent()) {
                    projectRole = projectBridgeheadUser.get().getProjectRole();
                }
            }
            result.add(new EmailRecipient(email.get(), project, bridgehead, projectRole));
        }
        return result;
    }

    private Optional<ProjectBridgeheadUser> fetchProjectBridgeheadUser(ProjectBridgehead bridgehead, String email) {
        List<ProjectBridgeheadUser> projectBridgeheadUserList =
                ProjectRolesUtils.orderCollectionInDescendentTime(
                        projectBridgeheadUserService.fetchUsers(email, bridgehead),
                        ProjectBridgeheadUser::getProjectRole);
        if (!projectBridgeheadUserList.isEmpty()) {
            return Optional.of(projectBridgeheadUserList.getFirst());
        } else {
            return Optional.empty();
        }
    }

    private Set<EmailRecipient> fetchEmailRecipientsForBridgeheadAdmin(Optional<Project> project, Optional<ProjectBridgehead> bridgehead) {
        Set<EmailRecipient> result = new HashSet<>();
        fetchProjectBridgeheads(project, bridgehead)
                .forEach(projectBridgehead -> userService
                        .fetchBridgeheadAdmin(projectBridgehead)
                        .forEach(bridgeheadAdminUser -> result.add(
                                new EmailRecipient(
                                        bridgeheadAdminUser.getEmail(),
                                        project,
                                        Optional.of(projectBridgehead),
                                        ProjectRole.BRIDGEHEAD_ADMIN))));
        return result;
    }

    private Set<EmailRecipient> fetchEmailRecipientsForBridgeheadAdminsWhoHaveNotAcceptedNorRejectedTheProject(Optional<Project> project) {
        Set<EmailRecipient> result = new HashSet<>();
        fetchProjectBridgeheadsNotAcceptedNorRejected(project)
                .forEach(projectBridgehead -> userService
                        .fetchBridgeheadAdmin(projectBridgehead)
                        .forEach(bridgeheadAdminUser -> result.add(
                                new EmailRecipient(
                                        bridgeheadAdminUser.getEmail(),
                                        project,
                                        Optional.of(projectBridgehead),
                                        ProjectRole.BRIDGEHEAD_ADMIN))));
        return result;
    }

    private Set<ProjectBridgehead> fetchProjectBridgeheadsNotAcceptedNorRejected(Optional<Project> project) {
        return project
                .map(p -> projectBridgeheadService.fetchBridgeheads(p, ProjectBridgeheadState.CREATED))
                .orElse(Collections.emptySet());
    }


    private Set<ProjectBridgehead> fetchProjectBridgeheads(Optional<Project> project, Optional<ProjectBridgehead> bridgehead) {
        if (project.isEmpty()) {
            return new HashSet<>();
        }
        return bridgehead.map(Set::of).orElseGet(() -> projectBridgeheadService.fetchBridgeheads(project.get()));
    }

    private Set<EmailRecipient> fetchEmailRecipientsForProjectManagerAdmin(Optional<Project> project, Optional<ProjectBridgehead> bridgehead) {
        Set<EmailRecipient> result = new HashSet<>();
        userService.fetchAllProjectManagerAdmins().forEach(projectManagerAdminUser ->
                result.add(new EmailRecipient(projectManagerAdminUser.getEmail(), project, bridgehead, ProjectRole.PROJECT_MANAGER_ADMIN)));
        return result;
    }

    private Set<EmailRecipient> fetchEmailRecipientsForAllProjectUsers(Optional<Project> project, Optional<ProjectBridgehead> bridgehead) {
        Map<String, EmailRecipient> userEmailRecipientMap = new HashMap<>();
        fetchProjectBridgeheads(project, bridgehead).forEach(projectBridgehead -> {
            projectBridgeheadUserService.fetchUsers(projectBridgehead).forEach(projectBridgeheadUser -> {
                boolean addUser;
                EmailRecipient emailRecipient = userEmailRecipientMap.get(projectBridgeheadUser.getEmail());
                if (emailRecipient != null) {
                    addUser = ProjectRolesUtils.compare(emailRecipient.getRole(), projectBridgeheadUser.getProjectRole()) > 0;
                } else {
                    addUser = true;
                }
                if (addUser) {
                    userEmailRecipientMap.put(projectBridgeheadUser.getEmail(),
                            new EmailRecipient(projectBridgeheadUser.getEmail(), project, Optional.of(projectBridgehead), projectBridgeheadUser.getProjectRole()));
                }
            });
            userService
                    .fetchBridgeheadAdmin(projectBridgehead)
                    .forEach(bridgeheadAdminUser -> {
                        if (!userEmailRecipientMap.containsKey(bridgeheadAdminUser.getEmail())) {
                            userEmailRecipientMap.put(
                                    bridgeheadAdminUser.getEmail(),
                                    new EmailRecipient(
                                            bridgeheadAdminUser.getEmail(),
                                            project,
                                            project.flatMap(p -> projectBridgeheadService.fetchBridgehead(p, bridgeheadAdminUser.getBridgehead())),
                                            ProjectRole.BRIDGEHEAD_ADMIN));
                        }
                    });
        });
        userService.fetchAllProjectManagerAdmins().forEach(projectManagerAdminUser -> {
            if (!userEmailRecipientMap.containsKey(projectManagerAdminUser.getEmail())) {
                userEmailRecipientMap.put(projectManagerAdminUser.getEmail(), new EmailRecipient(projectManagerAdminUser.getEmail(), project, Optional.empty(), ProjectRole.PROJECT_MANAGER_ADMIN));
            }
        });
        return new HashSet<>(userEmailRecipientMap.values());
    }


}
