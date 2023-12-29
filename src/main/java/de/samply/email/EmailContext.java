package de.samply.email;

import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class EmailContext {

    @Getter
    private Map<String, String> keyValues = new HashMap<>();


}
