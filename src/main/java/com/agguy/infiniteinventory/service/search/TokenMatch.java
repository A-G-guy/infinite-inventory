package com.agguy.infiniteinventory.service.search;

import com.agguy.infiniteinventory.database.DatabaseSearchField;

record TokenMatch(DatabaseSearchField field, MatchLevel level, double score) {
    private static final TokenMatch NO_MATCH = new TokenMatch(null, MatchLevel.NONE, Double.NEGATIVE_INFINITY);

    static TokenMatch noMatch() {
        return NO_MATCH;
    }

    TokenMatch withField(DatabaseSearchField field) {
        if (!this.matched()) {
            return this;
        }
        return new TokenMatch(field, this.level, this.score);
    }

    boolean matched() {
        return this.level != MatchLevel.NONE;
    }

    boolean isBetterThan(TokenMatch other) {
        return this.betterOf(other) == this;
    }

    TokenMatch betterOf(TokenMatch other) {
        if (other == null || other.level.rank < this.level.rank) {
            return this;
        }
        if (other.level.rank > this.level.rank) {
            return other;
        }
        return other.score > this.score ? other : this;
    }
}
