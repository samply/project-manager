package de.samply.form.core;

import de.samply.db.model.Project;
import de.samply.db.model.ProjectForm;
import de.samply.db.model.ProjectFormField;
import de.samply.db.repository.ProjectFormFieldRepository;
import de.samply.db.repository.ProjectFormRepository;
import de.samply.form.core.condition.FormFieldConditionEvaluator;
import de.samply.frontend.dto.DtoFactory;
import de.samply.frontend.dto.Form;
import de.samply.frontend.dto.FormField;
import de.samply.frontend.dto.ProjectAndForms;
import de.samply.notification.NotificationService;
import de.samply.project.DtoProjectService;
import de.samply.project.state.ProjectState;
import de.samply.security.SessionUser;
import de.samply.utils.directory.ExistingDirectory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * A form marked "active": false is no longer offered to projects that do not
 * use it; projects that selected it or have a value in it keep it.
 */
class InactiveFormTest {

    private final FormConfig formConfig = mock(FormConfig.class);
    private final ProjectFormFieldRepository values = mock(ProjectFormFieldRepository.class);
    private final ProjectFormRepository selections = mock(ProjectFormRepository.class);
    private final FormService formService = new FormService(
            formConfig, values, selections, mock(NotificationService.class), mock(SessionUser.class));
    private final Project project = new Project();

    InactiveFormTest() {
        project.setState(ProjectState.DRAFT);
        when(formConfig.getFormTitleLabelFieldMap()).thenReturn(Map.of("ethics", Map.of(), "ethics-v2", Map.of()));
        when(formConfig.isFormInactive("ethics")).thenReturn(true);
        when(selections.findByProjectAndFormTitle(any(), anyString())).thenReturn(Optional.empty());
    }

    @Test
    void readsTheFormLevelActiveFlag(@TempDir Path directory) throws Exception {
        Files.writeString(directory.resolve("ethics.json"), "{\"title\": \"ethics\", \"active\": false, \"fields\": []}");
        Files.writeString(directory.resolve("ethics-v2.json"), "{\"title\": \"ethics-v2\", \"fields\": []}");

        FormConfig config = new FormConfig(new ExistingDirectory(directory));

        assertThat(config.isFormInactive("ethics")).isTrue();
        assertThat(config.isFormInactive("ethics-v2")).isFalse();
    }

    @Test
    void aProjectUsesAFormItSelectedOrHasANonBlankValueIn() {
        assertThat(formService.isFormInUse(project, "ethics")).isFalse();

        when(values.findByProjectAndFormTitle(project, "ethics")).thenReturn(List.of(value(" ")));
        assertThat(formService.isFormInUse(project, "ethics")).isFalse();

        when(values.findByProjectAndFormTitle(project, "ethics")).thenReturn(List.of(value("approved")));
        assertThat(formService.isFormInUse(project, "ethics")).isTrue();

        when(values.findByProjectAndFormTitle(project, "ethics")).thenReturn(List.of());
        when(selections.findByProjectAndFormTitle(project, "ethics")).thenReturn(Optional.of(new ProjectForm()));
        assertThat(formService.isFormInUse(project, "ethics")).isTrue();
    }

    @Test
    void anInactiveFormCannotBeAddedToAProjectThatDoesNotUseIt() {
        assertThatThrownBy(() -> formService.addSelectedForm(project, "ethics"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("inactive");
        verify(selections, never()).save(any());
    }

    @Test
    void anInactiveFormCanBeSelectedByAProjectWithValuesInIt() {
        when(values.findByProjectAndFormTitle(project, "ethics")).thenReturn(List.of(value("approved")));

        formService.addSelectedForm(project, "ethics");

        verify(selections).save(any(ProjectForm.class));
    }

    @Test
    void aConfigurationDoesNotAddAnInactiveFormToAProjectThatDoesNotUseIt() {
        when(selections.findByProject(project)).thenReturn(List.of());

        formService.syncSelectedForms(project, List.of("ethics", "ethics-v2"));

        ArgumentCaptor<ProjectForm> saved = ArgumentCaptor.forClass(ProjectForm.class);
        verify(selections).save(saved.capture());
        assertThat(saved.getValue().getFormTitle()).isEqualTo("ethics-v2");
    }

    @Test
    void selectedFormsAndOfferedTitlesLeaveOutAnInactiveFormTheProjectDoesNotUse() throws Exception {
        DtoFactory dtoFactory = mock(DtoFactory.class);
        when(dtoFactory.convertForm(anyString(), any(), any())).thenAnswer(invocation ->
                new Form(invocation.getArgument(0), null, null, null));
        when(dtoFactory.convert(anyString(), any(Optional.class), any(), any(), any(), any(), any())).thenAnswer(invocation ->
                FormField.builder().title(invocation.getArgument(0)).build());
        DtoProjectService dtoProjectService = mock(DtoProjectService.class);
        when(dtoProjectService.fetchCurrentProjectConfigurations(project)).thenReturn(List.of(new ProjectAndForms(
                null, new Form[]{new Form("ethics", null, null, null), new Form("ethics-v2", null, null, null)}, null)));
        when(selections.findByProject(project)).thenReturn(List.of());
        DtoFormService dtoFormService = new DtoFormService(
                formService, dtoFactory, formConfig, dtoProjectService, mock(FormFieldConditionEvaluator.class));

        assertThat(dtoFormService.fetchSelectedForms(project, Optional.of("en")))
                .extracting(Form::title).containsExactly("ethics-v2");
        assertThat(dtoFormService.fetchProjectFormTitles(project, Optional.of("en")))
                .extracting(FormField::title).containsExactly("ethics-v2");

        // Once the project has a value in it, the inactive form is part of it again.
        when(values.findByProjectAndFormTitle(eq(project), eq("ethics"))).thenReturn(List.of(value("approved")));
        assertThat(dtoFormService.fetchSelectedForms(project, Optional.of("en")))
                .extracting(Form::title).containsExactlyInAnyOrder("ethics", "ethics-v2");
        assertThat(dtoFormService.fetchProjectFormTitles(project, Optional.of("en")))
                .extracting(FormField::title).containsExactlyInAnyOrder("ethics", "ethics-v2");
    }

    private static ProjectFormField value(String value) {
        ProjectFormField field = new ProjectFormField();
        field.setValue(value);
        return field;
    }
}
