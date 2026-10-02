package org.citizenscience.turbimeter;

import java.util.Locale;

final class MeasurementFrame {
    enum Kind { DEMO, RATIO, RESULT }

    final Kind kind;
    final double value;
    final String unit;
    final String calibrationId;

    private MeasurementFrame(Kind kind, double value, String unit, String calibrationId) {
        this.kind = kind;
        this.value = value;
        this.unit = unit;
        this.calibrationId = calibrationId;
    }

    static MeasurementFrame parse(String line) {
        String[] fields = line.trim().split(",", -1);
        if (fields.length < 3 || !"TURB1".equals(fields[0])) return null;

        try {
            double value = Double.parseDouble(fields[2]);
            if (!Double.isFinite(value) || value < 0 || value > 10000) return null;
            if (fields.length == 3 && "DEMO".equals(fields[1]))
                return new MeasurementFrame(Kind.DEMO, value, "ratio", "");
            if (fields.length == 3 && "RATIO".equals(fields[1]))
                return new MeasurementFrame(Kind.RATIO, value, "ratio", "");
            if (fields.length == 6 && "RESULT".equals(fields[1]) && "OK".equals(fields[5])
                    && ("FNU".equals(fields[3]) || "NTU".equals(fields[3]))
                    && fields[4].matches("[A-Za-z0-9._-]{1,32}"))
                return new MeasurementFrame(Kind.RESULT, value, fields[3], fields[4]);
        } catch (NumberFormatException ignored) {
            // An incomplete or malformed USB line must never become a reading.
        }
        return null;
    }

    static String errorMessage(String line) {
        switch (line.trim()) {
            case "TURB1,ERROR,ADC_MISSING": return "ADS1115 not found at 0x48. Check its 3.3 V power and I²C wiring.";
            case "TURB1,ERROR,ADC_IO": return "ADS1115 read failed. Check D4/D5 and the I²C connections.";
            case "TURB1,ERROR,ADC_CONFIG": return "ADS1115 rejected its configuration. Check I²C power and wiring.";
            case "TURB1,ERROR,ADC_READ": return "ADS1115 register read failed. Check the I²C connections.";
            case "TURB1,ERROR,ADC_TIMEOUT": return "ADS1115 conversion timed out. Check its power and I²C wiring.";
            case "TURB1,ERROR,SATURATED": return "Analog signal is near the ADC limit. Reduce gain or LED brightness.";
            case "TURB1,ERROR,REFERENCE_LOW": return "Reference signal is too weak. Check the LED, photodiode, and signal polarity.";
            case "TURB1,ERROR,SCATTER_NEGATIVE": return "Scatter signal is below its dark reading. Check optics and signal polarity.";
            case "TURB1,ERROR,RATIO_RANGE": return "Optical ratio is out of range. Check both sensor channels.";
            case "TURB1,ERROR,BAD_COMMAND": return "The sensor did not recognize the command.";
            default: return null;
        }
    }

    String displayValue() {
        return String.format(Locale.US, kind == Kind.RESULT ? "%.2f" : "%.4f", value);
    }
}
