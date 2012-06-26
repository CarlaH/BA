import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;

import com.google.common.io.Files;

public class Controller {

	final String outFile = "patterns.csv";
	//final String inputDir = "D:/Studium/Semester06_So12/Bachelorarbeit/KinectData";
	final String inputDir = "KinectData";

	// Vergroeberung
	final float faktor = 100f;
	final String format = "0,00";
	final DecimalFormat dcf = new DecimalFormat(format);
	final short framerate = 6;
	
	short limit = 4;
	List<Breakpoint> breakpoints = new ArrayList<Breakpoint>();
	List<Point[]> pointgroups = new ArrayList<Point[]>();
	
	final float[] firstLine = new float[60];
	private short[] help = null;
	private PriorityQueue<PatternInfo> storedMoves = new PriorityQueue<PatternInfo>();

	// TODO to be removed soon
	int foundCounter = 0;
	HashMap<Integer, Integer> patternsOccurence = new HashMap<Integer, Integer>();

	
	
	public Controller() {
		
		File[] fileList = initializeFiles();
		pointgroups.add(readData(fileList));
		
		for (int i=0; i<limit; i++) {
			
			List<Point[]> newpointsgroup = new ArrayList<Point[]>();
			
			for (Point[] points: pointgroups) {
				Breakpoint lastBreak = chooseBreakpoint(points);
				breakpoints.add(lastBreak);
				System.out.println(lastBreak);
				newpointsgroup.addAll(seperatePoints(points, lastBreak, i));
			}
			
			pointgroups = newpointsgroup;
			int j=1;
			for (Point[] points: pointgroups) {
				System.out.println(i + "/" + j + ": " + points.length);
				j++;
			}
		}
		
		
		//printForViewer(indices, fileList);
		
		System.out.println("complete!");
	}

	
	
	private File[] initializeFiles() {
		File dir = new File(inputDir);
		File[] fileList = dir.listFiles();
		
		return fileList;

	}	

	
	private Point[] readData(File[] fileList) {
		ArrayList<Point> data = new ArrayList<Point>();
		int amountOfFiles = fileList.length;
		int fileCounter = 0;
		short[] help = null;
		int startI = 0;
		short takeEachNth = (short) Math.round(30 / framerate);
		System.out.println("take each nth: " + takeEachNth);

		for (File file : fileList) {
			fileCounter++;
			System.out.println(file.getName() + " - reading " + fileCounter	+ "/" + amountOfFiles + "...");
			try {
				// read file content
				BufferedReader br = new BufferedReader(new FileReader(file));
				String strLine;
				boolean initFirst;
				int i = -1;

				while ((strLine = br.readLine()) != null) {
					i++;
					if (i % takeEachNth != 0) {
						continue;
					}
					initFirst = (help == null) ? true : false;
					short[][] result = processLine(strLine, help, initFirst);
					short[] dataset = result[0];
					if (!initFirst) {
						data.add(new Point(startI, dataset, limit)); // add new Array to data
					}
					help = result[1];
					startI++;
				}

				System.out.println("...reading complete");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		
		Point[] points = new Point[data.size()];
		data.toArray(points);
		System.out.println("Anzahl Punkte: " + points.length);
		return points;
		
	}
	
	/**
	* For one line of a file parses the content and rounds the values
	*
	* @param strLine
	* Line
	* @param help
	* help-Array containing the values of the line before
	* @param initFirst
	* whether it's the first line of the first file or not
	* @return short[] containing the values
	* @throws ParseException
	*/
	private short[][] processLine(String strLine, short[] help,	boolean initFirst) throws ParseException {
		String[] strValues = strLine.split(";");
		int di = 0;
		short newValue = 0;
		short[] dataset = new short[60];
		short[] helpNew = new short[60];

		for (int i = 1; i < strValues.length; i++) {

			if (i % 4 == 0)
				continue; // Koordinate W

			float parsedValue = dcf.parse(strValues[i]).floatValue() * faktor;

			if (initFirst) {
				firstLine[di] = Math.round(parsedValue) / faktor;
				helpNew[di] = (short) (parsedValue);
				// System.out.println("help " + di + ": " + s[i] + " => " +
				// d[di]);

			} else {
				newValue = (short) (parsedValue);
				if (Math.abs(newValue - help[di]) > Short.MAX_VALUE) {
					System.out.println(">>> value too big: "
							+ (newValue - help[di]));
				}
				dataset[di] = (short) (newValue - help[di]);
				helpNew[di] = newValue;
			}

			di++;
		}

		short[][] result = { dataset, helpNew };
		return result;
	}
	
	
	
	/**
	* chooses the Breakpoint to divide the given Points into
	* two equal halfs under consideration of the standard deviation
	*/
	private Breakpoint chooseBreakpoint(Point[] points) {
		
		PriorityQueue<Short> coordinates = new PriorityQueue<Short>();
		
		double max = 0; short dim = 0;
		
		for (short j = 0; j < 60; j++) {
			for (int i = 0; i < points.length; i++) {
				coordinates.add(points[i].getCoord(j));
			}
			
			double entropy = calculateEntropy(coordinates);
			if (entropy > max) {
				max = entropy;
				dim = j;
			}
			
		}
		
		for (int i = 0; i < points.length; i++) {
			coordinates.add(points[i].getCoord(dim));
		}
		while (coordinates.size() > points.length/2) {
			coordinates.remove();
		}
		
		return new Breakpoint(dim, coordinates.poll());
	}
	
	
	/**
	* calculates the standard deviation of the given coordinates
	*/
	private double calculateSD(PriorityQueue<Short> coord) {
		int length = coord.size();
		short[] coordinates = new short[length];
		for (int i = 0; i < length; i++) {
			coordinates[i] = coord.poll().shortValue();
		}
		
		int sum = 0;
		for(int i=0; i<length; i++) {
			sum += coordinates[i];
		}
		double mean = sum/length;
		double deviation = 0;
		for(int i=0; i<length; i++) {
			double d = coordinates[i]-mean;
			deviation += (d*d);
		}
		deviation = Math.sqrt(deviation/length);
		
		return deviation;
	}
	
	
	/**
	 * calculates the Entropy of the given coordinates
	 * Berechnung: Wahrscheinlichkeit p jedes "Zeichens", also jedes Wertes, 
	 * dann jeweils p*log2(p), summiere das alles auf und negiere
	 */
	private double calculateEntropy(PriorityQueue<Short> coord) {
		double entropy = 0.0;
		double amount = coord.size();
		int nullcounter = 0;
		
		HashMap<Short,Integer> occur = new HashMap<Short, Integer>();
		
		while (coord.size() > 0) {
			short value = coord.poll();
			if (value == 0) { nullcounter++; }
			if (occur.containsKey(value)) {
				occur.put(value, occur.get(value)+1);
			} else {
				occur.put(value, 1);
			}
		}
		
		System.out.println("percentage 0: " + (nullcounter/amount)*100 + " %");
		
		for (Short value : occur.keySet()) {
			double prob = occur.get(value)/amount;
			entropy = entropy + (prob* (Math.log(prob)/Math.log(2)));
		}
		entropy = - entropy;
		
		return entropy;
	}
	
	
	
	

	/**
	* takes an array of Points, separates it according to the given Breakpoint,
	* updates their iSAXRep and returns the new two arrays as a list
	*/
	private List<Point[]> seperatePoints(Point[] points, Breakpoint lastBreak, int iSAXIndex) {
		ArrayList<Point> firstHalf = new ArrayList<Point>();
		ArrayList<Point> secondHalf = new ArrayList<Point>();
		short dimension = lastBreak.getDimension();
		short value = lastBreak.getValue();
		
		for(Point point: points) {
			if(point.getCoord(dimension) < value) {
				// no need to extend ISAXRep as the bit is already 0
				firstHalf.add(point);
			} else {
				point.extendISAXRep(iSAXIndex);
				secondHalf.add(point);
			}
		}
		
		Point[] firstH = new Point[firstHalf.size()];
		firstHalf.toArray(firstH);
		Point[] secondH = new Point[secondHalf.size()];
		secondHalf.toArray(secondH);
		
		ArrayList<Point[]> ret = new ArrayList<Point[]>();
		
		ret.add(firstH);
		ret.add(secondH);
		
		return ret;
	}
	
	
	
		
	/**
	 * checks whether the suggested Pattern already exists in the storedMoves
	 * and adds it either by heightening the counter at the fitting position or
	 * by adding a new entry
	 * @param suggestedPattern new Move that could be a pattern
	 * @param storedMoves already found moves
	 * @return true if move was found, false if a new entry was created
	 */
	private boolean addPattern(List<short[]> suggestedPattern, int startI, short len) {
		for (PatternInfo patInfo : storedMoves){
			
			if (patInfo.getLength()!=len) { continue; }  // check size
			
			if (patInfo.moveEquals(suggestedPattern)) {
				patInfo.augmentCounter();
				foundCounter++;
				return true;
			}
			
		}

		storedMoves.add(new PatternInfo(startI, len, 1, suggestedPattern));
		return false;
		
	}
	
	
	
	
	
	private HashMap<Integer, Short> buildPatternHashMap(int filenr) {
		HashMap<Integer, Short> indices = new HashMap<Integer, Short>();
	
		PatternInfo moveInfos;
		
		while(storedMoves.size() > 15){
			storedMoves.remove();
		}
		
		while (!storedMoves.isEmpty()) {
			moveInfos = storedMoves.poll();
			
			if(moveInfos.getCounter()>filenr){
				int startI = moveInfos.getStartIndex();
				/* if there is already a pattern with a near startI
				 * remove it - the new pattern is guaranteed to have 
				 * a higher counter and the patterns are almost
				 * the same
				 */
				indices.remove(startI-2);
				indices.remove(startI-1);
				indices.remove(startI+1);
				indices.remove(startI+2);
				indices.put(moveInfos.getStartIndex(), moveInfos.getLength());					
				
				patternsOccurence.remove(startI-2);
				patternsOccurence.remove(startI-1);
				patternsOccurence.remove(startI+1);
				patternsOccurence.remove(startI+2);
				patternsOccurence.put(startI, moveInfos.getCounter());
				
			}
		}
		
		
		return indices;
	}

	
	/**
	 * neue Variante;
	 * Durchläuft Datei für Datei und kopiert Zeilen daraus, falls sie zum ersten 
	 * Auftreten eines gefundenen Patterns gehören
	 * @param patternIndices
	 * @param fileList
	 */
	private void printForViewer(HashMap<Integer, Short> patternIndices, File[] fileList) {
		System.out.println("print patterns for viewer...");
		
		StringBuffer buff = new StringBuffer();
		int startI = 0;
		short takeEachNth = (short) Math.round(30/framerate);
		HashMap<Integer, String> lines;
		
		for (int f = 0; f < fileList.length; f++) {
			lines = new HashMap<Integer, String>();
			try {
				// read file content
				BufferedReader br = new BufferedReader(new FileReader(fileList[f]));
				System.out.println("read file " + fileList[f]);
				String strLine;
				int i = 0;
				

				while ((strLine = br.readLine()) != null) {
					i++;
					if(i%takeEachNth != 0) { continue; }
					lines.put(startI, strLine);
					startI++;
				}
				
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			for (int i = lines.size(); i>0 ; i--) {

				if (patternIndices.containsKey(startI-i)) {
					System.out.println("...contains: " + (startI-i));
					
					for (int j = 0; j < patternIndices.get(startI-i); j++) {
						buff.append(lines.get(startI-i+j) + "\n");
					}
					// am Ende einer Sequenz eine Sekunde lang Nuller
					String origin = "0;0;0;" + (startI-i) + " " + patternsOccurence.get(startI-i);
					int numJoints = 20;

					String originLine = "0;";
					for (int j = 0; j < numJoints - 1; j++) {
						originLine += origin + ";";
					}
					originLine += origin + "\n";

					for (int j = 0; j < framerate; j++) {
						buff.append(originLine);
					}
				}
				
			}
		}
		
		System.out.println("Zeilen gesamt: " + startI);
		
		try {
			Files.write(buff.toString().replace(".", ",").getBytes(), new File(outFile));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	
	

	/**
	 * Calculates the new Joints
	 * 
	 * @param pointDataset
	 *            old Joints
	 * @param dataset
	 *            the vectors
	 */
	private float[] calculateNewPoints(float[] pointDataset, short[] dataset) {
		for (int j = 0; j < pointDataset.length; j++) {
			pointDataset[j] = pointDataset[j] + ((float) dataset[j]) / faktor;
		}
		return pointDataset;
	}

}
