package de.samply.annotations;

import de.samply.email.EmailRecipientType;
import de.samply.email.EmailTemplateType;

import java.lang.annotation.*;

@Repeatable(EmailSenders.class)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface EmailSender {

    // Recipients of the email
    EmailRecipientType[] recipients() default {};

    EmailTemplateType templateType();
}
