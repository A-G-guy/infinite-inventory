package com.agguy.infiniteinventory.service.search;

import java.util.ArrayList;
import java.util.List;

public final class DatabaseSearchQueryParser {
    public static final DatabaseSearchQueryParser INSTANCE = new DatabaseSearchQueryParser();

    private DatabaseSearchQueryParser() {
    }

    public DatabaseParsedSearchQuery parse(String searchText) {
        return this.parse(searchText, DatabaseSearchQueryParserContext.empty());
    }

    public DatabaseParsedSearchQuery parse(String searchText, DatabaseSearchQueryParserContext context) {
        Parser parser = new Parser(searchText, context == null ? DatabaseSearchQueryParserContext.empty() : context);
        return parser.parse();
    }

    private static final class Parser {
        private final String text;
        private final DatabaseSearchQueryParserContext context;
        private final int length;
        private int index;

        private Parser(String text, DatabaseSearchQueryParserContext context) {
            this.text = text == null ? "" : text;
            this.context = context;
            this.length = this.text.length();
        }

        private DatabaseParsedSearchQuery parse() {
            java.util.ArrayList<DatabaseSearchClause> clauses = new java.util.ArrayList<>();
            while (true) {
                DatabaseSearchClause clause = this.parseClause();
                if (clause.active()) {
                    clauses.add(clause);
                }
                this.skipWhitespace();
                if (this.index >= this.length) {
                    break;
                }
                if (this.text.charAt(this.index) == '|') {
                    this.index++;
                    continue;
                }
                this.index++;
            }
            if (clauses.isEmpty()) {
                return DatabaseParsedSearchQuery.empty();
            }
            String normalizedExpression = clauses.stream()
                    .map(DatabaseSearchClause::canonicalExpression)
                    .filter(expression -> !expression.isEmpty())
                    .reduce((left, right) -> left + " | " + right)
                    .orElse("");
            return new DatabaseParsedSearchQuery(clauses, normalizedExpression);
        }

        private DatabaseSearchClause parseClause() {
            java.util.ArrayList<DatabaseSearchFilter> positiveFilters = new java.util.ArrayList<>();
            java.util.ArrayList<DatabaseSearchFilter> negativeFilters = new java.util.ArrayList<>();
            java.util.ArrayList<String> positivePlainTerms = new java.util.ArrayList<>();
            java.util.ArrayList<String> negativePlainTerms = new java.util.ArrayList<>();

            while (this.index < this.length) {
                this.skipWhitespace();
                if (this.index >= this.length || this.text.charAt(this.index) == '|') {
                    break;
                }

                boolean negative = false;
                if (this.text.charAt(this.index) == '-') {
                    negative = true;
                    this.index++;
                    this.skipWhitespace();
                }
                if (this.index >= this.length || this.text.charAt(this.index) == '|') {
                    continue;
                }

                DatabaseSearchFilterType filterType = DatabaseSearchFilterType.fromPrefix(this.text.charAt(this.index));
                if (filterType != null) {
                    this.index++;
                    ParsedValue parsedValue = this.parseValue(filterType);
                    DatabaseSearchFilter filter = new DatabaseSearchFilter(filterType, parsedValue.value());
                    if (!filter.isMeaningful()) {
                        continue;
                    }
                    if (negative) {
                        negativeFilters.add(filter);
                    } else {
                        positiveFilters.add(filter);
                    }
                    continue;
                }

                ParsedValue parsedValue = this.parsePlainValue();
                String normalizedTerm = SearchTextNormalizer.normalizeQueryText(parsedValue.value());
                if (normalizedTerm.isEmpty()) {
                    continue;
                }
                if (negative) {
                    negativePlainTerms.add(normalizedTerm);
                } else {
                    positivePlainTerms.add(normalizedTerm);
                }
            }
            return new DatabaseSearchClause(positiveFilters, negativeFilters, positivePlainTerms, negativePlainTerms);
        }

        private ParsedValue parseValue(DatabaseSearchFilterType filterType) {
            ParsedValue firstValue = this.parseNextAtom();
            if (firstValue.value().isEmpty() || firstValue.quoted()) {
                return firstValue;
            }
            if (filterType != DatabaseSearchFilterType.MOD && filterType != DatabaseSearchFilterType.CREATIVE_TAB) {
                return firstValue;
            }

            String resolvedValue = firstValue.value();
            while (true) {
                int checkpoint = this.index;
                this.skipWhitespace();
                if (this.index >= this.length) {
                    return new ParsedValue(resolvedValue, false);
                }
                char nextCharacter = this.text.charAt(this.index);
                if (nextCharacter == '|' || nextCharacter == '-' || DatabaseSearchFilterType.fromPrefix(nextCharacter) != null || nextCharacter == '"') {
                    this.index = checkpoint;
                    return new ParsedValue(resolvedValue, false);
                }
                ParsedValue nextValue = this.parseNextAtom();
                if (nextValue.value().isEmpty() || nextValue.quoted()) {
                    this.index = checkpoint;
                    return new ParsedValue(resolvedValue, false);
                }
                String combinedValue = resolvedValue + " " + nextValue.value();
                if (!this.context.canExtend(filterType, combinedValue)) {
                    this.index = checkpoint;
                    return new ParsedValue(resolvedValue, false);
                }
                resolvedValue = combinedValue;
            }
        }

        private ParsedValue parsePlainValue() {
            return this.parseNextAtom();
        }

        private ParsedValue parseNextAtom() {
            this.skipWhitespace();
            if (this.index >= this.length) {
                return ParsedValue.empty();
            }
            if (this.text.charAt(this.index) == '"') {
                this.index++;
                int start = this.index;
                while (this.index < this.length && this.text.charAt(this.index) != '"') {
                    this.index++;
                }
                String value = this.text.substring(start, this.index);
                if (this.index < this.length && this.text.charAt(this.index) == '"') {
                    this.index++;
                }
                return new ParsedValue(value, true);
            }
            int start = this.index;
            while (this.index < this.length) {
                char character = this.text.charAt(this.index);
                if (Character.isWhitespace(character) || character == '|') {
                    break;
                }
                this.index++;
            }
            return new ParsedValue(this.text.substring(start, this.index), false);
        }

        private void skipWhitespace() {
            while (this.index < this.length && Character.isWhitespace(this.text.charAt(this.index))) {
                this.index++;
            }
        }
    }

    private record ParsedValue(String value, boolean quoted) {
        private static ParsedValue empty() {
            return new ParsedValue("", false);
        }
    }
}
