import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import com.mongodb.MongoClient;


public class KeywordsFromFileSampler extends KeywordSampler{

	public KeywordsFromFileSampler(ValidUser user, MongoClient mongoClient,
			String outputDirectory, String dbName, String collectionName, File file)
			throws InterruptedException {
		super(user,  outputDirectory, dbName, collectionName, mongoClient);
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
		setKeywords(keywords);
	}

}
