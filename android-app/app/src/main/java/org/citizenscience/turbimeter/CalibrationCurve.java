package org.citizenscience.turbimeter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Reference comparison calibration. Design: Ing. Alex M. Rivera. */
final class CalibrationCurve {
    static final int REPEATS = 5;
    static final int MAX_POINTS = 12;
    final String id;
    final String sensorId;
    final String unit;
    final String reference;
    final String createdAt;
    final List<Point> points;

    static final class Point {
        final double referenceValue;
        final double[] readings;
        final String capturedAt;
        final double ratio;
        final double sd;

        Point(double referenceValue, double[] readings, String capturedAt) {
            if (!validValue(referenceValue) || readings.length != REPEATS)
                throw new IllegalArgumentException("Each point needs a reference value and five sensor readings.");
            Instant.parse(capturedAt);
            this.referenceValue = referenceValue;
            this.readings = readings.clone();
            this.capturedAt = capturedAt;
            double sum = 0;
            for (double reading : readings) {
                if (!validValue(reading)) throw new IllegalArgumentException("Invalid optical ratio.");
                sum += reading;
            }
            ratio = sum / readings.length;
            double squares = 0;
            for (double reading : readings) squares += (reading - ratio) * (reading - ratio);
            sd = Math.sqrt(squares / (readings.length - 1));
        }

        double minRatio() {
            double min = readings[0];
            for (double reading : readings) min = Math.min(min, reading);
            return min;
        }

        double maxRatio() {
            double max = readings[0];
            for (double reading : readings) max = Math.max(max, reading);
            return max;
        }
    }

    CalibrationCurve(String id, String sensorId, String unit, String reference,
                     String createdAt, List<Point> points) {
        if (id == null || !id.matches("[A-Za-z0-9._-]{1,32}"))
            throw new IllegalArgumentException("Invalid calibration ID.");
        if (sensorId == null || !sensorId.matches("[A-Za-z0-9._:-]{1,160}"))
            throw new IllegalArgumentException("A stable USB sensor identity is required.");
        if (!"FNU".equals(unit) && !"NTU".equals(unit))
            throw new IllegalArgumentException("Choose the reference instrument's FNU or NTU unit.");
        if (reference == null || reference.trim().isEmpty() || reference.length() > 200)
            throw new IllegalArgumentException("Enter the reference instrument or standard identification.");
        Instant.parse(createdAt);
        if (points.size() < 3 || points.size() > MAX_POINTS)
            throw new IllegalArgumentException("Capture 3 to 12 different reference points.");
        List<Point> ordered = new ArrayList<>(points);
        ordered.sort(Comparator.comparingDouble(point -> point.referenceValue));
        for (int i = 1; i < ordered.size(); i++) {
            Point previous = ordered.get(i - 1), next = ordered.get(i);
            if (previous.referenceValue >= next.referenceValue)
                throw new IllegalArgumentException("Reference values must be different.");
            if (previous.maxRatio() >= next.minRatio())
                throw new IllegalArgumentException("Higher reference values must have higher, non-overlapping sensor readings. Recheck these samples or narrow the range.");
        }
        this.id = id;
        this.sensorId = sensorId;
        this.unit = unit;
        this.reference = reference.trim();
        this.createdAt = createdAt;
        this.points = Collections.unmodifiableList(ordered);
    }

    double convert(double ratio) {
        if (!validValue(ratio)) return Double.NaN;
        int i = 1;
        while (i < points.size() - 1 && ratio > points.get(i).ratio) i++;
        Point lower = points.get(i - 1), upper = points.get(i);
        return lower.referenceValue + (ratio - lower.ratio)
                * (upper.referenceValue - lower.referenceValue) / (upper.ratio - lower.ratio);
    }

    boolean isExtrapolated(double ratio) {
        return ratio < points.get(0).ratio || ratio > points.get(points.size() - 1).ratio;
    }

    static boolean validValue(double value) {
        return Double.isFinite(value) && value >= 0 && value <= 10000;
    }
}
