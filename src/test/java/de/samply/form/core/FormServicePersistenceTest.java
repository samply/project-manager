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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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

    @Test
    void rejectsValueNotMatchingTheConfiguredDataTypeWithoutSavingAnything() {
        when(fieldRepository.findByProject(project)).thenReturn(List.of());
        configureSingleValueField();
        configureTemporalField("contact-email", DataType.EMAIL);
        FormField validDate = dateField("2026-09-11");
        FormField invalidEmail = FormField.builder()
                .title("request")
                .label("contact-email")
                // The configured type counts, not the one sent by the client
                .type(DataType.STRING)
                .value("not-an-email")
                .build();

        assertThatThrownBy(() -> service().editProjectFormFieldValues(
                Optional.of(new FormField[]{validDate, invalidEmail}), project))
                .isInstanceOf(InvalidFormFieldValueException.class)
                .hasMessageContaining("not-an-email");
        verify(fieldRepository, never()).save(any());
    }

    @Test
    void savesValidEmail() {
        when(fieldRepository.findByProject(project)).thenReturn(List.of());
        configureTemporalField("contact-email", DataType.EMAIL);

        service().editProjectFormFieldValues(
                Optional.of(new FormField[]{emailField("a@b.de")}), project);

        ArgumentCaptor<ProjectFormField> saved = ArgumentCaptor.forClass(ProjectFormField.class);
        verify(fieldRepository).save(saved.capture());
        assertThat(saved.getValue().getValue()).isEqualTo("a@b.de");
    }

    @Test
    void savesValueWithoutSurroundingWhitespace() {
        when(fieldRepository.findByProject(project)).thenReturn(List.of());
        configureTemporalField("contact-email", DataType.EMAIL);

        service().editProjectFormFieldValues(
                Optional.of(new FormField[]{emailField(" a@b.de\t")}), project);

        ArgumentCaptor<ProjectFormField> saved = ArgumentCaptor.forClass(ProjectFormField.class);
        verify(fieldRepository).save(saved.capture());
        assertThat(saved.getValue().getValue()).isEqualTo("a@b.de");
    }

    @Test
    void savesDecimalCommaAsPoint() {
        when(fieldRepository.findByProject(project)).thenReturn(List.of());
        configureTemporalField("volume", DataType.DOUBLE);
        FormField field = FormField.builder().title("request").label("volume").value("1,5").build();

        service().editProjectFormFieldValues(Optional.of(new FormField[]{field}), project);

        ArgumentCaptor<ProjectFormField> saved = ArgumentCaptor.forClass(ProjectFormField.class);
        verify(fieldRepository).save(saved.capture());
        assertThat(saved.getValue().getValue()).isEqualTo("1.5");
    }

    @Test
    void resubmitWithOnlyAddedWhitespaceDoesNotWriteAgain() {
        ProjectFormField persisted = persistedField("contact-email", "a@b.de");
        when(fieldRepository.findByProject(project)).thenReturn(List.of(persisted));
        configureTemporalField("contact-email", DataType.EMAIL);

        service().editProjectFormFieldValues(
                Optional.of(new FormField[]{emailField("a@b.de ")}), project);

        verify(fieldRepository, never()).save(any());
    }

    @Test
    void clearingAnEmailFieldIsNotRejected() {
        ProjectFormField persisted = persistedField("contact-email", "a@b.de");
        when(fieldRepository.findByProject(project)).thenReturn(List.of(persisted));
        configureTemporalField("contact-email", DataType.EMAIL);

        service().editProjectFormFieldValues(
                Optional.of(new FormField[]{emailField("")}), project);

        verify(fieldRepository).save(persisted);
        assertThat(persisted.getValue()).isEmpty();
    }

    @Test
    void invalidEditKeepsTheStoredValueAndNotifiesNobody() {
        ProjectFormField persisted = persistedField("contact-email", "a@b.de");
        when(fieldRepository.findByProject(project)).thenReturn(List.of(persisted));
        configureTemporalField("contact-email", DataType.EMAIL);
        NotificationService notificationService = mock(NotificationService.class);

        assertThatThrownBy(() -> service(notificationService).editProjectFormFieldValues(
                Optional.of(new FormField[]{emailField("a@b")}), project))
                .isInstanceOf(InvalidFormFieldValueException.class);

        assertThat(persisted.getValue()).isEqualTo("a@b.de");
        verify(fieldRepository, never()).save(any());
        verifyNoInteractions(notificationService);
    }

    @Test
    void invalidValueFirstInTheEditAlsoSavesNothing() {
        when(fieldRepository.findByProject(project)).thenReturn(List.of());
        configureSingleValueField();
        configureTemporalField("contact-email", DataType.EMAIL);

        assertThatThrownBy(() -> service().editProjectFormFieldValues(
                Optional.of(new FormField[]{emailField("x"), dateField("2026-09-11")}), project))
                .isInstanceOf(InvalidFormFieldValueException.class);
        verify(fieldRepository, never()).save(any());
    }

    @Test
    void messageNamesFormFieldAndReason() {
        when(fieldRepository.findByProject(project)).thenReturn(List.of());
        configureSingleValueField();

        assertThatThrownBy(() -> service().editProjectFormFieldValues(
                Optional.of(new FormField[]{dateField("2026-02-30")}), project))
                .isInstanceOf(InvalidFormFieldValueException.class)
                .hasMessage("Field 'planned-start-date' of form 'request': "
                        + "\"2026-02-30\" is not a valid date (YYYY-MM-DD)");
    }

    @Test
    void clientCannotBypassTheCheckBySendingAnotherType() {
        when(fieldRepository.findByProject(project)).thenReturn(List.of());
        configureTemporalField("count", DataType.INTEGER);
        FormField field = FormField.builder()
                .title("request").label("count").type(DataType.STRING).value("many").build();

        assertThatThrownBy(() -> service().editProjectFormFieldValues(
                Optional.of(new FormField[]{field}), project))
                .isInstanceOf(InvalidFormFieldValueException.class)
                .hasMessageContaining("not a valid whole number");
    }

    @Test
    void clientTypeIsIgnoredAlsoWhenItIsStricterThanTheConfiguredOne() {
        when(fieldRepository.findByProject(project)).thenReturn(List.of());
        configureTemporalField("free-text", DataType.STRING);
        FormField field = FormField.builder()
                .title("request").label("free-text").type(DataType.EMAIL).value("no email").build();

        service().editProjectFormFieldValues(Optional.of(new FormField[]{field}), project);

        verify(fieldRepository).save(any());
    }

    @Test
    void fieldWithoutConfiguredDataTypeIsNotChecked() {
        when(fieldRepository.findByProject(project)).thenReturn(List.of());
        configureTemporalField("untyped", null);
        FormField field = FormField.builder()
                .title("request").label("untyped").type(DataType.EMAIL).value("anything").build();

        service().editProjectFormFieldValues(Optional.of(new FormField[]{field}), project);

        verify(fieldRepository).save(any());
    }

    @Test
    void emptyEditDoesNothing() {
        service().editProjectFormFieldValues(Optional.empty(), project);
        service().editProjectFormFieldValues(Optional.of(new FormField[0]), project);

        verifyNoInteractions(fieldRepository, formConfig);
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
        return service(mock(NotificationService.class));
    }

    private FormService service(NotificationService notificationService) {
        return new FormService(
                formConfig,
                fieldRepository,
                mock(ProjectFormRepository.class),
                notificationService,
                mock(SessionUser.class));
    }

    private FormField emailField(String value) {
        return FormField.builder()
                .title("request")
                .label("contact-email")
                .type(DataType.EMAIL)
                .value(value)
                .build();
    }

    private ProjectFormField persistedField(String label, String value) {
        ProjectFormField field = new ProjectFormField();
        field.setProject(project);
        field.setFormTitle("request");
        field.setLabel(label);
        field.setValue(value);
        return field;
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
