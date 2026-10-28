package com.bnp.kingmainandhiskingdom.security;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Translates the groups of the identity provider into the roles of the kingdom. The IdP only knows
 * "who is in which group"; what a group may do is decided here, by the application.
 */
public final class SsoRoleMapper {

    private static final Map<String, Set<String>> GROUP_ROLES = Map.of(
            "royal-court", Set.of(AccountService.ROLE_KING, AccountService.ROLE_KNIGHT),
            "guards", Set.of(AccountService.ROLE_KNIGHT),
            "villagers", Set.of(AccountService.ROLE_SUBJECT));

    private SsoRoleMapper() {
    }

    /** Unknown groups give no rights; someone without any known group is a plain SUBJECT. */
    public static Set<String> rolesFor(Collection<String> groups) {
        Set<String> roles = new TreeSet<>();
        for (String group : groups) {
            roles.addAll(GROUP_ROLES.getOrDefault(group, Set.of()));
        }
        if (roles.isEmpty()) {
            roles.add(AccountService.ROLE_SUBJECT);
        }
        return roles;
    }
}
