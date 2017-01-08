package spi.lcd.oled;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;

// import com.pi4j.io.spi.SpiChannel;
// import com.pi4j.io.spi.SpiDevice;
// import com.pi4j.io.spi.impl.SpiDeviceImpl;

import com.pi4j.wiringpi.Spi;

/**
 * SSD1306, small OLED screen. SPI. 128x32
 */
public class SSD1306
{
  public final static int SSD1306_I2C_ADDRESS         = 0x3C; // 011110+SA0+RW - 0x3C or 0x3D
  public final static int SSD1306_SETCONTRAST         = 0x81;
  public final static int SSD1306_DISPLAYALLON_RESUME = 0xA4;
  public final static int SSD1306_DISPLAYALLON        = 0xA5;
  public final static int SSD1306_NORMALDISPLAY       = 0xA6;
  public final static int SSD1306_INVERTDISPLAY       = 0xA7;
  public final static int SSD1306_DISPLAYOFF          = 0xAE;
  public final static int SSD1306_DISPLAYON           = 0xAF;
  public final static int SSD1306_SETDISPLAYOFFSET    = 0xD3;
  public final static int SSD1306_SETCOMPINS          = 0xDA;
  public final static int SSD1306_SETVCOMDETECT       = 0xDB;
  public final static int SSD1306_SETDISPLAYCLOCKDIV  = 0xD5;
  public final static int SSD1306_SETPRECHARGE        = 0xD9;
  public final static int SSD1306_SETMULTIPLEX        = 0xA8;
  public final static int SSD1306_SETLOWCOLUMN        = 0x00;
  public final static int SSD1306_SETHIGHCOLUMN       = 0x10;
  public final static int SSD1306_SETSTARTLINE        = 0x40;
  public final static int SSD1306_MEMORYMODE          = 0x20;
  public final static int SSD1306_COLUMNADDR          = 0x21;
  public final static int SSD1306_PAGEADDR            = 0x22;
  public final static int SSD1306_COMSCANINC          = 0xC0;
  public final static int SSD1306_COMSCANDEC          = 0xC8;
  public final static int SSD1306_SEGREMAP            = 0xA0;
  public final static int SSD1306_CHARGEPUMP          = 0x8D;
  public final static int SSD1306_EXTERNALVCC         = 0x1;
  public final static int SSD1306_SWITCHCAPVCC        = 0x2;

  // Scrolling constants
  public final static int SSD1306_ACTIVATE_SCROLL                      = 0x2F;
  public final static int SSD1306_DEACTIVATE_SCROLL                    = 0x2E;
  public final static int SSD1306_SET_VERTICAL_SCROLL_AREA             = 0xA3;
  public final static int SSD1306_RIGHT_HORIZONTAL_SCROLL              = 0x26;
  public final static int SSD1306_LEFT_HORIZONTAL_SCROLL               = 0x27;
  public final static int SSD1306_VERTICAL_AND_RIGHT_HORIZONTAL_SCROLL = 0x29;
  public final static int SSD1306_VERTICAL_AND_LEFT_HORIZONTAL_SCROLL  = 0x2A;

  //private final static int SPI_PORT   =  0;
  private final static int SPI_DEVICE = Spi.CHANNEL_0; // 0

  private int width  = 128, 
              height =  32;
  private int clockHertz = 8000000; // 8 MHz
  private int vccstate = 0;
  private int pages = 0;
  private int[] buffer = null;

  //private SpiDevice spiDevice = null; // Only available in PI4J Jan-2015

  // SPI: Serial Peripheral Interface. Default pin values.
  private static Pin spiClk  = RaspiPin.GPIO_14; // Pin #23, SCLK, GPIO_11
  private static Pin spiMosi = RaspiPin.GPIO_12; // Pin #19, SPI0_MOSI
  private static Pin spiCs   = RaspiPin.GPIO_10; // Pin #24, SPI0_CE0_N
  private static Pin spiRst  = RaspiPin.GPIO_05; // Pin #18, GPIO_24
  private static Pin spiDc   = RaspiPin.GPIO_04; // Pin #16, GPIO_23

  private static GpioController gpio;

  private static GpioPinDigitalOutput mosiOutput       = null;
  private static GpioPinDigitalOutput clockOutput      = null;
  private static GpioPinDigitalOutput chipSelectOutput = null;
  private static GpioPinDigitalOutput resetOutput      = null;
  private static GpioPinDigitalOutput dcOutput         = null;

  public SSD1306()
  {
    initSSD1306(this.width, this.height);
  }

  /**
   * @param w Buffer width (pixles).  Default is 128
   * @param h Buffer height (pixels). Default is  32
   */
  public SSD1306(int w, int h)
  {
    initSSD1306(w, h);
  }
  
  /**
   *              | function                     | Wiring/PI4J    |Cobbler | Name      |GPIO 
   * -------------+------------------------------+----------------+--------=-----------+----
   * @param clock | Clock Pin.        Default is |RaspiPin.GPIO_14|Pin #23 |SPI0_SCLK  | 11    Clock
   * @param mosi, | MOSI Pin.         Default is |RaspiPin.GPIO_12|Pin #19 |SPI0_MOSI  | 10    Master Out Slave In
   * @param cs,   | CS Pin.           Default is |RaspiPin.GPIO_10|Pin #24 |SPI0_CE0_N |  8    Chip Select
   * @param rst,  | RST Pin.          Default is |RaspiPin.GPIO_05|Pin #18 |GPIO_24    | 24    Reset
   * @param dc,   | DC Pin.           Default is |RaspiPin.GPIO_04|Pin #16 |GPIO_23    | 23    Data Control (?)
   * -------------+------------------------------+----------------+--------=-----------+----
   */
  public SSD1306(Pin clock, Pin mosi, Pin cs, Pin rst, Pin dc)
  {
    spiClk = clock;
    spiMosi = mosi;
    spiCs = cs;
    spiRst = rst;
    spiDc = dc;
    initSSD1306(this.width, this.height);
  }

  /**
   *              | function                     | Wiring/PI4J    |Cobbler | Name      |GPIO 
   * -------------+------------------------------+----------------+--------=-----------+----
   * @param clock | Clock Pin.        Default is |RaspiPin.GPIO_14|Pin #23 |SPI0_SCLK  | 11    Clock
   * @param mosi, | MOSI Pin.         Default is |RaspiPin.GPIO_12|Pin #19 |SPI0_MOSI  | 10    Master Out Slave In
   * @param cs,   | CS Pin.           Default is |RaspiPin.GPIO_10|Pin #24 |SPI0_CE0_N |  8    Chip Select
   * @param rst,  | RST Pin.          Default is |RaspiPin.GPIO_05|Pin #18 |GPIO_24    | 24    Reset
   * @param dc,   | DC Pin.           Default is |RaspiPin.GPIO_04|Pin #16 |GPIO_23    | 23    Data Control (?)
   * -------------+------------------------------+----------------+--------=-----------+----
   * @param w Buffer width (pixels).  Default is 128
   * @param h Buffer height (pixels). Default is 32
   */
  public SSD1306(Pin clock, Pin mosi, Pin cs, Pin rst, Pin dc, int w, int h)
  {
    spiClk = clock;
    spiMosi = mosi;
    spiCs = cs;
    spiRst = rst;
    spiDc = dc;
    initSSD1306(w, h);
  }

  private void initSSD1306(int w, int h)
  {
    this.width = w;
    this.height = h;
    this.pages = this.height / 8; // Number of lines
    this.buffer = new int[this.width * this.pages];
    clear();

    int fd = Spi.wiringPiSPISetup(SPI_DEVICE, clockHertz);
    if (fd < 0)
    {
      System.err.println("SPI Setup failed");
      System.exit(1);
    }

    gpio = GpioFactory.getInstance();

    mosiOutput       = gpio.provisionDigitalOutputPin(spiMosi, "MOSI", PinState.LOW);
    clockOutput      = gpio.provisionDigitalOutputPin(spiClk,  "CLK",  PinState.LOW);
    chipSelectOutput = gpio.provisionDigitalOutputPin(spiCs,   "CS",   PinState.HIGH);
    resetOutput      = gpio.provisionDigitalOutputPin(spiRst,  "RST",  PinState.LOW);
    dcOutput         = gpio.provisionDigitalOutputPin(spiDc,   "DC",   PinState.LOW);
  }

  public void shutdown()
  {
    gpio.shutdown();
  }

  public void setBuffer(int[] buffer)
  {
    this.buffer = buffer;
  }

  public int[] getBuffer()
  {
    return buffer;
  }

  /**
   * Use if the screen is to be seen in a mirror.
   * Left and right are inverted.
   *
   * @param buff the screen buffer to invert
   * @param w width (in pixels) of the above
   * @param h height (in pixels) of the above. One row has 8 pixels.
   * @return the mirrored buffer.
   */
  public static int[] mirror(int[] buff, int w, int h) {
    int len = buff.length;
    if (len != w * (h / 8)) {
      throw new RuntimeException(String.format("Invalid buffer length %d, should be %d (%d * %d)", len, (w * (h / 8)), w, h));
    }
    int[] mirror = new int[len];
    for (int row=0; row<(h / 8); row++) {
      for (int col=0; col<w; col++) {
        int buffIdx = (row * w) + col;
        int mirrorBuffIdx = (row * w) + (w - col - 1);
        mirror[mirrorBuffIdx] = buff[buffIdx];
      }
    }
    return mirror;
  }

  /**
   * Half-duplex SPI write.  If assert_ss is True, the SS line will be
   * asserted low, the specified bytes will be clocked out the MOSI line, and
   * if deassert_ss is True the SS line be put back high.
   */
  private void write(int[] data)
  {
    write(data, true, true);
  }

  private final int MASK = 0x80; // MSBFIRST, 0x80 = 0&10000000
//private final int MASK = 0x01; // LSBFIRST

  private void write(int[] data, boolean assert_ss, boolean deassert_ss)
  {
    // Fail if MOSI is not specified.
    if (mosiOutput == null)
      throw new RuntimeException("Write attempted with no MOSI pin specified.");
    if (assert_ss && chipSelectOutput != null)
      chipSelectOutput.low();
    for (int i = 0; i < data.length; i++)
    {
      byte b = (byte) data[i];
      for (int j = 0; j < 8; j++)
      {
        byte bit = (byte) ((b << j) & MASK);
        // Write bit to MOSI.
        if (bit != 0)
          mosiOutput.high();
        else
          mosiOutput.low();
        // Flip clock off base. // TODO Check the value of the base (LOW Here)
        clockOutput.high();
        // Return clock to base.
        clockOutput.low();
      }
    }
    if (deassert_ss && chipSelectOutput != null)
      chipSelectOutput.high();
  }

  private void command(int c)
  {
    dcOutput.low();
//  try { spiDevice.write((byte)c); }
//  catch (IOException ioe) { ioe.printStackTrace(); }
    this.write(new int[] { c });
  }

  private void reset()
  {
    resetOutput.high();
    delay(1);
    // Set reset low for 10 milliseconds.
    resetOutput.low();
    delay(10);
    // Set reset high again.
    resetOutput.high();
  }

  public void data(int c)
  {
    // SPI write.
    dcOutput.high();
//  try { spiDevice.write((byte)c); }
//  catch (IOException ioe) { ioe.printStackTrace(); }
    this.write(new int[] { c });
  }

  /**
   * Initialize display
   */
  public void begin()
  {
    begin(SSD1306_SWITCHCAPVCC);
  }

  public void begin(int vcc)
  {
    // Save vcc state.
    this.vccstate = vcc;
    // Reset and initialize display.
    this.reset();
    this.initialize();
    // Turn on the display.
    this.command(SSD1306_DISPLAYON);
  }

  private void initialize() // SPI, 128x32
  {
    // 128x32 pixel specific initialization.
    this.command(SSD1306_DISPLAYOFF);          // 0xAE
    this.command(SSD1306_SETDISPLAYCLOCKDIV);  // 0xD5
    this.command(0x80);                        // the suggested ratio 0x80
    this.command(SSD1306_SETMULTIPLEX);        // 0xA8
    this.command(0x1F);
    this.command(SSD1306_SETDISPLAYOFFSET);    // 0xD3
    this.command(0x0);                         // no offset
    this.command(SSD1306_SETSTARTLINE | 0x0);  // line //0
    this.command(SSD1306_CHARGEPUMP);          // 0x8D
    if (this.vccstate == SSD1306_EXTERNALVCC)
      this.command(0x10);
    else
      this.command(0x14);
    this.command(SSD1306_MEMORYMODE);          // 0x20
    this.command(0x00);                        // 0x0 act like ks0108
    this.command(SSD1306_SEGREMAP | 0x1);
    this.command(SSD1306_COMSCANDEC);
    this.command(SSD1306_SETCOMPINS);          // 0xDA
    this.command(0x02);
    this.command(SSD1306_SETCONTRAST);         // 0x81
    this.command(0x8F);
    this.command(SSD1306_SETPRECHARGE);        // 0xd9
    if (this.vccstate == SSD1306_EXTERNALVCC)
      this.command(0x22);
    else
      this.command(0xF1);
    this.command(SSD1306_SETVCOMDETECT);       // 0xDB
    this.command(0x40);
    this.command(SSD1306_DISPLAYALLON_RESUME); // 0xA4
    this.command(SSD1306_NORMALDISPLAY);       // 0xA6
  }

  public void clear()
  {
    for (int i = 0; this.buffer != null && i < this.buffer.length; i++)
      this.buffer[i] = 0;
  }

  public void setContrast(int contrast)
    throws IllegalArgumentException
  {
    if (contrast < 0 || contrast > 255)
    {
      throw new IllegalArgumentException("Contrast must be a value in [0, 255]");
    }
    this.command(SSD1306_SETCONTRAST);
    this.command(contrast);
  }

  /**
   * Write display buffer to physical display.
   */
  public void display()
  {
    this.command(SSD1306_COLUMNADDR);
    this.command(0); // Column start address. (0 = reset)
    this.command(this.width - 1); // Column end address.
    this.command(SSD1306_PAGEADDR);
    this.command(0); // Page start address. (0 = reset)
    this.command(this.pages - 1); // Page end address.
    // Write buffer data.
    //   Set DC high for data.
    dcOutput.high();
    this.write(this.buffer);
  }

  /**
   * Adjusts contrast to dim the display if dim is True, otherwise sets the
   * contrast to normal brightness if dim is False.
   */
  public void dim(boolean dim) // ???? WTF ?????
  {
    // Assume dim display.
    int contrast = 0;
    // Adjust contrast based on VCC if not dimming.
    if (!dim)
    {
      if (this.vccstate == SSD1306_EXTERNALVCC)
        contrast = 0x9F;
      else
        contrast = 0xCF;
    }
  }

  private static void delay(long ms)
  {
    try
    {
      Thread.sleep(ms);
    }
    catch (Exception ex)
    {
      ex.printStackTrace();
    }
  }
}
