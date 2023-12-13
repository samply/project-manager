package de.samply.exporter;

import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ExporterService {

    public void sendQueryToBridgehead(@NotNull String projectCode, @NotNull String bridgehead, @NotNull String queryCode) {
        //TODO
        log.info("Sending query" + queryCode + " to bridgehead" + bridgehead + "...");
    }

    public void sendQueryToBridgeheadAndExecute(@NotNull String projectCode, @NotNull String bridgehead, @NotNull String queryCode) {
        log.info("Sending query" + queryCode + " to bridgehead " + bridgehead + " to be executed...");
    }


}
