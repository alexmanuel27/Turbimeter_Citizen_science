package org.citizenscience.turbimeter;

import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

final class UsbCdcReader {
    interface Listener {
        void onReady();
        void onLine(String line);
        void onError(String message);
        void onClosed();
    }

    private final UsbManager manager;
    private final UsbDevice device;
    private final Listener listener;
    private volatile boolean running;
    private volatile UsbDeviceConnection connection;
    private volatile UsbEndpoint writeEndpoint;

    UsbCdcReader(UsbManager manager, UsbDevice device, Listener listener) {
        this.manager = manager;
        this.device = device;
        this.listener = listener;
    }

    static boolean supports(UsbDevice device) {
        return controlInterface(device) != null && dataInterface(device) != null;
    }

    private static UsbInterface controlInterface(UsbDevice device) {
        for (int i = 0; i < device.getInterfaceCount(); i++) {
            UsbInterface candidate = device.getInterface(i);
            if (candidate.getInterfaceClass() == UsbConstants.USB_CLASS_COMM) return candidate;
        }
        return null;
    }

    private static UsbInterface dataInterface(UsbDevice device) {
        for (int i = 0; i < device.getInterfaceCount(); i++) {
            UsbInterface candidate = device.getInterface(i);
            if (candidate.getInterfaceClass() != UsbConstants.USB_CLASS_CDC_DATA) continue;
            if (endpoint(candidate, UsbConstants.USB_DIR_IN) != null
                    && endpoint(candidate, UsbConstants.USB_DIR_OUT) != null) return candidate;
        }
        return null;
    }

    private static UsbEndpoint endpoint(UsbInterface intf, int direction) {
        for (int i = 0; i < intf.getEndpointCount(); i++) {
            UsbEndpoint candidate = intf.getEndpoint(i);
            if (candidate.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK
                    && candidate.getDirection() == direction) return candidate;
        }
        return null;
    }

    void start() {
        running = true;
        new Thread(this::read, "turbimeter-usb").start();
    }

    void stop() {
        running = false;
        UsbDeviceConnection current = connection;
        if (current != null) current.close();
    }

    void measure() {
        UsbDeviceConnection current = connection;
        UsbEndpoint output = writeEndpoint;
        if (!running || current == null || output == null) return;
        new Thread(() -> {
            byte[] command = "MEASURE\n".getBytes(StandardCharsets.US_ASCII);
            if (current.bulkTransfer(output, command, command.length, 1000) != command.length)
                listener.onError("Could not request a measurement");
        }, "turbimeter-command").start();
    }

    private void read() {
        UsbInterface control = controlInterface(device);
        UsbInterface data = dataInterface(device);
        UsbDeviceConnection current = null;
        boolean controlClaimed = false;
        boolean dataClaimed = false;
        try {
            if (control == null || data == null) throw new IOException("USB-CDC interface not found");
            current = manager.openDevice(device);
            if (current == null) throw new IOException("Could not open USB device");
            connection = current;
            if (!running) return;
            controlClaimed = current.claimInterface(control, true);
            dataClaimed = current.claimInterface(data, true);
            if (!controlClaimed || !dataClaimed) throw new IOException("Could not claim USB interfaces");

            byte[] lineCoding = {0, (byte) 0xc2, 1, 0, 0, 0, 8}; // 115200, 8N1
            if (current.controlTransfer(0x21, 0x20, 0, control.getId(), lineCoding,
                    lineCoding.length, 1000) != lineCoding.length)
                throw new IOException("USB-CDC line setup failed");
            if (current.controlTransfer(0x21, 0x22, 3, control.getId(), null, 0, 1000) < 0)
                throw new IOException("USB-CDC DTR setup failed");

            UsbEndpoint input = endpoint(data, UsbConstants.USB_DIR_IN);
            writeEndpoint = endpoint(data, UsbConstants.USB_DIR_OUT);
            listener.onReady();

            byte[] buffer = new byte[256];
            LineFramer lines = new LineFramer();
            while (running) {
                int count = current.bulkTransfer(input, buffer, buffer.length, 500);
                if (count > 0) lines.accept(buffer, count, listener::onLine);
            }
        } catch (Exception error) {
            if (running) listener.onError(error.getMessage() == null ? "USB error" : error.getMessage());
        } finally {
            running = false;
            writeEndpoint = null;
            connection = null;
            if (current != null) {
                if (dataClaimed) current.releaseInterface(data);
                if (controlClaimed) current.releaseInterface(control);
                current.close();
            }
            listener.onClosed();
        }
    }
}
