package org.citizenscience.turbimeter;

import java.util.function.Consumer;

final class LineFramer {
    private final StringBuilder pending = new StringBuilder();
    private boolean overflow;

    void accept(byte[] data, int count, Consumer<String> onLine) {
        for (int i = 0; i < count; i++) {
            int ch = data[i] & 0xff;
            if (ch == '\n') {
                if (!overflow && pending.length() > 0) onLine.accept(pending.toString());
                pending.setLength(0);
                overflow = false;
            } else if (ch == '\r') {
                // Accept either LF or CRLF lines.
            } else if (!overflow) {
                if (ch < 32 || ch > 126 || pending.length() == 120) overflow = true;
                else pending.append((char) ch);
            }
        }
    }
}
