#!/usr/bin/env python3
#
# May require:
# pip install pyserial
#
# Also see:
# - https://pythonhosted.org/pyserial/pyserial.html
# - https://pyserial.readthedocs.io/en/latest/pyserial.html
#
import nmea_parser as NMEAParser
import serial

DEBUG = False


def read_nmea_sentence(serial_port):
    """
    Reads the serial port until a '\n' is met.
    :param serial_port: the port to read, as returned by serial.Serial
    :return: the full NMEA String, with its EOS '\r\n'
    """
    rv = []
    while True:
        ch = serial_port.read()
        if DEBUG:
            print("Read {} from Serial Port".format(ch))
        rv.append(ch)
        if ch == b'\n':
            # string = [x.decode('utf-8') for x in rv]
            string = "".join(map(bytes.decode, rv))
            if DEBUG:
                print("Returning {}".format(string))
            return string


# On mac, USB GPS on port /dev/tty.usbmodem14101,
# Raspberry Pi, use /dev/ttyUSB0 or so.
port_name = "/dev/tty.usbmodem14101"
baud_rate = 4800
port = serial.Serial(port_name, baudrate=baud_rate, timeout=3.0)
print("Let's go. Hit Ctrl+C to stop")
while True:
    rcv = read_nmea_sentence(port)
    # print("\tReceived:" + repr(rcv))  # repr: displays also non printable characters between quotes.
    try:
        nmea_obj = NMEAParser.parse_nmea_sentence(rcv)
        print("=> {}".format(nmea_obj))
    except Exception as ex:
        print("\t\tOoops! {}".format(ex))