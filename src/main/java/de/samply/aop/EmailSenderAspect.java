package de.samply.aop;

import de.samply.annotations.EmailSender;
import de.samply.annotations.EmailSenderIfError;
import de.samply.annotations.EmailSenders;
import de.samply.annotations.EmailSendersIfError;
import de.samply.app.ProjectManagerConst;
import de.samply.db.model.Project;
import de.samply.db.model.ProjectBridgehead;
import de.samply.db.model.ProjectBridgeheadUser;
import de.samply.db.repository.*;
import de.samply.email.*;
import de.samply.project.state.ProjectBridgeheadState;
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

import java.lang.annotation.Annotation;
import java.util.*;
import java.util.function.Supplier;

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

    @Pointcut("@annotation(de.samply.annotations.EmailSender)")
    public void emailSenderPointcut() {
    }

    @Pointcut("@annotation(de.samply.annotations.EmailSenders)")
    public void emailSendersPointcut() {
    }

    @Pointcut("@annotation(de.samply.annotations.EmailSenderIfError)")
    public void emailSenderIfErrorPointcut() {
    }

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

    private <T extends Annotation> Object aroundEmailSender(ProceedingJoinPoint joinPoint, boolean isSingleEmailSender, boolean ifError) throws Throwable {
        try {
            ResponseEntity responseEntity = (ResponseEntity) joinPoint.proceed();
            if (responseEntity.getStatusCode().is2xxSuccessful() ^ ifError) {
                sendEmail(joinPoint, isSingleEmailSender, ifError);
            }
            return responseEntity;
        } catch (Exception e) {
            if (ifError) {
                sendEmail(joinPoint, isSingleEmailSender, ifError);
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
        emailSenderOptional.ifPresent(emailSender -> fetchEmailRecipients(() -> emailSender.recipients(), joinPoint)
                .forEach(emailRecipient -> sendEmail(emailRecipient, () -> emailSender.templateType())));
    }

    private void sendEmailFromEmailSendersIfError(ProceedingJoinPoint joinPoint, Optional<EmailSendersIfError> emailSendersIfErrorOptional) {
        emailSendersIfErrorOptional.ifPresent(emailSendersIfError -> Arrays.stream(emailSendersIfError.value()).forEach(emailSenderIfError ->
                sendEmailFromEmailSenderIfError(joinPoint, Optional.of(emailSenderIfError))));
    }

    private void sendEmailFromEmailSenderIfError(ProceedingJoinPoint joinPoint, Optional<EmailSenderIfError> emailSenderIfErrorOptional) {
        emailSenderIfErrorOptional.ifPresent(emailSenderIfError -> fetchEmailRecipients(() -> emailSenderIfError.recipients(), joinPoint)
                .forEach(emailRecipient -> sendEmail(emailRecipient, () -> emailSenderIfError.templateType())));
    }

    private void sendEmail(EmailRecipient emailRecipient, Supplier<EmailTemplateType> emailTemplateTypeSupplier) {
        try {
            Map<String, String> keyValues = (emailRecipient.getMessage().isPresent()) ? Map.of(ProjectManagerConst.EMAIL_CONTEXT_MESSAGE, emailRecipient.getMessage().get()) : new HashMap<>();
            emailService.sendEmail(emailRecipient.getEmail(), emailRecipient.getProjectCode(), emailRecipient.getBridgehead(), emailRecipient.getRole(), emailTemplateTypeSupplier.get(), keyValues);
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
        Optional<String> projectCode = AspectUtils.fetchProjectCode(joinPoint);
        Optional<String> bridgehead = AspectUtils.fetchBridgehead(joinPoint);
        Optional<String> email = AspectUtils.fetchEmail(joinPoint);
        Optional<String> message = AspectUtils.fetchMessage(joinPoint);
        Arrays.stream(emailRecipientTypesSupplier.get()).forEach(emailRecipientType ->
                result.addAll(switch (emailRecipientType) {
                    case SESSION_USER -> fetchEmailRecipientsForSessionUser(projectCode, bridgehead);
                    case CREATOR -> fetchEmailRecipientsForCreator(projectCode, bridgehead);
                    case EMAIL_ANNOTATION -> fetchEmailRecipientsForEmailAnnotation(projectCode, bridgehead, email);
                    case ALL_BRIDGEHEAD_ADMINS -> fetchEmailRecipientsForAllBridgeheadAdminsOfTheProject(projectCode);
                    case ALL_DEVELOPERS -> fetchEmailRecipientsForAllDeveloperUsersOfTheProject(projectCode);
                    case ALL_PILOTS -> fetchEmailRecipientsForAllPilotUsersOfTheProject(projectCode);
                    case ALL_FINALS -> fetchEmailRecipientsForAllFinalUsersOfTheProject(projectCode);
                    case BRIDGEHEAD_ADMIN -> fetchEmailRecipientsForBridgeheadAdmin(projectCode, bridgehead);
                    case BRIDGHEAD_ADMINS_WHO_HAVE_NOT_ACCEPTED_NOR_REJECTED_THE_PROJECT ->
                            fetchEmailRecipientsForBridgeheadAdminsWhoHaveNotAcceptedNorRejectedTheProject(projectCode);
                    case PROJECT_MANAGER_ADMIN -> fetchEmailRecipientsForProjectManagerAdmin(projectCode, bridgehead);
                    case PROJECT_ALL -> fetchEmailRecipientsForAllProjectUsers(projectCode, bridgehead);
                }));
        if (message.isPresent()) {
            result.forEach(emailRecipient -> emailRecipient.setMessage(message));
        }
        return result;
    }

    private Set<EmailRecipient> fetchEmailRecipientsForCreator(Optional<String> projectCode, Optional<String> bridgehead) {
        Set<EmailRecipient> result = new HashSet<>();
        if (projectCode.isPresent()) {
            Optional<Project> project = this.projectRepository.findByCode(projectCode.get());
            if (project.isPresent()) {
                result.add(new EmailRecipient(project.get().getCreatorEmail(), projectCode, bridgehead, ProjectRole.CREATOR));
            }
        }
        return result;
    }

    private Set<EmailRecipient> fetchEmailRecipientsForAllDeveloperUsersOfTheProject(Optional<String> projectCode) {
        return fetchEmailRecipientsForAllDeveloperUsersOfTheProject(projectCode, ProjectRole.DEVELOPER);
    }

    private Set<EmailRecipient> fetchEmailRecipientsForAllPilotUsersOfTheProject(Optional<String> projectCode) {
        return fetchEmailRecipientsForAllDeveloperUsersOfTheProject(projectCode, ProjectRole.PILOT);
    }

    private Set<EmailRecipient> fetchEmailRecipientsForAllFinalUsersOfTheProject(Optional<String> projectCode) {
        return fetchEmailRecipientsForAllDeveloperUsersOfTheProject(projectCode, ProjectRole.FINAL);
    }

    private Set<EmailRecipient> fetchEmailRecipientsForAllDeveloperUsersOfTheProject(Optional<String> projectCode, ProjectRole projectRole) {
        Set<EmailRecipient> result = new HashSet<>();
        if (projectCode.isPresent()) {
            projectBridgeheadUserRepository.getDistinctByProjectRoleAndProjectCode(projectRole, projectCode.get()).stream()
                    .filter(projectBridgeheadUser -> !projectBridgeheadUser.getEmail().equals(sessionUser.getEmail())).forEach(projectBridgeheadUser ->
                            result.add(new EmailRecipient(projectBridgeheadUser.getEmail(), projectCode, Optional.of(projectBridgeheadUser.getProjectBridgehead().getBridgehead()), projectRole)));
        }
        return result;
    }


    private Set<EmailRecipient> fetchEmailRecipientsForAllBridgeheadAdminsOfTheProject(Optional<String> projectCode) {
        Set<EmailRecipient> result = new HashSet<>();
        if (projectCode.isPresent()) {
            Optional<Project> project = this.projectRepository.findByCode(projectCode.get());
            if (project.isPresent()) {
                this.projectBridgeheadRepository.findByProject(project.get()).forEach(projectBridgehead ->
                        this.bridgeheadAdminUserRepository.findByBridgehead(projectBridgehead.getBridgehead()).forEach(bridgeheadAdminUser ->
                                result.add(new EmailRecipient(bridgeheadAdminUser.getEmail(), projectCode,
                                        Optional.of(projectBridgehead.getBridgehead()), ProjectRole.BRIDGEHEAD_ADMIN))));
            }
        }
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
            result.add(new EmailRecipient(sessionUser.getEmail(), projectCode, bridgehead, projectRole));
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
            result.add(new EmailRecipient(email.get(), projectCode, bridgehead, projectRole));
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
                        result.add(new EmailRecipient(bridgeheadAdminUser.getEmail(), projectCode, Optional.of(projectBridgehead.getBridgehead()), ProjectRole.BRIDGEHEAD_ADMIN))));
        return result;
    }

    private Set<EmailRecipient> fetchEmailRecipientsForBridgeheadAdminsWhoHaveNotAcceptedNorRejectedTheProject(Optional<String> projectCode) {
        Set<EmailRecipient> result = new HashSet<>();
        fetchProjectBridgeheadsNotAcceptedNorRejected(projectCode).forEach(projectBridgehead ->
                bridgeheadAdminUserRepository.findByBridgehead(projectBridgehead.getBridgehead()).forEach(bridgeheadAdminUser ->
                        result.add(new EmailRecipient(bridgeheadAdminUser.getEmail(), projectCode, Optional.of(projectBridgehead.getBridgehead()), ProjectRole.BRIDGEHEAD_ADMIN))));
        return result;
    }

    private Set<ProjectBridgehead> fetchProjectBridgeheadsNotAcceptedNorRejected(Optional<String> projectCode) {
        if (projectCode.isPresent()) {
            Optional<Project> project = projectRepository.findByCode(projectCode.get());
            if (project.isPresent()) {
                return projectBridgeheadRepository.findByProjectAndState(project.get(), ProjectBridgeheadState.CREATED);
            }
        }
        return new HashSet<>();
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

    private Set<EmailRecipient> fetchEmailRecipientsForProjectManagerAdmin(Optional<String> projectCode, Optional<String> bridgehead) {
        Set<EmailRecipient> result = new HashSet<>();
        projectManagerAdminUserRepository.findAll().forEach(projectManagerAdminUser ->
                result.add(new EmailRecipient(projectManagerAdminUser.getEmail(), projectCode, bridgehead, ProjectRole.PROJECT_MANAGER_ADMIN)));
        return result;
    }

    private Set<EmailRecipient> fetchEmailRecipientsForAllProjectUsers(Optional<String> projectCode, Optional<String> bridgehead) {
        Map<String, EmailRecipient> userEmailRecipientMap = new HashMap<>();
        fetchProjectBridgeheads(projectCode, bridgehead).forEach(projectBridgehead -> {
            projectBridgeheadUserRepository.getByProjectBridgehead(projectBridgehead).forEach(projectBridgeheadUser -> {
                boolean addUser;
                EmailRecipient emailRecipient = userEmailRecipientMap.get(projectBridgeheadUser.getEmail());
                if (emailRecipient != null) {
                    addUser = ProjectRolesUtils.compare(emailRecipient.getRole(), projectBridgeheadUser.getProjectRole()) > 0;
                } else {
                    addUser = true;
                }
                if (addUser) {
                    userEmailRecipientMap.put(projectBridgeheadUser.getEmail(),
                            new EmailRecipient(projectBridgeheadUser.getEmail(), projectCode, Optional.of(projectBridgehead.getBridgehead()), projectBridgeheadUser.getProjectRole()));
                }
            });
            bridgeheadAdminUserRepository.findByBridgehead(projectBridgehead.getBridgehead()).forEach(bridgeheadAdminUser -> {
                if (!userEmailRecipientMap.containsKey(bridgeheadAdminUser.getEmail())) {
                    userEmailRecipientMap.put(bridgeheadAdminUser.getEmail(), new EmailRecipient(bridgeheadAdminUser.getEmail(), projectCode, Optional.of(bridgeheadAdminUser.getBridgehead()), ProjectRole.BRIDGEHEAD_ADMIN));
                }
            });
        });
        projectManagerAdminUserRepository.findAll().forEach(projectManagerAdminUser -> {
            if (!userEmailRecipientMap.containsKey(projectManagerAdminUser.getEmail())) {
                userEmailRecipientMap.put(projectManagerAdminUser.getEmail(), new EmailRecipient(projectManagerAdminUser.getEmail(), projectCode, Optional.empty(), ProjectRole.PROJECT_MANAGER_ADMIN));
            }
        });
        return new HashSet<>(userEmailRecipientMap.values());
    }


}
