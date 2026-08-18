package de.samply.aop;

import de.samply.annotations.ProjectConstraints;
import de.samply.db.model.Project;
import de.samply.db.model.Query;
import de.samply.project.ProjectBridgeheadUserService;
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

class ConstraintsServiceProjectConstraintsTest {

    private ConstraintsService constraintsService;

    @BeforeEach
    void setUp() {
        constraintsService = new ConstraintsService(
                mock(ProjectBridgeheadUserService.class),
                mock(OrganisationRoleToProjectRoleMapper.class),
                mock(SessionUser.class));
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
    }
}
