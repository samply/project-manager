package de.samply.app;

import de.samply.db.model.Project;
import de.samply.form.core.FormService;
import de.samply.form.core.InvalidFormFieldValueException;
import de.samply.frontend.dto.FormField;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProjectManagerControllerFormFieldValuesTest {

    @Mock
    private FormService formService;
    @InjectMocks
    private ProjectManagerController controller;

    private final Project project = new Project();
    private final FormField[] formFields = {
            FormField.builder().title("project").label("principal_investigator_email").value("abc").build()
    };

    @Test
    void savedValuesAnswerOk() {
        var response = controller.editProjectFormValues(project, formFields);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(formService).editProjectFormFieldValues(Optional.of(formFields), project);
    }

    @Test
    void invalidValueAnswersBadRequestWithTheReason() {
        doThrow(new InvalidFormFieldValueException(
                "Field 'principal_investigator_email' of form 'project': \"abc\" is not a valid e-mail address"))
                .when(formService).editProjectFormFieldValues(any(), eq(project));

        var response = controller.editProjectFormValues(project, formFields);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        // Only the message: no stack trace for a user error
        assertThat(response.getBody()).isEqualTo(
                "Field 'principal_investigator_email' of form 'project': \"abc\" is not a valid e-mail address");
    }

    @Test
    void otherIllegalArgumentsStayInternalServerErrors() {
        doThrow(new IllegalArgumentException("Form title not found: x"))
                .when(formService).editProjectFormFieldValues(any(), eq(project));

        var response = controller.editProjectFormValues(project, formFields);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void missingFormFieldsAreForwardedAsEmpty() {
        var response = controller.editProjectFormValues(project, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(formService).editProjectFormFieldValues(Optional.empty(), project);
    }
}
