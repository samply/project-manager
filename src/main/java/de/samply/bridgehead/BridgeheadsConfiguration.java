package de.samply.bridgehead;

import de.samply.app.ProjectManagerConst;
import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Configuration
@ConfigurationProperties(prefix = ProjectManagerConst.REGISTERED_BRIDGEHEADS)
@Data
public class BridgeheadsConfiguration {

    private Map<String, BridgeheadConfig> config = new HashMap<>();
    private Map<String, String> explorerIdBridgeheadMap = new HashMap<>();
    private Map<String, String> tokenManagerIdBridgeheadMap = new HashMap<>();
    private Map<String, String> focusIdBridgeheadMap = new HashMap<>();
    // Site group (lower case) -> bridgeheads of the site, e.g. dresden -> [dresden, dresden-test]
    private Map<String, List<String>> groupBridgeheadsMap = new HashMap<>();


    @Data
    public static class BridgeheadConfig {
        // Site group of the identity provider (BRIDGEHEADS_CONFIG_<ID>_GROUP); empty = the bridgehead id
        private String group;
        private String explorerId;
        private String focusBeamId;
        private String fileDispatcherBeamId;
        private String tokenManagerId;
        private String humanReadable;
        private String affiliation;
        private List<Contact> contacts = new ArrayList<>();
        // People who run the site's local approval; they have no access to the project manager, they get emails
        private List<Contact> approvalContacts = new ArrayList<>();
    }

    @Data
    public static class Contact {
        private Map<String, String> name = new HashMap<>();
        private Map<String, String> description = new HashMap<>();
        private String emailAddress;
    }

    @PostConstruct
    private void initIdBridgeheadMaps() {
        replaceHyphenInBridgeheads();
        checkUniqueIds("explorer id", BridgeheadConfig::getExplorerId);
        checkUniqueIds("focus beam id", BridgeheadConfig::getFocusBeamId);
        checkUniqueIds("token manager id", BridgeheadConfig::getTokenManagerId);
        config.forEach((bridgehead, bridgeheadConfig) -> {
            addBridgeheadId(bridgehead, bridgeheadConfig.getExplorerId(), explorerIdBridgeheadMap);
            addBridgeheadId(bridgehead, bridgeheadConfig.getFocusBeamId(), focusIdBridgeheadMap);
            addBridgeheadId(bridgehead, bridgeheadConfig.getTokenManagerId(), tokenManagerIdBridgeheadMap);
        });
        groupBridgeheadsMap = config.keySet().stream().sorted()
                .collect(Collectors.groupingBy(this::fetchGroup, HashMap::new, Collectors.toList()));
        logBridgeheads();
    }

    // Two bridgeheads with the same id would make one of them unreachable: a configuration error, the start fails
    private void checkUniqueIds(String idName, Function<BridgeheadConfig, String> idGetter) {
        Map<String, List<String>> idBridgeheads = config.entrySet().stream()
                .filter(entry -> StringUtils.hasText(idGetter.apply(entry.getValue())))
                .collect(Collectors.groupingBy(entry -> idGetter.apply(entry.getValue()),
                        Collectors.mapping(Map.Entry::getKey, Collectors.toList())));
        String duplicates = idBridgeheads.entrySet().stream()
                .filter(entry -> entry.getValue().size() > 1)
                .map(entry -> entry.getKey() + " " + entry.getValue().stream().sorted().toList())
                .sorted()
                .collect(Collectors.joining(", "));
        if (!duplicates.isEmpty()) {
            throw new IllegalStateException("Bridgeheads with the same " + idName + ": " + duplicates);
        }
    }

    private String fetchGroup(String bridgehead) {
        String group = config.get(bridgehead).getGroup();
        return StringUtils.hasText(group) ? group.trim().toLowerCase() : bridgehead;
    }

    private void replaceHyphenInBridgeheads() {
        Map<String, BridgeheadConfig> tempConfig = new HashMap<>();
        config.forEach(((bridgehead, bridgeheadConfig) -> tempConfig.put(replaceHyphen(bridgehead), bridgeheadConfig)));
        config = tempConfig;
    }

    private void logBridgeheads() {
        log.info("Registered bridgeheads: ({})", config.size());
        config.keySet().stream().sorted().forEach(bridgehead -> log.info("\t- {} ({}, group {})", bridgehead,
                getHumanReadable(bridgehead).orElse(""), fetchGroup(bridgehead)));
    }

    private String replaceHyphen(String var) {
        return var.replace(ProjectManagerConst.HYPHEN, "-");
    }

    private String fetchBridgehead(String id, Map<String, String> idBridgeheadMap) {
        return idBridgeheadMap.get(id);
    }

    private Optional<String> fetchBridgeheadOptional(String id, Map<String, String> idBridgeheadMap) {
        return Optional.ofNullable(fetchBridgehead(id, idBridgeheadMap));
    }

    private void addBridgeheadId(String bridgehead, String id, Map<String, String> idBridgeheadMap) {
        if (bridgehead != null && id != null) {
            idBridgeheadMap.put(id, bridgehead);
        }
    }

    public boolean isRegisteredBridgehead(String bridgehead) {
        return config.containsKey(bridgehead);
    }

    // Registered bridgeheads whose site group is the given one (case-insensitive); empty if none
    public List<String> getBridgeheadsForGroup(String group) {
        return (group == null) ? List.of() : groupBridgeheadsMap.getOrDefault(group.trim().toLowerCase(), List.of());
    }

    public Optional<String> getFocusBeamId(String bridgehead) {
        return getProperty(bridgehead, BridgeheadConfig::getFocusBeamId);
    }

    public Optional<String> getHumanReadable(String bridgehead) {
        return getProperty(bridgehead, BridgeheadConfig::getHumanReadable);
    }

    public Optional<String> getAffiliation(String bridgehead) {
        return getProperty(bridgehead, BridgeheadConfig::getAffiliation);
    }

    public List<Contact> getContacts(String bridgehead) {
        BridgeheadConfig bridgeheadConfig = config.get(bridgehead);
        return bridgeheadConfig == null || bridgeheadConfig.getContacts() == null
                ? List.of()
                : bridgeheadConfig.getContacts();
    }

    public List<Contact> getApprovalContacts(String bridgehead) {
        BridgeheadConfig bridgeheadConfig = config.get(bridgehead);
        return bridgeheadConfig == null || bridgeheadConfig.getApprovalContacts() == null
                ? List.of()
                : bridgeheadConfig.getApprovalContacts();
    }

    public Optional<String> getTokenManagerId(String bridgehead) {
        return getProperty(bridgehead, BridgeheadConfig::getTokenManagerId);
    }

    Optional<String> getProperty(@NotNull String bridgehead, Function<BridgeheadConfig, String> propertyGetter) {
        BridgeheadConfig bridgeheadConfig = config.get(bridgehead);
        if (bridgeheadConfig == null) {
            return Optional.empty();
        }
        String result = propertyGetter.apply(bridgeheadConfig);
        return StringUtils.hasText(result) ? Optional.of(result) : Optional.empty();
    }

    public Optional<String> getBridgeheadForExplorerId(String explorerId) {
        return fetchBridgeheadOptional(explorerId, explorerIdBridgeheadMap);
    }

    @SuppressWarnings("unused")
    public String fetchBridgeheadForExplorerId(String explorerId) {
        return fetchBridgehead(explorerId, explorerIdBridgeheadMap);
    }

    @SuppressWarnings("unused")
    public Optional<String> getBridgeheadForFocusId(String focusId) {
        return fetchBridgeheadOptional(focusId, focusIdBridgeheadMap);
    }

    @SuppressWarnings("unused")
    public String fetchBridgeheadForFocusId(String focusId) {
        return fetchBridgehead(focusId, focusIdBridgeheadMap);
    }

    public String getFileDispatcherBeamId(String bridgehead) {
        return config.get(bridgehead).getFileDispatcherBeamId();
    }

    @SuppressWarnings("unused")
    public Optional<String> getBridgeheadForTokenManagerId(String tokenManagerId) {
        return fetchBridgeheadOptional(tokenManagerId, tokenManagerIdBridgeheadMap);
    }

    public String fetchBridgeheadForTokenManagerId(String tokenManagerId) {
        return fetchBridgehead(tokenManagerId, tokenManagerIdBridgeheadMap);
    }

    public Set<String> getRegisteredBridgeheads() {
        return config.keySet();
    }


}
