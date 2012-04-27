import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;


public class Controller {
	ArrayList<short[]> data;
	
	// Genauigkeit der Daten: eine Stelle weniger wird zu ungenau, deformiert gesamtes Skelett
	// Java-Prozessgroesse: 436,5 MB
	float faktor = 100f;
	String format = "0,00";
	
	long start;
	long ende;
	float[] firstLine;
	
	public Controller(){
		//init Data: read from files, parse, store in data
		start = System.currentTimeMillis();
		data = new ArrayList<short[]>();
		//File dir = new File("KinectData");
		File dir = new File("D:/Studium/Semester06_So12/Bachelorarbeit/KinectData");
		File[] fileList = dir.listFiles();
		int anz = fileList.length;
		int count = 0;
		int max = 0;
		short[] help = null;
		
		for (File f : fileList) { // for each file in this directory
			count++;
			System.out.println(f.getName() + " - reading " + count + "/"+ anz + "...");
			try {
				// read file content
				BufferedReader br = new BufferedReader(new FileReader(f));
				String strLine;
				String[] s;
				short[] d;
				
				while ((strLine = br.readLine()) != null) { // for each line
					s = strLine.split(";");
					d = new short[60];
					int di = 0;
					short newValue = 0;
					if(help==null){ // first line of first file
						help = new short[60];
						firstLine = new float[60];
						for (int i = 1; i < s.length; i++) {
							if(i%4==0){
								//leave out
							}else{
								help[di] = (short) (new DecimalFormat(format).parse(s[i]).floatValue() * faktor);
								//System.out.println(help[di]);
								firstLine[di] = Math.round(new DecimalFormat(format).parse(s[i]).floatValue() *faktor)/faktor;
								//System.out.println(firstLine[di]);
								//System.out.println("help " + di + ": " + s[i] + " => " + help[di]);
								di++;
							}
						}
					} else { // not first line of first file
						for (int i = 1; i < s.length; i++) { // add numbers difference to int[]; actual number: x*10^-p
							if (i%4==0) {
								// leave out
							} else {
								newValue = (short) (new DecimalFormat(format).parse(s[i]).floatValue() * faktor);
								d[di] = (short)(newValue - help[di]);
								if(d[di]>max){
									max = d[di];
								}
								//System.out.println(newValue + " - " + help[di] + " = " + d[di]);
								help[di] = newValue;
								di++;
							}
						}
					}
					data.add(d); // add new Array to data
				} // end for each line
				
				System.out.println("...reading complete");
			} catch (Exception e) {
				e.printStackTrace();
			}
		} // end for each file
		ende = System.currentTimeMillis();
		System.out.println("Dauer Einlesen: " + (ende-start));
		System.out.println("Größe von data: " + data.size() + ", Maximaler Wert: " + max);
		
		while(true){}
		
		// HashMap mit Startindex und Anzahl Frames
//		HashMap<Integer,Integer> indices = new HashMap<Integer,Integer>();
//		indices.put(3100, 120);
//		indices.put(6000, 500);
//		convertForViewer(indices);
	}
	
	
	/**
	 * Durchlaeuft Vektoren-Array, berechnet jeweils den aktuellen Punktdatensatz und schreibt diesen in eine Datei,
	 * falls er zu einem gefundenen Pattern gehört
	 * @param indices HashMap die die Position als key und Laenge als value der gefundenen Patterns enthaelt
	 */
	private void convertForViewer(HashMap<Integer,Integer> indices){
		StringBuffer buff = new StringBuffer();
		int ms = 0;
		short[] d;
		float[] l = new float[60];
		for (int i = 0; i < l.length; i++) {  // beginnend vom ersten Datensatz Punkte...
			l[i] = firstLine[i];  
		}  
		
		for (int i = 0; i < data.size(); i++) {
			d = data.get(i);
			for (int j = 0; j < l.length; j++) {    // ..die Vektoren immer aufrechnen um wieder Punkte zu erhalten
				l[j] = l[j] + ((float)d[j])/faktor;
			}
			
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
					for (int y = 0; y < l.length; y++) {
						l[y] = l[y] + ((float)d[y])/faktor;
					}
				}
				for (int k = 0; k < 30; k++) { // am Ende der Sequenz eine Sekunde lang Nuller
					buff.append("0;0;0;0;1;0;0;0;1;0;0;0;1;0;0;0;1;0;0;0;1;0;0;0;1;0;0;0;1;0;0;0;1;0;0;0;1;0;0;0;1;");
					buff.append("0;0;0;1;0;0;0;1;0;0;0;1;0;0;0;1;0;0;0;1;0;0;0;1;0;0;0;1;0;0;0;1;0;0;0;1;0;0;0;1\n");
				}  // danach wieder weiter Punkt-Datensaetze berechnen bis wieder i in indices
			}
		}
		
		String filename = "patterns.csv";
		FileWriter writer;
		try {
			writer = new FileWriter(filename, false);
			writer.write(buff.toString().replace(".", ","));
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}


}
