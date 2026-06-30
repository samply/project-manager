package de.samply.frontend;

import de.samply.annotations.*;
import de.samply.aop.ConstraintsService;
import de.samply.app.ProjectManagerConst;
import de.samply.app.ProjectManagerController;
import de.samply.db.model.Project;
import de.samply.db.model.ProjectBridgehead;
import de.samply.db.model.ProjectBridgeheadUser;
import de.samply.project.ProjectBridgeheadUserService;
import de.samply.security.SessionUser;
import de.samply.user.roles.RolesExtractor;
import de.samply.utils.AspectUtils;
import de.samply.utils.LanguageUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.util.Pair;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.lang.reflect.Method;
import java.util.*;

@Service
public class FrontendService {

    // Services
    private final ConstraintsService constraintsService;
    private final ProjectBridgeheadUserService projectBridgeheadUserService;

    private final FrontendConfiguration frontendConfiguration;
    private final ActionExplanations actionExplanations;
    private final SessionUser sessionUser;

    private final String explorerUrlRedirectUriParameter;
    private final String defaultLanguage;


    public FrontendService(
            ConstraintsService constraintsService, ProjectBridgeheadUserService projectBridgeheadUserService,
            FrontendConfiguration frontendConfiguration,
            ActionExplanations actionExplanations,
            SessionUser sessionUser,
            @Value(ProjectManagerConst.EXPLORER_REDIRECT_URI_PARAMETER_SV) String explorerUrlRedirectUriParameter,
            @Value(ProjectManagerConst.DEFAULT_LANGUAGE_SV) String defaultLanguage) {
        this.constraintsService = constraintsService;
        this.projectBridgeheadUserService = projectBridgeheadUserService;
        this.frontendConfiguration = frontendConfiguration;
        this.explorerUrlRedirectUriParameter = explorerUrlRedirectUriParameter;
        this.actionExplanations = actionExplanations;
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
        if (frontendSiteModule != null && site.equals(frontendSiteModule.site()) && frontendAction != null && path.isPresent()) {
            Optional<RoleConstraints> roleConstraints = Optional.ofNullable(method.getAnnotation(RoleConstraints.class));
            Optional<StateConstraints> stateConstraints = Optional.ofNullable(method.getAnnotation(StateConstraints.class));
            Optional<ResponseEntity> responseEntity = this.constraintsService.checkRoleConstraints(roleConstraints, stateConstraints, project, bridgehead);
            if (responseEntity.isEmpty()) {
                responseEntity = this.constraintsService.checkStateConstraints(stateConstraints, project, bridgehead);
            }
            if (responseEntity.isEmpty()) {
                Optional<ProjectConstraints> projectConstraints = Optional.ofNullable(method.getAnnotation(ProjectConstraints.class));
                responseEntity = this.constraintsService.checkProjectConstraints(projectConstraints, project);
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
        Optional<Pair<String, Integer>> explanationPriority = actionExplanations.fetchExplanationAndPriority(frontendAction.action(), frontendSiteModule.module(),
                language, project, projectBridgehead, projectBridgeheadUser, sessionUser);
        String explanation = explanationPriority.map(Pair::getFirst).orElse(null);
        Integer priority = explanationPriority.map(Pair::getSecond).orElse(null);
        String resolvedPath = path.orElseThrow(
                () -> new IllegalStateException("Path must be present for action " + frontendAction.action())
        );
        actionNameActionsMap.put(frontendAction.action(),
                new Action(rootPath + resolvedPath, fetchHttpMethod(method), fetchHttpParams(method), explanation, priority));
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
