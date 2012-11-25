package fr.imag.adele.apam.application.kitchen.impl;

import fr.imag.adele.apam.ApamComponent;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.application.kitchen.KitchenApp;
import fr.imag.adele.apam.application.kitchen.KitchenMessage;
import fr.imag.adele.apam.device.microwave.Microwave;
import fr.imag.adele.apam.device.oven.Oven;
import fr.liglab.adele.icasa.device.DeviceListener;
import fr.liglab.adele.icasa.device.light.BinaryLight;
import fr.liglab.adele.icasa.device.presence.PresenceSensor;

import java.util.List;


public class KitchenAppImpl implements KitchenApp {

    private List<BinaryLight> lights;

    private PresenceSensor presenceSensor;

    private  MyPresenceListener presenceListener;

    private boolean presence =false;

    public void start(){
        if (presenceSensor!=null){
            presenceListener = new MyPresenceListener();
            presenceSensor.addListener(presenceListener);
            presence = presenceSensor.getSensedPresence();
            setLightsStates(presence);
        }
    }

    public void stop(){
        if (presenceSensor!=null){

            presenceSensor.removeListener(presenceListener);

        }
    }


    public void setLightsStates(boolean state){
        if (lights!=null){
            for( BinaryLight light : lights  ){
                light.setPowerStatus(presence);
            }
        }
    }

    class MyPresenceListener implements DeviceListener{

        @Override
        public void notifyDeviceEvent(String s) {
            presence = presenceSensor.getSensedPresence();
            setLightsStates(presence);
        }
    }



}
