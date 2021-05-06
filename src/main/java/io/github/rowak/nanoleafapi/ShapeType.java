package io.github.rowak.nanoleafapi;

/**
 * A simple storage class for storing the shape type of a panel. Also can be
 * used to get the side length of panel shape types.
 */
public class ShapeType {
	
	/** Aurora panel */
	public static final int TRIANGLE_AURORA = 0;
	
	/** Aurora rhythm module */
	public static final int RHYTHM = 1;
	
	/** Canvas regular panel */
	public static final int SQUARE = 2;
	
	/** Canvas control panel */
	public static final int SQUARE_MASTER = 3;
	
	/** Canvas regular panel?? */
	public static final int SQUARE_PASSIVE = 4;
	
	/** Shapes Hexagons panel */
	public static final int HEXAGON = 7;
	
	/** Shapes Triangle panel */
	public static final int TRIANGLE_SHAPES = 8;
	
	/** Shapes Mini Triangle panel */
	public static final int MINI_TRIANGLE = 9;
	
	/** Shapes controller panel */
	public static final int SHAPES_CONTROLLER = 12;
	
	private final int TRIANGLE_AURORA_SIDELEN = 150;
	private final int RHYTHM_SIDELEN = 0;
	private final int SQUARE_SIDELEN = 100;
	private final int SQUARE_MASTER_SIDELEN = 100;
	private final int SQUARE_PASSIVE_SIDELEN = 100;
	private final int HEXAGON_SIDELEN = 67;
	private final int TRIANGLE_SHAPES_SIDELEN = 134;
	private final int MINI_TRIANGLE_SIDELEN = 67;
	private final int SHAPES_CONTROLLER_SIDELEN = 0;
	
	private int shape;
	
	/** Convenience method for creating an Aurora panel shape type */
	public static ShapeType triangleAurora() { return new ShapeType(TRIANGLE_AURORA); }
	
	/** Convenience method for creating an Aurora rhythm panel shape type */
	public static ShapeType rhythm() { return new ShapeType(RHYTHM); }
	
	/** Convenience method for creating a Canvas regular panel shape type */
	public static ShapeType square() { return new ShapeType(SQUARE); }
	
	/** Convenience method for creating an Canvas control panel shape type */
	public static ShapeType squareMaster() { return new ShapeType(SQUARE_MASTER); }
	
	/** Convenience method for creating an Hexagon (Shapes) panel shape type */
	public static ShapeType hexagon() { return new ShapeType(HEXAGON); }
	
	/** Convenience method for creating an Triangle (Shapes) panel shape type */
	public static ShapeType triangleShapes() { return new ShapeType(TRIANGLE_SHAPES); }
	
	/** Convenience method for creating an Mini Triangle (Shapes) panel shape type */
	public static ShapeType miniTriangle() { return new ShapeType(MINI_TRIANGLE); }
	
	/** Convenience method for creating an Shapes controller panel shape type */
	public static ShapeType shapesController() { return new ShapeType(SHAPES_CONTROLLER); }

	/**
	 * Creates an instance of a shape type. The shape parameter takes
	 * any of the shape types defined in the ShapeType class. For example,
	 * ShapeType.TRIANGLE_AURORA represents an Aurora panel.
	 * @param shape   the shape type
	 */
	public ShapeType(int shape) {
		this.shape = shape;
	}
	
	/**
	 * Gets the integer value of this shape type. This is the value used to
	 * represent shape types in JSON format.
	 * @return   the shape type integer value
	 */
	public int getValue() {
		return shape;
	}
	
	/**
	 * Gets the side length of this shape type.
	 * @return   the shape length
	 */
	public int getSideLength() {
		switch (shape) {
			case TRIANGLE_AURORA: return TRIANGLE_AURORA_SIDELEN;
			case RHYTHM: return RHYTHM_SIDELEN;
			case SQUARE: return SQUARE_SIDELEN;
			case SQUARE_MASTER: return SQUARE_MASTER_SIDELEN;
			case SQUARE_PASSIVE: return SQUARE_PASSIVE_SIDELEN;
			case HEXAGON: return HEXAGON_SIDELEN;
			case TRIANGLE_SHAPES: return TRIANGLE_SHAPES_SIDELEN;
			case MINI_TRIANGLE: return MINI_TRIANGLE_SIDELEN;
			case SHAPES_CONTROLLER: return SHAPES_CONTROLLER_SIDELEN;
			default: return 0;
		}
	}
	
	@Override
	public String toString() {
		switch (shape) {
			case TRIANGLE_AURORA: return "TRIANGLE_AURORA";
			case RHYTHM: return "RHYTHM";
			case SQUARE: return "SQUARE";
			case SQUARE_MASTER: return "SQUARE_MASTER";
			case SQUARE_PASSIVE: return "SQUARE_PASSIVE";
			case HEXAGON: return "HEXAGON";
			case TRIANGLE_SHAPES: return "TRIANGLE_SHAPES";
			case MINI_TRIANGLE: return "MINI_TRIANGLE";
			case SHAPES_CONTROLLER: return "SHAPES_CONTROLLER";
			default: return null;
		}
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null || obj.getClass() != this.getClass()) {
			return false;
		}
		ShapeType other = (ShapeType)obj;
		return this.shape == other.shape;
	}
}
