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

	final String outFile = "data_encoded.txt";
	final String outFileBreaks = "breakpoints.txt";
	final String inputDir = "D:/Studium/Semester06_So12/Bachelorarbeit/KinectData";
	//final String inputDir = "KinectData";

	// Vergroeberung
	final float faktor = 100f;
	final String format = "0,00";
	final DecimalFormat dcf = new DecimalFormat(format);
	final short framerate = 6;
	
	short limit = 52;
	int count = 0;
	List<Breakpoint> breakpoints = new ArrayList<Breakpoint>();
	List<Point[]> pointgroups = new ArrayList<Point[]>();
	
	final float[] firstLine = new float[60];
	private short[] help = null;
	private PriorityQueue<PatternInfo> storedMoves = new PriorityQueue<PatternInfo>();

	
	public Controller() {
		
		File[] fileList = initializeFiles();
		pointgroups.add(readData(fileList));
		
		for (int i=0; i<limit; i++) {
			
			//List<Point[]> newpointsgroup = new ArrayList<Point[]>();
			
			int maxLength = 0; int index = 0;
			for (int j=0; j< pointgroups.size(); j++) {
				Point[] points = pointgroups.get(j);
				if (points.length > maxLength) {
					maxLength = points.length;
					index = j;
				}
			}
			
			Point[] points = pointgroups.get(index);
			pointgroups.remove(index);
			
			Breakpoint lastBreak = chooseBreakpoint(points);
			breakpoints.add(lastBreak);
			System.out.println(lastBreak);
			pointgroups.addAll(seperatePoints(points, lastBreak));
			
			
			int j=1;
			for (Point[] pointgroup: pointgroups) {
				System.out.println(i + "/" + j + ": " + pointgroup.length);
				j++;
			}
		}
		
		
		printPoints(pointgroups);
		printBreakpoints(breakpoints);
		
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
			
			double sd = calculateSD(coordinates);
			if (sd > max) {
				max = sd;
				dim = j;
			}
			
		}
		
		for (int i = 0; i < points.length; i++) {
			coordinates.add(points[i].getCoord(dim));
		}
		while (coordinates.size() > points.length/2) {
			coordinates.remove();
		}
		
		return new Breakpoint(dim, coordinates.poll(), points[0].getiSAXRep());
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
	* takes an array of Points, separates it according to the given Breakpoint,
	* updates their iSAXRep and returns the new two arrays as a list
	*/
	private List<Point[]> seperatePoints(Point[] points, Breakpoint lastBreak) {
		ArrayList<Point> firstHalf = new ArrayList<Point>();
		ArrayList<Point> secondHalf = new ArrayList<Point>();
		short dimension = lastBreak.getDimension();
		short value = lastBreak.getValue();
		int counterborder = 0;
		count++;
		
		if (count % 2 == 0) {
			breakpoints.get(breakpoints.size()-1).setThreshBit(false);
			for(Point point: points) {
				if (point.getCoord(dimension)==value) { counterborder++; }
				if (point.getCoord(dimension) < value) {
					point.extendISAXRep(false);
					firstHalf.add(point);
				} else {
					point.extendISAXRep(true);
					secondHalf.add(point);
				}
			}
		} else {
			breakpoints.get(breakpoints.size()-1).setThreshBit(true);
			for(Point point: points) {
				if (point.getCoord(dimension)==value) { counterborder++; }
				if (point.getCoord(dimension) <= value) {
					point.extendISAXRep(false);
					firstHalf.add(point);
				} else {
					point.extendISAXRep(true);
					secondHalf.add(point);
				}
			}
		}
		
		System.out.println(counterborder);
		
		Point[] firstH = new Point[firstHalf.size()];
		firstHalf.toArray(firstH);
		Point[] secondH = new Point[secondHalf.size()];
		secondHalf.toArray(secondH);
		
		ArrayList<Point[]> ret = new ArrayList<Point[]>();
		
		ret.add(firstH);
		ret.add(secondH);
		
		return ret;
	}
	
	
	
	private void printPoints(List<Point[]> pointgroups) {
		System.out.println("print point...");
		
		PriorityQueue<Point> points = new PriorityQueue<Point>();
		
		for (Point[] pointarr : pointgroups) {
			for (Point point : pointarr) {
				points.add(point);
			}
		}
		
		StringBuffer buff = new StringBuffer();
		int startI = 0;
		while(points.peek()!=null) {
			buff.append(points.poll().toString() + "\n");
			startI++;
		}
				
		System.out.println("Zeilen gesamt: " + startI);
		
		try {
			Files.write(buff.toString().replace(".", ",").getBytes(), new File(framerate + "fps_" + outFile));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	private void printBreakpoints(List<Breakpoint> breakpoints) {
		System.out.println("print breakpoints...");
		
		StringBuffer buff = new StringBuffer();
		
		for (Breakpoint breakpoint : breakpoints) {
			buff.append(breakpoint.toString()  + "\n");
		}
		
		try {
			Files.write(buff.toString().replace(".", ",").getBytes(), new File(outFileBreaks));
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
