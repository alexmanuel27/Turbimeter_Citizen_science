package org.citizenscience.turbimeter;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Insets;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;

public final class MainActivity extends Activity {
    private static final String USB_PERMISSION = "org.citizenscience.turbimeter.USB_PERMISSION";
    private static final String CSV_NAME = "measurements.csv";
    private static final int INK = 0xff18343c;
    private static final int MUTED = 0xff63777f;
    private static final int TEAL = 0xff087f83;
    private static final int PAPER = 0xfff3f7f7;
    private static final int BORDER = 0xffdce7e7;
    private static final int AMBER = 0xff8b5b14;

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
    private TextView detail;
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
            status.setText("USB host mode unavailable on this device");
    }

    private void buildScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(PAPER);
        if (Build.VERSION.SDK_INT >= 35) scroll.setOnApplyWindowInsetsListener((view, insets) -> {
            Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
            view.setPadding(0, bars.top, 0, bars.bottom);
            return insets;
        });
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        if (Build.VERSION.SDK_INT < 35) {
            getWindow().setStatusBarColor(PAPER);
            getWindow().setNavigationBarColor(PAPER);
        }
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(22), dp(26), dp(22), dp(32));
        scroll.addView(content);

        TextView eyebrow = label("CITIZEN SCIENCE  /  FIELD TOOL", 12);
        eyebrow.setTextColor(TEAL);
        eyebrow.setLetterSpacing(0.08f);
        content.addView(eyebrow);
        TextView title = label("Turbimeter", 32);
        title.setTypeface(null, Typeface.BOLD);
        content.addView(title);
        TextView subtitle = label("Your phone is the display for the USB-C sensor.", 14);
        subtitle.setTextColor(MUTED);
        content.addView(subtitle, spaced(4));

        LinearLayout connection = card(content);
        TextView connectionHeading = label("SENSOR", 12);
        connectionHeading.setTextColor(TEAL);
        connectionHeading.setLetterSpacing(0.08f);
        connection.addView(connectionHeading);
        status = label("Sensor disconnected", 18);
        status.setTypeface(null, Typeface.BOLD);
        connection.addView(status, spaced(8));
        TextView connectionHint = label("XIAO SAMD21 · USB-C data cable", 14);
        connectionHint.setTextColor(MUTED);
        connection.addView(connectionHint, spaced(3));
        connectButton = button("Connect sensor", connection, false, () -> {
            if (reader == null) connect();
            else disconnect("Disconnected");
        });

        LinearLayout reading = card(content);
        TextView readingHeading = label("CURRENT READING", 12);
        readingHeading.setTextColor(TEAL);
        readingHeading.setLetterSpacing(0.08f);
        reading.addView(readingHeading);
        value = label("—", 64);
        value.setTypeface(null, Typeface.BOLD);
        reading.addView(value, spaced(10));
        unit = label("No measurement", 20);
        unit.setTextColor(MUTED);
        reading.addView(unit);
        quality = label("WAITING FOR SENSOR", 12);
        quality.setTypeface(null, Typeface.BOLD);
        quality.setLetterSpacing(0.06f);
        quality.setPadding(dp(12), dp(8), dp(12), dp(8));
        reading.addView(quality, spaced(20));
        setQuality("WAITING FOR SENSOR", 0xffe9f0f1, MUTED);
        detail = label("Connect the sensor, then request a reading.", 14);
        detail.setTextColor(MUTED);
        reading.addView(detail, spaced(12));

        measureButton = button("Measure now", content, true, () -> {
            if (reader != null) {
                latest = null;
                enabled(saveButton, false);
                value.setText("—");
                unit.setText("Waiting for sensor");
                setQuality("MEASURING", 0xffe7f4f1, TEAL);
                detail.setText("Requesting a new reading from the sensor.");
                status.setText("Measuring…");
                reader.measure();
            }
        });
        enabled(measureButton, false);
        saveButton = button("Save reading on phone", content, false, this::saveReading);
        enabled(saveButton, false);
        shareButton = button("Share saved readings (CSV)", content, false, this::shareReadings);
        enabled(shareButton, getFileStreamPath(CSV_NAME).exists());

        if ((getApplicationInfo().flags & android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
            button("Preview example screen", content, false, () -> {
                latest = null;
                value.setText("12.30");
                unit.setText("FNU · example only");
                setQuality("SCREEN PREVIEW", 0xfffff2da, AMBER);
                detail.setText("Illustrative value. No sensor was measured; saving is disabled.");
                enabled(saveButton, false);
            });
            button("Test XIAO via Mac bridge", content, false, this::testMacBridge);
        }

        TextView note = label("Readings stay on this phone until you choose to share them. Nothing is uploaded automatically.", 13);
        note.setTextColor(MUTED);
        content.addView(note, spaced(20));
        setContentView(scroll);
    }

    private TextView label(String text, int size) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(INK);
        return view;
    }

    private LinearLayout card(LinearLayout parent) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(20), dp(20), dp(20), dp(20));
        card.setBackground(background(0xffffffff, BORDER, 18));
        parent.addView(card, spaced(20));
        return card;
    }

    private GradientDrawable background(int fill, int stroke, int radius) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(fill);
        shape.setCornerRadius(dp(radius));
        if (stroke != 0) shape.setStroke(dp(1), stroke);
        return shape;
    }

    private void setQuality(String text, int fill, int color) {
        quality.setText(text);
        quality.setTextColor(color);
        quality.setBackground(background(fill, 0, 8));
    }

    private Button button(String text, LinearLayout parent, boolean primary, Runnable action) {
        Button button = new Button(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextSize(16);
        button.setTypeface(null, Typeface.BOLD);
        button.setTextColor(primary ? 0xffffffff : TEAL);
        button.setBackgroundTintList(ColorStateList.valueOf(primary ? TEAL : 0xffe7f4f1));
        button.setMinimumHeight(dp(54));
        button.setOnClickListener(view -> action.run());
        parent.addView(button, spaced(12));
        return button;
    }

    private void enabled(Button button, boolean value) {
        button.setEnabled(value);
        button.setAlpha(value ? 1f : 0.45f);
    }

    private LinearLayout.LayoutParams spaced(int margin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(margin);
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
                    enabled(measureButton, true);
                });
            }

            @Override public void onLine(String line) {
                runOnUiThread(() -> {
                    if (generation != readerGeneration) return;
                    if (showLine(line)) status.setText("Sensor connected");
                });
            }

            @Override public void onError(String message) {
                runOnUiThread(() -> {
                    if (generation != readerGeneration) return;
                    lastError = message;
                    status.setText(message);
                    detail.setText("Check the USB connection and try again.");
                });
            }

            @Override public void onClosed() {
                runOnUiThread(() -> {
                    if (generation != readerGeneration) return;
                    reader = null;
                    activeDevice = null;
                    connectButton.setText("Connect sensor");
                    enabled(measureButton, false);
                    status.setText(lastError == null ? "Sensor disconnected" : lastError);
                });
            }
        });
        status.setText("Connecting…");
        connectButton.setText("Disconnect");
        enabled(measureButton, false);
        reader.start();
    }

    private void showFrame(MeasurementFrame frame) {
        latest = frame;
        value.setText(frame.displayValue());
        unit.setText(frame.kind == MeasurementFrame.Kind.RESULT ? frame.unit : "Optical ratio");
        if (frame.kind == MeasurementFrame.Kind.DEMO) {
            setQuality("DEMO SIGNAL", 0xfffff2da, AMBER);
            detail.setText("USB communication works. This is synthetic data, not a water measurement.");
        } else if (frame.kind == MeasurementFrame.Kind.RATIO) {
            setQuality("UNCALIBRATED", 0xfffff2da, AMBER);
            detail.setText("A real optical reading. Calibration is needed before reporting turbidity.");
        } else {
            setQuality("CALIBRATED", 0xffe7f4f1, TEAL);
            detail.setText("Calibration " + frame.calibrationId + " · ready to save on this phone.");
        }
        enabled(saveButton, frame.kind != MeasurementFrame.Kind.DEMO);
    }

    private boolean showLine(String line) {
        MeasurementFrame frame = MeasurementFrame.parse(line);
        String fault = MeasurementFrame.errorMessage(line);
        if (frame == null && fault == null) return false;
        if (fault == null) showFrame(frame);
        else {
            latest = null;
            value.setText("—");
            unit.setText("No measurement");
            setQuality("CHECK SENSOR", 0xfffff2da, AMBER);
            detail.setText(fault);
            enabled(saveButton, false);
        }
        return true;
    }

    private void testMacBridge() {
        latest = null;
        enabled(saveButton, false);
        status.setText("Testing XIAO via Mac bridge…");
        detail.setText("Sending MEASURE from the emulator to the XIAO.");
        new Thread(() -> {
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress("10.0.2.2", 8765), 3000);
                socket.setSoTimeout(5000);
                socket.getOutputStream().write("MEASURE\n".getBytes(StandardCharsets.US_ASCII));
                String line = new BufferedReader(new InputStreamReader(
                        socket.getInputStream(), StandardCharsets.US_ASCII)).readLine();
                if (line == null) throw new IOException("No reply from XIAO");
                runOnUiThread(() -> {
                    if (!showLine(line)) detail.setText("Unexpected reply: " + line);
                    latest = null; // Debug bridge data must not be saved as a phone measurement.
                    enabled(saveButton, false);
                    status.setText("XIAO replied via Mac bridge");
                });
            } catch (IOException error) {
                runOnUiThread(() -> {
                    status.setText("Mac bridge unavailable");
                    detail.setText(error.getMessage());
                });
            }
        }, "turbimeter-mac-bridge").start();
    }

    private void disconnect(String message) {
        ++readerGeneration;
        if (reader != null) reader.stop();
        reader = null;
        activeDevice = null;
        pendingDevice = null;
        connectButton.setText("Connect sensor");
        enabled(measureButton, false);
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
            enabled(saveButton, false);
            enabled(shareButton, true);
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
