package de.samply.project;

import de.samply.db.model.Project;
import de.samply.db.repository.ProjectRepository;
import de.samply.form.FormService;
import de.samply.frontend.dto.configuration.ProjectConfigurations;
import de.samply.notification.NotificationService;
import de.samply.query.QueryPersistenceService;
import de.samply.security.SessionUser;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProjectServiceJpaFilterTest {

    @Test
    void appliesCreatorAndBridgeheadFiltersThroughRepositoryPaging() {
        ProjectRepository projectRepository = mock(ProjectRepository.class);
        when(projectRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(Page.empty());
        SessionUser sessionUser = new SessionUser(true);
        sessionUser.setEmail("viewer@example.org");
        ProjectService service = new ProjectService(
                mock(NotificationService.class), projectRepository, sessionUser,
                mock(ProjectConfigurations.class), mock(FormService.class),
                mock(QueryPersistenceService.class), mock(ProjectBridgeheadService.class),
                mock(ProjectBridgeheadUserService.class));

        PageRequest pageRequest = PageRequest.of(1, 10, Sort.by(Sort.Direction.DESC, "modifiedAt"));
        service.fetchUserVisibleProjects(
                Optional.empty(), Optional.empty(), pageRequest,
                Optional.of("creator@example.org"), Optional.of("site-a"));

        verify(projectRepository).findAll(any(Specification.class), eq(pageRequest));
    }
}
