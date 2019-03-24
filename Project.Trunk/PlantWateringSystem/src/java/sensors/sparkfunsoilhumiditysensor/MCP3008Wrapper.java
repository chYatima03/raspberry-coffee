package sensors.sparkfunsoilhumiditysensor;

import analogdigitalconverter.mcp3008.MCP3008Reader;
import utils.PinUtil;

import java.util.function.Supplier;

import static utils.StringUtils.lpad;

public class MCP3008Wrapper {
	private boolean simulating = false;
	private int channel = 0;
	private Supplier<Double> humiditySimulator = null;

	private boolean debug = true;

	int lastRead = 0;
	int tolerance = 5;
	boolean first = true;

	private static MCP3008Wrapper instance = null;

	private MCP3008Wrapper() {}

	public static MCP3008Wrapper init(int misoPin, int mosiPin, int clkPin, int csPin, int channel) {
		return init(misoPin, mosiPin, clkPin, csPin, channel, false);
	}
	public static MCP3008Wrapper init(int misoPin, int mosiPin, int clkPin, int csPin, int channel, boolean debug) {
		if (instance == null) {
			instance = new MCP3008Wrapper();
		}
		instance.channel = channel;
		instance.debug = debug;
		try {
			MCP3008Reader.initMCP3008(PinUtil.getPinByGPIONumber(misoPin), PinUtil.getPinByGPIONumber(mosiPin), PinUtil.getPinByGPIONumber(clkPin), PinUtil.getPinByGPIONumber(csPin));

		} catch (UnsatisfiedLinkError ule) {
			// Not on a Pi?
			instance.simulating = true;
		}
		return instance;
	}

	public void setSimulators(Supplier<Double> humSimulator) {
		this.humiditySimulator = humSimulator;
	}

	public void shutdown() {
		if (!this.isSimulating()) {
			MCP3008Reader.shutdownMCP3008();
		}
	}

	public double readHumidity() {
		if (!this.simulating) {
			return readVolume(this.channel);
		} else {
			return this.humiditySimulator.get();
		}
	}

	public int readVolume(int channel) {

		int volume = 0;
		//	System.out.println("Reading channel " + adcChannel);
		int adc = MCP3008Reader.readMCP3008(this.channel);
		//	System.out.println(String.format("From ch %d: %d", adcChannel, adc));
		int postAdjust = Math.abs(adc - lastRead);
		if (first || postAdjust > tolerance) {
			volume = (int)(adc / 10.23); // [0, 1023] ~ [0x0000, 0x03FF] ~ [0&0, 0&1111111111]
			if (debug) {
				System.out.println("readAdc:" + Integer.toString(adc) +
						" (0x" + lpad(Integer.toString(adc, 16).toUpperCase(), 2, "0") +
						", 0&" + lpad(Integer.toString(adc, 2), 8, "0") + ")");
			}
			System.out.println(String.format("Volume: %03d%% (%04d) => %.03f V",
					volume,
					adc,
					(3.3 * (adc / 1023.0))));  // Volts
			lastRead = adc;
			first = false;
		}
		return volume;
	}

	public boolean isSimulating() {
		return this.simulating;
	}
}
