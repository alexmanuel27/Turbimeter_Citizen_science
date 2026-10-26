package org.citizenscience.turbimeter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Independent expected values, invalid inputs and migration checks. Design: Ing. Alex M. Rivera. */
public final class CalibrationCheck {
    private static final String WHEN = "2026-10-25T10:00:00Z";

    static CalibrationCurve.Point point(double reference, double ratio) {
        return new CalibrationCurve.Point(reference, new double[]{ratio-.0002, ratio-.0001, ratio,
                ratio+.0001, ratio+.0002}, WHEN);
    }

    private static CalibrationCurve curve(List<CalibrationCurve.Point> points) {
        return new CalibrationCurve("CAL-check", "2886:802f:TEST_SENSOR", "FNU", "Test reference", WHEN, points);
    }

    private static void rejects(Runnable run) {
        try { run.run(); throw new AssertionError("Invalid calibration accepted"); }
        catch (IllegalArgumentException expected) { }
    }

    public static void main(String[] args) throws Exception {
        List<CalibrationCurve.Point> points = Arrays.asList(point(100, 1), point(0, .6), point(10, .7));
        CalibrationCurve c = curve(points);
        assert Math.abs(c.convert(.65) - 5) < 1e-9;
        assert Math.abs(c.convert(.85) - 55) < 1e-9;
        assert Math.abs(c.convert(.6)) < 1e-9;
        assert Math.abs(c.convert(1) - 100) < 1e-9;
        assert Math.abs(c.convert(.5) - (-10)) < 1e-9;
        assert Math.abs(c.convert(1.3) - 190) < 1e-9;
        assert c.isExtrapolated(.5) && c.isExtrapolated(1.3);
        assert !c.isExtrapolated(.65) && !c.isExtrapolated(.6) && !c.isExtrapolated(1);
        assert MeasurementFrame.calibrated(-10, "FNU", c.id).value == -10;
        assert MeasurementFrame.calibrated(20000, "FNU", c.id).value == 20000;
        assert Double.isNaN(c.convert(Double.NaN));
        assert Double.isNaN(c.convert(Double.POSITIVE_INFINITY));
        rejects(() -> curve(Arrays.asList(point(0, .6), point(10, .6), point(20, .8))));
        rejects(() -> curve(Arrays.asList(point(0, .6), point(10, .6001), point(20, .8))));
        rejects(() -> curve(Arrays.asList(point(0, .6), point(10, .5), point(20, .8))));
        rejects(() -> curve(Arrays.asList(point(0, .6), point(0, .7), point(20, .8))));
        rejects(() -> curve(Collections.singletonList(point(0, .6))));
        rejects(() -> new CalibrationCurve("CAL-check", "", "FNU", "reference", WHEN, points));
        rejects(() -> new CalibrationCurve("CAL-check", "sensor", "ratio", "reference", WHEN, points));
        rejects(() -> new CalibrationCurve("CAL-check", "sensor", "FNU", "", WHEN, points));
        rejects(() -> point(-1, .6));
        rejects(() -> point(10001, .6));
        rejects(() -> point(0, Double.NaN));
        rejects(() -> new CalibrationCurve.Point(0, new double[]{.6}, WHEN));
        assert MeasurementFrame.parse("TURB1,DEMO,0.6") == null;
        assert MeasurementFrame.calibrated(55, "FNU", c.id).kind == MeasurementFrame.Kind.RESULT;

        String legacy = ReadingLog.LEGACY_HEADER + "\n2026-10-24T15:31:21Z,0.6457,ratio,UNCALIBRATED,\n";
        String migrated = ReadingLog.upgrade(legacy);
        assert migrated.startsWith(ReadingLog.HEADER + "\n");
        assert migrated.contains("2026-10-24T15:31:21Z,0.6457,ratio,UNCALIBRATED,,,\n");
        assert ReadingLog.rows(migrated).size() == 1;
        assert ReadingLog.rows(migrated).get(0).length == 7;
        assert ReadingLog.upgrade(migrated).equals(migrated);
        assert ReadingLog.rows(legacy).size() == 1;
        try { ReadingLog.upgrade("unexpected header\n"); throw new AssertionError("Unknown history overwritten"); }
        catch (IOException expected) { }
        try { ReadingLog.upgrade(ReadingLog.LEGACY_HEADER + "\nbroken\n"); throw new AssertionError("Damaged legacy row discarded"); }
        catch (IOException expected) { }
        System.out.println("Calibration, end-segment extrapolation, repeatability separation, synthetic rejection and lossless CSV migration checks passed");
    }
}
