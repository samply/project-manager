package de.samply.project;

import de.samply.db.model.Project;
import de.samply.db.model.Query;
import de.samply.db.model.QueryOutput;
import de.samply.db.repository.ProjectRepository;
import de.samply.modules.OptionalModules;
import de.samply.project.state.ProjectState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UnfinishedRequestsCheckTest {

    private ProjectRepository projectRepository;
    private OptionalModules optionalModules;
    private UnfinishedRequestsCheck check;

    @BeforeEach
    void setUp() {
        projectRepository = mock(ProjectRepository.class);
        optionalModules = mock(OptionalModules.class);
        when(optionalModules.isAvailable(any())).thenReturn(true);
        when(optionalModules.describeUnavailable(ProjectType.DATASHIELD))
                .thenReturn("Request type DATASHIELD requires module DATASHIELD (ENABLE_DATASHIELD=false)");
        check = new UnfinishedRequestsCheck(projectRepository, optionalModules);
    }

    @Test
    void doesNotAskTheDatabaseWhenAllTypesAreAvailable() {
        check.check();

        verify(projectRepository, never()).findByStateInAndProjectTypeIn(any(), any());
    }

    @Test
    void startsWhenNoUnfinishedRequestNeedsADisabledModule() {
        when(optionalModules.isAvailable(ProjectType.DATASHIELD)).thenReturn(false);
        when(projectRepository.findByStateInAndProjectTypeIn(UnfinishedRequestsCheck.NOT_FINISHED, List.of(ProjectType.DATASHIELD)))
                .thenReturn(List.of());

        assertThatCode(check::check).doesNotThrowAnyException();
    }

    @Test
    void stopsTheStartWithTheRequestsAndTheSqlToCloseThem() {
        when(optionalModules.isAvailable(ProjectType.DATASHIELD)).thenReturn(false);
        when(projectRepository.findByStateInAndProjectTypeIn(eq(UnfinishedRequestsCheck.NOT_FINISHED), eq(List.of(ProjectType.DATASHIELD))))
                .thenReturn(List.of(project("REQ-2", ProjectState.DRAFT), project("REQ-1", ProjectState.FINAL),
                        project("REQ-3", ProjectState.DEVELOP)));

        assertThatThrownBy(check::check)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("""
                        The backend does not start: 3 request(s) not finished need disabled modules.
                          REQ-1 (FINAL): Request type DATASHIELD requires module DATASHIELD (ENABLE_DATASHIELD=false)
                          REQ-2 (DRAFT): Request type DATASHIELD requires module DATASHIELD (ENABLE_DATASHIELD=false)
                          REQ-3 (DEVELOP): Request type DATASHIELD requires module DATASHIELD (ENABLE_DATASHIELD=false)
                        Either enable the modules again and archive or finish these requests in the UI, or close them with SQL:
                          UPDATE samply.project SET state = 'ARCHIVED', archived_at = now(), modified_at = now() WHERE code IN ('REQ-1', 'REQ-3');
                          UPDATE samply.project SET state = 'REJECTED', modified_at = now() WHERE code IN ('REQ-2'); -- drafts cannot be archived""");
    }

    private Project project(String code, ProjectState state) {
        QueryOutput output = new QueryOutput();
        output.setProjectType(ProjectType.DATASHIELD);
        Query query = new Query();
        query.addOutput(output);
        Project project = new Project();
        project.setCode(code);
        project.setState(state);
        project.setQuery(query);
        return project;
    }

}
