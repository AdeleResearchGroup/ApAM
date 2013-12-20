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
package fr.imag.adele.apam.perfAPAM;

import java.util.Set;

public class ComponentTestImpl implements Service {

    public static int  instances = 0;
    public static long startTime;
    private final int  miliers;

    private Service    instance1;
    private Service    instance2;

    private Service    testPerf;

    public ComponentTestImpl() {
        ComponentTestImpl.instances++;
        miliers = ComponentTestImpl.instances / 1000;
        if (miliers * 1000 == ComponentTestImpl.instances) {
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
        long nanoTimeStart;
        if (testPerf == null) {
            System.out.println("ya un pb, testPerf is null");
        }
        for (int i = 1; i < 100; i++) {
            // System.out.println("\n\n" + i * 1000 + " loop");
            nanoTimeStart = System.nanoTime();
            for (int j = 1; j < i * 1000; j++) {

                testPerf.callPerf(1);
            }
            long during = (System.nanoTime() - nanoTimeStart) / 1000;
            System.out.println(i * 1000 + "  calls loop. Invocation time (�S): " + during);
        }
    }

}
