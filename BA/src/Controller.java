import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;


public class Controller {
	
	final File outFile = new File("patterns.csv");
	final File inputDir = new File("D:/Studium/Semester06_So12/Bachelorarbeit/KinectData");
	
	// Genauigkeit der Daten:
	final float faktor = 100f;
	final String format = "0,00";
	final DecimalFormat decf = new DecimalFormat(format);

	
	final float[] firstLine = new float[60];
	final ArrayList<short[]> data = new ArrayList<short[]>();

	public Controller() {
		
		//init Data: read from files, parse, store in data
		final long start = System.currentTimeMillis();
			
		File[] fileList = inputDir.listFiles();
		
		int count = 0;
		for (File f : fileList) { 
			count++;
			System.out.println(f.getName() + " - reading " + count + "/" + 
					fileList.length + "...");
			
			try {
				List<String> lines = Files.readLines(f, Charset.forName("UTF-8"));
				
				if (lines.isEmpty()) continue;
				
				short[] prev = null;
				for (String strLine : lines) {
					short[] d = processLine(strLine, prev);
					data.add(d);
					prev = d;
				}
			} catch (Exception e1) {
				e1.printStackTrace();
			} finally {
				System.out.println("...reading complete");
			}
		}
		
		final long ende = System.currentTimeMillis();
		System.out.println("Dauer Einlesen: " + (ende - start));
		System.out.println("Größe von data: " + data.size());

		// HashMap mit Startindex und Anzahl Frames
		final ImmutableMap<Integer,Integer> indices = 
				new ImmutableMap.Builder<Integer, Integer>()
					.put(3100, 120)
					.put(6000, 500)
					.build();
		
		convertForViewer(indices, data);
	}

	private short[] processLine(final String strLine, final short[] prev) throws ParseException 
	{	
		String[] s = strLine.split(";");
		short[] d = new short[60];
		int di = 0;

		for (int i = 1; i < s.length; i++) {
			
			if (i % 4 == 0) continue;
			
			short newValue = (short) (decf.parse(s[i]).floatValue() * faktor);
			
			if (prev == null) {
				firstLine[di] = Math.round(decf.parse(s[i]).floatValue() * faktor) / faktor;
				d[di] = newValue;
			} else {
				
				if (Math.abs(newValue - prev[di]) > Short.MAX_VALUE) {
					System.err.println("unsafe short casting");
					System.exit(1);
				}
				
				d[di] = (short) (newValue - prev[di]);				
			}
			
			di++;
		}
		return d;
	}

	
	private void updateCurrentPoint(float[] l, final short[] diff) {
		for (int j = 0; j < l.length; j++) {
			l[j] = l[j] + ((float)diff[j]) / faktor;
		}
	}

	/**
	 * Durchlaeuft Vektoren-Array, berechnet jeweils den aktuellen Punktdatensatz 
	 * und schreibt diesen in eine Datei, falls er zu einem gefundenen Pattern gehört
	 * @param indices HashMap die die Position als key und Laenge als value 
	 * der gefundenen Patterns enthaelt
	 * @param data2 
	 */
	private void convertForViewer(final ImmutableMap<Integer,Integer> indices, final ArrayList<short[]> data) 
	{
		StringBuffer buff = new StringBuffer();
		int ms = 0;
		short[] d;
		float[] l = firstLine;
		
		for (int i = 0; i < data.size(); i++) {
			
			d = data.get(i);
			// ..die Vektoren immer aufrechnen um wieder Punkte zu erhalten
			updateCurrentPoint(l, d);

			if (indices.containsKey(i)) {
				int sequenceLength = indices.get(i);
								
				for (int j = 0; j < sequenceLength; j++) {
					buff.append(ms + ";");
					for (int k = 0; k < l.length; k++) {
						buff.append(l[k] + ";");
						if ((k + 1) % 3 == 0) {
							buff.append("1;");
						}
					}
					buff.append("\n");
					
					ms += 33;
					i++; 					
					
					if (i >= data.size()) break;
					
					d = data.get(i);
					updateCurrentPoint(l, d);
				}
				
				String origin = "0;0;0;1";
				int numFrames = 30; 
				int numJoints = 19;
				
				String[] originPoints = new String[numFrames * numJoints]; 
				Arrays.fill(originPoints, origin);
				
				String originPointsString = Joiner.on(";").join(originPoints);
				
				buff.append("0;");
				buff.append(originPointsString);
				buff.append("\n");
			}
		}

		try {
			Files.write(buff.toString().replace(".", ",").getBytes(), outFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}