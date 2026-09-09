package de.samply.form.template;

import de.samply.bridgehead.BridgeheadsConfiguration;
import de.samply.db.model.CreatorUser;
import de.samply.db.model.Project;
import de.samply.db.model.ProjectBridgehead;
import de.samply.db.model.ProjectDocument;
import de.samply.db.model.Query;
import de.samply.db.model.QueryOutput;
import de.samply.db.model.User;
import de.samply.db.repository.ProjectBridgeheadRepository;
import de.samply.document.DocumentService;
import de.samply.document.DocumentType;
import de.samply.project.ProjectType;
import de.samply.query.OutputFormat;
import de.samply.query.QueryFormat;
import de.samply.user.UserService;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProjectContextFactoryTest {

    @Test
    void resolvesNativeFieldsFromTheQueryAndDocumentsWhenPresent() {
        Project project = projectWithQuery(query -> {
            query.setContext("EXPORT_TARGET=cluster-a");
            query.setQueryFormat(QueryFormat.CQL);
            query.setCohortDefinition("Only include adult patients.");
            query.setHumanReadable("Patients with diagnosis X");
        });

        DocumentService documentService = mock(DocumentService.class);
        ProjectDocument votumForAllSites = documentWithFilename("votum-all-sites.pdf");
        ProjectDocument description = documentWithFilename("project-description.pdf");
        when(documentService.fetchLastDocumentOfThisType(project, Optional.empty(), DocumentType.VOTUM))
                .thenReturn(Optional.of(votumForAllSites));
        when(documentService.fetchLastDocumentOfThisType(project, Optional.empty(), DocumentType.DESCRIPTION))
                .thenReturn(Optional.of(description));

        ProjectContextFactory factory = factory(documentService, mock(ProjectBridgeheadRepository.class), null);

        Map<String, String> context = factory.createProjectContext(project, "en").fetchContext();

        assertThat(context)
                .containsEntry(ProjectContextKey.ETHICS_VOTE_FOR_ALL_SITES_FILENAME.getText(), "votum-all-sites.pdf")
                .containsEntry(ProjectContextKey.DESCRIPTION_UPLOAD_FILENAME.getText(), "project-description.pdf")
                .containsEntry(ProjectContextKey.ENVIRONMENT_VARIABLES.getText(), "EXPORT_TARGET=cluster-a")
                .containsEntry(ProjectContextKey.QUERY_FORMAT.getText(), "CQL")
                .containsEntry(ProjectContextKey.ADDITIONAL_FILTER_CRITERIA.getText(), "Only include adult patients.")
                .containsEntry(ProjectContextKey.SELECTED_COHORT.getText(), "Patients with diagnosis X");
    }

    @Test
    void resolvesNativeFieldsToBlankRatherThanLeavingAnUnresolvedPlaceholder() {
        // These keys are each used as a standalone project_fields value in
        // configuration, so they must always be present (blank when unset)
        // rather than omitted - an omitted key would leave a literal
        // "${...}" placeholder in the generated PDF instead of a blank field.
        Project project = projectWithQuery(query -> {
            // context, cohortDefinition, humanReadable, queryFormat, outputs all left unset.
        });

        DocumentService documentService = mock(DocumentService.class);
        when(documentService.fetchLastDocumentOfThisType(project, Optional.empty(), DocumentType.VOTUM))
                .thenReturn(Optional.empty());
        when(documentService.fetchLastDocumentOfThisType(project, Optional.empty(), DocumentType.DESCRIPTION))
                .thenReturn(Optional.empty());

        ProjectContextFactory factory = factory(documentService, mock(ProjectBridgeheadRepository.class), null);

        Map<String, String> context = factory.createProjectContext(project, "en").fetchContext();

        assertThat(context)
                .containsEntry(ProjectContextKey.ETHICS_VOTE_FOR_ALL_SITES_FILENAME.getText(), "")
                .containsEntry(ProjectContextKey.DESCRIPTION_UPLOAD_FILENAME.getText(), "")
                .containsEntry(ProjectContextKey.ENVIRONMENT_VARIABLES.getText(), "")
                .containsEntry(ProjectContextKey.QUERY_FORMAT.getText(), "")
                .containsEntry(ProjectContextKey.ADDITIONAL_FILTER_CRITERIA.getText(), "")
                .containsEntry(ProjectContextKey.SELECTED_COHORT.getText(), "")
                .containsEntry(ProjectContextKey.QUERIED_SITES.getText(), "")
                .containsEntry(ProjectContextKey.ETHICS_VOTES_PER_SITE.getText(), "")
                .containsEntry(ProjectContextKey.PROJECT_TYPES.getText(), "")
                .containsEntry(ProjectContextKey.OUTPUT_FORMATS.getText(), "")
                .containsEntry(ProjectContextKey.TEMPLATE_IDS.getText(), "")
                .containsEntry(ProjectContextKey.PROJECT_CONFIGURATION.getText(), "");
    }

    @Test
    void resolvesQueriedSitesAndPerSiteEthicsVotes() {
        Project project = projectWithQuery(query -> {
        });

        // Distinct ids matter: ProjectBridgehead's equals/hashCode is
        // id-only (@EqualsAndHashCode(onlyExplicitlyIncluded = true)), so two
        // unpersisted instances (id == null) would otherwise collapse into
        // one element once placed in a Set.
        ProjectBridgehead siteA = bridgehead(1L, "bridgehead-a");
        ProjectBridgehead siteB = bridgehead(2L, "bridgehead-b");
        ProjectBridgeheadRepository bridgeheadRepository = mock(ProjectBridgeheadRepository.class);
        when(bridgeheadRepository.findByProject(project)).thenReturn(orderedSet(siteA, siteB));

        BridgeheadsConfiguration bridgeheadsConfiguration = mock(BridgeheadsConfiguration.class);
        when(bridgeheadsConfiguration.getHumanReadable("bridgehead-a")).thenReturn(Optional.of("Site A"));
        when(bridgeheadsConfiguration.getHumanReadable("bridgehead-b")).thenReturn(Optional.of("Site B"));

        DocumentService documentService = mock(DocumentService.class);
        when(documentService.fetchLastDocumentOfThisType(project, Optional.empty(), DocumentType.VOTUM))
                .thenReturn(Optional.empty());
        when(documentService.fetchLastDocumentOfThisType(project, Optional.empty(), DocumentType.DESCRIPTION))
                .thenReturn(Optional.empty());
        when(documentService.fetchLastDocumentOfThisType(project, Optional.of(siteA), DocumentType.VOTUM))
                .thenReturn(Optional.of(documentWithFilename("vote-a.pdf")));
        when(documentService.fetchLastDocumentOfThisType(project, Optional.of(siteB), DocumentType.VOTUM))
                .thenReturn(Optional.empty());

        ProjectContextFactory factory = factory(documentService, bridgeheadRepository, bridgeheadsConfiguration);

        Map<String, String> context = factory.createProjectContext(project, "en").fetchContext();

        assertThat(context).containsEntry(ProjectContextKey.QUERIED_SITES.getText(), "Site A,Site B");
        assertThat(context).containsEntry(ProjectContextKey.ETHICS_VOTES_PER_SITE.getText(),
                "Site A: vote-a.pdf\nSite B: not uploaded");
    }

    @Test
    void resolvesProjectOutputsAndCustomConfiguration() {
        Project project = projectWithQuery(query -> query.addOutput(output(ProjectType.EXPORT, OutputFormat.CSV, "template-1")));
        project.setIsCustomConfigSelected(true);

        DocumentService documentService = mock(DocumentService.class);
        when(documentService.fetchLastDocumentOfThisType(project, Optional.empty(), DocumentType.VOTUM))
                .thenReturn(Optional.empty());
        when(documentService.fetchLastDocumentOfThisType(project, Optional.empty(), DocumentType.DESCRIPTION))
                .thenReturn(Optional.empty());

        ProjectContextFactory factory = factory(documentService, mock(ProjectBridgeheadRepository.class), null);

        Map<String, String> context = factory.createProjectContext(project, "en").fetchContext();

        assertThat(context)
                .containsEntry(ProjectContextKey.PROJECT_TYPES.getText(), "EXPORT")
                .containsEntry(ProjectContextKey.OUTPUT_FORMATS.getText(), "CSV")
                .containsEntry(ProjectContextKey.TEMPLATE_IDS.getText(), "template-1")
                .containsEntry(ProjectContextKey.PROJECT_CONFIGURATION.getText(), "Custom");
    }

    @Test
    void appendsAffiliationInParenthesesWhenTheCreatorHasOne() {
        Project project = projectWithQuery(query -> {
        });
        UserService userService = mock(UserService.class);
        User creator = new User();
        creator.setEmail("creator@example.org");
        creator.setFirstName("Anna");
        creator.setLastName("Weber");
        when(userService.fetchUser("creator@example.org")).thenReturn(Optional.of(creator));
        CreatorUser creatorUser = new CreatorUser();
        creatorUser.setBridgehead("bridgehead-a");
        when(userService.fetchCreatorUser("creator@example.org")).thenReturn(Set.of(creatorUser));
        BridgeheadsConfiguration bridgeheadsConfiguration = mock(BridgeheadsConfiguration.class);
        when(bridgeheadsConfiguration.getAffiliation("bridgehead-a")).thenReturn(Optional.of("University X"));

        DocumentService documentService = documentServiceWithNoDocuments(project);
        ProjectContextFactory factory = new ProjectContextFactory(
                userService, bridgeheadsConfiguration, "yyyy-MM-dd", documentService,
                mock(ProjectBridgeheadRepository.class));

        Map<String, String> context = factory.createProjectContext(project, "en").fetchContext();

        assertThat(context).containsEntry(
                ProjectContextKey.CREATOR_NAME_WITH_AFFILIATIONS.getText(), "Anna Weber (University X)");
    }

    @Test
    void omitsParenthesesWhenTheCreatorHasNoAffiliation() {
        // Regression test for point 1 (2026-09-09 feedback): a creator with
        // no known affiliation must show just their name, not a dangling
        // empty "()" or an unresolved "${creator-affiliations}" placeholder.
        Project project = projectWithQuery(query -> {
        });
        UserService userService = mock(UserService.class);
        User creator = new User();
        creator.setEmail("creator@example.org");
        creator.setFirstName("Anna");
        creator.setLastName("Weber");
        when(userService.fetchUser("creator@example.org")).thenReturn(Optional.of(creator));
        when(userService.fetchCreatorUser("creator@example.org")).thenReturn(Set.of());

        DocumentService documentService = documentServiceWithNoDocuments(project);
        ProjectContextFactory factory = new ProjectContextFactory(
                userService, mock(BridgeheadsConfiguration.class), "yyyy-MM-dd", documentService,
                mock(ProjectBridgeheadRepository.class));

        Map<String, String> context = factory.createProjectContext(project, "en").fetchContext();

        assertThat(context).containsEntry(
                ProjectContextKey.CREATOR_NAME_WITH_AFFILIATIONS.getText(), "Anna Weber");
    }

    private static DocumentService documentServiceWithNoDocuments(Project project) {
        DocumentService documentService = mock(DocumentService.class);
        when(documentService.fetchLastDocumentOfThisType(project, Optional.empty(), DocumentType.VOTUM))
                .thenReturn(Optional.empty());
        when(documentService.fetchLastDocumentOfThisType(project, Optional.empty(), DocumentType.DESCRIPTION))
                .thenReturn(Optional.empty());
        return documentService;
    }

    private static ProjectDocument documentWithFilename(String filename) {
        ProjectDocument document = new ProjectDocument();
        document.setOriginalFilename(filename);
        return document;
    }

    private static ProjectBridgehead bridgehead(Long id, String bridgehead) {
        ProjectBridgehead projectBridgehead = new ProjectBridgehead();
        projectBridgehead.setId(id);
        projectBridgehead.setBridgehead(bridgehead);
        return projectBridgehead;
    }

    private static QueryOutput output(ProjectType projectType, OutputFormat outputFormat, String templateId) {
        QueryOutput output = new QueryOutput();
        output.setProjectType(projectType);
        output.setOutputFormat(outputFormat);
        output.setTemplateId(templateId);
        return output;
    }

    // Preserves insertion order so assertions on the joined (comma-separated)
    // string don't flake on Set iteration order.
    private static Set<ProjectBridgehead> orderedSet(ProjectBridgehead... bridgeheads) {
        return new LinkedHashSet<>(java.util.List.of(bridgeheads));
    }

    private static Project projectWithQuery(Consumer<Query> queryCustomizer) {
        Query query = new Query();
        query.setLabel("Project title");
        query.setDescription("Project description");
        queryCustomizer.accept(query);

        Project project = new Project();
        project.setCode("PROJ-001");
        project.setCreatorEmail("creator@example.org");
        project.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        project.setQuery(query);
        return project;
    }

    private static ProjectContextFactory factory(
            DocumentService documentService,
            ProjectBridgeheadRepository bridgeheadRepository,
            BridgeheadsConfiguration bridgeheadsConfiguration) {
        BridgeheadsConfiguration configuration = bridgeheadsConfiguration != null
                ? bridgeheadsConfiguration : mock(BridgeheadsConfiguration.class);
        return new ProjectContextFactory(
                mock(UserService.class), configuration, "yyyy-MM-dd", documentService, bridgeheadRepository);
    }
}
