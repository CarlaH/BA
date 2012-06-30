import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

/**
* represents one Point in 60-dimensional space, that is
* one frame (vector) of the data; contains the original index,
* the 60 values and the iSAX-representation that is build
*/
public class Point implements Comparable<Point>{
	
	private int index;
	private short[] values;
	private List<Boolean> iSAXRep;
	
	/**
	 * generates a new point
	 * @param index number of the point over all files
	 * @param values the 60 values contained in that point
	 * @param iSAXlength length of the later-to-fill iSAXRepresentation
	 */
	public Point(int index, short[] values, short iSAXlength) {
		this.index = index;
		this.values = values;
		this.iSAXRep = new ArrayList<Boolean>();
	}
	
	/**
	* sets the bit at the specified position to 1
	*/
	public void extendISAXRep(boolean is) {
		iSAXRep.add(is);
	}
	
	public int getIndex() {
		return this.index;
	}
	
	public short getCoord(int i) {
		return values[i];
	}
	
	public List<Boolean> getiSAXRep() {
		return iSAXRep;
	}
	
	public String toString() {
		String str = this.index + ": ";
		
		for (int i = 0; i < iSAXRep.size(); i++) {
			if (iSAXRep.get(i)) {
				str = str + "1";
			} else {
				str = str + "0";
			}
		}
		
		return str;
	}
	
	@Override
	public int compareTo(Point o) {
		if (this.index < o.getIndex()) {
			return -1;
		} else if (this.index == o.getIndex()){
			return 0;
		} else {
			return 1;
		}
	}

}
