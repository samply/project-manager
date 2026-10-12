package de.samply.annotations;

import de.samply.email.EmailRecipientType;
import de.samply.email.EmailTemplateType;
import de.samply.project.state.ProjectState;

import java.lang.annotation.*;

@Repeatable(EmailSenders.class)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface EmailSender {

    // Recipients of the email
    EmailRecipientType[] recipients() default {};

    EmailTemplateType templateType();

    // Only sent when the project is in one of these states (after the call); empty: in any state
    ProjectState[] projectStates() default {};
}
