package com.bnp.kingmainandhiskingdom.security;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SsoRoleMapperTest {

    @Test
    void royalCourtIsKingAndKnight() {
        assertEquals(Set.of("KING", "KNIGHT"), SsoRoleMapper.rolesFor(List.of("royal-court")));
    }

    @Test
    void guardsAreKnights() {
        assertEquals(Set.of("KNIGHT"), SsoRoleMapper.rolesFor(List.of("guards")));
    }

    @Test
    void groupsAreCombined() {
        assertEquals(Set.of("KNIGHT", "SUBJECT"), SsoRoleMapper.rolesFor(List.of("guards", "villagers")));
    }

    @Test
    void unknownOrMissingGroupsOnlyGiveSubject() {
        assertEquals(Set.of("SUBJECT"), SsoRoleMapper.rolesFor(List.of("pirates")));
        assertEquals(Set.of("SUBJECT"), SsoRoleMapper.rolesFor(List.of()));
    }
}
