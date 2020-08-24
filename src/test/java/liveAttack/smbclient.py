import uuid
import struct

from smbprotocol.connection import Connection, Dialects

class SMB(object):
    def  __init__(self,ip):
        self.ip = ip

    def connect(self):
        connection = Connection(uuid.uuid4(), self.ip, 1025)
        try:
            connection.connect(Dialects.SMB_3_0_2)
        except struct.error as err:
            print(err)