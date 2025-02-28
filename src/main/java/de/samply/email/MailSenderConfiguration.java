package de.samply.email;

import de.samply.app.ProjectManagerConst;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

@Configuration
public class MailSenderConfiguration {

    @Bean
    @Qualifier(ProjectManagerConst.PRIMARY_MAIL_SENDER)
    @ConfigurationProperties(prefix = ProjectManagerConst.PRIMARY_MAIL_SENDER_PREFIX)
    public JavaMailSender primaryMailSender() {
        return new JavaMailSenderImpl();
    }

    @Bean
    @Qualifier(ProjectManagerConst.TEST_MAIL_SENDER)
    @ConditionalOnProperty(prefix = ProjectManagerConst.TEST_MAIL_SENDER_PREFIX, name = "host")
    @ConfigurationProperties(prefix = ProjectManagerConst.TEST_MAIL_SENDER_PREFIX)
    public JavaMailSender testMailSender() {
        return new JavaMailSenderImpl();
    }

}
