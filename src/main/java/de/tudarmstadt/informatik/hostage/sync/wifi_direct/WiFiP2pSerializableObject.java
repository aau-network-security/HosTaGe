package de.tudarmstadt.informatik.hostage.sync.wifi_direct;

import java.io.Serializable;

/**
 * Created by Julien on 07.01.2015.
 *
 * An instance of this class is send from client to host and another one will be send back as response.
 * You can create subclass for any variation. Any information will be transferred.
 *
 */
public class WiFiP2pSerializableObject implements Serializable {

    private String requestIdentifier;
    private String actingDevice_IP_address;


    public Serializable getObjectToSend() {
        return objectToSend;
    }

    public void setObjectToSend(Serializable objectToSend) {
        this.objectToSend = objectToSend;
    }

    private Serializable objectToSend;

    public void setRequestIdentifier(String m){
        this.requestIdentifier = m;
    }
    public String getRequestIdentifier(){
        return this.requestIdentifier;
    }

    public String getActingDevice_IP_address() {
        return actingDevice_IP_address;
    }

    public void setActingDevice_IP_address(String actingDevice_IP_address) {
        this.actingDevice_IP_address = actingDevice_IP_address;
    }
}
