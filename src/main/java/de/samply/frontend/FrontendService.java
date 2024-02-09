package de.samply.frontend;

import de.samply.annotations.*;
import de.samply.aop.ConstraintsService;
import de.samply.app.ProjectManagerController;
import de.samply.user.roles.RolesExtractor;
import de.samply.utils.AspectUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.lang.reflect.Method;
import java.util.*;

@Service
public class FrontendService {

    private final ConstraintsService constraintsService;
    private final FrontendConfiguration frontendConfiguration;

    public FrontendService(
            ConstraintsService constraintsService,
            FrontendConfiguration frontendConfiguration) {
        this.constraintsService = constraintsService;
        this.frontendConfiguration = frontendConfiguration;
    }

    public Map<String, Map<String, Action>> fetchModuleActionPackage(String site, Optional<String> projectCode, Optional<String> bridgehead, boolean withConstraints) {
        Map<String, Map<String, Action>> moduleActionMap = new HashMap<>();
        String rootPath = RolesExtractor.getRootPath();
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
                    fetchModuleActionsPackages(moduleActionMap, rootPath, path, tempFrontendSiteModule, frontendAction, site, projectCode, bridgehead, method, withConstraints));
        });
        return moduleActionMap;
    }

    private void fetchModuleActionsPackages(Map<String, Map<String, Action>> moduleActionsMap,
                                            String rootPath, Optional<String> path,
                                            FrontendSiteModule frontendSiteModule, FrontendAction frontendAction,
                                            String site, Optional<String> projectCode,
                                            Optional<String> bridgehead, Method method,
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
                addAction(moduleActionsMap, frontendSiteModule, frontendAction, rootPath, path, method);
            }
        }
    }

    private void addAction(Map<String, Map<String, Action>> moduleActionsMap, FrontendSiteModule frontendSiteModule, FrontendAction frontendAction, String rootPath, Optional<String> path, Method method) {
        Map<String, Action> actionNameActionsMap = moduleActionsMap.get(frontendSiteModule.module());
        if (actionNameActionsMap == null) {
            actionNameActionsMap = new HashMap<>();
            moduleActionsMap.put(frontendSiteModule.module(), actionNameActionsMap);
        }
        actionNameActionsMap.put(frontendAction.action(),
                new Action(rootPath + path.get(), fetchHttpMethod(method), fetchHttpParams(method)));
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

}
