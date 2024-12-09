package de.samply.frontend;

import de.samply.annotations.*;
import de.samply.aop.ConstraintsService;
import de.samply.app.ProjectManagerConst;
import de.samply.app.ProjectManagerController;
import de.samply.db.model.Project;
import de.samply.db.model.ProjectBridgehead;
import de.samply.db.model.ProjectBridgeheadUser;
import de.samply.db.repository.ProjectBridgeheadRepository;
import de.samply.db.repository.ProjectBridgeheadUserRepository;
import de.samply.db.repository.ProjectRepository;
import de.samply.security.SessionUser;
import de.samply.user.roles.RolesExtractor;
import de.samply.utils.AspectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.util.Pair;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.lang.reflect.Method;
import java.util.*;

@Service
public class FrontendService {

    private final ConstraintsService constraintsService;
    private final FrontendConfiguration frontendConfiguration;
    private final String explorerUrlRedirectUriParameter;
    private final ActionExplanations actionExplanations;
    private final String defaultLanguage;
    private final SessionUser sessionUser;
    private final ProjectRepository projectRepository;
    private final ProjectBridgeheadRepository projectBridgeheadRepository;
    private final ProjectBridgeheadUserRepository projectBridgeheadUserRepository;

    public FrontendService(
            ConstraintsService constraintsService,
            FrontendConfiguration frontendConfiguration,
            ActionExplanations actionExplanations,
            SessionUser sessionUser,
            ProjectBridgeheadUserRepository projectBridgeheadUserRepository,
            ProjectRepository projectRepository,
            ProjectBridgeheadRepository projectBridgeheadRepository,
            @Value(ProjectManagerConst.EXPLORER_REDIRECT_URI_PARAMETER_SV) String explorerUrlRedirectUriParameter,
            @Value(ProjectManagerConst.DEFAULT_LANGUAGE_SV) String defaultLanguage) {
        this.constraintsService = constraintsService;
        this.frontendConfiguration = frontendConfiguration;
        this.explorerUrlRedirectUriParameter = explorerUrlRedirectUriParameter;
        this.actionExplanations = actionExplanations;
        this.defaultLanguage = defaultLanguage;
        this.sessionUser = sessionUser;
        this.projectBridgeheadUserRepository = projectBridgeheadUserRepository;
        this.projectRepository = projectRepository;
        this.projectBridgeheadRepository = projectBridgeheadRepository;
    }

    public Map<String, Map<String, Action>> fetchModuleActionPackage(String site, Optional<String> projectCode,
                                                                     Optional<String> bridgehead, Optional<String> language, boolean withConstraints) {
        Map<String, Map<String, Action>> moduleActionMap = new HashMap<>();
        String rootPath = RolesExtractor.getRootPath();
        String tempLanguage = (language.isPresent()) ? language.get() : defaultLanguage;
        Optional<Project> project = fetchProject(projectCode);
        Optional<ProjectBridgehead> projectBridgehead = fetchProjectBridgehead(project, bridgehead);
        Optional<ProjectBridgeheadUser> projectBridgeheadUser = fetchProjectBridgeheadUser(projectBridgehead);
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
                            site, projectCode, project, projectBridgehead, projectBridgeheadUser, tempLanguage, bridgehead, method, withConstraints));
        });
        return moduleActionMap;
    }

    private void fetchModuleActionsPackages(Map<String, Map<String, Action>> moduleActionsMap,
                                            String rootPath, Optional<String> path,
                                            FrontendSiteModule frontendSiteModule, FrontendAction frontendAction,
                                            String site, Optional<String> projectCode,
                                            Optional<Project> project,
                                            Optional<ProjectBridgehead> projectBridgehead,
                                            Optional<ProjectBridgeheadUser> projectBridgeheadUser,
                                            String language, Optional<String> bridgehead, Method method,
                                            boolean withConstraints) {
        if (frontendSiteModule != null && site.equals(frontendSiteModule.site()) && frontendAction != null && path.isPresent()) {
            Optional<RoleConstraints> roleConstraints = Optional.ofNullable(method.getAnnotation(RoleConstraints.class));
            Optional<ResponseEntity> responseEntity = this.constraintsService.checkRoleConstraints(roleConstraints, projectCode, bridgehead);
            if (responseEntity.isEmpty()) {
                Optional<StateConstraints> stateConstraints = Optional.ofNullable(method.getAnnotation(StateConstraints.class));
                responseEntity = this.constraintsService.checkStateConstraints(stateConstraints, projectCode, bridgehead);
            }
            if (responseEntity.isEmpty()) {
                Optional<ProjectConstraints> projectConstraints = Optional.ofNullable(method.getAnnotation(ProjectConstraints.class));
                responseEntity = this.constraintsService.checkProjectConstraints(projectConstraints, projectCode);
            }
            if (responseEntity.isEmpty() || !withConstraints) { // If there are no restrictions
                addAction(moduleActionsMap, frontendSiteModule, frontendAction, rootPath, path, method,
                        project, projectBridgehead, projectBridgeheadUser, language);
            }
        }
    }

    private void addAction(Map<String, Map<String, Action>> moduleActionsMap,
                           FrontendSiteModule frontendSiteModule, FrontendAction frontendAction,
                           String rootPath, Optional<String> path, Method method,
                           Optional<Project> project, Optional<ProjectBridgehead> projectBridgehead,
                           Optional<ProjectBridgeheadUser> projectBridgeheadUser, String language) {
        Map<String, Action> actionNameActionsMap = moduleActionsMap.get(frontendSiteModule.module());
        if (actionNameActionsMap == null) {
            actionNameActionsMap = new HashMap<>();
            moduleActionsMap.put(frontendSiteModule.module(), actionNameActionsMap);
        }
        Optional<Pair<String, Integer>> explanationPriority = actionExplanations.fetchExplanationAndPriority(frontendAction.action(), frontendSiteModule.module(),
                language, project, projectBridgehead, projectBridgeheadUser, sessionUser);
        String explanation = explanationPriority.isPresent() ? explanationPriority.get().getFirst() : null;
        Integer priority = explanationPriority.isPresent() ? explanationPriority.get().getSecond() : null;
        actionNameActionsMap.put(frontendAction.action(),
                new Action(rootPath + path.get(), fetchHttpMethod(method), fetchHttpParams(method), explanation, priority));
    }

    private String fetchHttpMethod(Method method) {
        Optional<String> result = AspectUtils.fetchHttpMethod(method);
        return result.isPresent() ? result.get() : null;
    }

    private String[] fetchHttpParams(Method method) {
        return AspectUtils.fetchRequestParamNames(method);
    }

    public String fetchUrl(String site, Map<String, String> parameters) {
        UriComponentsBuilder result = UriComponentsBuilder.fromHttpUrl(frontendConfiguration.getBaseUrl());
        if (site != null) {
            Optional<String> sitePath = frontendConfiguration.getSitePath(site);
            if (sitePath.isPresent()) {
                result.path(sitePath.get());
            }
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

    private Optional<Project> fetchProject(Optional<String> projectCode) {
        return (projectCode.isPresent()) ? projectRepository.findByCode(projectCode.get()) : Optional.empty();
    }

    private Optional<ProjectBridgehead> fetchProjectBridgehead(Optional<Project> project, Optional<String> bridgehead) {
        return (project.isPresent() && bridgehead.isPresent()) ?
                projectBridgeheadRepository.findFirstByBridgeheadAndProject(bridgehead.get(), project.get()) :
                Optional.empty();
    }

    private Optional<ProjectBridgeheadUser> fetchProjectBridgeheadUser(Optional<ProjectBridgehead> projectBridgehead) {
        return (projectBridgehead.isPresent()) ?
                projectBridgeheadUserRepository.getFirstByEmailAndProjectBridgeheadOrderByModifiedAtDesc(sessionUser.getEmail(), projectBridgehead.get()) :
                Optional.empty();
    }


}
