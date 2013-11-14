import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;

import com.mongodb.MongoClient;


public class main {

	
	
	public static void main(String args[]) throws InterruptedException, UnknownHostException{
		String configFile = args[0];
		String outputDirectory = args[1];
		long timeToRunFor = Long.valueOf(args[2])*60*1000;
		System.out.println("Running w/ config: "+ configFile + ", outputDir: "+ outputDirectory +", for "+args[2] + " minutes");
		//TODO: make this more commandline/interface friendly

		ArrayList<ValidUser> users = null;
		try {
			users = ValidUser.getUsersFromConfig(configFile);
		} catch (IOException e) {
			e.printStackTrace();
		}

		MongoClient mongoClient = new MongoClient();
		ArrayList<Sampler> samplers = new ArrayList<Sampler>();
		samplers.add(new KeywordSampler(users.get(0),mongoClient,outputDirectory+ "_yolo/", "yolandaPH"));
		samplers.add(new LocationSampler(users.get(1), mongoClient, outputDirectory +"_phil/",116.147463,4.710566,127.836916,21.15944));
		samplers.add(new LocationSampler(users.get(2), mongoClient, outputDirectory+"_nam/",101.997074,7.942276,109.028324,23.032347));
		samplers.add(new KeywordSampler(users.get(3),mongoClient,outputDirectory+ "_haiyan/","Haiyan", "Quang Ngai", "Quang Tri", "Dong Ha"));
		
		for(Sampler s:samplers){
			s.start();
		}
		Thread.sleep(timeToRunFor);
		for(Sampler s:samplers){
			s.stop();
		}
	}
}
