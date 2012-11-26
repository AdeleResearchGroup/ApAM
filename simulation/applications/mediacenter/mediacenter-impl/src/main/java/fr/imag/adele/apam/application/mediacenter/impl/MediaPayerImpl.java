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
