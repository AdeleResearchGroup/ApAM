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

//import fr.imag.adele.apam.CompositeTypeImpl;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.Apam;
import fr.imag.adele.apam.ApamComponent;
import fr.imag.adele.apam.CompositeType;
//import fr.imag.adele.apam.apamAPI.ApamComponent;
import fr.imag.adele.apam.perfAPAM.ComponentTestImpl;
import fr.imag.adele.apam.perfAPAM.Service;

public class Injector implements Runnable, ApamComponent {

    private Service             testPerf;

    Apam                        apam;
    private static final String targetedNbInstance = "nbInstance";
    int                         limit;

    @Override
    public void apamStart(Instance inst) {
        new Thread(this, "APAM perf test").start();
    }

    @Override
    public void run() {
        // public void apamStart() {
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
                limit = Integer.valueOf(targeted).intValue();

                createTreeInstance();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void createTreeInstance() {

//        CompositeType testPerf = apam.createCompositeType("TestPerfApam", "TestApam",
//                null /* models */, null /* properties */);
//        ASMInst test = testPerf.createInst(null /* composite */, null/* properties */);
//        Service s = (Service) test.getServiceObject();
//        s.callTestPerf();

        testPerf.callTestPerf();

//        int k = 0;
//        
//        if (testPerf == null) {
//            System.out.println("ya un pb, testPerf is null");
//        }
//        for (int i = 1; i < 10; i++) {
//            nanoTimeStart = System.nanoTime();
//            for (int j = 1; j < 10000; j++) {
//                k = 1;
//            }
//            long during = (System.nanoTime() - nanoTimeStart) / 1000;
//            System.out.println("loop alone 10 000. Invocation time (µS): " + during);
//        }
//        for (int i = 1; i < 10; i++) {
//            nanoTimeStart = System.nanoTime();
//            for (int j = 1; j < 10000; j++) {
//                k = 1;
//                s.callPerf(1);
//            }
//            long during = (System.nanoTime() - nanoTimeStart) / 1000;
//            System.out.println("10 000 calls loop. Invocation time (µS): " + during);
//        }
        long nanoTimeStart;
        ComponentTestImpl.startTime = System.nanoTime();
        for (int i = 1; i < 10; i++) {
            nanoTimeStart = System.nanoTime();
            testPerf.call(limit);
            long during = (System.nanoTime() - nanoTimeStart) / 1000;
            System.out.println("Instances " + ComponentTestImpl.instances
                        + " / invocation time (µS): " + during);
        }
    }

    private int calculateI(double x) {
        double ret = 0;
        ret = Math.log10((x + 1) * 0.5) / Math.log10(2);

        return Double.valueOf(ret).intValue();
    }

    @Override
    public void apamRelease() {
        // TODO Auto-generated method stub

    }

    @Override
    public void apamStop() {
        // TODO Auto-generated method stub

    }
}
