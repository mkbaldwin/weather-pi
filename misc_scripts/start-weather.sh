#! /bin/bash

/usr/bin/rtl_433 -M utc -F json -R 40 | /opt/weather/weatherpi-1.0-SNAPSHOT/bin/weatherpi -c /opt/weather/weatherpi.properties -b
