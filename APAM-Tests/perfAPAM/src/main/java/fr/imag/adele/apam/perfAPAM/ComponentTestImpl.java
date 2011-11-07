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
package fr.imag.adele.apam.perfAPAM;

import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.InstanceManager;

public class ComponentTestImpl implements Service {

    public static int          instances = 0;
    public static Set<Integer> checkpoints;
    public static long         startTime;
    private final int          miliers;

    private Service            instance1;
    private Service            instance2;

    private Service            testPerf;

    public ComponentTestImpl() {
        ComponentTestImpl.instances++;
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

    @Override
    public void call(int level) {
        if (level == 0)
            return;
        instance1.call(level - 1);
        instance2.call(level - 1);
    }

    @Override
    public void callPerf(int i) {
    }

    @Override
    public void callTestPerf() {
        int k = 0;
        long nanoTimeStart;
        if (testPerf == null) {
            System.out.println("ya un pb, testPerf is null");
        }
        for (int i = 1; i < 10; i++) {
            nanoTimeStart = System.nanoTime();
            for (int j = 1; j < 10000; j++) {
                k = 1;
            }
            long during = (System.nanoTime() - nanoTimeStart) / 1000;
            System.out.println("loop alone 10 000. Invocation time (µS): " + during);
        }
        for (int i = 1; i < 10; i++) {
            nanoTimeStart = System.nanoTime();
            for (int j = 1; j < 10000; j++) {
                k = 1;
                testPerf.callPerf(1);
            }
            long during = (System.nanoTime() - nanoTimeStart) / 1000;
            System.out.println("10 000 calls loop. Invocation time (µS): " + during);
        }

    }

}
/*    
    public void start() {
        if (currentLevel == null) {
            throw new NullPointerException("currentLevel is null");
        }
        int i = Integer.valueOf(currentLevel).intValue();
        if (i <= ComponentTestImpl.limit) {
            try {
                i++;
                System.out.println("current level = " + currentLevel);
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

    @Override
    public void call() {
        if (notEnd) {
            instance1.call();
            instance2.call();
        }

    }
}
*/
