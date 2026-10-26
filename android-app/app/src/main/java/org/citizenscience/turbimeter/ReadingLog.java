package org.citizenscience.turbimeter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** CSV schema migration without recalculating old readings. Design: Ing. Alex M. Rivera. */
final class ReadingLog {
    static final String LEGACY_HEADER = "timestamp_utc,value,unit,quality,calibration_id";
    static final String HEADER = LEGACY_HEADER + ",optical_ratio,sensor_id";

    static String upgrade(String text) throws IOException {
        if (text.isEmpty()) return HEADER + "\n";
        String[] lines = text.split("\r?\n", -1);
        if (HEADER.equals(lines[0])) return text.endsWith("\n") ? text : text + "\n";
        if (!LEGACY_HEADER.equals(lines[0])) throw new IOException("Unknown history format.");
        StringBuilder result = new StringBuilder(HEADER).append('\n');
        for (int i = 1; i < lines.length; i++) {
            if (i == lines.length - 1 && lines[i].isEmpty()) continue;
            if (lines[i].split(",", -1).length != 5) throw new IOException("Damaged history row.");
            result.append(lines[i]).append(",,\n");
        }
        return result.toString();
    }

    static List<String[]> rows(String text) {
        List<String[]> rows = new ArrayList<>();
        String[] lines = text.split("\r?\n");
        for (int i = 1; i < lines.length; i++) {
            String[] fields = lines[i].split(",", -1);
            if (fields.length == 5 || fields.length == 7) rows.add(fields);
        }
        return rows;
    }
}
