package de.tudarmstadt.informatik.hostage.logging.formatter;

import android.app.IntentService;
import android.content.Intent;

import de.tudarmstadt.informatik.hostage.logging.Record;

/**
 * Created by Shreyas Srinivasa on 01.10.15.
 */
public class BroSignatureGenerator extends IntentService{

    public String protocol;
    public int port;
    public int port2;
    public String ip;
    public String protocol2;
    public Record crecord;



    public int protocol2Port(String protocol){
        if(protocol.contains("HTTP")){port=80;}
        else if(protocol.contains("MODBUS")){port=502;}
        else if(protocol.contains("TELNET")){port=23;}
        else if(protocol.contains("SMB")){port=80;}
        else if(protocol.contains("HTTPS")){port=443;}
        else if(protocol.contains("ECHO")){port=7;}
        else if(protocol.contains("FTP")){port=21;}
        else if(protocol.contains("MySQL")){port=3306;}
        else if(protocol.contains("S7COMM")){port=102;}
        else if(protocol.contains("SIP")){port=1025;}
        else if(protocol.contains("SMTP")){port=25;}
        else if(protocol.contains("SNMP")){port=161;}
        else if(protocol.contains("SSH")){port=22;}
        return port;
    }

    public Record getRecordInfo(Record record){

        String recordProtocol=record.getProtocol();
        String recordRemoteIp = record.getRemoteIP();






       return crecord;
    }




    public BroSignatureGenerator(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(Intent intent) {

    }
}
