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
import android.view.View;
import android.view.Gravity;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
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
import java.util.List;

public final class MainActivity extends Activity {
    private static final String USB_PERMISSION = "org.citizenscience.turbimeter.USB_PERMISSION";
    private static final String CSV_NAME = "measurements.csv";
    private static final String PREFS_NAME = "contributor";
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
    private Button uploadButton;
    private Button shareButton;
    private ScrollView homePage;
    private ScrollView historyPage;
    private ScrollView signaturePage;
    private ScrollView privacyPage;
    private LinearLayout historyList;
    private EditText contributorName;
    private EditText contributorOrganization;
    private TextView pageTitle;
    private View drawerScrim;
    private View drawer;

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
        buildScreen();
        IntentFilter filter = new IntentFilter(USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(usbReceiver, filter, RECEIVER_EXPORTED);
        else registerReceiver(usbReceiver, filter);
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_USB_HOST))
            status.setText("USB host mode unavailable on this device");
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
        signaturePage = buildSignature();
        privacyPage = buildPrivacy();
        pages.addView(homePage, new FrameLayout.LayoutParams(-1, -1));
        pages.addView(historyPage, new FrameLayout.LayoutParams(-1, -1));
        pages.addView(signaturePage, new FrameLayout.LayoutParams(-1, -1));
        pages.addView(privacyPage, new FrameLayout.LayoutParams(-1, -1));

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
        button("History", menuPanel, false, () -> selectPage(1));
        button("Signature", menuPanel, false, () -> selectPage(2));
        button("Privacy", menuPanel, false, () -> selectPage(3));
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

    private LinearLayout pageContent() {
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
            if (reader != null) {
                latest = null;
                enabled(uploadButton, false);
                value.setText("—");
                unit.setText("Waiting for sensor");
                setQuality("MEASURING", 0xffe7f4f1, TEAL);
                detail.setText("Requesting a new reading from the sensor.");
                status.setText("Measuring…");
                reader.measure();
            }
        });
        enabled(measureButton, false);
        uploadButton = button("Upload reading", content, false, this::uploadReading);
        enabled(uploadButton, false);

        if ((getApplicationInfo().flags & android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
            button("Preview example screen", content, false, () -> {
                latest = null;
                value.setText("12.30");
                unit.setText("FNU · example only");
                setQuality("SCREEN PREVIEW", 0xfffff2da, AMBER);
                detail.setText("Illustrative value. No sensor was measured; saving is disabled.");
                enabled(uploadButton, false);
            });
        }

        TextView note = label("Real readings are saved in History on this phone. Nothing is uploaded automatically.", 13);
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
        enabled(shareButton, getFileStreamPath(CSV_NAME).exists());
        historyList = new LinearLayout(this);
        historyList.setOrientation(LinearLayout.VERTICAL);
        content.addView(historyList);
        return page(content);
    }

    private ScrollView buildSignature() {
        LinearLayout content = pageContent();
        TextView title = label("Signature", 30);
        title.setTypeface(null, Typeface.BOLD);
        content.addView(title);
        TextView subtitle = label("Your self-declared identity will accompany future uploads. It is not a cryptographic signature.", 14);
        subtitle.setTextColor(MUTED);
        content.addView(subtitle, spaced(6));
        LinearLayout card = card(content);
        card.addView(label("Contributor name", 14));
        contributorName = new EditText(this);
        contributorName.setSingleLine(true);
        contributorName.setHint("Your name");
        contributorName.setText(getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getString("name", ""));
        card.addView(contributorName, spaced(4));
        card.addView(label("Institution or group (optional)", 14), spaced(16));
        contributorOrganization = new EditText(this);
        contributorOrganization.setSingleLine(true);
        contributorOrganization.setHint("e.g. community group");
        contributorOrganization.setText(getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getString("organization", ""));
        card.addView(contributorOrganization, spaced(4));
        button("Save signature", card, true, this::saveSignature);
        return page(content);
    }

    private ScrollView buildPrivacy() {
        LinearLayout content = pageContent();
        TextView title = label("Privacy", 30);
        title.setTypeface(null, Typeface.BOLD);
        content.addView(title);
        TextView policy = label("Turbimeter uses USB only to communicate with your sensor. It saves real readings and the name and group you enter in Signature on this phone. No account, analytics, advertising, location access, or automatic upload is used in this version.\n\nReadings remain here until you delete them or uninstall the app. Sharing a CSV is your choice; the app you choose to share with handles that copy.\n\nFor privacy questions, contact the project through GitHub Issues.", 15);
        policy.setTextColor(MUTED);
        content.addView(policy, spaced(14));
        button("Contact the project", content, false, () -> {
            Intent browser = new Intent(Intent.ACTION_VIEW, Uri.parse(
                    "https://github.com/alexmanuel27/Turbimeter_Citizen_science/issues"));
            startActivity(browser);
        });
        button("Delete all local data", content, false, this::confirmDeleteData);
        return page(content);
    }

    private void selectPage(int index) {
        homePage.setVisibility(index == 0 ? View.VISIBLE : View.GONE);
        historyPage.setVisibility(index == 1 ? View.VISIBLE : View.GONE);
        signaturePage.setVisibility(index == 2 ? View.VISIBLE : View.GONE);
        privacyPage.setVisibility(index == 3 ? View.VISIBLE : View.GONE);
        pageTitle.setText(index == 0 ? "Home" : index == 1 ? "History" : index == 2 ? "Signature" : "Privacy");
        if (index == 1) refreshHistory();
        showDrawer(false);
    }

    private void showDrawer(boolean visible) {
        drawerScrim.setVisibility(visible ? View.VISIBLE : View.GONE);
        drawer.setVisibility(visible ? View.VISIBLE : View.GONE);
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
            detail.setText("Real optical reading saved in History. Calibration is needed before upload.");
        } else {
            setQuality("CALIBRATED", 0xffe7f4f1, TEAL);
            detail.setText("Calibration " + frame.calibrationId + " · saved in History.");
        }
        enabled(uploadButton, frame.kind != MeasurementFrame.Kind.DEMO);
    }

    private boolean showLine(String line) {
        MeasurementFrame frame = MeasurementFrame.parse(line);
        String fault = MeasurementFrame.errorMessage(line);
        if (frame == null && fault == null) return false;
        if (fault == null) {
            showFrame(frame);
            if (frame.kind != MeasurementFrame.Kind.DEMO && !saveReading(frame)) {
                detail.setText("Reading received, but could not be saved in History.");
                enabled(uploadButton, false);
                return false;
            }
        }
        else {
            latest = null;
            value.setText("—");
            unit.setText("No measurement");
            setQuality("CHECK SENSOR", 0xfffff2da, AMBER);
            detail.setText(fault);
            enabled(uploadButton, false);
        }
        return true;
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

    private boolean saveReading(MeasurementFrame frame) {
        File file = getFileStreamPath(CSV_NAME);
        String qualityCode = frame.kind == MeasurementFrame.Kind.RESULT ? "OK" : "UNCALIBRATED";
        String row = Instant.now() + "," + Double.toString(frame.value) + ","
                + frame.unit + "," + qualityCode + "," + frame.calibrationId + "\n";
        try (FileOutputStream output = openFileOutput(CSV_NAME, MODE_APPEND)) {
            if (file.length() == 0) output.write("timestamp_utc,value,unit,quality,calibration_id\n"
                    .getBytes(StandardCharsets.UTF_8));
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
            List<String> rows = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
            int shown = 0;
            for (int i = rows.size() - 1; i > 0 && shown < 50; i--) {
                String[] fields = rows.get(i).split(",", -1);
                if (fields.length != 5) continue;
                LinearLayout item = card(historyList);
                TextView amount = label(fields[1] + " " + fields[2], 24);
                amount.setTypeface(null, Typeface.BOLD);
                item.addView(amount);
                TextView meta = label(fields[0] + " UTC · " + fields[3], 13);
                meta.setTextColor(MUTED);
                item.addView(meta, spaced(6));
                if (!fields[4].isEmpty()) item.addView(label("Calibration: " + fields[4], 13), spaced(4));
                shown++;
            }
            if (shown == 0) historyList.addView(label("No real readings yet.", 15), spaced(24));
            else if (rows.size() - 1 > shown) {
                TextView note = label("Showing the 50 newest readings. Share CSV for the complete record.", 13);
                note.setTextColor(MUTED);
                historyList.addView(note, spaced(16));
            }
        } catch (IOException error) {
            historyList.addView(label("Could not open reading history.", 15), spaced(24));
        }
    }

    private void saveSignature() {
        String name = contributorName.getText().toString().trim();
        if (name.isEmpty()) {
            contributorName.setError("Enter your name");
            return;
        }
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                .putString("name", name)
                .putString("organization", contributorOrganization.getText().toString().trim())
                .apply();
        Toast.makeText(this, "Signature saved on this phone", Toast.LENGTH_SHORT).show();
    }

    private void confirmDeleteData() {
        new AlertDialog.Builder(this)
                .setTitle("Delete local data?")
                .setMessage("This permanently removes saved readings and your Signature from this phone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    File file = getFileStreamPath(CSV_NAME);
                    if (file.exists() && !deleteFile(CSV_NAME)) {
                        Toast.makeText(this, "Could not delete readings", Toast.LENGTH_LONG).show();
                        return;
                    }
                    if (!getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().clear().commit()) {
                        Toast.makeText(this, "Could not delete Signature", Toast.LENGTH_LONG).show();
                        return;
                    }
                    contributorName.setText("");
                    contributorOrganization.setText("");
                    latest = null;
                    value.setText("—");
                    unit.setText("No measurement");
                    setQuality("WAITING FOR SENSOR", 0xffe9f0f1, MUTED);
                    detail.setText("Connect the sensor, then request a reading.");
                    enabled(uploadButton, false);
                    enabled(shareButton, false);
                    refreshHistory();
                    Toast.makeText(this, "Local data deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void uploadReading() {
        if (latest == null) return;
        if (latest.kind != MeasurementFrame.Kind.RESULT) {
            new AlertDialog.Builder(this)
                    .setTitle("Calibration required")
                    .setMessage("This optical ratio is saved in History, but it is not yet a quantitative turbidity result.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }
        String name = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getString("name", "");
        if (name.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle("Add your signature")
                    .setMessage("Enter your name in Signature before uploading a reading.")
                    .setPositiveButton("Open Signature", (dialog, which) -> selectPage(2))
                    .setNegativeButton("Cancel", null)
                    .show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Upload not available yet")
                .setMessage("The citizen-science API has not been defined. This reading is safe in History. When upload is connected, each submission will include your signature, its upload time, and a fresh phone location with your permission.")
                .setPositiveButton("OK", null)
                .show();
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
