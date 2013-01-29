/**
 * Copyright 2011-2012 Universite Joseph Fourier, LIG, ADELE team
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package fr.imag.adele.apam.application.room.impl;

import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.application.room.Room;
import fr.liglab.adele.icasa.device.DeviceListener;
import fr.liglab.adele.icasa.device.light.BinaryLight;
import fr.liglab.adele.icasa.device.presence.PresenceSensor;

import java.util.List;


public class RoomImpl implements Room {

    private List<BinaryLight> lights;

    private PresenceSensor presenceSensor;

    private  MyPresenceListener presenceListener = new MyPresenceListener();

    private boolean presence =false;

    public void start(){
        System.out.println("Start OK!");
        if (presenceSensor!=null){
            System.out.println("Presence OK!");
            presenceSensor.addListener(presenceListener);
            presence = presenceSensor.getSensedPresence();
            setLightsStates(presence);
        }
    }


    public void bindPresence(Instance instance){
        System.out.println("New instance bind " + instance.getName());
        presenceSensor.removeListener(presenceListener);
        presenceSensor.addListener(presenceListener);
        presence = presenceSensor.getSensedPresence();
        setLightsStates(presence);
    }

    public void unBindPresence(Instance instance){
        System.out.println("Instance unbind " + instance.getName());

    }

    public void bindLight(Instance instance){
        System.out.println("New instance bind " + instance.getName());
        //setLightsStates(presence);
    }

    public void unBindLight(Instance instance){
        System.out.println("Instance unbind " + instance.getName());
    }

    public void stop(){
        System.out.println("Stop OK!");
        if (presenceSensor!=null){
            presenceSensor.removeListener(presenceListener);
        }
    }


    public void setLightsStates(boolean state){
        if (lights!=null){
            System.out.println("lights OK");
            for( BinaryLight light : lights  ){
                light.setPowerStatus(presence);
            }
        }
    }

    class MyPresenceListener implements DeviceListener{

        @Override
        public void notifyDeviceEvent(String s) {
            System.out.println("Presence sense " +  s);
            if (presenceSensor!=null){
                presence = presenceSensor.getSensedPresence();
                setLightsStates(presence);
            }
        }
    }



}
