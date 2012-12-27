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
package fr.imag.adele.apam.application.mediacenter.impl;

import fr.imag.adele.apam.application.mediacenter.MediaPlayer;
import fr.imag.adele.apam.device.speaker.Speaker;
import fr.imag.adele.apam.device.tvscreen.TvScreen;



public class MediaPayerImpl implements MediaPlayer {

	private Speaker speaker;

	private TvScreen tvScreen;

	@Override
	public void startAllDevices() {
		if (speaker != null) {
			speaker.start();
		}
		
		if (tvScreen != null) {
			tvScreen.start();
		}
	}

	@Override
	public void stopAllDevices() {
		if (speaker != null) {
			speaker.stop();
		}
		
		if (tvScreen != null) {
			tvScreen.stop();
		}
		
	}

	@Override
	public void startTv() {
		if (tvScreen!=null) {
			tvScreen.start();
		}
	}

	@Override
	public void stopTv() {
		if (tvScreen!=null) {
			tvScreen.start();
		}
	}

	@Override
	public void startSpeaker() {
		if (speaker!=null) {
			speaker.stop();
		}
		
	}

	@Override
	public void stopSpeaker() {
		if (speaker!=null) {
			speaker.start();
		}
		
	}


}
