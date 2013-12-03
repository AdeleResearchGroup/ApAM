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
package fr.imag.adele.apam.pax.test.implS1;

import java.util.Set;

import org.osgi.framework.BundleContext;

import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.pax.test.iface.S1;
import fr.imag.adele.apam.pax.test.iface.S2;
import fr.imag.adele.apam.pax.test.iface.S3;
import fr.imag.adele.apam.pax.test.iface.device.Eletronic;

public class S1Impl implements S1 {
    Boolean isOnInitCallbackCalled = false;
    Boolean isOnRemoveCallbackCalled = false;
    Boolean isBindUnbindReceivedInstanceParameter = false;

    String stateInternal;
    String stateNotInternal;

    Eletronic simpleDevice110v;

    S2 s2;
    S3 s3;

    Set<Eletronic> eletronicInstancesInSet;

    Eletronic[] eletronicInstancesInArray;

    Set<Eletronic> eletronicInstancesConstraintsInstance;

    Eletronic devicePreference110v;

    Eletronic deviceConstraint110v;

    BundleContext context;

    public S1Impl(BundleContext context) {
	this.context = context;
    }

    public void bindWithInstance(Instance instance) {
	System.out.println("Starting:" + this.getClass().getName());

	if (instance != null) {
	    isBindUnbindReceivedInstanceParameter = true;
	}

	isOnInitCallbackCalled = true;
    }

    public void bindWithoutInstance() {
	isOnInitCallbackCalled = true;
    }

    public BundleContext getContext() {
	return context;
    }

    public Eletronic getDeviceConstraint110v() {
	return deviceConstraint110v;
    }

    public Eletronic getDevicePreference110v() {
	return devicePreference110v;
    }

    public Set<Eletronic> getEletronicInstancesConstraintsInstance() {
	return eletronicInstancesConstraintsInstance;
    }

    public Eletronic[] getEletronicInstancesInArray() {
	return eletronicInstancesInArray;
    }

    public Set<Eletronic> getEletronicInstancesInSet() {
	return eletronicInstancesInSet;
    }

    public Boolean getIsBindUnbindReceivedInstanceParameter() {
	return isBindUnbindReceivedInstanceParameter;
    }

    public Boolean getIsOnInitCallbackCalled() {
	return isOnInitCallbackCalled;
    }

    public Boolean getIsOnRemoveCallbackCalled() {
	return isOnRemoveCallbackCalled;
    }

    public S2 getS2() {
	return s2;
    }

    public S3 getS3() {
	return s3;
    }

    public Eletronic getSimpleDevice110v() {
	return simpleDevice110v;
    }

    public String getStateInternal() {
	return stateInternal;
    }

    public String getStateNotInternal() {
	return stateNotInternal;
    }

    public void setContext(BundleContext context) {
	this.context = context;
    }

    public void setDeviceConstraint110v(Eletronic deviceConstraint110v) {
	this.deviceConstraint110v = deviceConstraint110v;
    }

    public void setDevicePreference110v(Eletronic devicePreference110v) {
	this.devicePreference110v = devicePreference110v;
    }

    public void setEletronicInstancesConstraintsInstance(
	    Set<Eletronic> eletronicInstancesConstraintsInstance) {
	this.eletronicInstancesConstraintsInstance = eletronicInstancesConstraintsInstance;
    }

    public void setEletronicInstancesInArray(
	    Eletronic[] eletronicInstancesInArray) {
	if (eletronicInstancesInArray != null) {
	    this.eletronicInstancesInArray = new Eletronic[eletronicInstancesInArray.length];
	    System.arraycopy(eletronicInstancesInArray, 0,
		    this.eletronicInstancesInArray, 0,
		    eletronicInstancesInArray.length);
	}

	// this.eletronicInstancesInArray = eletronicInstancesInArray;
    }

    public void setEletronicInstancesInSet(
	    Set<Eletronic> eletronicInstancesInSet) {
	this.eletronicInstancesInSet = eletronicInstancesInSet;
    }

    public void setIsBindUnbindReceivedInstanceParameter(
	    Boolean isBindUnbindReceivedInstanceParameter) {
	this.isBindUnbindReceivedInstanceParameter = isBindUnbindReceivedInstanceParameter;
    }

    public void setIsOnInitCallbackCalled(Boolean isOnInitCallbackCalled) {
	this.isOnInitCallbackCalled = isOnInitCallbackCalled;
    }

    public void setIsOnRemoveCallbackCalled(Boolean isOnRemoveCallbackCalled) {
	this.isOnRemoveCallbackCalled = isOnRemoveCallbackCalled;
    }

    public void setS2(S2 s2) {
	this.s2 = s2;
    }

    public void setS3(S3 s3) {
	this.s3 = s3;
    }

    public void setSimpleDevice110v(Eletronic simpleDevice110v) {
	this.simpleDevice110v = simpleDevice110v;
    }

    public void setStateInternal(String stateInternal) {
	this.stateInternal = stateInternal;
    }

    public void setStateNotInternal(String stateNotInternal) {
	this.stateNotInternal = stateNotInternal;
    }

    public void start() {
	System.out.println("Starting:" + this.getClass().getName());
	isOnInitCallbackCalled = true;
    }

    public void stop() {
	System.out.println("Stopping:" + this.getClass().getName());
	isOnRemoveCallbackCalled = true;
    }

    public void unbindWithInstance(Instance instance) {
	System.out.println("Stopping:" + this.getClass().getName());

	if (instance != null) {
	    isBindUnbindReceivedInstanceParameter = true;
	}

	isOnRemoveCallbackCalled = true;
    }

    public void unbindWithoutInstance() {
	isOnRemoveCallbackCalled = true;
    }

    @Override
    public String whoami() {
	return this.getClass().getName();
    }

}
