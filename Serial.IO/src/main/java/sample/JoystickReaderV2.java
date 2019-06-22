package sample;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Warning: This is not serial port related!
 * Not Serial, ByteArrayInputStream
 */
public class JoystickReaderV2 {
	private final static String JOYSTICK_INPUT = "/dev/input/js0";
	private final static int BUFFER_SIZE = 16_384; // Should be big enough ;)
	private final static int MAX_DISPLAY_LEN = 32;

	public static void main(String... args) {
		try (DataInputStream joystick = new DataInputStream(new FileInputStream(JOYSTICK_INPUT))) { // Auto-close
			byte[] data = new byte[BUFFER_SIZE];
			List<Byte> byteStream = new ArrayList<>();
			int nb;
			long lastReadTime = System.currentTimeMillis();
			while ((nb = joystick.read(data, 0, data.length)) != -1) {
				long now = System.currentTimeMillis();
				if (now - lastReadTime > 1_000) { // New line, reset buffer
					byteStream.clear();
				}
				lastReadTime = now;
				for (int i=0; i<nb; i++) {
					byteStream.add(data[i]);
					while (byteStream.size() > MAX_DISPLAY_LEN) {
						byteStream.remove(0);
					}
					if (byteStream.size() != 0 && byteStream.size() % 8 == 0) {
						String dump = byteStream.stream()
								.map(b -> String.format("%02X", (b & 0xFF)))
								.collect(Collectors.joining(" "));

						String pos = "None";
						if (byteStream.get(5) == (byte)0x80) {
							if (byteStream.get(7) == 0x00) {
								pos = "Down";
							} else if (byteStream.get(7) == 0x01) {
								pos = "Left";
							}
						} else if (byteStream.get(5) == (byte)0x7F) {
							if (byteStream.get(7) == 0x00) {
								pos = "Up";
							} else if (byteStream.get(7) == 0x01) {
								pos = "Right";
							}
						}
						System.out.println(String.format("%s %s", dump, pos));
						byteStream.clear();
					}
				}
			}
			System.out.println("Done!");
		} catch (FileNotFoundException fnfe) {
			System.err.println("Ooops!");
			fnfe.printStackTrace();
		} catch (IOException ioe) {
			System.err.println("Argh!");
			ioe.printStackTrace();
		}
	}
}