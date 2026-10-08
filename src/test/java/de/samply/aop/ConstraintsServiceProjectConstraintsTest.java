package de.samply.aop;

import de.samply.annotations.ProjectConstraints;
import de.samply.db.model.Project;
import de.samply.db.model.Query;
import de.samply.modules.OptionalModules;
import de.samply.project.ProjectBridgeheadUserService;
import de.samply.project.ProjectType;
import de.samply.query.QueryFormat;
import de.samply.security.SessionUser;
import de.samply.user.roles.OrganisationRoleToProjectRoleMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.lang.reflect.Method;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConstraintsServiceProjectConstraintsTest {

    private ConstraintsService constraintsService;
    private OptionalModules optionalModules;

    @BeforeEach
    void setUp() {
        optionalModules = mock(OptionalModules.class);
        constraintsService = new ConstraintsService(
                mock(ProjectBridgeheadUserService.class),
                mock(OrganisationRoleToProjectRoleMapper.class),
                mock(SessionUser.class),
                optionalModules);
    }

    @Test
    void acceptsProjectOfAnAvailableType() throws NoSuchMethodException {
        when(optionalModules.isAvailable(ProjectType.DATASHIELD)).thenReturn(true);

        assertThat(constraintsService.checkProjectConstraints(constraintsFrom("dataShieldOnly"), projectOfType(ProjectType.DATASHIELD)))
                .isEmpty();
    }

    // A request created before its type's module was disabled: its actions are neither offered nor accepted
    @Test
    void rejectsProjectOfATypeWhoseModulesAreDisabled() throws NoSuchMethodException {
        when(optionalModules.isAvailable(ProjectType.DATASHIELD)).thenReturn(false);

        Optional<ResponseEntity> response = constraintsService.checkProjectConstraints(
                constraintsFrom("dataShieldOnly"), projectOfType(ProjectType.DATASHIELD));

        assertThat(response).isPresent();
        assertThat(response.orElseThrow().getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
    }

    private Optional<Project> projectOfType(ProjectType projectType) {
        Project project = mock(Project.class);
        when(project.hasProjectType(projectType)).thenReturn(true);
        return Optional.of(project);
    }

    @Test
    void acceptsProjectWithAllowedQueryFormat() throws NoSuchMethodException {
        Optional<ResponseEntity> response = constraintsService.checkProjectConstraints(
                constraintsFrom("astDataOnly"), projectWithQueryFormat(QueryFormat.AST_DATA));

        assertThat(response).isEmpty();
    }

    @Test
    void rejectsProjectWithDifferentQueryFormat() throws NoSuchMethodException {
        Optional<ResponseEntity> response = constraintsService.checkProjectConstraints(
                constraintsFrom("astDataOnly"), projectWithQueryFormat(QueryFormat.CQL_DATA));

        assertThat(response).isPresent();
        assertThat(response.orElseThrow().getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
    }

    @Test
    void acceptsProjectWhenAnyConfiguredQueryFormatMatches() throws NoSuchMethodException {
        Optional<ResponseEntity> response = constraintsService.checkProjectConstraints(
                constraintsFrom("multipleQueryFormats"), projectWithQueryFormat(QueryFormat.AST_DATA));

        assertThat(response).isEmpty();
    }

    @Test
    void rejectsMissingProjectWhenQueryFormatConstraintIsPresent() throws NoSuchMethodException {
        Optional<ResponseEntity> response = constraintsService.checkProjectConstraints(
                constraintsFrom("astDataOnly"), Optional.empty());

        assertThat(response).isPresent();
        assertThat(response.orElseThrow().getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void doesNotRestrictQueryFormatWhenNoFormatsAreConfigured() throws NoSuchMethodException {
        Optional<ResponseEntity> response = constraintsService.checkProjectConstraints(
                constraintsFrom("withoutQueryFormat"), projectWithQueryFormat(QueryFormat.CQL));

        assertThat(response).isEmpty();
    }

    private Optional<Project> projectWithQueryFormat(QueryFormat queryFormat) {
        Query query = new Query();
        query.setQueryFormat(queryFormat);
        Project project = new Project();
        project.setQuery(query);
        return Optional.of(project);
    }

    private Optional<ProjectConstraints> constraintsFrom(String methodName) throws NoSuchMethodException {
        Method method = ConstraintFixtures.class.getDeclaredMethod(methodName);
        return Optional.ofNullable(method.getAnnotation(ProjectConstraints.class));
    }

    private static class ConstraintFixtures {

        @ProjectConstraints(queryFormats = QueryFormat.AST_DATA)
        void astDataOnly() {
        }

        @ProjectConstraints(queryFormats = {QueryFormat.CQL, QueryFormat.AST_DATA})
        void multipleQueryFormats() {
        }

        @ProjectConstraints
        void withoutQueryFormat() {
        }

        @ProjectConstraints(projectTypes = ProjectType.DATASHIELD)
        void dataShieldOnly() {
        }
    }
}
