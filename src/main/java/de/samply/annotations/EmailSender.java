package de.samply.annotations;

import de.samply.notification.smtp.EmailRecipientType;
import de.samply.notification.smtp.EmailTemplateType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface EmailSender {

    // Recipients of the email
    EmailRecipientType[] recipients() default {};

    EmailTemplateType templateType();
}
