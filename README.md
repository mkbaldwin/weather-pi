# weather-pi

:warning: Warning: This code is a work in progress and has not been thoroughly 
tested. Use at your own risk!

## What is this?

Weather-pi is an experiment in collecting data from an Acurite 5-in-1 weather station.
The code is primarilarily written in Kotlin and submits the weather observations to
an InfluxDB database. Data is received using an USB software defined radio (SDR)
receiver using [rtl_433](https://github.com/merbanan/rtl_433). Currently it has only 
been tested with protocol 40, but should be able to be adapted to others if needed.

Additionally there is optional support for barometric pressure readings from a board
based on the Bosch BMP280 sensor. This script is written in Python.

## Parts List

Below is a list of all of the parts I used for testing and links to where I bought
them. Note these are NOT affiliate links. They are simply provided as a reference
to the parts I bought. 

  * [Raspberry Pi Zero 2 W](https://www.raspberrypi.com/products/raspberry-pi-zero-2-w/)
  * [USB RTL-SDR](https://www.amazon.com/gp/product/B009U7WZCA/ref=ppx_yo_dt_b_search_asin_title)
  * [Adafruit BMP280 I2C or SPI Barometric Pressure & Altitude Sensor](https://www.pishop.us/product/adafruit-bmp280-i2c-or-spi-barometric-pressure-altitude-sensor/)
  
You can easily use any other Raspberry Pi model you already have (or any computer
if you are not using the BMP200 sensor). The same should be true of the SDR as long
as it is compatible with the rtl_433 package.

## Known Issues

  * The Accurite weather station broadcasts each observation three times with a `sequence_num`
    of 0, 1, 2. This is presumably done as redundancy in case the first transmission is interrupted. 
    This code currently does NOT deduplicate the received data. So, you will get three entries in 
    InfluxDB for each observation. 
  * Yes, I know I committed my token for InfluxDB authentication, but it is just for a local
    instance on my network when the Pi is running so... 

## Barometric Pressure

The Accurite weather station's barometric pressure sensor is located in the base station and
not the external unit. Therefore the values are never broadcast to pick up. As a result I had 
to find an alternative way to get this information. My solution was a BMP280 board that I could
connect to the PI via SPI. A schematic of my solution is [here](barometer_connections_spi.png)

## Usage 

TODO: Add instructions

## License

[MIT](LICENSE)
