package com.github.rowak.nanoleafapi;

import java.util.ArrayList;
import java.util.List;

public class Panel {
	private int id;
	private int x;
	private int y;
	private int orientation;
	private ShapeType shape;
	
	public Panel(int id, int x, int y, int orientation, ShapeType shape) {
		this.id = id;
		this.x = x;
		this.y = y;
		this.orientation = orientation;
		this.shape = shape;
	}
	
	/**
	 * Gets the unique ID for the panel.
	 * @return  the panel's unique ID
	 */
	public int getId() {
		return id;
	}
	
	public void setId(int id) {
		this.id = id;
	}
	
	public int getX() {
		return x;
	}
	
	public void setX(int x) {
		this.x = x;
	}
	
	public int getY() {
		return y;
	}
	
	public void setY(int y) {
		this.y = y;
	}
	
	public int getOrientation() {
		return orientation;
	}
	
	public void setOrientation(int orientation) {
		this.orientation = orientation;
	}
	
	public ShapeType getShape() {
		return shape;
	}
	
	public void setShape(ShapeType shape) {
		this.shape = shape;
	}
	
//	/**
//	 * Gets the direct neighbors of this panel (maximum is 3, minimum is 1).
//	 * @param panels  all connected panels in the Aurora.
//	 * @return  an array of type <code>Panel</code> containing the
//	 * 			direct neighbors of this panel
//	 */
//	public Panel[] getNeighbors(Panel[] panels)
//	{
//		// Distance constant represents the vertical/horizontal/diagonal distance
//		// that all neighboring panels are within
//		final int DISTANCE_CONST = 86;
//		List<Panel> neighbors = new ArrayList<Panel>();
//		int p1x = this.getX();
//		int p1y = this.getY();
//		for (Panel p2 : panels)
//		{
//			int p2x = p2.getX();
//			int p2y = p2.getY();
//			if (Math.floor(Math.sqrt(Math.pow((p1x - p2x), 2) +
//					Math.pow((p1y - p2y), 2))) == DISTANCE_CONST)
//			{
//				neighbors.add(p2);
//			}
//		}
//		return neighbors.toArray(new Panel[]{});
//	}
}
