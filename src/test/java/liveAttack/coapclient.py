from coapthon.client.helperclient import HelperClient

class COAP(object):
    def  __init__(self,ip):
        self.ip = ip

    def connect(self):
        client = HelperClient(server=(self.ip, 5683))
        response = client.get('other/block')
        client.stop()