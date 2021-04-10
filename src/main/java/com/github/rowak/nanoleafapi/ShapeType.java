package com.github.rowak.nanoleafapi;

public class ShapeType {
	public static final int TRIANGLE_AURORA = 0;
	public static final int RHYTHM = 1;
	public static final int SQUARE = 2;
	public static final int SQUARE_MASTER = 3;
	public static final int SQUARE_PASSIVE = 4;
	public static final int HEXAGON = 7;
	public static final int TRIANGLE_SHAPES = 8;
	public static final int MINI_TRIANGLE = 9;
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
	
	public static ShapeType triangleAurora() { return new ShapeType(TRIANGLE_AURORA); }
	public static ShapeType rhythm() { return new ShapeType(RHYTHM); }
	public static ShapeType square() { return new ShapeType(SQUARE); }
	public static ShapeType squareMaster() { return new ShapeType(SQUARE_MASTER); }
	public static ShapeType hexagon() { return new ShapeType(HEXAGON); }
	public static ShapeType triangleShapes() { return new ShapeType(TRIANGLE_SHAPES); }
	public static ShapeType miniTriangle() { return new ShapeType(MINI_TRIANGLE); }
	public static ShapeType shapesController() { return new ShapeType(SHAPES_CONTROLLER); }

	public ShapeType(int shape) {
		this.shape = shape;
	}
	
	public int getValue() {
		return shape;
	}
	
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
}
