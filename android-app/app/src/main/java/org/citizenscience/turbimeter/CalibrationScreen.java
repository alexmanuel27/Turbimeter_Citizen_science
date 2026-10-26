package org.citizenscience.turbimeter;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** Paired reference/sensor measurements. Design: Ing. Alex M. Rivera. */
final class CalibrationScreen extends ScrollView {
    private final MainActivity host;
    private final CalibrationStore store;
    private final TextView activeText;
    private final TextView progress;
    private final EditText referenceInput;
    private final EditText referenceValue;
    private final Spinner unit;
    private final LinearLayout pointsList;
    private final Button captureButton;
    private final Button cancelButton;
    private final Button addButton;
    private final Button saveButton;
    private final Button importButton;
    private final Button exportButton;
    private final Button deactivateButton;
    private final Button clearButton;
    private final List<CalibrationCurve.Point> points = new ArrayList<>();
    private String draftSensorId;
    private String capturedAt;
    private double[] captured;
    private boolean capturing;

    CalibrationScreen(MainActivity host, CalibrationStore store) {
        super(host);
        this.host = host;
        this.store = store;
        setFillViewport(true);
        LinearLayout content = host.pageContent();
        addView(content);
        content.addView(host.label("Calibration", 30));
        content.addView(host.label("Compare the same sample with your reference turbidimeter. Keep the cuvette clean, the enclosure closed and the sample well mixed, without bubbles.", 14), host.spaced(8));

        LinearLayout activeCard = host.card(content);
        activeCard.addView(host.label("SAVED CALIBRATION", 12));
        activeText = host.label("Connect a sensor to see its calibration.", 15);
        activeCard.addView(activeText, host.spaced(8));
        exportButton = host.button("Export this calibration (JSON)", activeCard, false, () -> {
            try {
                host.exportCalibration(host.activeCalibration().id + ".json",
                        CalibrationStore.encode(host.activeCalibration()).toString(2));
            } catch (Exception error) { message(error.getMessage()); }
        });
        importButton = host.button("Import calibration (JSON)", activeCard, false, host::importCalibration);
        deactivateButton = host.button("Use uncalibrated ratios", activeCard, false, () ->
                new AlertDialog.Builder(host).setTitle("Deactivate calibration?")
                        .setMessage("Future readings will be optical ratios. Existing readings and calibration records are kept.")
                        .setPositiveButton("Deactivate", (dialog, which) -> {
                            if (!store.deactivate(host.sensorId())) message("Could not deactivate calibration.");
                            else host.reloadCalibration();
                        }).setNegativeButton("Cancel", null).show());

        LinearLayout settings = host.card(content);
        settings.addView(host.label("NEW REFERENCE COMPARISON", 12));
        referenceInput = input(settings, "Reference instrument and ID", false);
        referenceInput.setSingleLine(true);
        settings.addView(host.label("Reference unit", 14), host.spaced(12));
        unit = new Spinner(host);
        unit.setContentDescription("Reference turbidity unit");
        unit.setAdapter(new ArrayAdapter<>(host, android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Choose unit", "FNU", "NTU"}));
        settings.addView(unit, host.spaced(4));
        settings.addView(host.label("Use the unit reported by the reference instrument. A comparison does not certify ISO/EPA compliance.", 13), host.spaced(6));

        LinearLayout sample = host.card(content);
        sample.addView(host.label("CURRENT SAMPLE", 12));
        progress = host.label("1. Measure our sensor. 2. Enter the reference reading. 3. Add the pair.", 15);
        sample.addView(progress, host.spaced(8));
        captureButton = host.button("Measure sample (5 readings)", sample, true, () -> {
            if (!points.isEmpty() && !host.sensorId().equals(draftSensorId)) {
                message("This draft belongs to another sensor. Clear it before capturing a new sensor.");
                return;
            }
            host.startCalibrationCapture();
        });
        cancelButton = host.button("Cancel capture", sample, false, host::cancelCalibrationCapture);
        referenceValue = input(sample, "Reference reading", true);
        addButton = host.button("Add calibration point", sample, false, this::addPoint);

        pointsList = new LinearLayout(host);
        pointsList.setOrientation(LinearLayout.VERTICAL);
        content.addView(pointsList);
        saveButton = host.button("Save and use calibration", content, true, this::save);
        clearButton = host.button("Clear draft", content, false, () ->
                new AlertDialog.Builder(host).setTitle("Clear calibration draft?")
                        .setMessage("The saved calibration and your measurement history are kept.")
                        .setPositiveButton("Clear draft", (dialog, which) -> clearDraft())
                        .setNegativeButton("Cancel", null).show());
        content.addView(host.label("Capture at least 3 different samples covering your working range, including a low-turbidity sample. Conversion uses straight lines between points. Outside that range, the nearest end segment is extended and results are marked EXTRAPOLATED. Check a separate sample before field use. Changes to the optics, electronics or cuvette require a new calibration.", 13), host.spaced(18));
        refresh();
    }

    private EditText input(LinearLayout parent, String title, boolean numeric) {
        parent.addView(host.label(title, 14), host.spaced(12));
        EditText input = new EditText(host);
        input.setContentDescription(title);
        input.setHint(numeric ? "Value from reference instrument" : "e.g. model / serial / calibration date");
        input.setInputType(numeric ? InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL
                : InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        parent.addView(input, host.spaced(4));
        return input;
    }

    void refresh() {
        CalibrationCurve curve = host.activeCalibration();
        boolean connected = host.sensorReady() && host.sensorId() != null;
        if (!host.sensorReady()) activeText.setText("Connect the sensor from Home to calibrate it.");
        else if (host.sensorId() == null) activeText.setText("USB serial number unavailable. Readings work, but a calibration cannot be linked safely to this sensor.");
        else if (curve == null) activeText.setText("No active calibration for this sensor.\n" + host.sensorId());
        else {
            StringBuilder summary = new StringBuilder(curve.id + "\n" + curve.reference + "\n" + curve.createdAt
                    + "\nSensor: " + curve.sensorId + "\n" + range(curve)
                    + "\n" + curve.points.size() + " points · linear interpolation");
            for (CalibrationCurve.Point point : curve.points)
                summary.append(String.format(Locale.US, "\n%.3f %s ↔ %.5f", point.referenceValue, curve.unit, point.ratio));
            activeText.setText(summary);
        }
        host.enabled(captureButton, connected && !capturing && !host.measurementBusy());
        host.enabled(cancelButton, capturing);
        host.enabled(addButton, connected && captured != null && !capturing);
        host.enabled(saveButton, connected && points.size() >= 3 && !capturing);
        host.enabled(importButton, connected && !capturing && !host.measurementBusy());
        host.enabled(exportButton, curve != null && !capturing);
        host.enabled(deactivateButton, curve != null && !capturing && !host.measurementBusy());
        exportButton.setVisibility(curve == null ? GONE : VISIBLE);
        deactivateButton.setVisibility(curve == null ? GONE : VISIBLE);
        host.enabled(clearButton, !capturing && (!points.isEmpty() || captured != null));
        unit.setEnabled(points.isEmpty() && !capturing);
        referenceInput.setEnabled(!capturing);
        referenceValue.setEnabled(!capturing);
    }

    void captureStarted() {
        captured = null;
        capturing = true;
        renderPoints();
        message("Keep the enclosure closed and the sample still. Reading 1 / 5…");
        refresh();
    }

    void captureProgress(int count) {
        message("Captured " + count + " / 5 readings. Keep the sample still.");
    }

    void captureComplete(double[] readings, String sensorId) {
        capturing = false;
        captured = readings.clone();
        draftSensorId = sensorId;
        capturedAt = Instant.now().toString();
        CalibrationCurve.Point sample = new CalibrationCurve.Point(0, captured, capturedAt);
        message(String.format(Locale.US, "Mean optical ratio: %.5f\nSD: %.5f · 5 readings\nEnter the reference reading and add this point.", sample.ratio, sample.sd));
        renderPoints();
        refresh();
    }

    void captureFailed(String reason) {
        capturing = false;
        captured = null;
        message(reason + " No point was added.");
        renderPoints();
        refresh();
    }

    void sensorChanged() {
        // An unfinished capture must not be paired with a newly connected instrument.
        captured = null;
        refresh();
    }

    private void addPoint() {
        try {
            if (captured == null || !host.sensorId().equals(draftSensorId))
                throw new IllegalArgumentException("Measure this sensor's sample first.");
            if (points.size() >= CalibrationCurve.MAX_POINTS)
                throw new IllegalArgumentException("The draft already has 12 points.");
            if (unit.getSelectedItemPosition() == 0)
                throw new IllegalArgumentException("Choose the unit shown by the reference instrument.");
            double known = Double.parseDouble(referenceValue.getText().toString().trim().replace(',', '.'));
            for (CalibrationCurve.Point point : points) {
                if (point.referenceValue == known) throw new IllegalArgumentException("That reference value already has a point. Remove it to replace it.");
            }
            points.add(new CalibrationCurve.Point(known, captured, capturedAt));
            captured = null;
            referenceValue.setText("");
            renderPoints();
            message("Point added. Prepare the next sample and measure its reference value.");
        } catch (NumberFormatException error) {
            message("Enter a numeric reference reading.");
        } catch (IllegalArgumentException error) { message(error.getMessage()); }
        refresh();
    }

    private void renderPoints() {
        pointsList.removeAllViews();
        pointsList.addView(host.label("PAIRED POINTS · " + points.size() + " / 12", 14), host.spaced(20));
        for (CalibrationCurve.Point point : points) {
            LinearLayout row = host.card(pointsList);
            row.addView(host.label(String.format(Locale.US, "%.3f %s  ↔  ratio %.5f", point.referenceValue, unit.getSelectedItem(), point.ratio), 18));
            row.addView(host.label(String.format(Locale.US, "5 readings · SD %.5f", point.sd), 13));
            Button remove = host.button("Remove point", row, false, () -> {
                points.remove(point);
                renderPoints();
                refresh();
            });
            host.enabled(remove, !capturing);
        }
    }

    private void save() {
        try {
            if (!host.sensorReady() || !host.sensorId().equals(draftSensorId))
                throw new IllegalArgumentException("Reconnect the sensor used for these points.");
            CalibrationCurve curve = new CalibrationCurve("CAL-" + UUID.randomUUID().toString().replace("-", "").substring(0, 20),
                    draftSensorId, unit.getSelectedItem().toString(), referenceInput.getText().toString(),
                    Instant.now().toString(), points);
            store.activate(curve);
            clearDraft();
            host.reloadCalibration();
            message("Calibration saved for this sensor. Check a separate reference sample before field use.");
        } catch (Exception error) { message(error.getMessage()); }
    }

    void importCurve(String text) {
        try {
            CalibrationCurve curve = CalibrationStore.decode(text);
            if (!host.sensorReady() || !curve.sensorId.equals(host.sensorId()))
                throw new IllegalArgumentException("This calibration belongs to another sensor. Connect its sensor before importing.");
            new AlertDialog.Builder(host).setTitle("Use imported calibration?")
                    .setMessage(curve.id + "\n" + curve.reference + "\n" + range(curve)
                            + "\n\nThis will replace the active calibration for this sensor. History and earlier curves are kept.")
                    .setPositiveButton("Use calibration", (dialog, which) -> {
                        try {
                            if (!host.sensorReady() || !curve.sensorId.equals(host.sensorId()))
                                throw new IllegalArgumentException("Reconnect the matching sensor.");
                            store.activate(curve);
                            host.reloadCalibration();
                            message("Imported calibration activated.");
                        } catch (Exception error) { message(error.getMessage()); }
                    }).setNegativeButton("Cancel", null).show();
        } catch (Exception error) { message("Import failed: " + error.getMessage()); }
    }

    void clearDraft() {
        points.clear();
        captured = null;
        draftSensorId = null;
        referenceValue.setText("");
        referenceInput.setText("");
        unit.setSelection(0);
        renderPoints();
        message("Draft cleared. The saved calibration is kept.");
        refresh();
    }

    Bundle saveDraft() {
        Bundle draft = new Bundle();
        draft.putString("sensor", draftSensorId);
        draft.putString("reference", referenceInput.getText().toString());
        draft.putString("value", referenceValue.getText().toString());
        draft.putInt("unit", unit.getSelectedItemPosition());
        draft.putInt("count", points.size());
        for (int i = 0; i < points.size(); i++) {
            CalibrationCurve.Point point = points.get(i);
            Bundle item = new Bundle();
            item.putDouble("reference", point.referenceValue);
            item.putDoubleArray("ratios", point.readings);
            item.putString("time", point.capturedAt);
            draft.putBundle("point" + i, item);
        }
        return draft;
    }

    void restoreDraft(Bundle draft) {
        if (draft == null) return;
        draftSensorId = draft.getString("sensor");
        referenceInput.setText(draft.getString("reference", ""));
        referenceValue.setText(draft.getString("value", ""));
        unit.setSelection(draft.getInt("unit"));
        for (int i = 0; i < draft.getInt("count"); i++) {
            Bundle item = draft.getBundle("point" + i);
            points.add(new CalibrationCurve.Point(item.getDouble("reference"),
                    item.getDoubleArray("ratios"), item.getString("time")));
        }
        renderPoints();
        message("Reconnect the sensor to continue. Added points are kept; any unfinished capture must be repeated.");
        refresh();
    }

    void message(String text) {
        progress.setText(text == null ? "Calibration could not be completed." : text);
    }

    private static String range(CalibrationCurve curve) {
        return String.format(Locale.US, "Range: %.3f–%.3f %s", curve.points.get(0).referenceValue,
                curve.points.get(curve.points.size() - 1).referenceValue, curve.unit);
    }
}
