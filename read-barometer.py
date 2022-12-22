#! /usr/bin/python

import board
import digitalio
import adafruit_bmp280

spi = board.SPI()
cs = digitalio.DigitalInOut(board.D5)
sensor = adafruit_bmp280.Adafruit_BMP280_SPI(spi,cs)


temp = sensor.temperature
pressure = sensor.pressure

print("""{ "temperature_c":"%s", "pressure_hpa":"%s" }"""%(temp, pressure))