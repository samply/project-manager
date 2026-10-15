package de.samply.app;

import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ComponentScan(basePackages = {"de.samply"})
@EntityScan(basePackages = {"de.samply.db.model"})
@EnableJpaRepositories("de.samply.db.repository")
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = ProjectManagerConst.SHED_LOCK_MAX_TIME_SV)
public class ProjectManagerApplication {

    static void main(String[] args) {
        SpringApplication.run(ProjectManagerApplication.class, args);
    }

}
