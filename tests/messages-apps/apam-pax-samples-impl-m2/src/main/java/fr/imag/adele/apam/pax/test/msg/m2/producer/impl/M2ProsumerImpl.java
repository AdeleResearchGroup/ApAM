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
package fr.imag.adele.apam.pax.test.msg.m2.producer.impl;

import fr.imag.adele.apam.pax.test.msg.M1;
import fr.imag.adele.apam.pax.test.msg.M2;
import fr.imag.adele.apam.pax.test.msg.M4;
import fr.imag.adele.apam.pax.test.msg.M5;
import fr.imag.adele.apam.pax.test.msg.device.EletronicMsg;
import fr.imag.adele.apam.pax.test.msg.device.HouseMeterMsg;

import java.util.Queue;

public class M2ProsumerImpl{

    Queue<EletronicMsg> deadMansSwitch;

    Queue<M4> s4;
    Queue<M5> s5;

    Queue<HouseMeterMsg> houseMeter;

    Queue<M1> inner;

    public M2 produceM2(String msg){
        return new M2(msg);
    }

    public String whoami()
    {
        return this.getClass().getName();
    }

    public void start(){
        System.out.println("Starting:"+this.getClass().getName());
    }

    public void stop(){
        System.out.println("Stopping:"+this.getClass().getName());
    }

    public HouseMeterMsg getHouseMeter() {
        return houseMeter.poll();
    }

    public EletronicMsg getDeadMansSwitch() {
        return deadMansSwitch.poll();
    }

}
