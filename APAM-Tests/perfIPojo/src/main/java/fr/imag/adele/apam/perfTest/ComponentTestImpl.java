/**
 * Copyright Universite Joseph Fourier (www.ujf-grenoble.fr)
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fr.imag.adele.apam.perfTest;

import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.InstanceManager;

public class ComponentTestImpl implements Service {

    public static final String COMPONENTTESTIMPLFACTORYNAME = "component.test.impl.ipojo";
    public static final String CURRENTLEVELPROPERTY         = "currentLevel";

    public static int          limit;
    public static int          instances;
    public static Set<Integer> checkpoints;
    public static long         startTime;
    private boolean            notEnd                       = true;
    private String             currentLevel;

    public static Factory      factory;

    private ComponentTestImpl  instance1;
    private ComponentTestImpl  instance2;
    private final int          miliers;

    public ComponentTestImpl() {
        ComponentTestImpl.instances++;
//        System.out.println("called; Instances : " + ComponentTestImpl.instances);
        miliers = ComponentTestImpl.instances / 1000;
        if (miliers * 1000 == ComponentTestImpl.instances) {
//        if (ComponentTestImpl.checkpoints.contains(Integer.valueOf(ComponentTestImpl.instances))) {
            Runtime r = Runtime.getRuntime();
            r.gc();
            long usedMemory = r.totalMemory() - r.freeMemory();
            usedMemory = usedMemory / 1024;
            usedMemory = usedMemory / 1024;
            long duration = (System.nanoTime() - ComponentTestImpl.startTime) / 1000000;
            System.out.println("NbInstances " + ComponentTestImpl.instances +
                    " / usedMemory: " + usedMemory + "; duration (mili sec): " + duration);
        }
    }

//    public ComponentTestImpl() {
//        Runtime r = Runtime.getRuntime();
//
//        ComponentTestImpl.instances = ComponentTestImpl.instances + 1;
//        if (ComponentTestImpl.checkpoints.contains(Integer.valueOf(ComponentTestImpl.instances))) {
//            r.gc();
//            double usedMemory = r.totalMemory() - r.freeMemory();
//            usedMemory = usedMemory / 1024;
//            usedMemory = usedMemory / 1024;
//            long duration = (System.nanoTime() - ComponentTestImpl.startTime) / 1000000;
//            System.out.println("NbInstances " + ComponentTestImpl.instances +
//                    " / usedMemory: " + usedMemory + "; duration (mili sec): " + duration);
//        }
//    }

    public void start() {
        if (currentLevel == null) {
            return;
            // throw new NullPointerException("currentLevel is null");
        }
        int i = Integer.valueOf(currentLevel).intValue();
        if (i <= ComponentTestImpl.limit) {
            try {
                i++;
                // System.out.println("current level = " + currentLevel);
                Properties props = new Properties();
                props.put(ComponentTestImpl.CURRENTLEVELPROPERTY, Integer.valueOf(i).toString());
                props.put("instance.name", UUID.randomUUID().toString());

                ComponentInstance instanceRef = ComponentTestImpl.factory.createComponentInstance(props);
                instanceRef.start();
                instance1 = (ComponentTestImpl) ((InstanceManager) instanceRef).getPojoObject();

                props.put("instance.name", UUID.randomUUID().toString());
                instanceRef = ComponentTestImpl.factory.createComponentInstance(props);
                instanceRef.start();
                instance2 = (ComponentTestImpl) ((InstanceManager) instanceRef).getPojoObject();

                props = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            notEnd = false;
        }
    }

    public void stop() {
        instance1 = null;
        instance2 = null;
    }

    // @Override
    public void call(int i) {
        if (notEnd) {
            instance1.call(i - 1);
            instance2.call(i - 1);
        }

    }

    public void callPerf(int i) {
    }
}
