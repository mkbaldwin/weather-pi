#! /bin/bash

sudo cp /opt/weather/weatherpi.service /etc/systemd/system/
sudo chmod 644 /etc/systemd/system/weatherpi.service

sudo systemctl daemon-reload

echo "To enable service run:"
echo "   sudo systemctl enable weatherpi"
echo "   sudo systemctl start weatherpi"
