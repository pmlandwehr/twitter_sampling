package samplers;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import user.ValidUser;

import com.mongodb.MongoClient;


public class KeywordsFromFileSampler extends KeywordSampler{

	private ArrayList<String> getKeywordsFromFile(File file) {
		ArrayList<String> keywords = new ArrayList<String>();
		try {
			BufferedReader reader =  new BufferedReader(new FileReader(file)); 
			String line = null;
		
			while((line = reader.readLine()) != null){
				keywords.add(line.trim());
			}
			reader.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return keywords;
	}
	
	public KeywordsFromFileSampler(ValidUser user, MongoClient mongoClient,
			String outputDirectory, String dbName, String collectionName, File file)
			throws InterruptedException {
		super(user,  outputDirectory, dbName, collectionName, mongoClient);
		
		ArrayList<String> keywords = getKeywordsFromFile(file);
		setKeywords(keywords);
	}
	
	public KeywordsFromFileSampler(ValidUser user, String outputDirectory,
			String tweetFolder, File file)
			throws InterruptedException {
		super(user,  outputDirectory, tweetFolder);
		
		ArrayList<String> keywords = getKeywordsFromFile(file);
		setKeywords(keywords);
	}

}
