package de.samply.frontend;

import de.samply.annotations.*;
import de.samply.aop.ConstraintsService;
import de.samply.app.ProjectManagerController;
import de.samply.user.roles.RolesExtractor;
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

    public Collection<ModuleActionsPackage> fetchModuleActionPackage(String site, Optional<String> projectCode, Optional<String> bridgehead) {
        Map<String, ModuleActionsPackage> moduleModuleActionsPackageMap = new HashMap<>();
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
                    fetchModuleActionsPackages(moduleModuleActionsPackageMap, rootPath, path, tempFrontendSiteModule, frontendAction, site, projectCode, bridgehead, method));
        });
        return moduleModuleActionsPackageMap.values();
    }

    private Map<String, ModuleActionsPackage> fetchModuleActionsPackages(Map<String, ModuleActionsPackage> moduleModuleActionsPackageMap,
                                                                         String rootPath, Optional<String> path,
                                                                         FrontendSiteModule frontendSiteModule, FrontendAction frontendAction,
                                                                         String site, Optional<String> projectCode,
                                                                         Optional<String> bridgehead, Method method) {
        if (frontendSiteModule != null && site.equals(frontendSiteModule.site()) && frontendAction != null && path.isPresent()) {
            Optional<RoleConstraints> roleConstraints = Optional.ofNullable(method.getAnnotation(RoleConstraints.class));
            Optional<ResponseEntity> responseEntity = this.constraintsService.checkRoleConstraints(roleConstraints, projectCode, bridgehead);
            if (responseEntity.isEmpty()) {
                Optional<StateConstraints> stateConstraints = Optional.ofNullable(method.getAnnotation(StateConstraints.class));
                responseEntity = this.constraintsService.checkStateConstraints(stateConstraints, projectCode, bridgehead);
            }
            if (responseEntity.isEmpty()) { // If there are no restrictions
                addAction(moduleModuleActionsPackageMap, frontendSiteModule, frontendAction, rootPath, path);
            }
        }
        return moduleModuleActionsPackageMap;
    }

    private void addAction(Map<String, ModuleActionsPackage> moduleModuleActionsPackageMap, FrontendSiteModule frontendSiteModule, FrontendAction frontendAction, String rootPath, Optional<String> path) {
        ModuleActionsPackage moduleActionsPackage = moduleModuleActionsPackageMap.get(frontendSiteModule.module());
        if (moduleActionsPackage == null) {
            moduleActionsPackage = new ModuleActionsPackage();
            moduleActionsPackage.setModule(frontendSiteModule.module());
            moduleModuleActionsPackageMap.put(frontendSiteModule.module(), moduleActionsPackage);
        }
        moduleActionsPackage.addAction(new Action(frontendAction.action(), rootPath + path.get()));
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
