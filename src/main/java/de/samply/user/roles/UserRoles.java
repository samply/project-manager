package de.samply.user.roles;

import lombok.Getter;

import java.util.*;
import java.util.stream.Collectors;

public class UserRoles<T> {

    private final Map<String, Set<T>> bridgheadRolesMap = new HashMap<>();
    @Getter
    private final Set<T> rolesNotDependentOnBridgeheads = new HashSet<>();

    public void addBridgeheadRole(String bridgehead, T role) {
        if (bridgehead != null && role != null) {
            Set<T> bridgeheadRoles = bridgheadRolesMap.computeIfAbsent(bridgehead, _ -> new HashSet<>());
            bridgeheadRoles.add(role);
        }
    }

    public void addRoleNotDependentOnBridgehead(T role) {
        if (role != null) {
            rolesNotDependentOnBridgeheads.add(role);
        }
    }

    public Set<T> getBridgeheadRoles(String bridgehead) {
        Set<T> result = bridgheadRolesMap.get(bridgehead);
        return (result != null) ? result : new HashSet<>();
    }

    public Set<String> getBridgeheads() {
        return bridgheadRolesMap.keySet();
    }

    public boolean containsRole(T role) {
        return containsRole(role, Optional.empty());
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType") // bridghead as optional
    public boolean containsRole(T role, Optional<String> bridgehead) {
        if (rolesNotDependentOnBridgeheads.contains(role)) {
            return true;
        } else if (bridgehead.isPresent()) {
            Set<T> bridgeheadRoles = bridgheadRolesMap.get(bridgehead.get());
            if (bridgeheadRoles != null) {
                return bridgeheadRoles.contains(role);
            }
        }
        return false;
    }

    // If the bridgehead is not provided, checks the role independently of the bridgehead
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType") // bridghead as optional
    public boolean containsAnyRole(T role, Optional<String> bridgehead) {
        if (bridgehead.isPresent()) {
            return containsRole(role, bridgehead);
        } else {
            if (rolesNotDependentOnBridgeheads.contains(role)) {
                return true;
            } else {
                return bridgheadRolesMap.values().stream()
                        .flatMap(Set::stream)
                        .collect(Collectors.toSet()).contains(role);
            }
        }
    }

}
