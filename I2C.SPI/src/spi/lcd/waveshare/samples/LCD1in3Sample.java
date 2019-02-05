package spi.lcd.waveshare.samples;

import spi.lcd.waveshare.LCD1in3;
import utils.TimeUtil;

import static spi.lcd.waveshare.LCD1in3.IMAGE_COLOR_POSITIVE;
import static spi.lcd.waveshare.LCD1in3.IMAGE_RGB;
import static spi.lcd.waveshare.LCD1in3.IMAGE_ROTATE_0;
import static spi.lcd.waveshare.LCD1in3.LCD_HEIGHT;
import static spi.lcd.waveshare.LCD1in3.LCD_WIDTH;
import static spi.lcd.waveshare.LCD1in3.WHITE;
import static spi.lcd.waveshare.LCD1in3.BLACK;
import static spi.lcd.waveshare.LCD1in3.RED;
import static spi.lcd.waveshare.LCD1in3.GREEN;

public class LCD1in3Sample {

	public static void main(String... args) {
		LCD1in3 lcd = new LCD1in3(LCD1in3.HORIZONTAL, LCD1in3.BLUE);
		lcd.GUINewImage(IMAGE_RGB, LCD_WIDTH, LCD_HEIGHT, IMAGE_ROTATE_0, IMAGE_COLOR_POSITIVE);
		lcd.GUIClear(WHITE);

		TimeUtil.delay(500L);

		System.out.println("...Dots");

		/*2.Drawing on the image*/
		lcd.GUIDrawPoint(5, 10, RED, LCD1in3.DotPixel.DOT_PIXEL_1X1, LCD1in3.DOT_STYLE_DFT);
		lcd.GUIDrawPoint(5, 25, RED, LCD1in3.DotPixel.DOT_PIXEL_2X2, LCD1in3.DOT_STYLE_DFT);
		lcd.GUIDrawPoint(5, 40, BLACK, LCD1in3.DotPixel.DOT_PIXEL_3X3, LCD1in3.DOT_STYLE_DFT);
		lcd.GUIDrawPoint(5, 55, BLACK, LCD1in3.DotPixel.DOT_PIXEL_4X4, LCD1in3.DOT_STYLE_DFT);
		lcd.GUIDrawPoint(5, 70, GREEN, LCD1in3.DotPixel.DOT_PIXEL_5X5, LCD1in3.DOT_STYLE_DFT);
		lcd.GUIDrawPoint(5, 85, GREEN, LCD1in3.DotPixel.DOT_PIXEL_6X6, LCD1in3.DOT_STYLE_DFT);

		System.out.println("Displaying...");
		lcd.LCDDisplay();

		TimeUtil.delay(5_000L);
		lcd.LCDClear(LCD1in3.BLACK);
		lcd.shutdown();
	}

}
