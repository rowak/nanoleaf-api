package com.github.rowak.nanoleafapi;

/**
 * The Frame class is an immutable storage for frames that make up an animation.
 * 
 * Each frame in an animation is associated with a panel, and it stores an RGB color
 * that will be displayed at the start of the frame, as well as a transition time that
 * defines how long it takes to display the color.
 */
public class Frame {
	
	// Frame components: red, green, blue, white, transition time
	private int r, g, b, w, t;
	
	/**
	 * Creates a new frame from RGB components.
	 * @param red              the red RGB value of the frame's color
	 * @param green            the green RGB value of the frame's color
	 * @param blue             the blue RGB value of the frame's color
	 * @param transitionTime   the duration of transition between
	 * 						   the previous frame and this frame
	 */
	public Frame(int red, int green, int blue, int transitionTime) {
		this.r = red;
		this.g = green;
		this.b = blue;
		this.w = 0;
		this.t = transitionTime;
	}
	
	/**
	 * Creates a new frame from a color.
	 * @param color            the color of the frame
	 * @param transitionTime   the duration of transition between
	 * 						   the previous frame and this frame
	 */
	public Frame(Color color, int transitionTime) {
		this.r = color.getRed();
		this.g = color.getGreen();
		this.b = color.getBlue();
		this.w = 0;
		this.t = transitionTime;
	}
	
	/**
	 * Creates a new INITIAL frame. Initial frames have no transition time
	 * and are displayed only once in the animation. This is only different from
	 * a frame with 0 transition time if the animation is looping.
	 * @param red     the red RGB value of the frame's color
	 * @param green   the green RGB value of the frame's color
	 * @param blue    the blue RGB value of the frame's color
	 */
	public Frame(int red, int green, int blue) {
		this(red, green, blue, -1);
	}
	
	/**
	 * Creates a new INITIAL frame. Initial frames have no transition time
	 * and are displayed only once in the animation. This is only different from
	 * a frame with 0 transition time if the animation is looping.
	 * 
	 * @param color   the color of the frame
	 */
	public Frame(Color color) {
		
	}
	
	/**
	 * Gets the red RGB value of the frame's color.
	 * 
	 * @return  the frame's red value
	 */
	public int getRed() {
		return this.r;
	}
	
	/**
	 * Gets the green RGB value of the frame's color.
	 * 
	 * @return  the frame's green value
	 */
	public int getGreen() {
		return this.g;
	}
	
	/**
	 * Gets the blue RGB value of the frame's color.
	 * 
	 * @return   the frame's blue value
	 */
	public int getBlue() {
		return this.b;
	}
	
	/**
	 * Gets the transition time of this frame (the duration of transition between
	 * the previous frame and this frame).
	 * 
	 * @return   the frame's transition time
	 */
	public int getTransitionTime() {
		return this.t;
	}
	
	@Override
	public String toString() {
		return "[r=" + this.r +
				", g=" + this.g + ", b=" + this.b +
				", w=" + this.w + ", t=" + this.t + "]";
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		else if (obj.getClass() != this.getClass()) {
			return false;
		}
		Frame other = (Frame)obj;
		return this.r == other.r &&
				this.g == other.g &&
				this.b == other.b &&
				this.w == other.w &&
				this.t == other.t;
	}
}
