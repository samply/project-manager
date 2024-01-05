package de.samply.frontend.dto;

import de.samply.db.model.ProjectDocument;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Service;

@Service
public class DtoFactory {

    public Project convert (@NotNull de.samply.db.model.Project project){
        return new Project();
        //TODO
    }

    public Notification convert (@NotNull de.samply.db.model.Notification notification){
        return new Notification();
        //TODO
    }

    public OtherDocument convert(@NotNull ProjectDocument projectDocument){
        return new OtherDocument();
        //TODO
    }



}
