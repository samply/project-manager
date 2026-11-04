package de.samply.app;

import de.samply.annotations.RoleConstraints;
import de.samply.annotations.StateConstraints;
import de.samply.project.state.ProjectState;
import de.samply.user.roles.OrganisationRole;
import de.samply.user.roles.ProjectRole;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * What the central approval committee may do (plan "central committee" §4.2): read the request, accept it, request
 * changes. It never edits the form, rejects or archives; any new endpoint that changes something for the committee
 * has to be added here on purpose.
 */
class ProjectManagerControllerCentralApprovalCommitteeTest {

    private static boolean allowsCommittee(Method method) {
        RoleConstraints constraints = method.getAnnotation(RoleConstraints.class);
        return constraints != null && (
                Arrays.asList(constraints.organisationRoles()).contains(OrganisationRole.CENTRAL_APPROVAL_COMMITTEE)
                        || Arrays.asList(constraints.projectRoles()).contains(ProjectRole.CENTRAL_APPROVAL_COMMITTEE));
    }

    private static List<Method> committeeEndpoints() {
        return Arrays.stream(ProjectManagerController.class.getDeclaredMethods())
                .filter(ProjectManagerControllerCentralApprovalCommitteeTest::allowsCommittee)
                .toList();
    }

    private static Method endpoint(String name) {
        return Arrays.stream(ProjectManagerController.class.getDeclaredMethods())
                .filter(method -> method.getName().equals(name))
                .findFirst()
                .orElseThrow();
    }

    @Test
    void theCommitteeOnlyAcceptsAndRequestsChanges() {
        assertThat(committeeEndpoints().stream()
                .filter(method -> method.getAnnotation(GetMapping.class) == null)
                .map(Method::getName))
                .containsExactlyInAnyOrder("acceptProject", "requestChangesInApproval", "startLocalApprovalStage");
    }

    @Test
    void theCommitteeDoesNotEditRejectOrArchive() {
        List.of("editProject", "editProjectFormValues", "rejectProject", "archiveProject", "startDevelopStage",
                        "startFinalStage")
                .forEach(name -> assertThat(allowsCommittee(endpoint(name))).as(name).isFalse());
    }

    @Test
    void theCommitteeActsOnlyInTheApprovalStates() {
        assertThat(endpoint("acceptProject").getAnnotation(StateConstraints.class).projectStates())
                .containsExactlyInAnyOrder(ProjectState.REVIEW, ProjectState.ARCHIVED);
        assertThat(endpoint("requestChangesInApproval").getAnnotation(StateConstraints.class).projectStates())
                .containsExactlyInAnyOrder(ProjectState.REVIEW, ProjectState.CENTRAL_APPROVAL);
        assertThat(endpoint("startLocalApprovalStage").getAnnotation(StateConstraints.class).projectStates())
                .containsExactly(ProjectState.CENTRAL_APPROVAL);
    }

    @Test
    void theProjectManagerAdminCanDoEverythingTheCommitteeCanDo() {
        committeeEndpoints().forEach(method -> {
            RoleConstraints constraints = method.getAnnotation(RoleConstraints.class);
            assertThat(Arrays.asList(constraints.organisationRoles()).contains(OrganisationRole.PROJECT_MANAGER_ADMIN)
                    || Arrays.asList(constraints.projectRoles()).contains(ProjectRole.PROJECT_MANAGER_ADMIN))
                    .as(method.getName())
                    .isTrue();
        });
    }

}
