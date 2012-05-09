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

import com.google.common.io.Files;

public class Controller {

	final String outFile = "patterns.csv";
	//final String inputDir = "D:/Studium/Semester06_So12/Bachelorarbeit/KinectData";
	final String inputDir = "KinectData";

	// Genauigkeit der Daten
	final float faktor = 100f;
	final String format = "0,00";
	final DecimalFormat dcf = new DecimalFormat(format);
	
	final short minFrames = 15;
	final short maxFrames = 16;

	final ArrayList<short[]> data = new ArrayList<short[]>();
	final float[] firstLine = new float[60];
	private List<PatternInfo> storedMoves;

	// TODO to be removed soon
	int foundCounter = 0;

	
	
	public Controller() {

		initializeData();

		HashMap<Integer, Short> indices = searchPatterns();
		System.out.println("Moves " + foundCounter + " mal wieder erkannt");
		
		printForViewer(indices);
	}

	
	
	/**
	 * goes through all files in the directory and store the values in data
	 */
	private void initializeData() {
		final long start = System.currentTimeMillis();

		File dir = new File(inputDir);
		File[] fileList = dir.listFiles();

		int amountOfFiles = fileList.length;
		int fileCounter = 0;
		short[] help = null;

		for (File file : fileList) {
			fileCounter++;
			System.out.println(file.getName() + " - reading " + fileCounter	+ "/" + amountOfFiles + "...");
			try {
				// read file content
				BufferedReader br = new BufferedReader(new FileReader(file));
				String strLine;
				boolean initFirst;

				while ((strLine = br.readLine()) != null) {
					initFirst = (help == null) ? true : false;
					short[][] result = processLine(strLine, help, initFirst);
					short[] dataset = result[0];
					if (!initFirst) {
						data.add(dataset); // add new Array to data
					}
					help = result[1];
				}

				System.out.println("...reading complete");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		final long end = System.currentTimeMillis();
		System.out.println("Dauer Einlesen: " + (end - start));
		System.out.println("Größe von data: " + data.size());
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
				// System.out.println("help " + di + ": " + s[i] + " => " +
				// d[di]);

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
	 * searches in data for patterns of lengths between minFrames and maxFrames
	 */
	private HashMap<Integer, Short> searchPatterns() {
		storedMoves = new LinkedList<PatternInfo>();
		
		List<short[]> suggestedPattern;
		
		int size = data.size();
		
		for (int startI = 0; startI < size-maxFrames; startI++) {	
			System.out.println(startI+"/"+size);
			
			for(short len=maxFrames; len>=minFrames; len--){  // for each Framelength differen storedMoves?
				suggestedPattern = data.subList(startI, startI+len);
				boolean isOldPattern = addPattern(suggestedPattern, startI, len);
				// break inner loop if pattern was found?
			}
			
		}
		
		return buildPatternHashMap();
		
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
			List<short[]> move = data.subList(patInfo.getStartIndex(), patInfo.getStartIndex()+ patInfo.getLength());
			
//			if(move.equals(suggestedPattern)){  //TODO mehr toleranz
//				found = true; break;
//			}
			if(movesAreAlike(move, suggestedPattern)){
				patInfo.augmentCounter();
				foundCounter++;
				return true;
			}
			
		}

		int[] newMove = {startI, len, 1};
		storedMoves.add(new PatternInfo(startI, len, 1));
		return false;
		
	}
	
	private boolean movesAreAlike(List<short[]> moveOne, List<short[]> moveTwo) {
		if (moveOne.size() != moveTwo.size()){  // shouldn't occur anymore
			return false;
		}
		
		int minMatches = 20*3;
		short[] arrayOne; short[] arrayTwo; int matches;
		
		for (int i = 0; i < moveOne.size(); i++) {
			arrayOne = moveOne.get(i);
			arrayTwo = moveTwo.get(i);
			matches = 0;
			for (int j = 0; j < arrayOne.length; j++) {
				if (arrayOne[j]==arrayTwo[j]) { matches++; }
			}
			if (matches < minMatches) { minMatches = matches; }
		}
		if (minMatches >= 13*3) {  // seven means i.e.: both arms or both legs
			return true;
		} else {
			return false;
		}
	}
	
	private HashMap<Integer, Short> buildPatternHashMap() {
		HashMap<Integer, Short> indices = new HashMap<Integer, Short>();
	
		for (PatternInfo moveInfos : storedMoves) {
			if(moveInfos.getCounter()>1){
				indices.put(moveInfos.getStartIndex(), moveInfos.getLength());
			}
		}			
		
//		// Fake-Patterns: HashMap mit Startindex und Anzahl Frames
//		HashMap<Integer, Integer> indices = new HashMap<Integer, Integer>();
//		indices.put(3100, 120);
//		indices.put(6000, 500);
		
		return indices;
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
	private void printForViewer(HashMap<Integer, Short> patternIndices) {
		StringBuffer buff = new StringBuffer();
		int ms = 0;
		short[] dataset;
		float[] pointDataset = firstLine.clone();

		for (int i = 0; i < data.size(); i++) {
			dataset = data.get(i);
			pointDataset = calculateNewPoints(pointDataset, dataset);

			if (patternIndices.containsKey(i)) {
				float[] patternPoints = pointDataset.clone();
				int patternEnd = patternIndices.get(i);
				for (int j = 0; j < patternEnd; j++) {
					buff.append(ms + ";");
					for (int k = 0; k < patternPoints.length; k++) {
						buff.append(patternPoints[k] + ";");
						if ((k + 1) % 3 == 0) {
							buff.append("1;");
						}
					}
					ms += 33;
					buff.append("\n");

					dataset = data.get(i + j + 1);
					patternPoints = calculateNewPoints(patternPoints, dataset);
				}
				
				// am Ende einer Sequenz eine Sekunde lang Nuller
				String origin = "0;0;0;1";
				int numFrames = 30;
				int numJoints = 20;
				
				String originLine = "0;";
				for (int j = 0; j < numJoints-1; j++) {
					originLine += origin+";";
				}
				originLine += origin+"\n";
				
				for (int j = 0; j < numFrames; j++) {
					buff.append(originLine);
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
