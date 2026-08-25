package de.samply.frontend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record Form(
        String title,
        String titleDisplayName,
        String titleDescription,
        String titleShortDescription,
        String titlePreInfo,
        String titlePostInfo
) {

    public Form(String title, String titleDisplayName, String titleDescription, String titleShortDescription) {
        this(title, titleDisplayName, titleDescription, titleShortDescription, null, null);
    }
}
