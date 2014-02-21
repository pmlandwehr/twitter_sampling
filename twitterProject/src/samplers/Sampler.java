/**
 * @author kjoseph
 * This is the base class for all of the samplers. 
 * Tweets are put into a mongodb or a directory
 * Also generated is a directory of files that contains three .csvs
 * The first, *_tweets.csv, just keeps track of the ids and times of the sampled tweets
 * Second, *_captured.csv keeps track of how your keywords have been rate-limited
 * Finally, *_stats.csv is written out at the end of the sampling period to assess reconnection stats
 */

package samplers;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import user.ValidUser;

import com.google.common.io.Files;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.util.JSON;
import com.twitter.hbc.ClientBuilder;
import com.twitter.hbc.core.Constants;
import com.twitter.hbc.core.Constants.FilterLevel;
import com.twitter.hbc.core.endpoint.DefaultStreamingEndpoint;
import com.twitter.hbc.core.processor.StringDelimitedProcessor;
import com.twitter.hbc.httpclient.BasicClient;
import com.twitter.hbc.httpclient.auth.Authentication;
import com.twitter.hbc.httpclient.auth.OAuth1;



public class Sampler implements Runnable{

	protected String outputDirectory;
	protected boolean useMongo;
	protected File tweetFolder;
	protected File curOutputFile;
	protected DB db;
	protected ValidUser user;
	protected Thread runThread;
	protected BufferedWriter tweetWriter;
	//Make these file writers so that they immediately write
	protected FileWriter capturedWriter;
	protected FileWriter statsWriter;
	protected boolean stop = false;
	protected DBCollection collection;
	protected DefaultStreamingEndpoint endpoint;
	protected Calendar cal;
	
	/**
	 * Helper function for initializing directories.
	 * 
	 * @param outputDirectory the path to the directory to create
	 * @return true if the directory is created, false otherwise
	 */
	public boolean initializeDirectory(File outputDirectory) {
		if (outputDirectory.exists()) { return true; }	
		System.out.println("creating directory: " + outputDirectory.getAbsolutePath());
		boolean result = outputDirectory.mkdir();  
		if(result) { System.out.println(" Success!");}
		else       { System.out.println(" Failure"); }
		return result;
	}
	
	public boolean setNewCurOutputFile() {
		String outfilename = this.cal.getTime().toString().replace(" ","_")+".txt";
		try {
			Files.touch(new File(tweetFolder,outfilename));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		this.curOutputFile = new File(tweetFolder,outfilename);
		return true;
		
	}

	public Sampler(ValidUser user, 
				  MongoClient mongoClient,
				  String outputDirectory,
				  String dbName,
				  String collectionName){
		
		this.outputDirectory = outputDirectory;
		this.user = user;
		this.db = mongoClient.getDB(dbName);
		this.useMongo = true;
		System.out.println("Putting into DB: " + dbName + " in collection: " + collectionName);
		this.collection = db.getCollection(collectionName);
		this.runThread = new Thread(this, user.name);
		
		initializeDirectory(new File(outputDirectory));
		try {
			System.out.println("CREATING FILES");
			tweetWriter = new BufferedWriter(new FileWriter(outputDirectory+collectionName+"_tweets.csv"));
			capturedWriter = new FileWriter(outputDirectory+collectionName+"_captured.csv");
			statsWriter = new FileWriter(outputDirectory+collectionName+"_stats.csv");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public Sampler(ValidUser user, 
			  String outputDirectory,
			  String tweetFolder) {
	
		this.outputDirectory = outputDirectory;
		this.user = user;
		this.tweetFolder = new File(outputDirectory,tweetFolder);
		this.useMongo = false;
		System.out.println("Putting into Folder: " + tweetFolder);
		this.runThread = new Thread(this, user.name);
		
		initializeDirectory(new File(outputDirectory));
		initializeDirectory(this.tweetFolder);
		
		try {
			System.out.println("CREATING FILES");
			tweetWriter = new BufferedWriter(new FileWriter(outputDirectory+"default_tweets.csv"));
			capturedWriter = new FileWriter(outputDirectory+"default_captured.csv");
			statsWriter = new FileWriter(outputDirectory+"default_stats.csv");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		setNewCurOutputFile();
	
	}
	
	public void setEndpoint(DefaultStreamingEndpoint endpoint) throws InterruptedException{
		this.endpoint = endpoint;

		endpoint.stallWarnings(false);
		endpoint = endpoint.filterLevel(FilterLevel.None);

	}
	
	public void start(){
		runThread.start();
	}
	public void run() {
		System.out.println("Thread: " + runThread + " running");
		BlockingQueue<String> queue = new LinkedBlockingQueue<String>(10000);
		
		
		Authentication auth = new OAuth1(user.consumerKey, 
				user.consumerSecret, 
				user.accessToken, 
				user.accessTokenSecret);
		
		// Create a new BasicClient. By default gzip is enabled.
		BasicClient client = new ClientBuilder()
		.hosts(Constants.STREAM_HOST)
		.endpoint(endpoint)
		.authentication(auth)
		.processor(new StringDelimitedProcessor(queue))
		.build();
	
		System.out.println(client.getEndpoint().getPostParamString());
		 
		
		// Establish a connection
		client.connect();
	
		double numCaptured = 0;
		double lastTotal = 0;
		double lastTime = System.currentTimeMillis();
		while(!client.isDone() && !stop) {
			String msg=null;
			//Get tweet
			try {
				msg = queue.poll(5, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if(msg == null){
				continue;
			}
			JsonObject tweet = new JsonParser().parse(msg).getAsJsonObject();
			if(tweet.has("limit")){
				double time = System.currentTimeMillis();
				double total = tweet.get("limit").getAsJsonObject().get("track").getAsDouble();
				try {
					capturedWriter.append(numCaptured+","+(total-lastTotal)+","+(time-lastTime)+"\n");
				} catch (IOException e) {
					e.printStackTrace();
				}
				if (!useMongo) {
					setNewCurOutputFile();
				}
				//System.out.println(user.name + " rate limited");
				lastTime=time;
				lastTotal=total;
				numCaptured=0;
			} 
			else if(tweet.has("created_at")) {
				numCaptured++;
				try {
					tweetWriter.append(tweet.get("id_str").getAsString()+","+String.valueOf(numCaptured)+","
							+tweet.get("created_at").getAsString()+"\n");
					if (useMongo) {
						collection.insert((DBObject) JSON.parse(msg));
					}
					else {
						Files.append(JSON.parse(msg)+"\n", this.curOutputFile, StandardCharsets.UTF_8);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
	
		}
		//System.out.println(client.getExitEvent().getMessage());
		try {
			statsWriter.append(user.name + "," +
					String.valueOf(client.getStatsTracker().getNum200s()) + "," + 
					String.valueOf(client.getStatsTracker().getNum400s()) + "," + 
					String.valueOf(client.getStatsTracker().getNum500s()) + "," + 
					String.valueOf(client.getStatsTracker().getNumClientEventsDropped()) + "," + 
					String.valueOf(client.getStatsTracker().getNumConnectionFailures()) + "," + 
					String.valueOf(client.getStatsTracker().getNumConnects()) + "," + 
					String.valueOf(client.getStatsTracker().getNumDisconnects()) + "," + 
					String.valueOf(client.getStatsTracker().getNumMessagesDropped()) + "," + 
					String.valueOf(client.getStatsTracker().getNumMessages()) + "\n");
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		client.stop();
		try {
			tweetWriter.close();
			capturedWriter.close();
			statsWriter.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	public void stop() {
		stop = true;
	}
	
}
