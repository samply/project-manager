package de.samply.frontend;

import de.samply.annotations.*;
import de.samply.aop.ConstraintsService;
import de.samply.app.ProjectManagerConst;
import de.samply.app.ProjectManagerController;
import de.samply.db.model.Project;
import de.samply.db.model.ProjectBridgehead;
import de.samply.db.model.ProjectBridgeheadUser;
import de.samply.email.EmailRecipientType;
import de.samply.project.ProjectBridgeheadUserService;
import de.samply.security.SessionUser;
import de.samply.user.roles.RolesExtractor;
import de.samply.utils.AspectUtils;
import de.samply.utils.LanguageUtils;
import de.samply.utils.ParamMetaUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.MethodParameter;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.lang.reflect.Method;
import java.util.stream.IntStream;
import java.util.*;

@Service
public class FrontendService {

    // Services
    private final ConstraintsService constraintsService;
    private final ProjectBridgeheadUserService projectBridgeheadUserService;

    private final FrontendConfiguration frontendConfiguration;
    private final ActionMessages actionMessages;
    private final SessionUser sessionUser;

    private final String explorerUrlRedirectUriParameter;
    private final String defaultLanguage;


    public FrontendService(
            ConstraintsService constraintsService, ProjectBridgeheadUserService projectBridgeheadUserService,
            FrontendConfiguration frontendConfiguration,
            ActionMessages actionMessages,
            SessionUser sessionUser,
            @Value(ProjectManagerConst.EXPLORER_REDIRECT_URI_PARAMETER_SV) String explorerUrlRedirectUriParameter,
            @Value(ProjectManagerConst.DEFAULT_LANGUAGE_SV) String defaultLanguage) {
        this.constraintsService = constraintsService;
        this.projectBridgeheadUserService = projectBridgeheadUserService;
        this.frontendConfiguration = frontendConfiguration;
        this.explorerUrlRedirectUriParameter = explorerUrlRedirectUriParameter;
        this.actionMessages = actionMessages;
        this.defaultLanguage = LanguageUtils.normalize(defaultLanguage);
        this.sessionUser = sessionUser;
    }

    public Map<String, Map<String, Action>> fetchModuleActionPackage(String site, Optional<Project> project,
                                                                     Optional<ProjectBridgehead> bridgehead, Optional<String> language, boolean withConstraints) {
        Map<String, Map<String, Action>> moduleActionMap = new HashMap<>();
        String rootPath = RolesExtractor.getRootPath();
        String tempLanguage = language.orElse(defaultLanguage);
        Optional<ProjectBridgeheadUser> projectBridgeheadUser = fetchProjectBridgeheadUser(bridgehead);
        Arrays.stream(ProjectManagerController.class.getDeclaredMethods()).forEach(method -> {
            FrontendSiteModules frontendSiteModules = method.getAnnotation(FrontendSiteModules.class);
            FrontendSiteModule frontendSiteModule = method.getAnnotation(FrontendSiteModule.class);
            FrontendAction frontendAction = method.getAnnotation((FrontendAction.class));
            Optional<String> path = RolesExtractor.fetchPath(method);
            List<FrontendSiteModule> frontendSiteModuleList = new ArrayList<>();
            if (frontendSiteModule != null) {
                frontendSiteModuleList.add(frontendSiteModule);
            }
            if (frontendSiteModules != null && frontendSiteModules.value() != null && frontendSiteModules.value().length > 0) {
                frontendSiteModuleList.addAll(List.of(frontendSiteModules.value()));
            }
            frontendSiteModuleList.forEach(tempFrontendSiteModule ->
                    fetchModuleActionsPackages(moduleActionMap, rootPath, path, tempFrontendSiteModule, frontendAction,
                            site, project, bridgehead, projectBridgeheadUser, tempLanguage, method, withConstraints));
        });
        return moduleActionMap;
    }

    /**
     * The action package without a bridgehead (under "") and of each given bridgehead (under its id): which actions
     * are allowed depends on the bridgehead, because the bridgehead roles count only for their bridgehead. Empty
     * entries (an empty id in the request) are skipped.
     */
    public Map<String, Map<String, Map<String, Action>>> fetchModuleActionPackagesByBridgehead(
            String site, Optional<Project> project, List<ProjectBridgehead> bridgeheads, Optional<String> language) {
        Map<String, Map<String, Map<String, Action>>> result = new LinkedHashMap<>();
        result.put("", fetchModuleActionPackage(site, project, Optional.empty(), language, true));
        bridgeheads.stream().filter(Objects::nonNull).forEach(bridgehead -> result.putIfAbsent(bridgehead.getBridgehead(),
                fetchModuleActionPackage(site, project, Optional.of(bridgehead), language, true)));
        return result;
    }

    @SuppressWarnings("rawtypes") // For Optional<ResponseEntity>. Otherwise, it would be too complex
    private void fetchModuleActionsPackages(Map<String, Map<String, Action>> moduleActionsMap,
                                            String rootPath,
                                            Optional<String> path,
                                            FrontendSiteModule frontendSiteModule,
                                            FrontendAction frontendAction,
                                            String site,
                                            Optional<Project> project,
                                            Optional<ProjectBridgehead> bridgehead,
                                            Optional<ProjectBridgeheadUser> projectBridgeheadUser,
                                            String language,
                                            Method method,
                                            boolean withConstraints) {
        // The action of a disabled module is never offered, not even without the other constraints
        boolean moduleEnabled = this.constraintsService.checkModuleConstraints(
                Optional.ofNullable(method.getAnnotation(RequiresModule.class))).isEmpty();
        if (moduleEnabled && frontendSiteModule != null && site.equals(frontendSiteModule.site()) && frontendAction != null && path.isPresent()) {
            Optional<RoleConstraints> roleConstraints = Optional.ofNullable(method.getAnnotation(RoleConstraints.class));
            Optional<StateConstraints> stateConstraints = Optional.ofNullable(method.getAnnotation(StateConstraints.class));
            Optional<ResponseEntity> responseEntity = this.constraintsService.checkRoleConstraints(roleConstraints, stateConstraints, project, bridgehead);
            if (responseEntity.isEmpty()) {
                responseEntity = this.constraintsService.checkStateConstraints(stateConstraints, project, bridgehead);
            }
            if (responseEntity.isEmpty()) {
                Optional<ProjectConstraints> projectConstraints = Optional.ofNullable(method.getAnnotation(ProjectConstraints.class));
                responseEntity = this.constraintsService.checkProjectConstraints(projectConstraints, project,
                        ConstraintsService.isReadOnly(method));
            }
            if (responseEntity.isEmpty() || !withConstraints) { // If there are no restrictions
                addAction(moduleActionsMap, frontendSiteModule, frontendAction, rootPath, path, method,
                        project, bridgehead, projectBridgeheadUser, language);
            }
        }
    }

    private void addAction(Map<String, Map<String, Action>> moduleActionsMap,
                           FrontendSiteModule frontendSiteModule, FrontendAction frontendAction,
                           String rootPath, Optional<String> path, Method method,
                           Optional<Project> project, Optional<ProjectBridgehead> projectBridgehead,
                           Optional<ProjectBridgeheadUser> projectBridgeheadUser, String language) {
        Map<String, Action> actionNameActionsMap = moduleActionsMap.computeIfAbsent(frontendSiteModule.module(), _ -> new HashMap<>());
        Optional<ResolvedActionMessages> resolvedMessages = actionMessages.fetchMessages(frontendAction.action(), frontendSiteModule.module(),
                language, project, projectBridgehead, projectBridgeheadUser, sessionUser);
        String resolvedPath = path.orElseThrow(
                () -> new IllegalStateException("Path must be present for action " + frontendAction.action())
        );
        actionNameActionsMap.put(frontendAction.action(),
                new Action(
                        rootPath + resolvedPath,
                        fetchHttpMethod(method),
                        fetchHttpParams(method),
                        resolvedMessages.map(ResolvedActionMessages::explanation).orElse(null),
                        resolvedMessages.map(ResolvedActionMessages::successMessage).orElse(null),
                        resolvedMessages.map(ResolvedActionMessages::errorMessage).orElse(null),
                        resolvedMessages.map(ResolvedActionMessages::priority).orElse(null),
                        requiresBridgehead(method),
                        fetchEmailRecipients(method)
                ));
    }

    // The recipients of the emails sent when the action succeeds (@EmailSender). Not @EmailSenderIfError, which only
    // sends when the action fails, and not the user who performs the action (SESSION_USER).
    static List<String> fetchEmailRecipients(Method method) {
        return Arrays.stream(method.getAnnotationsByType(EmailSender.class))
                .flatMap(emailSender -> Arrays.stream(emailSender.recipients()))
                .filter(recipient -> recipient != EmailRecipientType.SESSION_USER)
                .map(Enum::name)
                .distinct()
                .toList();
    }

    static boolean requiresBridgehead(Method method) {
        return IntStream.range(0, method.getParameterCount())
                .mapToObj(index -> new MethodParameter(method, index))
                .filter(parameter -> parameter.hasParameterAnnotation(Bridgehead.class))
                .map(ParamMetaUtils::extractParamMeta)
                .anyMatch(paramMeta -> paramMeta != null && paramMeta.required());
    }

    private String fetchHttpMethod(Method method) {
        Optional<String> result = AspectUtils.fetchHttpMethod(method);
        return result.orElse(null);
    }

    private String[] fetchHttpParams(Method method) {
        return AspectUtils.fetchRequestParamNames(method);
    }

    public String fetchUrl(String site, Map<String, String> parameters) {
        UriComponentsBuilder result = UriComponentsBuilder.fromUriString(frontendConfiguration.getBaseUrl());
        if (site != null) {
            Optional<String> sitePath = frontendConfiguration.getSitePath(site);
            sitePath.ifPresent(result::path);
        }
        if (parameters != null && !parameters.isEmpty()) {
            parameters.keySet().forEach(parameter ->
                    result.queryParamIfPresent(parameter, Optional.ofNullable(parameters.get(parameter))));
        }
        return result.toUriString();
    }

    public Map<String, String> fetchExplorerRedirectUri(String site, Map<String, String> parameters) {
        Map<String, String> result = new HashMap<>();
        result.put(explorerUrlRedirectUriParameter, fetchUrl(site, parameters));
        return result;
    }

    private Optional<ProjectBridgeheadUser> fetchProjectBridgeheadUser(Optional<ProjectBridgehead> projectBridgehead) {
        return (projectBridgehead.isPresent()) ?
                projectBridgeheadUserService.fetchFirstUsersOrderByModifiedAtDesc(sessionUser.getEmail(), projectBridgehead.get()) :
                Optional.empty();
    }

}
