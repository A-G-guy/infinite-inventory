package com.agguy.infiniteinventory.service.search;

import java.util.List;
import java.util.stream.Collectors;

public record DatabaseSearchClause(
        List<DatabaseSearchFilter> positiveFilters,
        List<DatabaseSearchFilter> negativeFilters,
        List<String> positivePlainTerms,
        List<String> negativePlainTerms
) {
    public DatabaseSearchClause {
        positiveFilters = List.copyOf(positiveFilters == null ? List.of() : positiveFilters);
        negativeFilters = List.copyOf(negativeFilters == null ? List.of() : negativeFilters);
        positivePlainTerms = normalizePlainTerms(positivePlainTerms);
        negativePlainTerms = normalizePlainTerms(negativePlainTerms);
    }

    public boolean active() {
        return !this.positiveFilters.isEmpty()
                || !this.negativeFilters.isEmpty()
                || !this.positivePlainTerms.isEmpty()
                || !this.negativePlainTerms.isEmpty();
    }

    public String positivePlainQuery() {
        return String.join(" ", this.positivePlainTerms);
    }

    public String canonicalExpression() {
        java.util.ArrayList<String> tokens = new java.util.ArrayList<>();
        for (DatabaseSearchFilter filter : this.positiveFilters) {
            String token = filter.canonicalToken();
            if (!token.isEmpty()) {
                tokens.add(token);
            }
        }
        for (String term : this.positivePlainTerms) {
            String normalizedTerm = SearchTextNormalizer.normalizeQueryText(term);
            if (!normalizedTerm.isEmpty()) {
                tokens.add(normalizedTerm);
            }
        }
        for (DatabaseSearchFilter filter : this.negativeFilters) {
            String token = filter.canonicalToken();
            if (!token.isEmpty()) {
                tokens.add("-" + token);
            }
        }
        for (String term : this.negativePlainTerms) {
            String normalizedTerm = SearchTextNormalizer.normalizeQueryText(term);
            if (!normalizedTerm.isEmpty()) {
                tokens.add("-" + normalizedTerm);
            }
        }
        return String.join(" ", tokens);
    }

    private static List<String> normalizePlainTerms(List<String> terms) {
        if (terms == null || terms.isEmpty()) {
            return List.of();
        }
        return terms.stream()
                .map(SearchTextNormalizer::normalizeQueryText)
                .filter(term -> !term.isEmpty())
                .collect(Collectors.toUnmodifiableList());
    }
}
