package de.samply.user.roles;

import lombok.Getter;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class UserRoles<T> {

    private final Map<String, Set<T>> bridgheadRolesMap = new ConcurrentHashMap<>();

    @Getter
    private final Set<T> rolesNotDependentOnBridgeheads = ConcurrentHashMap.newKeySet();

    public void addBridgeheadRole(String bridgehead, T role) {
        if (bridgehead != null && role != null) {
            bridgheadRolesMap
                    .computeIfAbsent(bridgehead, _ -> ConcurrentHashMap.newKeySet())
                    .add(role);
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

    // Checks the role independently of the bridgehead
    public boolean containsAnyRole(T role) {
        if (rolesNotDependentOnBridgeheads.contains(role)) {
            return true;
        } else {
            return bridgheadRolesMap.values().stream()
                    .flatMap(Set::stream)
                    .collect(Collectors.toSet()).contains(role);
        }
    }

}
