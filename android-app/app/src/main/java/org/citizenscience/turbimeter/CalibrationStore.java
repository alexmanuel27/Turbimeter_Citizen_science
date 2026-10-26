package org.citizenscience.turbimeter;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

/** Keeps immutable curves and active sensor associations locally. Design: Ing. Alex M. Rivera. */
final class CalibrationStore {
    private final SharedPreferences preferences;

    CalibrationStore(Context context) {
        preferences = context.getSharedPreferences("calibrations", Context.MODE_PRIVATE);
    }

    CalibrationCurve active(String sensorId) throws JSONException {
        if (sensorId == null) return null;
        String id = preferences.getString("active:" + sensorId, null);
        if (id == null) return null;
        String data = preferences.getString("curve:" + id, null);
        if (data == null) throw new JSONException("Saved calibration is missing.");
        CalibrationCurve curve = decode(data);
        if (!sensorId.equals(curve.sensorId)) throw new JSONException("Saved sensor identity does not match.");
        return curve;
    }

    void activate(CalibrationCurve curve) throws JSONException {
        String key = "curve:" + curve.id;
        String data = encode(curve).toString();
        String previous = preferences.getString(key, null);
        // Reusing an ID must never silently change the meaning of old readings.
        if (previous != null && !encode(decode(previous)).toString().equals(data))
            throw new JSONException("This calibration ID already exists with different data.");
        if (!preferences.edit().putString(key, data)
                .putString("active:" + curve.sensorId, curve.id).commit())
            throw new JSONException("Could not save calibration.");
    }

    boolean deactivate(String sensorId) {
        return preferences.edit().remove("active:" + sensorId).commit();
    }

    boolean clear() {
        return preferences.edit().clear().commit();
    }

    String archive() throws JSONException {
        JSONArray curves = new JSONArray();
        for (String key : preferences.getAll().keySet()) {
            if (key.startsWith("curve:")) curves.put(new JSONObject(preferences.getString(key, "")));
        }
        return new JSONObject().put("design", "Ing. Alex M. Rivera")
                .put("calibrations", curves).toString(2);
    }

    static JSONObject encode(CalibrationCurve curve) throws JSONException {
        JSONArray points = new JSONArray();
        for (CalibrationCurve.Point point : curve.points) {
            JSONArray readings = new JSONArray();
            for (double ratio : point.readings) readings.put(ratio);
            points.put(new JSONObject().put("reference_value", point.referenceValue)
                    .put("ratios", readings).put("captured_at", point.capturedAt));
        }
        return new JSONObject().put("schema", 1).put("design", "Ing. Alex M. Rivera")
                .put("method", "piecewise_linear").put("extrapolation", "end_segments").put("id", curve.id)
                .put("sensor_id", curve.sensorId).put("unit", curve.unit)
                .put("reference", curve.reference).put("created_at", curve.createdAt)
                .put("points", points);
    }

    static CalibrationCurve decode(String data) throws JSONException {
        try {
            JSONObject root = new JSONObject(data);
            if (root.getInt("schema") != 1 || !"piecewise_linear".equals(root.getString("method"))
                    || !"end_segments".equals(root.getString("extrapolation")))
                throw new JSONException("Unsupported calibration format.");
            JSONArray values = root.getJSONArray("points");
            if (values.length() < 3 || values.length() > CalibrationCurve.MAX_POINTS)
                throw new JSONException("A calibration needs 3 to 12 points.");
            List<CalibrationCurve.Point> points = new ArrayList<>();
            for (int i = 0; i < values.length(); i++) {
                JSONObject point = values.getJSONObject(i);
                JSONArray raw = point.getJSONArray("ratios");
                if (raw.length() != CalibrationCurve.REPEATS)
                    throw new JSONException("Each point must contain five readings.");
                double[] readings = new double[raw.length()];
                for (int j = 0; j < readings.length; j++) readings[j] = raw.getDouble(j);
                points.add(new CalibrationCurve.Point(point.getDouble("reference_value"), readings,
                        point.getString("captured_at")));
            }
            return new CalibrationCurve(root.getString("id"), root.getString("sensor_id"),
                    root.getString("unit"), root.getString("reference"),
                    root.getString("created_at"), points);
        } catch (IllegalArgumentException | java.time.DateTimeException error) {
            throw new JSONException(error.getMessage());
        }
    }
}
