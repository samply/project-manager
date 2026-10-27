package de.samply.project;

import de.samply.db.model.Project;
import de.samply.db.repository.ProjectRepository;
import de.samply.form.core.FormService;
import de.samply.frontend.dto.configuration.ProjectConfigurations;
import de.samply.notification.NotificationService;
import de.samply.project.state.ProjectState;
import de.samply.query.QueryPersistenceService;
import de.samply.security.SessionUser;
import de.samply.user.roles.OrganisationRole;
import de.samply.user.roles.UserOrganisationRoles;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

// Project manager admins and the central approval committee see every project except the drafts of others
class ProjectServiceVisibilityTest {

    @Test
    void projectManagerAdminSeesEveryProjectThatIsNoDraft() {
        CriteriaBuilder criteriaBuilder = visibilityCriteria(OrganisationRole.PROJECT_MANAGER_ADMIN);

        verify(criteriaBuilder).notEqual(any(), eq(ProjectState.DRAFT));
        // Own drafts stay visible through the creator criterion
        verify(criteriaBuilder, atLeastOnce()).equal(any(), eq("viewer@example.org"));
    }

    @Test
    void committeeSeesEveryProjectThatIsNoDraft() {
        CriteriaBuilder criteriaBuilder = visibilityCriteria(OrganisationRole.CENTRAL_APPROVAL_COMMITTEE);

        verify(criteriaBuilder).notEqual(any(), eq(ProjectState.DRAFT));
    }

    @Test
    void researcherHasNoSuchRule() {
        CriteriaBuilder criteriaBuilder = visibilityCriteria(OrganisationRole.RESEARCHER);

        verify(criteriaBuilder, never()).notEqual(any(), eq(ProjectState.DRAFT));
    }

    @SuppressWarnings("unchecked")
    private CriteriaBuilder visibilityCriteria(OrganisationRole role) {
        ProjectRepository projectRepository = mock(ProjectRepository.class);
        when(projectRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of());
        SessionUser sessionUser = new SessionUser(true);
        sessionUser.setEmail("viewer@example.org");
        UserOrganisationRoles roles = new UserOrganisationRoles();
        roles.addRoleNotDependentOnBridgehead(role);
        sessionUser.setUserOrganisationRoles(roles);
        ProjectService service = new ProjectService(
                mock(NotificationService.class), projectRepository, sessionUser,
                mock(ProjectConfigurations.class), mock(FormService.class),
                mock(QueryPersistenceService.class), mock(ProjectBridgeheadService.class),
                mock(ProjectBridgeheadUserService.class));

        service.fetchAllUserVisibleProjects();

        ArgumentCaptor<Specification<Project>> specification = ArgumentCaptor.forClass(Specification.class);
        verify(projectRepository).findAll(specification.capture(), any(Sort.class));
        CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class, RETURNS_DEEP_STUBS);
        specification.getValue().toPredicate(mock(Root.class, RETURNS_DEEP_STUBS),
                mock(CriteriaQuery.class, RETURNS_DEEP_STUBS), criteriaBuilder);
        return criteriaBuilder;
    }
}
