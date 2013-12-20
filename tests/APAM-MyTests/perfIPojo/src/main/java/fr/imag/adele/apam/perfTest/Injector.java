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
package fr.imag.adele.apam.perfTest;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.InstanceManager;

import fr.imag.adele.apam.perfTest.ComponentTestImpl;

public class Injector {

    private Service             testPerf;

    private static final String targetedNbInstance = "nbInstance";
    private ComponentTestImpl   instance;
    private ComponentInstance   instanceRef;
    private Factory             factory;
    private int                 limit;

//    private void start() {
//        new Thread(this, "APAM perf test").start();
//    }
//
//    public void stop() {
//
//    }

    public void start() {
        System.out.println("starting injector");
        Integer[] temp = new Integer[] { Integer.valueOf(10),
                Integer.valueOf(50), Integer.valueOf(100),
                Integer.valueOf(250), Integer.valueOf(500),
                Integer.valueOf(1000), Integer.valueOf(2000),
                Integer.valueOf(3000), Integer.valueOf(4000),
                Integer.valueOf(5000), Integer.valueOf(6000),
                Integer.valueOf(7000), Integer.valueOf(8000),
                Integer.valueOf(9000), Integer.valueOf(10000),
                Integer.valueOf(11000), Integer.valueOf(12000),
                Integer.valueOf(13000), Integer.valueOf(14000),
                Integer.valueOf(15000), Integer.valueOf(16000),
                Integer.valueOf(17000), Integer.valueOf(18000),
                Integer.valueOf(19000), Integer.valueOf(20000)
        };
        ComponentTestImpl.checkpoints = new HashSet(Arrays.asList(temp));

        JFileChooser fileChooser = new JFileChooser();
        FileFilter filter = new FileNameExtensionFilter("*.properties",
                new String[] { "properties" });
        fileChooser.setFileFilter(filter);
        int returnVal = fileChooser.showOpenDialog(null);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            Properties properties = new Properties();
            try {
                InputStream inputStream = (file.toURI()).toURL().openStream();
                properties.load(inputStream);
                inputStream.close();
                String targeted = (String) properties
                        .get(Injector.targetedNbInstance);
                System.out.println("nb instances = " + targeted);
                ComponentTestImpl.limit = Integer.valueOf(targeted).intValue();
                limit = ComponentTestImpl.limit;

                createTreeInstance();
                invoke();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void stop() {
        if (instance != null) {
            instance = null;
            if (instanceRef != null) {
                instanceRef.stop();
                instanceRef.dispose();
            }
        }
    }

    private void invoke() {
        if (instance != null) {
            long nanoTimeStart;
            for (int i = 1; i < 10; i++) {
                nanoTimeStart = System.nanoTime();
                instance.call(limit);
                long during = (System.nanoTime() - nanoTimeStart) / 1000;
                System.out.println("Instances " + ComponentTestImpl.instances
                            + " / invocation time (micro-s): " + during);

//                long during = System.nanoTime() - nanoTimeStart;
//                System.out.println("Limit:" + ComponentTestImpl.limit + " / invocation time (nS): " + during);
            }
//
//            long nanoTimeStart = System.nanoTime();
//            instance.call();
//            long nanoTimeEnd = System.nanoTime();
//            long during = nanoTimeEnd - nanoTimeStart;
//            // double limit = Math.pow(2.0, this.limitDouble + 1.0) - 1.0;
//            System.out.println("Limit:" + ComponentTestImpl.limit
//                    + " / invocation time (nS): " + during);
        }
    }

    private void createTreeInstance() {
        int k = 0;
        long nanoTimeStart;
        if (testPerf == null) {
            System.out.println("ya un pb, testPerf is null");
        }
        for (int i = 1; i < 10; i++) {
            nanoTimeStart = System.nanoTime();
            for (int j = 1; j < 10000; j++) {
                k = 1;
                testPerf.callPerf(1);
            }
            long during = (System.nanoTime() - nanoTimeStart) / 1000;
            System.out.println("10 000 calls loop. Invocation time (micro-S): " + during);
        }

        System.out.println("limit: " + ComponentTestImpl.limit);
        try {
            ComponentTestImpl.startTime = System.nanoTime();
            nanoTimeStart = System.nanoTime();
            ComponentTestImpl.factory = factory;
            ComponentTestImpl.instances = 0;

            Runtime r = Runtime.getRuntime();
            double usedMemory = r.totalMemory() - r.freeMemory();
            usedMemory = usedMemory / 1024;
            usedMemory = usedMemory / 1024;
            System.out.println("NbInstances " + ComponentTestImpl.instances
                    + " / usedMemory: " + usedMemory);

            Properties props = new Properties();
            props.put(ComponentTestImpl.CURRENTLEVELPROPERTY, Integer
                    .valueOf(0).toString());

            instanceRef = ComponentTestImpl.factory
                    .createComponentInstance(props);
            instanceRef.start();
            instance = (ComponentTestImpl) ((InstanceManager) instanceRef)
                    .getPojoObject();

            props = null;

            long nanoTimeEnd = System.nanoTime();
            long during = nanoTimeEnd - nanoTimeStart;

            System.out.println("Instances " + ComponentTestImpl.instances
                    + " / creation time (miliS): " + during / 1000000);

        } catch (Exception e) {
            System.out.println("Error in Injector: " + e);
            e.printStackTrace();
        }
    }

    private int calculateI(double x) {
        double ret = 0;
        ret = Math.log10((x + 1) * 0.5) / Math.log10(2);

        return Double.valueOf(ret).intValue();
    }
}
