package org.citizenscience.turbimeter;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;

public final class MainActivity extends Activity {
    private static final String USB_PERMISSION = "org.citizenscience.turbimeter.USB_PERMISSION";
    private static final String CSV_NAME = "measurements.csv";

    private UsbManager usbManager;
    private UsbDevice pendingDevice;
    private UsbDevice activeDevice;
    private UsbCdcReader reader;
    private MeasurementFrame latest;
    private int readerGeneration;
    private String lastError;
    private TextView status;
    private TextView value;
    private TextView unit;
    private TextView quality;
    private Button connectButton;
    private Button measureButton;
    private Button saveButton;
    private Button shareButton;

    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (USB_PERMISSION.equals(intent.getAction())) {
                UsbDevice device = pendingDevice;
                pendingDevice = null;
                if (device != null && usbManager.hasPermission(device)) open(device);
                else status.setText("USB access was not granted");
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(intent.getAction())
                    && activeDevice != null
                    && !usbManager.getDeviceList().containsKey(activeDevice.getDeviceName())) {
                disconnect("Sensor disconnected");
            }
        }
    };

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        usbManager = (UsbManager) getSystemService(USB_SERVICE);
        buildScreen();
        IntentFilter filter = new IntentFilter(USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(usbReceiver, filter, RECEIVER_EXPORTED);
        else registerReceiver(usbReceiver, filter);
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_USB_HOST))
            status.setText("This phone does not support USB host mode");
    }

    private void buildScreen() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(24), dp(32), dp(24), dp(32));
        scroll.addView(content);

        TextView title = label("Turbimeter", 28);
        content.addView(title);
        status = label("Connect the sensor to your Android phone", 16);
        content.addView(status, spaced());
        value = label("—", 52);
        content.addView(value, spaced());
        unit = label("No measurement", 20);
        content.addView(unit);
        quality = label("The app will identify demo and uncalibrated readings.", 15);
        content.addView(quality, spaced());

        connectButton = button("Connect sensor", content, () -> {
            if (reader == null) connect();
            else disconnect("Disconnected");
        });
        measureButton = button("Measure", content, () -> {
            if (reader != null) {
                latest = null;
                saveButton.setEnabled(false);
                value.setText("—");
                unit.setText("Waiting for sensor");
                quality.setText("Requesting a new reading");
                status.setText("Measuring…");
                reader.measure();
            }
        });
        measureButton.setEnabled(false);
        saveButton = button("Save reading on phone", content, this::saveReading);
        saveButton.setEnabled(false);
        shareButton = button("Share saved readings", content, this::shareReadings);
        shareButton.setEnabled(getFileStreamPath(CSV_NAME).exists());

        TextView note = label("Real readings can be saved. Demo data cannot. Nothing is uploaded automatically.", 14);
        content.addView(note, spaced());
        setContentView(scroll);
    }

    private TextView label(String text, int size) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(size);
        return view;
    }

    private Button button(String text, LinearLayout parent, Runnable action) {
        Button button = new Button(this);
        button.setText(text);
        button.setOnClickListener(view -> action.run());
        parent.addView(button, spaced());
        return button;
    }

    private LinearLayout.LayoutParams spaced() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(16);
        return params;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void connect() {
        UsbDevice device = null;
        for (UsbDevice candidate : usbManager.getDeviceList().values()) {
            if (UsbCdcReader.supports(candidate)) {
                device = candidate;
                break;
            }
        }
        if (device == null) {
            status.setText("No USB-CDC sensor found");
            return;
        }
        if (usbManager.hasPermission(device)) {
            open(device);
            return;
        }
        pendingDevice = device;
        status.setText("Waiting for USB permission…");
        Intent intent = new Intent(USB_PERMISSION).setPackage(getPackageName());
        PendingIntent permission = PendingIntent.getBroadcast(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        usbManager.requestPermission(device, permission);
    }

    private void open(UsbDevice device) {
        activeDevice = device;
        lastError = null;
        int generation = ++readerGeneration;
        reader = new UsbCdcReader(usbManager, device, new UsbCdcReader.Listener() {
            @Override public void onReady() {
                runOnUiThread(() -> {
                    if (generation != readerGeneration) return;
                    status.setText("Sensor connected");
                    measureButton.setEnabled(true);
                });
            }

            @Override public void onLine(String line) {
                MeasurementFrame frame = MeasurementFrame.parse(line);
                if (frame == null) return;
                runOnUiThread(() -> {
                    if (generation != readerGeneration) return;
                    latest = frame;
                    value.setText(frame.displayValue());
                    unit.setText(frame.unit);
                    if (frame.kind == MeasurementFrame.Kind.DEMO)
                        quality.setText("Demo signal — not a water measurement");
                    else if (frame.kind == MeasurementFrame.Kind.RATIO)
                        quality.setText("Optical ratio — not calibrated turbidity");
                    else quality.setText("Calibrated · " + frame.calibrationId);
                    saveButton.setEnabled(frame.kind != MeasurementFrame.Kind.DEMO);
                    status.setText("Measurement received");
                });
            }

            @Override public void onError(String message) {
                runOnUiThread(() -> {
                    if (generation != readerGeneration) return;
                    lastError = message;
                    status.setText(message);
                });
            }

            @Override public void onClosed() {
                runOnUiThread(() -> {
                    if (generation != readerGeneration) return;
                    reader = null;
                    activeDevice = null;
                    connectButton.setText("Connect sensor");
                    measureButton.setEnabled(false);
                    status.setText(lastError == null ? "Sensor disconnected" : lastError);
                });
            }
        });
        status.setText("Connecting…");
        connectButton.setText("Disconnect");
        measureButton.setEnabled(false);
        reader.start();
    }

    private void disconnect(String message) {
        ++readerGeneration;
        if (reader != null) reader.stop();
        reader = null;
        activeDevice = null;
        pendingDevice = null;
        connectButton.setText("Connect sensor");
        measureButton.setEnabled(false);
        status.setText(message);
    }

    private void saveReading() {
        if (latest == null || latest.kind == MeasurementFrame.Kind.DEMO) return;
        File file = getFileStreamPath(CSV_NAME);
        String qualityCode = latest.kind == MeasurementFrame.Kind.RESULT ? "OK" : "UNCALIBRATED";
        String row = Instant.now() + "," + Double.toString(latest.value) + ","
                + latest.unit + "," + qualityCode + "," + latest.calibrationId + "\n";
        try (FileOutputStream output = openFileOutput(CSV_NAME, MODE_APPEND)) {
            if (file.length() == 0) output.write("timestamp_utc,value,unit,quality,calibration_id\n"
                    .getBytes(StandardCharsets.UTF_8));
            output.write(row.getBytes(StandardCharsets.UTF_8));
            saveButton.setEnabled(false);
            shareButton.setEnabled(true);
            Toast.makeText(this, "Reading saved", Toast.LENGTH_SHORT).show();
        } catch (IOException error) {
            status.setText("Could not save the reading");
        }
    }

    private void shareReadings() {
        try {
            byte[] csv = Files.readAllBytes(getFileStreamPath(CSV_NAME).toPath());
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("text/plain");
            share.putExtra(Intent.EXTRA_SUBJECT, "Turbimeter readings.csv");
            share.putExtra(Intent.EXTRA_TEXT, new String(csv, StandardCharsets.UTF_8));
            startActivity(Intent.createChooser(share, "Share readings"));
        } catch (IOException error) {
            status.setText("Could not share saved readings");
        }
    }

    @Override protected void onDestroy() {
        unregisterReceiver(usbReceiver);
        disconnect("Disconnected");
        super.onDestroy();
    }
}
