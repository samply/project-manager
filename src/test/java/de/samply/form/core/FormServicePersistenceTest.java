package de.samply.form.core;

import de.samply.db.model.Project;
import de.samply.db.model.ProjectFormField;
import de.samply.db.repository.ProjectFormFieldRepository;
import de.samply.db.repository.ProjectFormRepository;
import de.samply.form.core.model.DataType;
import de.samply.form.core.model.FormFieldConfig;
import de.samply.frontend.dto.FormField;
import de.samply.notification.NotificationService;
import de.samply.security.SessionUser;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FormServicePersistenceTest {

    private final FormConfig formConfig = mock(FormConfig.class);
    private final ProjectFormFieldRepository fieldRepository = mock(ProjectFormFieldRepository.class);
    private final Project project = new Project();

    @Test
    void createsDateFieldWithSubmittedCanonicalValueUnchanged() {
        when(fieldRepository.findByProject(project)).thenReturn(List.of());
        configureSingleValueField();

        service().editProjectFormFieldValues(
                Optional.of(new FormField[]{dateField("2026-09-11")}), project);

        ArgumentCaptor<ProjectFormField> saved = ArgumentCaptor.forClass(ProjectFormField.class);
        verify(fieldRepository).save(saved.capture());
        assertThat(saved.getValue().getValue()).isEqualTo("2026-09-11");
    }

    @Test
    void editsDateFieldWithSubmittedCanonicalValueUnchanged() {
        ProjectFormField persisted = persistedDateField("2026-09-10");
        when(fieldRepository.findByProject(project)).thenReturn(List.of(persisted));
        configureSingleValueField();

        service().editProjectFormFieldValues(
                Optional.of(new FormField[]{dateField("2026-09-11")}), project);

        verify(fieldRepository).save(persisted);
        assertThat(persisted.getValue()).isEqualTo("2026-09-11");
    }

    @Test
    void reloadAndResubmitOfUnchangedDateDoesNotWriteAgain() {
        ProjectFormField persisted = persistedDateField("2026-09-11");
        when(fieldRepository.findByProject(project)).thenReturn(List.of(persisted));
        configureSingleValueField();

        service().editProjectFormFieldValues(
                Optional.of(new FormField[]{dateField("2026-09-11")}), project);

        verify(fieldRepository, never()).save(persisted);
        assertThat(persisted.getValue()).isEqualTo("2026-09-11");
    }

    private void configureSingleValueField() {
        configureTemporalField("planned-start-date", DataType.DATE);
    }

    private void configureTemporalField(String label, DataType dataType) {
        FormFieldConfig field = new FormFieldConfig();
        field.setDataType(dataType);
        when(formConfig.fetchFormFieldConfig("request", label)).thenReturn(field);
    }

    private FormService service() {
        return new FormService(
                formConfig,
                fieldRepository,
                mock(ProjectFormRepository.class),
                mock(NotificationService.class),
                mock(SessionUser.class));
    }

    private FormField dateField(String value) {
        return FormField.builder()
                .title("request")
                .label("planned-start-date")
                .type(DataType.DATE)
                .value(value)
                .build();
    }

    private ProjectFormField persistedDateField(String value) {
        ProjectFormField field = new ProjectFormField();
        field.setProject(project);
        field.setFormTitle("request");
        field.setLabel("planned-start-date");
        field.setValue(value);
        return field;
    }
}
