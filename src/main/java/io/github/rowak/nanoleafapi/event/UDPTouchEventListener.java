package io.github.rowak.nanoleafapi.event;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

public class UDPTouchEventListener implements Runnable {
	
	/* This number is a bit arbitrary, but it should be able to allow for
	 * about 1600 panels per packet */
	private final int MAX_PACKET_LEN = 8192;
	
	private byte[] data;
	private boolean listening;
	
	private List<NanoleafTouchEventListener> listeners;
	private DatagramSocket socket;
	
	public UDPTouchEventListener(int port) throws SocketException {
		listeners = new ArrayList<NanoleafTouchEventListener>();
		data = new byte[MAX_PACKET_LEN];
		socket = new DatagramSocket(port);
		listening = true;
	}
	
	public void addListener(NanoleafTouchEventListener listener) {
		listeners.add(listener);
	}
	
	public void close() {
		listening = false;
		socket.close();
	}
	
	@Override
	public void run() {
		DatagramPacket packet;
		while (listening) {
			packet = new DatagramPacket(data, data.length);
			try {
				socket.receive(packet);
				signalListeners(parsePacket(packet));
			}
			catch (IOException e) {}
		}
	}
	
	private DetailedTouchEvent[] parsePacket(DatagramPacket packet) {
		byte[] data = packet.getData();
		int b = 0;
		int numPanels = twoByteToOne(data, b);
		b+=2; // read 2 bytes (num panels)
		int panelId = -1;
		int type = -1;
		int strength = 0;
		int swipePanel = -1;
		DetailedTouchEvent[] events = new DetailedTouchEvent[numPanels];
		for (int i = 0; i < numPanels; i++) {
			panelId = twoByteToOne(data, b);
			b+=2; // read 2 bytes (panel id)
			type = (data[b] & 0x70)>>4; // the type is in the first three bits after the first bit
			strength = data[b] & 0x0F; // the strength is in the last four bits
			b++; // read 1 byte (type/strength)
			swipePanel = twoByteToOne(data, b);
			if (swipePanel == 0xFFFF) {
				swipePanel = -1;
			}
			b+=2; // read 2 bytes (swipe panel id)
			events[i] = new DetailedTouchEvent(panelId, type, strength, swipePanel);
		}
		return events;
	}
	
	// Merges two consecutive bytes into one
	private int twoByteToOne(byte[] bytes, int i) {
		return ((bytes[i] & 0xFF) << 8) | (bytes[i+1] & 0xFF);
	}
	
	private void signalListeners(DetailedTouchEvent[] events) {
		for (int i = 0; i < listeners.size(); i++) {
			if (listeners.get(i) != null) {
				listeners.get(i).onEvent(events);
			}
		}
	}
}
