import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.DecimalFormat;
import java.util.ArrayList;


public class Controller {
	ArrayList<int[]> data;
	
	public Controller(){
		//init Data: read from files, parse, store in data
		data = new ArrayList<int[]>();
		File dir = new File("KinectData");
		File[] fileList = dir.listFiles();
		int max = 0;
		
		for (File f : fileList) { // for each file in this directory
			System.out.println(f.getName() + " - reading...");
			try {
				// read file content
				BufferedReader br = new BufferedReader(new FileReader(f));
				String strLine;
				String[] s;
				int[] d;
				int[] help = null;
				
				while ((strLine = br.readLine()) != null) { // for each line
					s = strLine.split(";");
					d = new int[60];
					int di = 0;
					int newValue = 0;
					if(help==null){ // first line of file
						help = new int[60];
						for (int i = 1; i < s.length; i++) {
							if(i%4==0){
								//leave out
								//System.out.println("help leave out");
							}else{
								help[di] = (int) (new DecimalFormat("0,000000").parse(s[i]).floatValue() * 1000000f);
								//System.out.println("help " + di + " " + help[di]);
								di++;
							}
						}
						//System.out.println("help initialized");
					} else { // not first line of file
						//System.out.println("next line");
						for (int i = 1; i < s.length; i++) { // add numbers difference to int[]; actual number: x*10^-6
							if (i%4==0) {
								// leave out
							} else {
								newValue = (int) (new DecimalFormat("0,000000").parse(s[i]).floatValue() * 1000000f);
								d[di] = newValue - help[di];
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
		System.out.println("Größe von data: " + data.size());
	}


}
