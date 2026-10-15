package de.samply.frontend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Bridgehead {
    private final String bridgehead;
    private final String humanReadable;
    private final BridgeheadContact[] contacts;
}
