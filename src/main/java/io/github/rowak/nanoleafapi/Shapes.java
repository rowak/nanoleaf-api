package io.github.rowak.nanoleafapi;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;

/**
 * The Shapes class is one of the three main concrete classes for creating a NanoleafDevice.
 * This class should only be used to connect to physical Hexagons, Triangles, or Mini
 * Triangles (Nanoleaf Shapes) devices. For other devices such as the Canvas or Hexagons, use
 * the Aurora and Canvas classes, respectively.
 */
public class Shapes extends NanoleafDevice {

	private String deviceName;
	
	/**
	 * Creates a new instance of the Shapes controller.
	 * 
	 * @param hostname             the hostname of the controller
	 * @param port                 the port of the controller (default=16021)
	 * @param accessToken          a unique authentication token
	 * @throws NanoleafException   If the access token is invalid
	 * @throws IOException         If an HTTP exception occurs
	 */
	public Shapes(String hostname, int port, String accessToken)
			throws NanoleafException, IOException {
		
		super(hostname, port, accessToken);
		deviceName = getName();
	}
	
	/**
	 * Creates a new instance of the Shapes controller.
	 * 
	 * @param hostname             the hostname of the controller
	 * @param accessToken          a unique authentication token
	 * @throws NanoleafException   If the access token is invalid
	 * @throws IOException         If an HTTP exception occurs
	 */
	public Shapes(String hostname, String accessToken)
			throws NanoleafException, IOException {
		super(hostname, DEFAULT_PORT, accessToken);
		deviceName = getName();
	}
	
	/**
	 * Asynchronously creates a new instance of the Shapes.
	 * 
	 * @param hostname             the hostname of the Shapes
	 * @param port                 the port of the Shapes (default=16021)
	 * @param accessToken          a unique authentication token
	 * @param callback             the callback that is called when the device has
	 *                             been initialized
	 * @throws NanoleafException   If the access token is invalid
	 * @throws IOException         If an HTTP exception occurs
	 */
	public Shapes(String hostname, int port, String accessToken,
			NanoleafCallback<Shapes> callback) {
		super(hostname, port, accessToken, callback);
		deviceName = getName();
	}
	
	/**
	 * Asynchronously creates a new instance of the Shapes.
	 * 
	 * @param hostname             the hostname of the Shapes
	 * @param accessToken          a unique authentication token
	 * @param callback             the callback that is called when the device has
	 *                             been initialized
	 * @throws NanoleafException   If the access token is invalid
	 * @throws IOException         If an HTTP exception occurs
	 */
	public Shapes(String hostname, String accessToken,
			NanoleafCallback<Shapes> callback) {
		super(hostname, DEFAULT_PORT, accessToken, callback);
	}
	
	protected Shapes(String hostname, int port, String accessToken, OkHttpClient client)
			throws NanoleafException, IOException {
		super(hostname, port, accessToken, client);
		deviceName = getName();
	}
	
	public List<Panel> getNeighborPanels(Panel panel, List<Panel> panels) {
		// The centroid distance is the (roughly) the distance between the
		// centroids of two neighboring panels
		final int CENTROID_DISTANCE = 121; // normally: 116
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
	 * <p>Gets the shape type of the Shapes panels.</p>
	 * 
	 * <p><b>Important:</b> This method is incomplete. There is currently
	 * no obvious way to determine the exact type of a Shapes device.
	 * This method will return the shape type for hexagon panels as
	 * a placeholder.</p>
	 */
	@Override
	public ShapeType getShapeType() {
		if (deviceName.contains("DBC2")) {
			return ShapeType.hexagon();
		}
		else if (deviceName.contains("C83F")) {
			return ShapeType.triangleShapes();
		}
		else {
			return ShapeType.miniTriangle();
		}
	}
	
	/**
	 * Gets the shape of the Shapes controller. <b>This will always return
	 * a shape type with type {@link ShapeType#SHAPES_CONTROLLER}</b>
	 * 
	 * @return   a shapes controller shape type
	 */
	@Override
	public ShapeType getControllerShapeType() {
		return ShapeType.shapesController();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null || obj.getClass() != this.getClass()) {
			return false;
		}
		Shapes other = (Shapes)obj;
		return this.getHostname().equals(other.getHostname()) && this.getPort() == other.getPort() &&
				this.getName().equals(other.getName()) && this.getSerialNumber().equals(other.getSerialNumber()) &&
				this.getManufacturer().equals(other.getManufacturer()) && this.getModel().equals(other.getModel());
	}
}
