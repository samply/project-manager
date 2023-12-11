package de.samply.frontend;

import de.samply.annotations.FrontendAction;
import de.samply.annotations.FrontendSiteModule;
import de.samply.annotations.RoleConstraints;
import de.samply.annotations.StateConstraints;
import de.samply.aop.ConstraintsService;
import de.samply.app.ProjectManagerController;
import de.samply.user.roles.RolesExtractor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class FrontendService {

    private final ConstraintsService constraintsService;

    public FrontendService(ConstraintsService constraintsService) {
        this.constraintsService = constraintsService;
    }

    public Collection<ModuleActionsPackage> fetchModuleActionPackage(String site, Optional<String> projectCode, Optional<String> bridgehead) {
        Map<String, ModuleActionsPackage> moduleModuleActionsPackageMap = new HashMap<>();
        String rootPath = RolesExtractor.getRootPath();
        Arrays.stream(ProjectManagerController.class.getDeclaredMethods()).forEach(method -> {
            FrontendSiteModule frontendSiteModule = method.getAnnotation(FrontendSiteModule.class);
            FrontendAction frontendAction = method.getAnnotation((FrontendAction.class));
            Optional<String> path = RolesExtractor.fetchPath(method);

            if (frontendSiteModule != null && site.equals(frontendSiteModule.site()) && frontendAction != null && path.isPresent()) {
                Optional<RoleConstraints> roleConstraints = Optional.ofNullable(method.getAnnotation(RoleConstraints.class));
                Optional<ResponseEntity> responseEntity = this.constraintsService.checkRoleConstraints(roleConstraints, projectCode, bridgehead);
                if (responseEntity.isEmpty()) {
                    Optional<StateConstraints> stateConstraints = Optional.ofNullable(method.getAnnotation(StateConstraints.class));
                    responseEntity = this.constraintsService.checkStateConstraints(stateConstraints, projectCode, bridgehead);
                }
                if (responseEntity.isEmpty()) { // If there are no restrictions
                    ModuleActionsPackage moduleActionsPackage = moduleModuleActionsPackageMap.get(frontendSiteModule.module());
                    if (moduleActionsPackage == null) {
                        moduleActionsPackage = new ModuleActionsPackage();
                        moduleActionsPackage.setModule(frontendSiteModule.module());
                        moduleModuleActionsPackageMap.put(frontendSiteModule.module(), moduleActionsPackage);
                    }
                    moduleActionsPackage.addAction(new Action(frontendAction.action(), rootPath + path.get()));
                }
            }
        });
        return moduleModuleActionsPackageMap.values();
    }

}
