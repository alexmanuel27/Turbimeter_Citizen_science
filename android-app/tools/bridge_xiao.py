#!/usr/bin/env python3
"""Debug-only TCP bridge from Android emulator to the XIAO USB serial port."""

import os
import select
import socket
import sys
import termios
import tty

port = sys.argv[1] if len(sys.argv) > 1 else "/dev/cu.usbmodem101"
serial = os.open(port, os.O_RDWR | os.O_NOCTTY | os.O_NONBLOCK)
tty.setraw(serial)
settings = termios.tcgetattr(serial)
settings[4] = settings[5] = termios.B115200
termios.tcsetattr(serial, termios.TCSANOW, settings)

try:
    with socket.socket() as server:
        server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        server.bind(("127.0.0.1", 8765))
        server.listen(1)
        print(f"XIAO bridge ready on {port}", flush=True)
        while True:
            connection, _ = server.accept()
            with connection:
                connection.settimeout(5)
                command = connection.makefile("rb").readline()
                if not command.endswith(b"\n"):
                    continue
                print("emulator -> XIAO:", command.decode().strip(), flush=True)
                os.write(serial, command)
                response = bytearray()
                while not response.endswith(b"\n"):
                    ready, _, _ = select.select([serial], [], [], 5)
                    if not ready:
                        raise TimeoutError("No reply from XIAO")
                    chunk = os.read(serial, 256)
                    if not chunk:
                        raise ConnectionError("XIAO serial port closed")
                    response.extend(chunk)
                print("XIAO -> emulator:", response.decode().strip(), flush=True)
                connection.sendall(response)
except KeyboardInterrupt:
    print("\nBridge stopped")
finally:
    os.close(serial)
