
/**
* represents a Breakpoint to devide the Points; thus specifies
* the dimension and the value to divide them
*/
public class Breakpoint {
	
	/**
	* value between 0 and 59 representing the 60 dimensions
	*/
	private short dimension;
	private short value;
	
	public Breakpoint(short dimension, short value) {
		this.dimension = dimension;
		this.value = value;
	}
	
	/**
	* @return value between 0 and 59 representing one of the 60 dimensions
	*/
	public short getDimension() {
		return this.dimension;
	}
	
	public short getValue() {
		return this.value;
	}
	
	public String toString() {
		return "dimension: " +  dimension + "; value: " + value;
	}

} 