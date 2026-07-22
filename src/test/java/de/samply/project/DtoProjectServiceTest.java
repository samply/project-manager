package de.samply.project;

import de.samply.frontend.dto.DtoFactory;
import de.samply.frontend.dto.configuration.ProjectConfigurations;
import de.samply.project.state.ProjectState;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DtoProjectServiceTest {

    @ParameterizedTest
    @CsvSource({"true,DESC", "false,ASC"})
    void delegatesFilteringAndAppliesRequestedSort(
            boolean modifiedDescendant, Sort.Direction expectedDirection) {
        ProjectService projectService = mock(ProjectService.class);
        DtoFactory dtoFactory = mock(DtoFactory.class);
        DtoProjectService service = new DtoProjectService(
                projectService, dtoFactory, mock(ProjectConfigurations.class));
        Optional<ProjectState> state = Optional.of(ProjectState.REVIEW);
        Optional<Boolean> archived = Optional.of(false);
        when(projectService.fetchUserVisibleProjects(eq(state), eq(archived), any(PageRequest.class)))
                .thenReturn(Page.empty());

        service.fetchUserVisibleProjects(state, archived, 2, 25, modifiedDescendant);

        ArgumentCaptor<PageRequest> pageRequest = ArgumentCaptor.forClass(PageRequest.class);
        verify(projectService).fetchUserVisibleProjects(eq(state), eq(archived), pageRequest.capture());
        assertThat(pageRequest.getValue().getPageNumber()).isEqualTo(2);
        assertThat(pageRequest.getValue().getPageSize()).isEqualTo(25);
        assertThat(pageRequest.getValue().getSort().getOrderFor("modifiedAt"))
                .isNotNull()
                .extracting(Sort.Order::getDirection)
                .isEqualTo(expectedDirection);
    }
}
