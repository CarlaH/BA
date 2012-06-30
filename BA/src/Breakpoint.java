import java.util.List;


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
	/**
	 * holds the iSAXRep for the group, that was divided by this breakpoint;
	 * that means: the prefix of the complete SAX-word;
	 * the next bit after this prefix is defined by this breakpoint
	 */
	private List<Boolean> group;
	/**
	 * the bit that is assigned to points lying on the threshold
	 */
	private boolean thresholdvalue;
	
	public Breakpoint(short dimension, short value) {
		this.dimension = dimension;
		this.value = value;
	}
	
	public Breakpoint(short dimension, short value, List<Boolean> group) {
		this.dimension = dimension;
		this.value = value;
		this.group = group;
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
	
	public void setGroup(List<Boolean> group) {
		this.group = group;
	}
	
	public void setThreshBit(boolean is) {
		this.thresholdvalue = is;
	}
	
	public String toString() {
		String str = "group: ";
		
		for (int i = 0; i < group.size(); i++) {
			if (group.get(i)) {
				str = str + "1";
			} else {
				str = str + "0";
			}
		}
		
		str = str + "; dimension: " +  dimension + "; value: " + value + "; threshold bit: ";
		if (thresholdvalue) {
			str = str + "1";
		} else {
			str = str + "0";
		}
		
		return str;
	}

} 