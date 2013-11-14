import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

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
	protected DB db;
	protected ValidUser user;
	protected Thread runThread;
	protected BufferedWriter tweetWriter;
	protected BufferedWriter capturedWriter;
	protected BufferedWriter statsWriter;
	protected boolean stop = false;
	protected DBCollection collection;
	protected DefaultStreamingEndpoint endpoint;
	
	public Sampler(ValidUser user, 
				  MongoClient mongoClient,
				  String outputDirectory,
				  String collectionName){
		
		this.outputDirectory = outputDirectory;
		this.user = user;
		this.db = mongoClient.getDB(user.name+ "_pml");
		this.collection = db.getCollection(collectionName);
		this.runThread = new Thread(this, user.name);

		File theDir = new File(outputDirectory);
		if (!theDir.exists()) {
			System.out.println("creating directory: " + outputDirectory);
			boolean result = theDir.mkdir();  
			if(result) {    
				System.out.println("DIR created");  
			}
		}
		try {
			tweetWriter = new BufferedWriter(new FileWriter(outputDirectory+"tweets.csv"));
			capturedWriter = new BufferedWriter(new FileWriter(outputDirectory+"captured.csv"));
			statsWriter = new BufferedWriter(new FileWriter(outputDirectory+"stats.csv"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
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
					System.out.println("NEED A STACK TRACE");
					// TODO Auto-generated catch block
					e.printStackTrace();
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
					collection.insert((DBObject) JSON.parse(msg));
				} catch (IOException e) {
					// TODO Auto-generated catch block
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
