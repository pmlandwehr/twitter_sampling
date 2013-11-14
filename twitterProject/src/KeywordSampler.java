import java.util.ArrayList;
import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;

import com.mongodb.MongoClient;
import com.twitter.hbc.core.endpoint.StatusesFilterEndpoint;


public class KeywordSampler extends Sampler{
	
	
	public KeywordSampler(
			ValidUser user, 
			MongoClient mongoClient,
			String outputDirectory,
			String ... keywords) throws InterruptedException {
		
		super(user,
			mongoClient,
			outputDirectory,
			keywords[0]);
		
		StatusesFilterEndpoint endpoint = new StatusesFilterEndpoint();
		// add some track terms
		ArrayList<String> kw = new ArrayList<String>(Arrays.asList(keywords));
		endpoint.trackTerms(kw);
		setEndpoint(endpoint);
	}
}
