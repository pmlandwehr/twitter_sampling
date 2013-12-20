import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;

import com.mongodb.MongoClient;


public class main {

	
	
	public static void main(String args[]) throws InterruptedException, UnknownHostException{
		//TODO: make this more commandline/interface friendly
		String configFile = args[0];
		String outputDirectory = args[1];
		String keywordFilesDirectory = args[2];
		long timeToRunFor = Long.valueOf(args[3])*60*1000;
		System.out.println("Running w/ config: "+ configFile + ", outputDir: "+ outputDirectory +", for "+args[2] + " minutes");
		
		ArrayList<ValidUser> users = null;
		try {
			users = ValidUser.getUsersFromConfig(configFile);
		} catch (IOException e) {
			e.printStackTrace();
		}

		MongoClient mongoClient = new MongoClient();
		ArrayList<Sampler> samplers = new ArrayList<Sampler>();

		File folder = new File(keywordFilesDirectory);
		File[] listOfFiles = folder.listFiles(); 
		for (int i = 0; i < listOfFiles.length; i++){
			if (listOfFiles[i].isFile() & users.size() > 0  & listOfFiles[i].getName().charAt(0)!= '.'){
				System.out.println(listOfFiles[i].getName());
				samplers.add(new KeywordsFromFileSampler(users.get(0), 
														 mongoClient, 
														 outputDirectory,
														 "sports",
														 listOfFiles[i].getName(),
														 listOfFiles[i]));
				users.remove(0);
			} else {
				System.out.println("skipping: " + listOfFiles[i].getName());
			}
		  }
		
		for(Sampler s:samplers){
			s.start();
		}
		Thread.sleep(timeToRunFor);
		for(Sampler s:samplers){
			s.stop();
		}
	}
}
