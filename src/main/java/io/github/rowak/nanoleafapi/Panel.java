package io.github.rowak.nanoleafapi;

import org.json.JSONObject;

/**
 * A storage class for panels from a Nanoleaf Device.
 */
public class Panel {
	
	private int id;
	private int x;
	private int y;
	private int orientation;
	private ShapeType shape;
	
	/**
	 * Creates a new panel.
	 * 
	 * @param id            a unique, positive ID number
	 * @param x             the x coordinate position of the panel
	 * @param y             the y coordinate position of the panel
	 * @param orientation   the orientation of the panel in degrees
	 * @param shape         the shape type of the panel
	 */
	public Panel(int id, int x, int y, int orientation, ShapeType shape) {
		this.id = id;
		this.x = x;
		this.y = y;
		this.orientation = orientation;
		this.shape = shape;
	}
	
	/**
	 * Gets the unique ID for the panel.
	 * 
	 * @return   the panel's unique ID
	 */
	public int getId() {
		return id;
	}
	
	/**
	 * Sets the unique ID for the panel.
	 * 
	 * @param id   a unique ID for the panel
	 */
	public void setId(int id) {
		this.id = id;
	}
	
	/**
	 * Gets the x value of this panel's location.
	 * 
	 * @return   the x value
	 */
	public int getX() {
		return x;
	}
	
	/**
	 * Sets the x value of this panel's location.
	 * 
	 * @param x   the x value
	 */
	public void setX(int x) {
		this.x = x;
	}
	
	/**
	 * Gets the y value of this panel's location.
	 * 
	 * @return   the y value
	 */
	public int getY() {
		return y;
	}
	
	/**
	 * Sets the y value of this panel's location.
	 * 
	 * @param y   the y value
	 */
	public void setY(int y) {
		this.y = y;
	}
	
	/**
	 * Gets the orientation of the panel in degrees.
	 * 
	 * @return   the panel orientation
	 */
	public int getOrientation() {
		return orientation;
	}
	
	/**
	 * Sets the orientation of the panel in degrees.
	 * 
	 * @param orientation   the panel orientation
	 */
	public void setOrientation(int orientation) {
		this.orientation = orientation;
	}
	
	/**
	 * <p>Gets the shape type of the panel.</p>
	 * 
	 * <p><b>Note:</b> this can normally be assumed for Aurora and Canvas devices,
	 * but Shapes devices may contain more than one panel type.</p>
	 * 
	 * @return   the panel shape type
	 */
	public ShapeType getShape() {
		return shape;
	}
	
	/**
	 * <p>Sets the shape type of the panel.</p>
	 * 
	 * <p><b>Note:</b> this can normally be assumed for Aurora and Canvas devices,
	 * but Shapes devices may contain more than one panel type.</p>
	 * 
	 * @param shape   the panel shape type
	 */
	public void setShape(ShapeType shape) {
		this.shape = shape;
	}
	
	public static Panel fromJSON(JSONObject json) {
		try {
			return new Panel(json.getInt("id"),
							 json.getInt("x"),
							 json.getInt("y"),
							 json.getInt("o"),
							 new ShapeType(json.getInt("shapeType")));
		}
		catch (Exception e) {
			throw new JSONParserException("Invalid arguments");
		}
	}
	
	@Override
	public String toString() {
		return String.format("[id=%d, x=%d, y=%d, o=%d, s=%s]",
				id, x, y, orientation, shape);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null || obj.getClass() != this.getClass()) {
			return false;
		}
		Panel other = (Panel)obj;
		return this.id == other.id && this.x == other.x && this.y == other.y &&
				this.orientation == other.orientation && this.shape.equals(other.shape);
	}
}
