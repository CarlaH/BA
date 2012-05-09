
public class PatternInfo {
	
	private int startIndex;
	private short length;
	private int counter;
	
	public PatternInfo(int startIndex, short length, int counter) {
		this.startIndex = startIndex;
		this.length = length;
		this.counter = counter;
	}
	
	public int getStartIndex() {
		return this.startIndex;
	}
	public short getLength() {
		return this.length;
	}
	public int getCounter() {
		return this.counter;
	}
	
	public void augmentCounter() {
		this.counter++;
	}

}
