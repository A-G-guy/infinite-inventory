package com.agguy.build.versioning;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SemanticVersionTest {
    @Test
    void parseShouldRejectPreReleaseSuffix() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> SemanticVersion.parse("1.2.3-beta.1")
        );
        assertEquals("仅支持稳定版 semver，当前值为: 1.2.3-beta.1", exception.getMessage());
    }

    @Test
    void bumpShouldFollowStableSemverRules() {
        SemanticVersion version = SemanticVersion.parse("1.2.3");

        assertEquals("1.2.4", version.bump(ReleaseImpact.PATCH).toString());
        assertEquals("1.3.0", version.bump(ReleaseImpact.MINOR).toString());
        assertEquals("2.0.0", version.bump(ReleaseImpact.MAJOR).toString());
        assertEquals("1.2.3", version.bump(ReleaseImpact.NONE).toString());
    }
}
