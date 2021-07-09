package io.github.rowak.nanoleafapi;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;

/**
 * The Canvas class is one of the three main concrete classes for creating a NanoleafDevice.
 * This class should only be used to connect to physical Canvas devices. For other devices
 * such as the Aurora or Hexagons, use the Aurora and Shapes classes, respectively.
 */
public class Canvas extends NanoleafDevice {
	
	/**
	 * Creates a new instance of the Canvas controller.
	 * 
	 * @param hostname             the hostname of the controller
	 * @param port                 the port of the controller (default=16021)
	 * @param accessToken          a unique authentication token
	 * @throws NanoleafException   If the access token is invalid
	 * @throws IOException         If an HTTP exception occurs
	 */
	public Canvas(String hostname, int port, String accessToken)
			throws NanoleafException, IOException {
		
		super(hostname, port, accessToken);
	}
	
	/**
	 * Creates a new instance of the Canvas controller.
	 * 
	 * @param hostname             the hostname of the controller
	 * @param accessToken          a unique authentication token
	 * @throws NanoleafException   If the access token is invalid
	 * @throws IOException         If an HTTP exception occurs
	 */
	public Canvas(String hostname, String accessToken)
			throws NanoleafException, IOException {
		super(hostname, DEFAULT_PORT, accessToken);
	}
	
	/**
	 * Asynchronously creates a new instance of the Canvas.
	 * 
	 * @param hostname             the hostname of the Canvas
	 * @param port                 the port of the Canvas (default=16021)
	 * @param accessToken          a unique authentication token
	 * @param callback             the callback that is called when the device has
	 *                             been initialized
	 * @throws NanoleafException   If the access token is invalid
	 * @throws IOException         If an HTTP exception occurs
	 */
	public Canvas(String hostname, int port, String accessToken, NanoleafCallback<Canvas> callback) {
		super(hostname, port, accessToken, callback);
	}
	
	/**
	 * Asynchronously creates a new instance of the Canvas
	 * 
	 * @param hostname             the hostname of the Canvas
	 * @param accessToken          a unique authentication token
	 * @param callback             the callback that is called when the device has
	 *                             been initialized
	 * @throws NanoleafException   If the access token is invalid
	 * @throws IOException         If an HTTP exception occurs
	 */
	public Canvas(String hostname, String accessToken, NanoleafCallback<Canvas> callback) {
		super(hostname, DEFAULT_PORT, accessToken, callback);
	}
	
	protected Canvas(String hostname, int port, String accessToken, OkHttpClient client)
			throws NanoleafException, IOException {
		super(hostname, port, accessToken, client);
	}
	
	public List<Panel> getNeighborPanels(Panel panel, List<Panel> panels) {
		// The centroid distance is the (roughly) the distance between the
		// centroids of two neighboring panels
		final int CENTROID_DISTANCE = 105; // normally: 100
		List<Panel> neighbors = new ArrayList<Panel>();
		int p1x = panel.getX();
		int p1y = panel.getY();
		for (Panel p2 : panels) {
			if (!panel.equals(p2)) {
				int p2x = p2.getX();
				int p2y = p2.getY();
				if (Math.floor(Math.sqrt(Math.pow((p1x - p2x), 2) +
						Math.pow((p1y - p2y), 2))) <= CENTROID_DISTANCE) {
					neighbors.add(p2);
				}
			}
		}
		return neighbors;
	}
	
	/**
	 * Gets the shape of the Canvas panels. <b>This will always return
	 * a shape type with type {@link ShapeType#SQUARE}.</b>
	 * 
	 * @return   a canvas panel shape type
	 */
	@Override
	public ShapeType getShapeType() {
		return ShapeType.square();
	}
	
	/**
	 * Gets the shape of the Canvas controller. <b>This will always return
	 * a shape type with type {@link ShapeType#CANVAS_MASTER}.</b>
	 * 
	 * @return   a canvas master panel shape type
	 */
	@Override
	public ShapeType getControllerShapeType() {
		return ShapeType.squareMaster();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null || obj.getClass() != this.getClass()) {
			return false;
		}
		Canvas other = (Canvas)obj;
		return this.getHostname().equals(other.getHostname()) && this.getPort() == other.getPort() &&
				this.getName().equals(other.getName()) && this.getSerialNumber().equals(other.getSerialNumber()) &&
				this.getManufacturer().equals(other.getManufacturer()) && this.getModel().equals(other.getModel());
	}
}
