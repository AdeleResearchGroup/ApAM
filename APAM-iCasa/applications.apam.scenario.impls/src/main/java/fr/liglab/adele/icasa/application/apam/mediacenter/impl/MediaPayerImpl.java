package fr.liglab.adele.icasa.application.apam.mediacenter.impl;

import fr.liglab.adele.icasa.device.GenericDevice;
import fr.liglab.adele.icasa.devices.apam.Speaker;
import fr.liglab.adele.icasa.devices.apam.TvScreen;


public class MediaPayerImpl implements MediaPlayer {

	private Speaker speaker;

	private TvScreen tvScreen;

	@Override
	public void startAllDevices() {
		if (speaker != null) {
			speaker.setState(GenericDevice.STATE_ACTIVATED);
		}
		
		if (tvScreen != null) {
			tvScreen.setState(GenericDevice.STATE_ACTIVATED);
		}
	}

	@Override
	public void stopAllDevices() {
		if (speaker != null) {
			speaker.setState(GenericDevice.STATE_DEACTIVATED);
		}
		
		if (tvScreen != null) {
			tvScreen.setState(GenericDevice.STATE_DEACTIVATED);
		}
		
	}

	@Override
	public void startTv() {
		if (tvScreen!=null) {
			tvScreen.setState(TvScreen.STATE_ACTIVATED);
		}
	}

	@Override
	public void stopTv() {
		if (tvScreen!=null) {
			tvScreen.setState(TvScreen.STATE_DEACTIVATED);
		}
	}

	@Override
	public void startSpeaker() {
		if (speaker!=null) {
			speaker.setState(TvScreen.STATE_ACTIVATED);
		}
		
	}

	@Override
	public void stopSpeaker() {
		if (speaker!=null) {
			speaker.setState(TvScreen.STATE_DEACTIVATED);
		}
		
	}


}
