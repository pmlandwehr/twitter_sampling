/**
 * @author kjoseph
 * This class can be used to sample a set of keywords from Twitter
 */

package samplers;
import java.util.Arrays;
import java.util.List;

import user.ValidUser;

import com.mongodb.MongoClient;
import com.twitter.hbc.core.endpoint.StatusesFilterEndpoint;


public class KeywordSampler extends Sampler{
	
	
	public KeywordSampler(
			ValidUser user,
			String outputDirectory,
			String dbName,
			String collectionName,
			MongoClient mongoClient,
			String ... keywords) throws InterruptedException {
		
		super(user,
			mongoClient,
			outputDirectory,
			dbName,
			collectionName);
		
		setKeywords(Arrays.asList(keywords));
	}
	
	public void setKeywords(List<String> keywords) throws InterruptedException{
		StatusesFilterEndpoint endpoint = new StatusesFilterEndpoint();
		// add some track terms
		endpoint.trackTerms(keywords);
		setEndpoint(endpoint);
	}
}
