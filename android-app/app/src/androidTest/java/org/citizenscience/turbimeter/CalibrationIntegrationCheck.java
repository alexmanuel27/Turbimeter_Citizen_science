package org.citizenscience.turbimeter;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

/** Emulator-only fixtures; never part of the app APK. Design: Ing. Alex M. Rivera. */
public final class CalibrationIntegrationCheck extends Instrumentation {
    private static final String SENSOR = "2886:802f:TEST_SENSOR";
    private MainActivity activity;
    private Context context;

    @Override public void onCreate(Bundle arguments) { super.onCreate(arguments); start(); }

    @Override public void onStart() {
        Bundle result = new Bundle();
        try {
            if (!"ranchu".equals(android.os.Build.HARDWARE) && !"goldfish".equals(android.os.Build.HARDWARE))
                throw new AssertionError("This test may only run in a disposable emulator.");
            context = getTargetContext();
            check(new CalibrationStore(context).clear(), "Clear test calibration storage");
            context.deleteFile("measurements.csv");
            activity = (MainActivity) startActivitySync(new Intent(context, MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            checkStore();
            checkFlow();
            result.putString("stream", "Calibration integration passed: sensor isolation, export/import, immutable IDs, capture, conversion, range, history migration, cancellation, timeout, and synthetic rejection.\n");
            finish(Activity.RESULT_OK, result);
        } catch (Throwable error) {
            StringWriter trace = new StringWriter();
            error.printStackTrace(new PrintWriter(trace));
            result.putString("stream", trace.toString());
            finish(Activity.RESULT_CANCELED, result);
        } finally {
            if (context != null) {
                new CalibrationStore(context).clear();
                context.deleteFile("measurements.csv");
            }
        }
    }

    private void checkStore() throws Exception {
        CalibrationCurve.Point p0 = point(0, .6), p1 = point(10, .7), p2 = point(100, 1);
        CalibrationCurve original = new CalibrationCurve("CAL-store", SENSOR, "FNU", "Fixture reference",
                "2026-10-25T10:00:00Z", Arrays.asList(p0, p1, p2));
        CalibrationStore store = new CalibrationStore(context);
        store.activate(original);
        check(new CalibrationStore(context).active(SENSOR).id.equals(original.id), "Calibration survives store reload");
        check(store.active("2886:802f:OTHER_SENSOR") == null, "No cross-sensor calibration");
        CalibrationCurve imported = CalibrationStore.decode(CalibrationStore.encode(original).toString());
        check(Math.abs(imported.convert(.85) - 55) < 1e-8, "JSON round trip preserves mapping");
        store.activate(imported);
        try {
            store.activate(new CalibrationCurve(original.id, SENSOR, "FNU", "Changed reference",
                    original.createdAt, original.points));
            throw new AssertionError("Existing calibration was overwritten");
        } catch (org.json.JSONException expected) { }
        check(store.active(SENSOR).reference.equals("Fixture reference"), "Original immutable profile kept");
        try {
            CalibrationStore.decode(CalibrationStore.encode(original).put("schema", 999).toString());
            throw new AssertionError("Unknown import schema accepted");
        } catch (org.json.JSONException expected) { }
        check(store.deactivate(SENSOR), "Deactivate calibration");
        check(store.active(SENSOR) == null && store.archive().contains(original.id), "Deactivation keeps historical curve");
        store.clear();
    }

    private void checkFlow() throws Exception {
        main(() -> {
            check(!((Button) field(field(activity, "calibrationPage"), "captureButton")).isEnabled(), "Capture disabled without sensor");
            attach(SENSOR);
            invoke(activity, "selectPage", new Class<?>[]{int.class}, 3);
            Object screen = field(activity, "calibrationPage");
            ((EditText) field(screen, "referenceInput")).setText("Fixture reference, emulator only");
            ((Spinner) field(screen, "unit")).setSelection(1);
        });
        for (int point = 0; point < 3; point++) {
            final double ratio = new double[]{.6, .7, 1}[point];
            final String reference = new String[]{"0", "10", "100"}[point];
            main(() -> activity.startCalibrationCapture());
            for (int repeat = 0; repeat < 5; repeat++) {
                if (repeat != 0) SystemClock.sleep(250);
                main(() -> feed("TURB1,RATIO," + ratio));
            }
            main(() -> {
                Object screen = field(activity, "calibrationPage");
                ((EditText) field(screen, "referenceValue")).setText(reference);
                ((Button) field(screen, "addButton")).performClick();
                check(!context.getFileStreamPath("measurements.csv").exists(), "Calibration captures are not ordinary readings");
            });
        }
        main(() -> {
            Object screen = field(activity, "calibrationPage");
            CalibrationScreen restored = new CalibrationScreen(activity, new CalibrationStore(context));
            restored.restoreDraft(((CalibrationScreen) screen).saveDraft());
            check(((List<?>) field(restored, "points")).size() == 3
                    && ((EditText) field(restored, "referenceInput")).getText().toString().contains("Fixture reference"),
                    "Completed draft points and reference survive recreation");
            ((Button) field(screen, "saveButton")).performClick();
            check(activity.activeCalibration() != null && activity.activeCalibration().points.size() == 3, "Capture and activate curve");
            invoke(activity, "selectPage", new Class<?>[]{int.class}, 0);
        });
        String legacy = ReadingLog.LEGACY_HEADER + "\n2026-10-24T15:31:21Z,0.6457,ratio,UNCALIBRATED,\n";
        Files.write(context.getFileStreamPath("measurements.csv").toPath(), legacy.getBytes(StandardCharsets.UTF_8));
        main(() -> {
            ((Button) field(activity, "measureButton")).performClick();
            feed("TURB1,RATIO,0.85");
            check(((TextView) field(activity, "value")).getText().toString().equals("55.00"), "Home converted value");
            check(((TextView) field(activity, "unit")).getText().toString().equals("FNU"), "Reference unit on Home");
        });
        List<String[]> rows = history();
        check(rows.size() == 2 && rows.get(0)[1].equals("0.6457"), "Legacy history preserved");
        check(rows.get(1)[5].equals("0.85") && rows.get(1)[6].equals(SENSOR), "Raw ratio and sensor identity retained");
        main(() -> {
            ((Button) field(activity, "measureButton")).performClick();
            feed("TURB1,RATIO,2.0");
            check(((TextView) field(activity, "quality")).getText().toString().equals("EXTRAPOLATED"), "Extrapolation marked on Home");
            check(((TextView) field(activity, "value")).getText().toString().equals("400.00"), "Upper end-segment extrapolation");
        });
        check(history().get(2)[3].equals("EXTRAPOLATED") && history().get(2)[2].equals("FNU"), "Extrapolation provenance in history");
        int count = history().size();
        main(() -> {
            ((Button) field(activity, "measureButton")).performClick();
            feed("TURB1,DEMO,0.85");
            check(((TextView) field(activity, "quality")).getText().toString().equals("CHECK SENSOR"), "Synthetic firmware rejected on screen");
            feed("TURB1,RATIO,0.9");
        });
        check(history().size() == count, "No synthetic or unsolicited history rows");
        main(() -> {
            attach("2886:802f:OTHER_SENSOR");
            check(activity.activeCalibration() == null, "Home does not inherit another sensor's curve");
            ((Button) field(activity, "measureButton")).performClick();
            feed("TURB1,RATIO,0.85");
        });
        check(history().get(count)[3].equals("UNCALIBRATED"), "Second sensor stays uncalibrated");
        main(() -> {
            attach(SENSOR);
            activity.startCalibrationCapture();
            feed("TURB1,ERROR,SATURATED");
            check(field(activity, "calibrationReadings") == null, "Hardware errors abort capture");
            activity.startCalibrationCapture();
            activity.cancelCalibrationCapture();
            check(!activity.sensorReady(), "Cancellation disconnects to discard late replies");
            attach(SENSOR);
            activity.startCalibrationCapture();
            ((Runnable) field(activity, "readingTimeout")).run();
            check(!activity.sensorReady() && field(activity, "calibrationReadings") == null, "Timeout aborts without a partial point");
        });
    }

    private void attach(String id) {
        set(activity, "reader", new UsbCdcReader(null, null, null));
        set(activity, "sensorId", id);
        set(activity, "readerReady", true);
        activity.reloadCalibration();
        invoke(activity, "syncControls", new Class<?>[]{});
    }

    private void feed(String line) { invoke(activity, "showLine", new Class<?>[]{String.class}, line); }

    private List<String[]> history() throws Exception {
        return ReadingLog.rows(new String(Files.readAllBytes(context.getFileStreamPath("measurements.csv").toPath()), StandardCharsets.UTF_8));
    }

    private CalibrationCurve.Point point(double value, double ratio) {
        return new CalibrationCurve.Point(value, new double[]{ratio, ratio, ratio, ratio, ratio}, "2026-10-25T10:00:00Z");
    }

    private void main(Runnable run) {
        Throwable[] failure = new Throwable[1];
        runOnMainSync(() -> {
            try { run.run(); } catch (Throwable error) { failure[0] = error; }
        });
        if (failure[0] != null) throw new AssertionError("UI check failed", failure[0]);
    }
    private static void check(boolean condition, String name) { if (!condition) throw new AssertionError(name); }
    private static Object field(Object target, String name) {
        try { Field f = target.getClass().getDeclaredField(name); f.setAccessible(true); return f.get(target); }
        catch (Exception error) { throw new RuntimeException(error); }
    }
    private static void set(Object target, String name, Object value) {
        try { Field f = target.getClass().getDeclaredField(name); f.setAccessible(true); f.set(target, value); }
        catch (Exception error) { throw new RuntimeException(error); }
    }
    private static Object invoke(Object target, String name, Class<?>[] parameters, Object... args) {
        try { Method m = target.getClass().getDeclaredMethod(name, parameters); m.setAccessible(true); return m.invoke(target, args); }
        catch (Exception error) { throw new RuntimeException(error); }
    }
}
