package com.agguy.infiniteinventory.service.search;

enum MatchLevel {
    NONE(0),
    FUZZY(1),
    CONTAINS(2),
    PREFIX(3),
    EXACT(4);

    final int rank;

    MatchLevel(int rank) {
        this.rank = rank;
    }
}
