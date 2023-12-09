package de.samply.user.roles;

import java.util.*;

public class UserRoles<T> {

    private final Map<String, Set<T>> bridgheadRolesMap = new HashMap<>();
    private final Set<T> rolesNotDependentOnBridgeheads = new HashSet<>();

    public void addBridgeheadRole(String bridgehead, T role) {
        if (bridgehead != null && role != null) {
            Set<T> bridgeheadRoles = bridgheadRolesMap.get(bridgehead);
            if (bridgeheadRoles == null) {
                bridgeheadRoles = new HashSet<>();
                bridgheadRolesMap.put(bridgehead, bridgeheadRoles);
            }
            bridgeheadRoles.add(role);
        }
    }

    public void addRoleNotDependentOnBridgehead(T role) {
        if (role != null) {
            rolesNotDependentOnBridgeheads.add(role);
        }
    }

    public Set<T> getRolesNotDependentOnBridgeheads() {
        return rolesNotDependentOnBridgeheads;
    }

    public Optional<Set<T>> getBridgeheadRoles(String bridgehead) {
        return Optional.of(bridgheadRolesMap.get(bridgehead));
    }

    public Set<String> getBridgeheads() {
        return bridgheadRolesMap.keySet();
    }

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

}
