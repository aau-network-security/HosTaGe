import paho.mqtt.client as mqtt
import time, threading, ssl, random

class MQTT(object):

    def  __init__(self,ip):
        self.ip = ip
        self.clientId = "my_mqtt_python_client"
        self.device_name = "My Python MQTT device"
        self.tenant = "<<tenant_ID>>"
        self.username = "<<username>>"
        self.password = "<<password>>"
        self.receivedMessages = []

    def on_message(client, userdata, message):
        print("Received operation " + str(message.payload))
        if (message.payload.startswith("510")):
            print("Simulating device restart...")
            self.publish("s/us", "501,c8y_Restart")
            print("...restarting...")
            time.sleep(1)
            self.publish("s/us", "503,c8y_Restart")
            print("...done...")

    # send temperature measurement
    def sendMeasurements(self):
        try:
            print("Sending temperature measurement...")
            self.publish("s/us", "211," + str(random.randint(10, 20)))
            thread = threading.Timer(7, self.sendMeasurements)
            thread.daemon=True
            thread.start()
            while True: time.sleep(100)
        except (KeyboardInterrupt, SystemExit):
            print("Received keyboard interrupt, quitting ...")

    # publish a message
    def publish(self,topic, message, waitForAck = False):
        mid = client.publish(topic, message, 2)[1]
        if (waitForAck):
            while mid not in self.receivedMessages:
                time.sleep(0.25)

    def on_publish(self,client, userdata, mid):
        self.receivedMessages.append(mid)

    def connect(self):
        global client 
        client = mqtt.Client(self.clientId)
        #client.username_pw_set(self.tenant + "/" + self.username, self.password)
        client.on_message = self.on_message
        client.on_publish = self.on_publish

        client.connect(self.ip)
        client.loop_start()
        self.publish("s/us", "100," + self.device_name + ",c8y_MQTTDevice", True)
        self.publish("s/us", "110,S123456789,MQTT test model,Rev0.1")
        self.publish("s/us", "114,c8y_Restart")
        print("Device registered successfully!")

        client.subscribe("s/ds")
        #self.sendMeasurements()