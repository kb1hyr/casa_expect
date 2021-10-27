import java.io.IOException;
import org.apache.commons.net.telnet.TelnetClient;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.ArrayList;


/**
This is a utility program that reads a copied version of the DHCP cable modem provision file lists, logs into the CMTS
and gets the make and model of each one, and puts the result in a tab-delimited CSV file.  (Tabs are needed instead of commas
because the make field contains commas.)
<br>
This is a sort of quick and dirty utility to get the job done.  For more general application it would need more error checking
and maybe even a nice gui instead of requiring the user to replace all the variables at the top of the program.


@author Brett Markham
@version 1.1
*/
public class App 
{
	static String csvFilesDir = ""; // replace with location where you want the output csv file written
	static String csvFile = "cc.csv"; // replace with the name you want for your csv file
	static String ipAddr = ""; // replace with IP of CMTS
	static String userName = ""; // replace with CMTS username
	static String passWord = ""; // replace with CMTS password
	static String firstPrompt = ""; // replace with login prompt
	static String cmdPrompt = ""; // replace with the usual prompt
	static String nomsFilesDir = ""; // replace with location of files containing cable modem MACs
	static ArrayList<String> nomsMacs = new ArrayList<String>();
	static ArrayList<String> csvLines = new ArrayList<String>();
	
	
    public static void main( String[] args )
    {
    	ArrayList<String> modemfiles = getModemFiles(nomsFilesDir);
    	
    	
    	for (int i = 0; i < modemfiles.size(); i++) {
			getNomsMacs(nomsFilesDir+modemfiles.get(i));
		}
    	
    	// Setup the telnet client
    	TelnetClient telnet = new TelnetClient();
    	
    	try {
    		telnet.connect(ipAddr);
    	} catch(IOException e) {
    		System.out.println("An error occurred.");
  	      	e.printStackTrace();
    	}
    	
    	Expect expect = new Expect(telnet.getInputStream(),
    			telnet.getOutputStream());
    	// Log in to the CMTS
    	expect.expect(firstPrompt);
    	System.out.println(expect.before + expect.match);
    	expect.send(userName+"\n");
    	expect.expect("Password:");
    	System.out.println(expect.before + expect.match);
    	expect.send(passWord+"\n");
    	expect.expect(cmdPrompt);
    	
    	
    	// Query all the macs.  First see if online, if they are, capture verbose data
    
    	for (String mac : nomsMacs) {
    		
    		expect.send("show cable modem "+mac+"\n");
    		expect.expect(cmdPrompt);
    		
    		if (expect.before.contains("online cm 1")) {
        		expect.send("show cable modem "+mac+" verbose | include sysDescr\n");
        		expect.expect(cmdPrompt);
        		if (expect.before.contains(";")) {
        			System.out.println(mac + "\t" + getVendor(expect.before) + "\t" + getModel(expect.before));
        			csvLines.add(mac + "\t" + getVendor(expect.before) + "\t" + getModel(expect.before) );
        		}
        		else {
        			System.out.println(mac + "\t" + "NA" + "\t" + "NA");
        			csvLines.add(mac + "\t" + "NA" + "\t" + "NA" );
        		}
        	}
    	}
    	
    	expect.send("exit\n");
    	expect.close();
        
    	try {
    		telnet.disconnect();
    	} catch(IOException e) {
    		System.out.println("An error occurred.");
  	      	e.printStackTrace();
    	} 
    	
    	printCsvOne(csvLines);
    	System.out.println("Done!");
        
    }
    
  
    
    /**
	 * Checks for the files being valid, i.e. long enough to contain valid data, 
	 * not a lock file or backup file, not a directory etc.  Then adds them to the list 
	 * of files containing valid modem mac addresses. 
	 * 
	 * @param directory Location of the files from the provisioning system
	 * @return An ArrayList of Strings, each of which contains the name of a cable modem file
	 */
	public static ArrayList<String> getModemFiles(String directory) {
		File directoryPath = new File(directory);
		
		File filesList[] = directoryPath.listFiles();
		ArrayList<String> modemfiles = new ArrayList<String>();
				
		for (File file : filesList) {
			if (!file.isDirectory() && 
					(file.length() > 60) &&
					(!file.getName().contains("'")) &&
					(!file.getName().contains("lock")) &&
					(!file.getName().contains("UnprovCM"))) {
				modemfiles.add(file.getName());
			}
		}
				
		return modemfiles;
	}
    
	/**
	 * 
	 * Goes through each noms modem file and puts all the macs in nomsMacs
	 * 
	 * @param filename Name of the file containing cable modem provisioning
	 */
	public static void getNomsMacs(String filename) {
		FileInputStream stream = null;
        try {
            stream = new FileInputStream(filename);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        String strLine;
        String lastWord="";
        
        try {
            while ((strLine = reader.readLine()) != null) {
            	if (strLine.contains("hardware ethernet")) {
            		int spot = strLine.indexOf("hardware ethernet");
            		String rawMac = strLine.substring(spot+18, strLine.length()-1);
            		lastWord = cleanMac(rawMac);
            		if (lastWord.length() == 14) nomsMacs.add(lastWord);
                    lastWord="";
            	}                
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
    }
	
  /**
  Gets the vendor/manufacturer field from the text provided.
  
  @param before This string contains the most recent text that Expect received before the expected prompt
  @return A string containing the vendor as reported by the modem to the cmts
  
  */
	public static String getVendor(String before) {
		String vendor = "NA";
		String tokens[] = before.split(";");
		for (int i = 0; i < tokens.length; i++) {
			if (tokens[i].contains("VENDOR")) {
				vendor = tokens[i].substring(8);
				break;
			}
		}
		
		return vendor;
	}
	
  /**
  Gets the modem model number from the text provided
  
  @param before This string contains the most recent text that Expect received before the expected prompt
  @return A string containing the model as reported by the modem to the cmts
  */
	public static String getModel(String before) {
		String model = "NA";
		String tokens[] = before.split(";");
		for (int i = 0; i < tokens.length; i++) {
			if (tokens[i].contains("MODEL")) {
				model = tokens[i].substring(8);
				break;
			}
		}
		
		if (model.contains(">")) {
			model = model.substring(0, model.indexOf(">"));
		}
		
		return model;
	}

  /**
  This method takes the MAC address as it exists in a DHCP file and converts it to 
  the form expected by the CMTS.  
  
  @param dirtyMac In DHCP, MACs are in the form AA:BB:CC:DD:EE:FF
  @return A MAC in the form aabb.ccdd.eeff
   
  */
	public static String cleanMac(String dirtyMac) {
		String[] tokens = dirtyMac.split(":");
		String completed="";
		String cmts="";
		for (int i = 0; i < tokens.length; i++) {
			completed = completed+tokens[i];
		}
		completed=completed.toLowerCase();
		// format for the cmts to understand
		if (completed.length() == 12) {
		cmts = completed.substring(0, 4) + "." +
				completed.substring(4, 8) + "." +
				completed.substring(8, 12);
		}
		else {
			cmts = completed;
		}
		return cmts;
	}	
	
	/**
  Writes out the csv file.
  
  @param csv An arraylist containing the lines to be written out
  
  */
	public static void printCsvOne(ArrayList<String> csv ) {
		try {
			System.out.println("Creating the file ...");
		      File myObj = new File(csvFilesDir+csvFile);
		      if (myObj.createNewFile()) {
		    	  try {
		    	      FileWriter myWriter = new FileWriter(csvFilesDir+csvFile);
		    	      for (String theLine : csv) {
		    	    	  myWriter.write(theLine);
		    	    	  myWriter.write("\n");
		    	      }
		    	      myWriter.close();
		    	    } catch (IOException e) {
		    	      System.out.println("An error occurred.");
		    	      e.printStackTrace();
		    	    }  
		        
		      } else {
		        System.out.println("File already exists.");
		      }
		    } catch (IOException e) {
		      System.out.println("An error occurred.");
		      e.printStackTrace();
		    }
		
	}
	
	
    
}
