import java.util.BitSet;

/**
* represents one Point in 60-dimensional space, that is
* one frame (vector) of the data; contains the original index,
* the 60 values and the iSAX-representation that is build
*/
public class Point {
	
	private int index;
	private short[] values;
	private BitSet iSAXRep;
	
	/**
	 * generates a new point
	 * @param index number of the point over all files
	 * @param values the 60 values contained in that point
	 * @param iSAXlength length of the later-to-fill iSAXRepresentation
	 */
	public Point(int index, short[] values, short iSAXlength) {
		this.index = index;
		this.values = values;
		this.iSAXRep = new BitSet(iSAXlength);
	}
	
	/**
	* sets the bit at the specified position to 1
	*/
	public void extendISAXRep(int iSAXIndex) {
		iSAXRep.set(iSAXIndex);
	}
	
	public int getIndex() {
		return this.index;
	}
	
	public short getCoord(int i) {
		return values[i];
	}
	
	public String toString() {
		return this.index + ": " + values[0] + "/" +  values[1] + "/" + values[2] + "; iSAX: " + iSAXRep.toString();
	}

}
