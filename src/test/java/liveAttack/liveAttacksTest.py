import requests
import time
import amqpclient
import mqttclient
import coapclient
import smbclient

ip = '192.168.1.20'
#HTTP,ECHO,FTP,MYSQL,MODBUS,S7COMM,SNMP,SIP,SSH,SMPT,TELNET
ports = [80,7,21,3306,502,102,161,5060,22,25,23]
headers = { 'User-Agent': 'Mozilla/5.0 (Windows NT 6.0; WOW64; rv:24.0) Gecko/20100101 Firefox/24.0' }
def sendPackets():
    for i, val in enumerate(ports):
        time.sleep(30)
        try:
            requests.get("http://"+ip+":"+str(ports[i]),data = headers)
        except requests.ConnectionError as e:
            print(e)
#HTTPS
def sendPacketsHTTPS():
    requests.get('https://'+ip+":443", verify=False)

def sendPacketsAMQP():
    amqp = amqpclient.AMQP(ip)
    amqp.start()

def sendPacketsMQTT():
    mqtt = mqttclient.MQTT(ip)
    mqtt.connect()

def sendPacketsCOAP():
    coap = coapclient.COAP(ip)
    coap.connect()

def sendPacketsSMB():
    smb = smbclient.SMB(ip)
    smb.connect()

def startAttack():
    sendPacketsAMQP()
    sendPacketsCOAP()
    sendPacketsMQTT()
    sendPacketsSMB()
    sendPacketsHTTPS()
    sendPackets()

startAttack()









