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

        testPerf.callTestPerf();
        Runtime r = Runtime.getRuntime();
        r.gc();
        long usedMemory = r.totalMemory() - r.freeMemory();
        usedMemory = usedMemory / 1024;
        usedMemory = usedMemory / 1024;
        long duration = (System.nanoTime() - ComponentTestImpl.startTime) / 1000000;
        System.out.println("NbInstances " + ComponentTestImpl.instances +
                " / usedMemory: " + usedMemory + "; duration (mili sec): " + duration);

        testPerf.call(limit);
        r = Runtime.getRuntime();
        r.gc();
        usedMemory = r.totalMemory() - r.freeMemory();
        usedMemory = usedMemory / 1024;
        usedMemory = usedMemory / 1024;
        duration = (System.nanoTime() - ComponentTestImpl.startTime) / 1000000;
        System.out.println("Final NbInstances " + ComponentTestImpl.instances +
                " / usedMemory: " + usedMemory + "; duration (mili sec): " + duration);

        long nanoTimeStart;
        ComponentTestImpl.startTime = System.nanoTime();
        for (int i = 1; i < 10; i++) {
            nanoTimeStart = System.nanoTime();
            testPerf.call(limit);
            long during = (System.nanoTime() - nanoTimeStart) / 1000;
            System.out.println("Instances " + ComponentTestImpl.instances
                        + " / invocation time (�S): " + during);
        }
    }

    @Override
    public void apamStop() {
        // TODO Auto-generated method stub

    }

    @Override
    public void apamRelease() {
        // TODO Auto-generated method stub

    }

}
