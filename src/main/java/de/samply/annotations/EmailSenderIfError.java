package de.samply.annotations;

import de.samply.email.EmailRecipientType;
import de.samply.email.EmailTemplateType;

import java.lang.annotation.*;

@Repeatable(EmailSendersIfError.class)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface EmailSenderIfError {

    // Recipients of the email
    EmailRecipientType[] recipients() default {};

    EmailTemplateType templateType();
}
