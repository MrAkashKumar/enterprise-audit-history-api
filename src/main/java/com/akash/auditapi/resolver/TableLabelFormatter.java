package com.akash.auditapi.resolver;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Converts physical source-table names into readable hyphen-separated labels.
 * It removes the configured prefix and title-cases each underscore-delimited segment.
 */
@Component
public class TableLabelFormatter {
    public String format(String sourceTable, String sourceTablePrefix) {
        String tableWithoutPrefix = sourceTable.replaceFirst(
                "^" + Pattern.quote(sourceTablePrefix), "");
        return Arrays.stream(tableWithoutPrefix.split("_"))
                .filter(part -> !part.isEmpty())
                .map(this::capitalize)
                .collect(Collectors.joining("-"));
    }

    private String capitalize(String value) {
        String lowercase = value.toLowerCase(Locale.ROOT);
        return Character.toUpperCase(lowercase.charAt(0)) + lowercase.substring(1);
    }
}
