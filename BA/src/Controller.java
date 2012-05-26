import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;

import com.google.common.io.Files;

public class Controller {

	final String outFile = "patterns.csv";
	final String inputDir = "D:/Studium/Semester06_So12/Bachelorarbeit/KinectData";
	//final String inputDir = "KinectData";

	// Vergroeberung
	final float faktor = 100f;
	final String format = "0,00";
	final DecimalFormat dcf = new DecimalFormat(format);
	final short framerate = 6;
	
	// Laenge Patterns
	final short minFrames = 6;
	final short maxFrames = 18;
	
	final float[] firstLine = new float[60];
	private short[] help = null;
	private PriorityQueue<PatternInfo> storedMoves = new PriorityQueue<PatternInfo>();

	// TODO to be removed soon
	int foundCounter = 0;
	HashMap<Integer, Integer> patternsOccurence = new HashMap<Integer, Integer>();

	
	
	public Controller() {
		long start = System.currentTimeMillis();

		File[] fileList = initializeFiles();
		
		int startI = 0;
		
		for (int i = 0; i < fileList.length; i++) {
			File file = fileList[i];
			
			System.out.println(file.getName() + " - reading " + (i+1)+ "/" + fileList.length + "...");
			
			startI = searchPatterns(file, startI);	
			validatePatterns(i+1);
			
			System.out.println("...reading complete");
		}
		
		HashMap<Integer, Short> indices = buildPatternHashMap(fileList.length);
		
		// Fake-Patterns: HashMap mit Startindex und Anzahl Frames
//		HashMap<Integer, Short> indices = new HashMap<Integer, Short>();
//		indices.put(0, (short)1000);
//		indices.put(6000, (short)500);
		
		final long duration = System.currentTimeMillis() - start;
		
		printForViewer(indices, fileList);
		
		System.out.println("complete! Duration: " + duration);
	}

	
	
	private File[] initializeFiles() {
		File dir = new File(inputDir);
		File[] fileList = dir.listFiles();
		
		return fileList;

	}	

	
	/**
	 * searches in data for patterns of lengths between minFrames and maxFrames
	 * 
	 */
	private int searchPatterns(File file, int startI) {
		
		ArrayList<short[]> data = initializeFileData(file);
		List<short[]> suggestedPattern;
		int size = data.size();

		System.out.println("search patterns...");
		
		for (int i = 0; i < size-maxFrames; i++) {	
			//System.out.println(i+"/"+size);
			
			for(short len=maxFrames; len>=minFrames; len--){  // for each Framelength different storedMoves?
				suggestedPattern = data.subList(i, i+len);
				boolean isOldPattern = addPattern(suggestedPattern, startI, len);
				//if(isOldPattern) { break; }
			}
			
			startI++;
			
		}
		
		return startI;
		
	}
	
	private ArrayList<short[]> initializeFileData(File file) {
		System.out.println("initialize File Data...");
		ArrayList<short[]> data = new ArrayList<short[]>();
		
		short takeEachNth = (short) Math.round(30/framerate);

		try {
			// read file content
			BufferedReader br = new BufferedReader(new FileReader(file));
			String strLine;
			boolean initFirst;
			int i = -1;

			while ((strLine = br.readLine()) != null) {
				i++;
				if(i%takeEachNth != 0) { continue; }
				initFirst = (help == null) ? true : false;
				short[][] result = processLine(strLine, help, initFirst);
				short[] dataset = result[0];
				if (!initFirst) {
					data.add(dataset); // add new Array to data
				}
				help = result[1];
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		
	return data;
		
	}

	/**
	 * For one line of a file parses the content and rounds the values
	 * 
	 * @param strLine
	 *            Line
	 * @param help
	 *            help-Array containing the values of the line before
	 * @param initFirst
	 *            whether it's the first line of the first file or not
	 * @return short[] containing the values
	 * @throws ParseException
	 */
	private short[][] processLine(String strLine, short[] help, boolean initFirst) throws ParseException {
		String[] strValues = strLine.split(";");
		int di = 0;
		short newValue = 0;
		short[] dataset = new short[60];
		short[] helpNew = new short[60];

		for (int i = 1; i < strValues.length; i++) {

			if (i % 4 == 0)
				continue; // Koordinate W

			float parsedValue = dcf.parse(strValues[i]).floatValue()	* faktor;
			
			if (initFirst) {
				firstLine[di] = Math.round(parsedValue) / faktor;
				helpNew[di] = (short) (parsedValue);
				// System.out.println("help " + di + ": " + s[i] + " => " + d[di]);

			} else {
				newValue = (short) (parsedValue);
				if (Math.abs(newValue - help[di]) > Short.MAX_VALUE) {
					System.out.println(">>> value too big: " + (newValue - help[di]));
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
	
	
	
	private void validatePatterns(int filenr) {
		System.out.println("validate patterns...");
		
		System.out.println(storedMoves.size());
		while(storedMoves.size()>2500) {
			storedMoves.remove();
		}
		
		System.out.println("Verbleibende Patterns: " + storedMoves.size());
//		int size = storedMoves.size();
//		for (int i = 0; i < size; i++) {
//			System.out.println(storedMoves.poll().getCounter());			
//		}
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
					String origin = "0;0;0;" + patternsOccurence.get(startI-i);
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
		
		try {
			Files.write(buff.toString().replace(".", ",").getBytes(), new File(outFile));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	
	
	/**
	 * Durchlaeuft Vektoren-Array, berechnet jeweils den aktuellen
	 * Punktdatensatz und schreibt diesen in eine Datei, falls er zu einem
	 * gefundenen Pattern gehört
	 * 
	 * @param patternIndices
	 *            HashMap die die Position als key und Laenge als value der
	 *            gefundenen Patterns enthaelt
	 */
	//TODO: Debug!
	private void printForViewerOld(HashMap<Integer, Short> patternIndices, File[] fileList) {
		System.out.println("print patterns for viewer...");
		
		StringBuffer buff = new StringBuffer();
		int ms = 0;
		short[] dataset;
		float[] pointDataset = firstLine.clone();
//		short repeatLine = 30/framerate;
		help = null;
		int startI = 0;
		
		for (int f = 0; f < fileList.length; f++) {

			ArrayList<short[]> data = initializeFileData(fileList[f]);

			for (int i = 0; i < data.size(); i++) {
				dataset = data.get(i);
				pointDataset = calculateNewPoints(pointDataset, dataset);

				if (patternIndices.containsKey(startI)) {
					float[] patternPoints = pointDataset.clone();
					int patternEnd = patternIndices.get(startI);
					for (int j = 0; j < patternEnd; j++) {

						// for (int h = 0; h < repeatLine; h++) { // um auf
						// framerate 30 zu kommen
						buff.append(ms + ";");
						for (int k = 0; k < patternPoints.length; k++) {
							buff.append(patternPoints[k] + ";");
							if ((k + 1) % 3 == 0) {
								buff.append("1;");
							}
						}
						ms += Math.round(1000 / framerate);
						buff.append("\n");
						// }

						if (i+j+1 == data.size()) { break; }
						dataset = data.get(i + j + 1);
						patternPoints = calculateNewPoints(patternPoints,
								dataset);
					}

					// am Ende einer Sequenz eine Sekunde lang Nuller
					String origin = "0;0;0;1";
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
				startI++;
			}
		}
		
		try {
			Files.write(buff.toString().getBytes(), new File(outFile));
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
