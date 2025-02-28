package de.samply.exporter.focus;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class FileTransferBody {

    @JsonProperty("exporter-url")
    private String exporterUrl;
    @JsonProperty("beam-receiver")
    private String beamReceiver;
}
