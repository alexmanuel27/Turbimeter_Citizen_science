package org.citizenscience.turbimeter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
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
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.AtomicFile;
import android.view.View;
import android.view.Gravity;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

/** Design: Ing. Alex M. Rivera. */
public final class MainActivity extends Activity {
    private static final String USB_PERMISSION = "com.Turbidimeter.main.USB_PERMISSION";
    private static final String CSV_NAME = "measurements.csv";
    private static final String PREFS_NAME = "contributor";
    private static final int IMPORT_CALIBRATION = 10;
    private static final int EXPORT_CALIBRATION = 11;
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
    private int readerGeneration;
    private String lastError;
    private TextView status;
    private TextView value;
    private TextView unit;
    private TextView quality;
    private TextView detail;
    private Button connectButton;
    private Button measureButton;
    private Button shareButton;
    private ScrollView homePage;
    private ScrollView historyPage;
    private ScrollView privacyPage;
    private LinearLayout historyList;
    private TextView pageTitle;
    private View drawerScrim;
    private View drawer;
    private CalibrationScreen calibrationPage;
    private CalibrationStore calibrationStore;
    private CalibrationCurve calibration;
    private String sensorId;
    private boolean readerReady;
    private boolean waitingForReading;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable readingTimeout;
    private double[] calibrationReadings;
    private int calibrationCount;
    private String captureSensorId;
    private boolean csvReady;
    private String pendingCalibrationExport;

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

    @SuppressLint("UnspecifiedRegisterReceiverFlag") // Flags are unavailable before API 33.
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        usbManager = (UsbManager) getSystemService(USB_SERVICE);
        calibrationStore = new CalibrationStore(this);
        if (state != null) pendingCalibrationExport = state.getString("calibration_export");
        buildScreen();
        if (state != null) {
            calibrationPage.restoreDraft(state.getBundle("calibration_draft"));
            selectPage(state.getInt("page", 0));
        }
        IntentFilter filter = new IntentFilter(USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(usbReceiver, filter, RECEIVER_EXPORTED);
        else registerReceiver(usbReceiver, filter);
    }

    private void buildScreen() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(PAPER);
        if (Build.VERSION.SDK_INT >= 35) root.setOnApplyWindowInsetsListener((view, insets) -> {
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
        LinearLayout shell = new LinearLayout(this);
        shell.setOrientation(LinearLayout.VERTICAL);
        root.addView(shell, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setPadding(dp(12), dp(8), dp(18), dp(8));
        toolbar.setBackgroundColor(0xffffffff);
        shell.addView(toolbar, new LinearLayout.LayoutParams(-1, dp(64)));
        Button menu = new Button(this);
        menu.setText("☰");
        menu.setContentDescription("Open navigation menu");
        menu.setTextSize(24);
        menu.setAllCaps(false);
        menu.setTextColor(INK);
        menu.setBackgroundTintList(ColorStateList.valueOf(0xffffffff));
        menu.setOnClickListener(view -> showDrawer(true));
        toolbar.addView(menu, new LinearLayout.LayoutParams(dp(56), dp(50)));
        pageTitle = label("Home", 20);
        pageTitle.setTypeface(null, Typeface.BOLD);
        toolbar.addView(pageTitle, new LinearLayout.LayoutParams(0, -2, 1));
        ImageView smallLogo = new ImageView(this);
        smallLogo.setImageResource(R.drawable.ares_logo);
        smallLogo.setContentDescription("ARES Cuban Water Lab logo");
        toolbar.addView(smallLogo, new LinearLayout.LayoutParams(dp(42), dp(42)));

        FrameLayout pages = new FrameLayout(this);
        shell.addView(pages, new LinearLayout.LayoutParams(-1, 0, 1));
        homePage = buildHome();
        historyPage = buildHistory();
        privacyPage = buildPrivacy();
        calibrationPage = new CalibrationScreen(this, calibrationStore);
        pages.addView(homePage, new FrameLayout.LayoutParams(-1, -1));
        pages.addView(historyPage, new FrameLayout.LayoutParams(-1, -1));
        pages.addView(privacyPage, new FrameLayout.LayoutParams(-1, -1));
        pages.addView(calibrationPage, new FrameLayout.LayoutParams(-1, -1));

        drawerScrim = new View(this);
        drawerScrim.setBackgroundColor(0x88000000);
        drawerScrim.setOnClickListener(view -> showDrawer(false));
        root.addView(drawerScrim, new FrameLayout.LayoutParams(-1, -1));
        LinearLayout menuPanel = new LinearLayout(this);
        menuPanel.setOrientation(LinearLayout.VERTICAL);
        menuPanel.setPadding(dp(22), dp(28), dp(22), dp(20));
        menuPanel.setBackgroundColor(0xffffffff);
        FrameLayout.LayoutParams panelParams = new FrameLayout.LayoutParams(dp(280), -1, Gravity.START);
        root.addView(menuPanel, panelParams);
        drawer = menuPanel;
        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.ares_logo);
        logo.setContentDescription("ARES Cuban Water Lab logo");
        menuPanel.addView(logo, new LinearLayout.LayoutParams(dp(98), dp(98)));
        TextView menuTitle = label("Turbimeter", 24);
        menuTitle.setTypeface(null, Typeface.BOLD);
        menuPanel.addView(menuTitle, spaced(12));
        TextView menuSubtitle = label("Citizen science field tool", 14);
        menuSubtitle.setTextColor(MUTED);
        menuPanel.addView(menuSubtitle);
        button("Home", menuPanel, false, () -> selectPage(0));
        button("Calibration", menuPanel, false, () -> selectPage(3));
        button("History", menuPanel, false, () -> selectPage(1));
        button("Privacy", menuPanel, false, () -> selectPage(2));
        showDrawer(false);
        selectPage(0);
        setContentView(root);
    }

    private ScrollView page(LinearLayout content) {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.addView(content);
        return scroll;
    }

    LinearLayout pageContent() {
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(22), dp(26), dp(22), dp(32));
        return content;
    }

    private ScrollView buildHome() {
        LinearLayout content = pageContent();
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
            if (readerReady && !measurementBusy()) {
                value.setText("—");
                unit.setText("Waiting for sensor");
                setQuality("MEASURING", 0xffe7f4f1, TEAL);
                detail.setText("Requesting a new reading from the sensor.");
                status.setText("Measuring…");
                requestReading();
            }
        });
        enabled(measureButton, false);
        TextView note = label("Sensor readings are saved in History on this phone. Uncalibrated ratios are not turbidity values.", 13);
        note.setTextColor(MUTED);
        content.addView(note, spaced(20));
        return page(content);
    }

    private ScrollView buildHistory() {
        LinearLayout content = pageContent();
        TextView title = label("History", 30);
        title.setTypeface(null, Typeface.BOLD);
        content.addView(title);
        TextView subtitle = label("Real readings saved on this phone. Uncalibrated values are marked clearly.", 14);
        subtitle.setTextColor(MUTED);
        content.addView(subtitle, spaced(6));
        shareButton = button("Share readings (CSV)", content, false, this::shareReadings);
        button("Export calibration records (JSON)", content, false, () -> {
            try { exportCalibration("Turbimeter-calibration-records.json", calibrationStore.archive()); }
            catch (Exception error) { Toast.makeText(this, "Could not share calibration records", Toast.LENGTH_LONG).show(); }
        });
        enabled(shareButton, getFileStreamPath(CSV_NAME).exists());
        historyList = new LinearLayout(this);
        historyList.setOrientation(LinearLayout.VERTICAL);
        content.addView(historyList);
        return page(content);
    }

    private ScrollView buildPrivacy() {
        LinearLayout content = pageContent();
        TextView title = label("Privacy", 30);
        title.setTypeface(null, Typeface.BOLD);
        content.addView(title);
        TextView policy = label("WaterLab Turbidimeter uses USB only to communicate with your sensor. It saves readings, calibration samples, reference instrument details and the sensor's USB identity on this phone. It has no account, analytics, advertising, location access, or data upload.\n\nReadings and calibrations remain here until you delete them or uninstall the app. Sharing CSV or calibration JSON is your choice; the app you choose to share with handles that copy. If you used an earlier version, deleting local data also removes any saved contributor details.\n\nPrivacy contact: cubanwaterlab@gmail.com", 15);
        policy.setTextColor(MUTED);
        content.addView(policy, spaced(14));
        button("Full privacy policy", content, false, () -> {
            Intent browser = new Intent(Intent.ACTION_VIEW, Uri.parse(
                    "https://alexmanuelrivera.com/Turbimeter_Citizen_science/privacy-policy.html"));
            startActivity(browser);
        });
        button("Contact the project", content, false, () -> {
            Intent browser = new Intent(Intent.ACTION_VIEW, Uri.parse(
                    "https://github.com/alexmanuel27/Turbimeter_Citizen_science/issues"));
            startActivity(browser);
        });
        button("Delete all local data", content, false, this::confirmDeleteData);
        return page(content);
    }

    private void selectPage(int index) {
        if (index != 3 && calibrationReadings != null) cancelCalibrationCapture();
        homePage.setVisibility(index == 0 ? View.VISIBLE : View.GONE);
        historyPage.setVisibility(index == 1 ? View.VISIBLE : View.GONE);
        privacyPage.setVisibility(index == 2 ? View.VISIBLE : View.GONE);
        calibrationPage.setVisibility(index == 3 ? View.VISIBLE : View.GONE);
        pageTitle.setText(index == 0 ? "Home" : index == 1 ? "History" : index == 2 ? "Privacy" : "Calibration");
        if (index == 1) refreshHistory();
        if (index == 3) calibrationPage.refresh();
        showDrawer(false);
    }

    private void showDrawer(boolean visible) {
        drawerScrim.setVisibility(visible ? View.VISIBLE : View.GONE);
        drawer.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    TextView label(String text, int size) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(INK);
        return view;
    }

    LinearLayout card(LinearLayout parent) {
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

    Button button(String text, LinearLayout parent, boolean primary, Runnable action) {
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

    void enabled(Button button, boolean value) {
        button.setEnabled(value);
        button.setAlpha(value ? 1f : 0.45f);
    }

    LinearLayout.LayoutParams spaced(int margin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(margin);
        return params;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void connect() {
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_USB_HOST)) {
            status.setText("USB host mode unavailable on this device");
            return;
        }
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
        readerReady = false;
        sensorId = null;
        try {
            String serial = device.getSerialNumber();
            if (serial != null && !serial.isEmpty()) {
                String candidate = String.format(Locale.US, "%04x:%04x:%s", device.getVendorId(), device.getProductId(), serial);
                if (candidate.matches("[A-Za-z0-9._:-]{1,160}")) sensorId = candidate;
            }
        } catch (SecurityException ignored) {
            // Unidentified sensors may be read, but must not inherit another sensor's curve.
        }
        reloadCalibration();
        calibrationPage.sensorChanged();
        lastError = null;
        int generation = ++readerGeneration;
        reader = new UsbCdcReader(usbManager, device, new UsbCdcReader.Listener() {
            @Override public void onReady() {
                runOnUiThread(() -> {
                    if (generation != readerGeneration) return;
                    readerReady = true;
                    status.setText("Sensor connected");
                    syncControls();
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
                    failReading(message);
                    disconnect(message);
                    status.setText(message);
                    detail.setText("Check the USB connection and try again.");
                });
            }

            @Override public void onClosed() {
                runOnUiThread(() -> {
                    if (generation != readerGeneration) return;
                    reader = null;
                    readerReady = false;
                    activeDevice = null;
                    sensorId = null;
                    calibration = null;
                    clearPending();
                    if (calibrationReadings != null) {
                        calibrationReadings = null;
                        calibrationPage.captureFailed("Sensor disconnected.");
                    }
                    connectButton.setText("Connect sensor");
                    enabled(measureButton, false);
                    calibrationPage.sensorChanged();
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
        value.setText(frame.displayValue());
        unit.setText(frame.kind == MeasurementFrame.Kind.RESULT ? frame.unit : "Optical ratio");
        if (frame.kind == MeasurementFrame.Kind.RATIO) {
            setQuality("UNCALIBRATED", 0xfffff2da, AMBER);
            detail.setText("Optical ratio saved in History. Calibration is needed for a turbidity value.");
        } else {
            setQuality("CALIBRATED", 0xffe7f4f1, TEAL);
            detail.setText("Calibration " + frame.calibrationId + " · saved in History.");
        }
    }

    private boolean showLine(String line) {
        if (!waitingForReading) return false;
        MeasurementFrame frame = MeasurementFrame.parse(line);
        String fault = MeasurementFrame.errorMessage(line);
        if (frame == null && fault == null) return false;
        clearPending();
        if (calibrationReadings != null) {
            if (fault != null || frame.kind != MeasurementFrame.Kind.RATIO) {
                failReading(fault != null ? fault : "Calibration requires optical ratios from the measurement firmware.");
                return true;
            }
            if (sensorId == null || !sensorId.equals(captureSensorId)) {
                failReading("Sensor identity changed during capture.");
                return true;
            }
            calibrationReadings[calibrationCount++] = frame.value;
            calibrationPage.captureProgress(calibrationCount);
            if (calibrationCount == CalibrationCurve.REPEATS) {
                double[] captured = calibrationReadings;
                calibrationReadings = null;
                calibrationPage.captureComplete(captured, captureSensorId);
                syncControls();
            } else {
                int generation = readerGeneration;
                handler.postDelayed(() -> {
                    if (generation == readerGeneration && calibrationReadings != null) requestReading();
                }, 200);
            }
            return true;
        }
        if (fault == null) {
            Double rawRatio = frame.kind == MeasurementFrame.Kind.RATIO ? frame.value : null;
            String qualityCode = frame.kind == MeasurementFrame.Kind.RESULT ? "OK" : "UNCALIBRATED";
            String usedCalibration = frame.calibrationId;
            if (rawRatio != null && calibration != null && calibration.sensorId.equals(sensorId)) {
                usedCalibration = calibration.id;
                double converted = calibration.convert(rawRatio);
                if (Double.isFinite(converted)) {
                    qualityCode = calibration.isExtrapolated(rawRatio) ? "EXTRAPOLATED" : "OK";
                    frame = MeasurementFrame.calibrated(converted, calibration.unit, calibration.id);
                } else qualityCode = "CONVERSION_ERROR";
            }
            showFrame(frame);
            if ("EXTRAPOLATED".equals(qualityCode)) {
                setQuality("EXTRAPOLATED", 0xfffff2da, AMBER);
                detail.setText("Estimate beyond the measured reference range, using the nearest end segment. Calibration "
                        + usedCalibration + " · optical ratio " + rawRatio + " · saved in History."
                        + (frame.value < 0 ? " A negative estimate is not a physical turbidity value." : ""));
            } else if ("CONVERSION_ERROR".equals(qualityCode)) {
                setQuality("CONVERSION ERROR", 0xfffff2da, AMBER);
                detail.setText("Could not calculate a finite estimate. The raw optical ratio was saved.");
            }
            if (!saveReading(frame, qualityCode, usedCalibration, rawRatio)) {
                detail.setText("Reading received, but could not be saved in History.");
                syncControls();
                return false;
            }
        }
        else {
            value.setText("—");
            unit.setText("No measurement");
            setQuality("CHECK SENSOR", 0xfffff2da, AMBER);
            detail.setText(fault);
        }
        syncControls();
        if (historyPage.getVisibility() == View.VISIBLE) refreshHistory();
        return true;
    }

    private void disconnect(String message) {
        ++readerGeneration;
        readerReady = false;
        clearPending();
        if (reader != null) reader.stop();
        reader = null;
        activeDevice = null;
        sensorId = null;
        calibration = null;
        pendingDevice = null;
        if (calibrationReadings != null) {
            calibrationReadings = null;
            calibrationPage.captureFailed(message);
        }
        connectButton.setText("Connect sensor");
        enabled(measureButton, false);
        status.setText(message);
        if (calibrationPage != null) calibrationPage.sensorChanged();
    }

    private boolean saveReading(MeasurementFrame frame, String qualityCode, String usedCalibration, Double rawRatio) {
        File file = getFileStreamPath(CSV_NAME);
        String row = Instant.now() + "," + Double.toString(frame.value) + ","
                + frame.unit + "," + qualityCode + "," + usedCalibration + ","
                + (rawRatio == null ? "" : Double.toString(rawRatio)) + ","
                + (sensorId == null ? "" : sensorId) + "\n";
        if (!csvReady || !file.exists()) try {
            String original = file.exists() ? new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8) : "";
            String upgraded = ReadingLog.upgrade(original);
            if (!original.equals(upgraded)) {
                AtomicFile atomic = new AtomicFile(file);
                FileOutputStream output = atomic.startWrite();
                try {
                    output.write(upgraded.getBytes(StandardCharsets.UTF_8));
                    atomic.finishWrite(output);
                } catch (IOException error) {
                    atomic.failWrite(output);
                    throw error;
                }
            }
            csvReady = true;
        } catch (IOException error) {
            status.setText("Could not update history; existing data kept");
            return false;
        }
        try (FileOutputStream output = openFileOutput(CSV_NAME, MODE_APPEND)) {
            output.write(row.getBytes(StandardCharsets.UTF_8));
            enabled(shareButton, true);
            return true;
        } catch (IOException error) {
            status.setText("Could not save the reading");
            return false;
        }
    }

    private void refreshHistory() {
        historyList.removeAllViews();
        File file = getFileStreamPath(CSV_NAME);
        if (!file.exists()) {
            TextView empty = label("No real readings yet. Connect the sensor and tap Measure now.", 15);
            empty.setTextColor(MUTED);
            historyList.addView(empty, spaced(24));
            return;
        }
        try {
            List<String[]> rows = ReadingLog.rows(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8));
            int shown = 0;
            for (int i = rows.size() - 1; i >= 0 && shown < 50; i--) {
                String[] fields = rows.get(i);
                LinearLayout item = card(historyList);
                TextView amount = label(fields[1] + " " + fields[2], 24);
                amount.setTypeface(null, Typeface.BOLD);
                item.addView(amount);
                TextView meta = label(fields[0] + " UTC · " + fields[3], 13);
                meta.setTextColor(MUTED);
                item.addView(meta, spaced(6));
                if (!fields[4].isEmpty()) item.addView(label("Calibration: " + fields[4], 13), spaced(4));
                if (fields.length == 7 && !fields[5].isEmpty()) item.addView(label("Optical ratio: " + fields[5], 13), spaced(4));
                if (fields.length == 7 && !fields[6].isEmpty()) item.addView(label("Sensor: " + fields[6], 12), spaced(4));
                shown++;
            }
            if (shown == 0) historyList.addView(label("No real readings yet.", 15), spaced(24));
            else if (rows.size() > shown) {
                TextView note = label("Showing the 50 newest readings. Share CSV for the complete record.", 13);
                note.setTextColor(MUTED);
                historyList.addView(note, spaced(16));
            }
        } catch (IOException error) {
            historyList.addView(label("Could not open reading history.", 15), spaced(24));
        }
    }

    private void confirmDeleteData() {
        new AlertDialog.Builder(this)
                .setTitle("Delete local data?")
                .setMessage("This permanently removes saved readings, calibration profiles, calibration drafts and any contributor details from earlier versions of this app.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    disconnect("Disconnected");
                    File file = getFileStreamPath(CSV_NAME);
                    if (file.exists() && !deleteFile(CSV_NAME)) {
                        Toast.makeText(this, "Could not delete readings", Toast.LENGTH_LONG).show();
                        return;
                    }
                    csvReady = false;
                    if (!getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().clear().commit()) {
                        Toast.makeText(this, "Could not delete previous contributor details", Toast.LENGTH_LONG).show();
                        return;
                    }
                    if (!calibrationStore.clear()) {
                        Toast.makeText(this, "Could not delete saved calibrations", Toast.LENGTH_LONG).show();
                        return;
                    }
                    calibrationPage.clearDraft();
                    value.setText("—");
                    unit.setText("No measurement");
                    setQuality("WAITING FOR SENSOR", 0xffe9f0f1, MUTED);
                    detail.setText("Connect the sensor, then request a reading.");
                    enabled(shareButton, false);
                    refreshHistory();
                    Toast.makeText(this, "Local data deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void shareReadings() {
        try {
            byte[] csv = Files.readAllBytes(getFileStreamPath(CSV_NAME).toPath());
            shareText("Turbimeter readings.csv", "text/plain", new String(csv, StandardCharsets.UTF_8));
        } catch (IOException error) {
            status.setText("Could not share saved readings");
        }
    }

    String sensorId() { return sensorId; }
    boolean sensorReady() { return readerReady; }
    boolean measurementBusy() { return waitingForReading || calibrationReadings != null; }
    CalibrationCurve activeCalibration() { return calibration; }

    void reloadCalibration() {
        calibration = null;
        try { calibration = calibrationStore.active(sensorId); }
        catch (Exception error) {
            Toast.makeText(this, "Saved calibration could not be loaded. Using optical ratios.", Toast.LENGTH_LONG).show();
        }
        value.setText("—");
        unit.setText("No measurement");
        setQuality("WAITING FOR READING", 0xffe9f0f1, MUTED);
        detail.setText(calibration == null ? "No active sensor calibration. Readings will be optical ratios."
                : "Calibration " + calibration.id + " is active for this sensor.");
        if (calibrationPage != null) calibrationPage.refresh();
    }

    private void syncControls() {
        enabled(measureButton, readerReady && !measurementBusy());
        if (calibrationPage != null) calibrationPage.refresh();
    }

    void startCalibrationCapture() {
        if (!readerReady || reader == null || sensorId == null || measurementBusy()) {
            calibrationPage.message("Connect an identified sensor and wait for the current reading.");
            return;
        }
        captureSensorId = sensorId;
        calibrationReadings = new double[CalibrationCurve.REPEATS];
        calibrationCount = 0;
        calibrationPage.captureStarted();
        requestReading();
    }

    void cancelCalibrationCapture() {
        if (calibrationReadings != null) disconnect("Capture cancelled. Reconnect the sensor to continue.");
    }

    private void requestReading() {
        if (!readerReady || reader == null || waitingForReading) return;
        waitingForReading = true;
        int generation = readerGeneration;
        readingTimeout = () -> {
            if (generation != readerGeneration || !waitingForReading) return;
            disconnect("Sensor did not reply. Reconnect and try again.");
            value.setText("—");
            unit.setText("No measurement");
            setQuality("CHECK SENSOR", 0xfffff2da, AMBER);
            detail.setText("The measurement timed out. No reading or calibration point was saved.");
        };
        handler.postDelayed(readingTimeout, 5000);
        syncControls();
        reader.measure();
    }

    private void clearPending() {
        waitingForReading = false;
        if (readingTimeout != null) handler.removeCallbacks(readingTimeout);
        readingTimeout = null;
    }

    private void failReading(String message) {
        clearPending();
        if (calibrationReadings != null) {
            calibrationReadings = null;
            calibrationPage.captureFailed(message);
        }
        value.setText("—");
        unit.setText("No measurement");
        setQuality("CHECK SENSOR", 0xfffff2da, AMBER);
        detail.setText(message);
        syncControls();
    }

    void shareText(String title, String mime, String text) {
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType(mime);
        share.putExtra(Intent.EXTRA_SUBJECT, title);
        share.putExtra(Intent.EXTRA_TEXT, text);
        startActivity(Intent.createChooser(share, "Share"));
    }

    void importCalibration() {
        if (!readerReady || sensorId == null || measurementBusy()) {
            calibrationPage.message("Connect the sensor before importing its calibration.");
            return;
        }
        Intent select = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        select.addCategory(Intent.CATEGORY_OPENABLE);
        select.setType("*/*");
        select.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/json", "text/plain", "application/octet-stream"});
        startActivityForResult(select, IMPORT_CALIBRATION);
    }

    void exportCalibration(String filename, String json) {
        pendingCalibrationExport = json;
        Intent save = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        save.addCategory(Intent.CATEGORY_OPENABLE);
        save.setType("application/json");
        save.putExtra(Intent.EXTRA_TITLE, filename);
        startActivityForResult(save, EXPORT_CALIBRATION);
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        if (pendingCalibrationExport != null) state.putString("calibration_export", pendingCalibrationExport);
        state.putBundle("calibration_draft", calibrationPage.saveDraft());
        state.putInt("page", calibrationPage.getVisibility() == View.VISIBLE ? 3
                : historyPage.getVisibility() == View.VISIBLE ? 1
                : privacyPage.getVisibility() == View.VISIBLE ? 2 : 0);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == EXPORT_CALIBRATION) {
            String text = pendingCalibrationExport;
            pendingCalibrationExport = null;
            if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
            try (OutputStream output = getContentResolver().openOutputStream(data.getData(), "wt")) {
                if (output == null || text == null) throw new IOException("Export was interrupted. Try again.");
                output.write(text.getBytes(StandardCharsets.UTF_8));
                Toast.makeText(this, "Calibration file exported", Toast.LENGTH_SHORT).show();
            } catch (Exception error) { Toast.makeText(this, "Export failed: " + error.getMessage(), Toast.LENGTH_LONG).show(); }
            return;
        }
        if (requestCode != IMPORT_CALIBRATION || resultCode != RESULT_OK || data == null || data.getData() == null) return;
        selectPage(3);
        try (InputStream input = getContentResolver().openInputStream(data.getData());
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            if (input == null) throw new IOException("Could not open calibration file.");
            byte[] buffer = new byte[4096];
            int count;
            while ((count = input.read(buffer)) != -1) {
                if (output.size() + count > 65536) throw new IOException("Calibration file exceeds 64 KB.");
                output.write(buffer, 0, count);
            }
            calibrationPage.importCurve(new String(output.toByteArray(), StandardCharsets.UTF_8));
        } catch (Exception error) { calibrationPage.message("Import failed: " + error.getMessage()); }
    }

    @Override protected void onDestroy() {
        unregisterReceiver(usbReceiver);
        disconnect("Disconnected");
        super.onDestroy();
    }
}
