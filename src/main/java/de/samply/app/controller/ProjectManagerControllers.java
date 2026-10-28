package de.samply.app.controller;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 * All controllers whose endpoints are read by reflection: the frontend actions (FrontendService)
 * and the roles per path (RolesExtractor). A new controller must be added here
 * (checked by ProjectManagerControllersTest).
 */
public final class ProjectManagerControllers {

    // One controller per entity, ordered along the relations of the entities:
    // Query <- Project <- (ProjectForm, ProjectDocument, Notification, ProjectBridgehead
    // <- (ProjectBridgeheadUser, ProjectBridgeheadExecution)); User stands on its own.
    public static final List<Class<? extends AbstractController>> CONTROLLERS = List.of(
            ApplicationController.class,
            QueryController.class,
            ProjectController.class,
            ProjectStateController.class,
            ProjectFormController.class,
            ProjectDocumentController.class,
            NotificationController.class,
            ProjectBridgeheadController.class,
            ProjectBridgeheadUserController.class,
            ProjectBridgeheadExecutionController.class,
            UserController.class);

    private ProjectManagerControllers() {
    }

    public static Stream<Method> fetchDeclaredMethods() {
        return CONTROLLERS.stream().flatMap(controller -> Arrays.stream(controller.getDeclaredMethods()));
    }

}
