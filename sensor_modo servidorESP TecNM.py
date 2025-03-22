import network
import socket
import time
from machine import Pin
from onewire import OneWire
from ds18x20 import DS18X20

# Configuración de Wi-Fi
SSID = "POCO_F5"  # Cambia por tu SSID
# PASSWORD = ""  # Cambia por tu contraseña

# Configuración del servidor (aplicación Android)
SERVER_IP = "172.16.16.1"  # Reemplaza con la IP de tu teléfono   172.16.24.146
SERVER_PORT = 8080            # Asegúrate de usar el mismo puerto configurado en la aplicación

# Configuración del sensor DS18B20
ow_pin = Pin(4)  # Cambia al pin conectado al DS18B205555
ow = OneWire(ow_pin)
ds = DS18X20(ow)
roms = ds.scan()

if not roms:
    raise RuntimeError("No se encontraron sensores DS18B20.")

# Convertir las ROMs a cadenas para hacerlas utilizables como claves
roms_str = [rom.hex() for rom in roms]
print("Sensores encontrados:", roms_str)

# Asignar nombres a los sensores detectados
sensor_nombres = {
    "28990243d4e13da7": "Sensor 1",  # Reemplaza estas claves con las ROMs convertidas
    "289053b224230b5c": "Sensor 2"
}

# Conectar a Wi-Fi
def connect_wifi(ssid):
    wlan = network.WLAN(network.STA_IF)
    wlan.active(True)
    wlan.connect(ssid)

    for _ in range(10):  # Intentar 10 veces
        if wlan.isconnected():
            print("Conectado a Wi-Fi:", wlan.ifconfig())
            return
        print("Intentando conectar a Wi-Fi...")
        time.sleep(1)

    raise RuntimeError("No se pudo conectar a Wi-Fi.")

connect_wifi(SSID)

# Crear conexión TCP con el servidor
def connect_to_server(ip, port):
    client = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    client.connect((ip, port))
    print(f"Conectado al servidor en {ip}:{port}")
    return client

client = connect_to_server(SERVER_IP, SERVER_PORT)

# Enviar lecturas al servidor
try:
    while True:
        ds.convert_temp()
        time.sleep(1)  # Esperar a que las lecturas de temperatura estén listas
        for rom in roms:
            rom_str = rom.hex()  # Convertir la ROM a cadena
            temp = ds.read_temp(rom)
            # Asignar nombre al sensor o "Sensor desconocido" si no está en el diccionario
            nombre = sensor_nombres.get(rom_str, "Sensor desconocido")
            data = f"{nombre}: {temp:.2f} °C"
            client.send(data.encode())  # Enviar datos al servidor
            print("Enviado:", data)
        time.sleep(5)  # Enviar cada 5 segundos
except Exception as e:
    print("Error:", e)
finally:
    client.close()
    print("Conexión cerrada.")
