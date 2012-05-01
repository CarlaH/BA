import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;


public class Controller {
	
	final ArrayList<short[]> data = new ArrayList<short[]>();;
	final float[] firstLine = new float[60];
	
	final String outFile = "patterns.csv";
	final String inputDir = "D:/Studium/Semester06_So12/Bachelorarbeit/KinectData";
	//final String inputDir = "KinectData";
	
	// Genauigkeit der Daten: eine Stelle weniger wird zu ungenau, deformiert gesamtes Skelett
	// Java-Prozessgroesse: 436,5 MB
	final float faktor = 100f;
	final String format = "0,00";
	DecimalFormat dcf = new DecimalFormat(format);
	
	long start;
	long ende;
	int max;
	
	public Controller(){
		//read from files, parse, store in data
		start = System.currentTimeMillis();
		File dir = new File(inputDir);
		File[] fileList = dir.listFiles();
		int anz = fileList.length;
		int count = 0;
		short[] help = null;
		
		for (File f : fileList) { // for each file in this directory
			count++;
			System.out.println(f.getName() + " - reading " + count + "/"+ anz + "...");
			try {
				// read file content
				BufferedReader br = new BufferedReader(new FileReader(f));
				String strLine;
				boolean init;
				
				while ((strLine = br.readLine()) != null) { // for each line
					init = (help == null) ? true : false;
					short[][] result = processLine(strLine, help, init); 
					short[] d = result[0];
					if (!init){
						data.add(d); // add new Array to data						
					}
					help = result[1];
				}
				
				System.out.println("...reading complete");
			} catch (Exception e) {
				e.printStackTrace();
			}
		} // end for each file
		
		ende = System.currentTimeMillis();
		System.out.println("Dauer Einlesen: " + (ende-start));
		System.out.println("Größe von data: " + data.size() + ", Maximaler Wert: " + max);
		
		// HashMap mit Startindex und Anzahl Frames
		HashMap<Integer,Integer> indices = new HashMap<Integer,Integer>();
		indices.put(3100, 120);
		indices.put(6000, 500);
		convertForViewer(indices);
	}
	
	/**
	 * For one line of a file parses the content and rounds the values
	 * @param strLine Line
	 * @param help help-Array containing the values of the line before
	 * @param init whether it's the first line of the first file or not
	 * @return short[] containing the values
	 * @throws ParseException
	 */
	private short[][] processLine(String strLine, short[] help, boolean init) throws ParseException {
		String[] s = strLine.split(";");
		int di = 0;
		short newValue = 0;
		short[] d = new short[60];
		short[] h = new short[60];
		
		for(int i = 1; i<s.length; i++){
			
			if (i % 4 == 0) continue;  // Koordinate W	
			
			if(init){ // first line of first file, init help and firstLine	
				firstLine[di] = Math.round(dcf.parse(s[i]).floatValue() *faktor)/faktor;
				h[di] = (short) (dcf.parse(s[i]).floatValue() * faktor);
				//System.out.println(firstLine[di]);
				//System.out.println("help " + di + ": " + s[i] + " => " + d[di]);
				
			}else{ // add numbers difference to short[]; actual number: x*10^-p
				newValue = (short) (dcf.parse(s[i]).floatValue() * faktor);
				if (Math.abs(newValue - help[di]) > Short.MAX_VALUE) {
					System.out.println(">>> value too big: " + (newValue-help[di]));
					}
				d[di] = (short)(newValue - help[di]);
				if(d[di]>max){
					max = d[di];
				}
				h[di] = newValue;
			}
			
			di++;	
		}
		
		short[][] result = {d, h};
		return result;
	}
	
	/**
	 * Durchlaeuft Vektoren-Array, berechnet jeweils den aktuellen 
	 * Punktdatensatz und schreibt diesen in eine Datei,
	 * falls er zu einem gefundenen Pattern gehört
	 * @param indices HashMap die die Position als key und Laenge 
	 * als value der gefundenen Patterns enthaelt
	 */
	private void convertForViewer(HashMap<Integer,Integer> indices){
		StringBuffer buff = new StringBuffer();
		int ms = 0;
		short[] d;
		float[] l = firstLine;  // beginnend vom ersten Datensatz Punkte... 
		
		for (int i = 0; i < data.size(); i++) {
			d = data.get(i);
			l = calculatePoint(l, d);
			
			if(indices.containsKey(i)){
				int end = indices.get(i);
				for (int j = 0; j < end; j++) {
					buff.append(ms+";");
					for (int k = 0; k < l.length; k++) {
						buff.append(l[k] + ";");
						if((k+1)%3 == 0){
							buff.append("1;");
						}
					}
					ms += 33;
					buff.append("\n");
					
					i++;  // i weiter hochzaehlen und naechsten Datensatz berechnen
					d = data.get(i);
					calculatePoint(l, d);
				}
				for (int k = 0; k < 30; k++) { // am Ende der Sequenz eine Sekunde lang Nuller
					buff.append("0;0;0;0;1;0;0;0;1;0;0;0;1;0;0;0;1;0;0;0;1;0;0;0;1;0;0;0;1;0;0;0;1;0;0;0;1;0;0;0;1;");
					buff.append("0;0;0;1;0;0;0;1;0;0;0;1;0;0;0;1;0;0;0;1;0;0;0;1;0;0;0;1;0;0;0;1;0;0;0;1;0;0;0;1\n");
				}  // danach wieder weiter Punkt-Datensaetze berechnen bis wieder i in indices
			}
		}
		
		FileWriter writer;
		try {
			writer = new FileWriter(outFile, false);
			writer.write(buff.toString().replace(".", ","));
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	/**
	 * Calculates the new Joints and stores them in l
	 * @param l old Joints
	 * @param d vectors
	 */
	private float[] calculatePoint(float[] l, short[] d){
		for (int j = 0; j < l.length; j++) {    // ..die Vektoren immer aufrechnen um wieder Punkte zu erhalten
			l[j] = l[j] + ((float)d[j])/faktor;
		}
		return l;
	}


}
