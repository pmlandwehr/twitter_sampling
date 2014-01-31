/**
 * @author kjoseph
 * Sampling with a bounding box
 * See examples directory for how to use
 */

package samplers;
import java.util.ArrayList;
import java.util.List;


import user.ValidUser;

import com.mongodb.MongoClient;
import com.twitter.hbc.core.endpoint.Location.Coordinate;
import com.twitter.hbc.core.endpoint.StatusesFilterEndpoint;
import com.twitter.hbc.core.endpoint.Location;

public class LocationSampler extends Sampler {

	public LocationSampler(
			ValidUser user, 
			String dbName,
			String collectionName,
			MongoClient mongoClient,
			String outputDirectory, 
			List<String> terms,
			double ... coordinates) throws InterruptedException {
		super(user, 
			  mongoClient, 
			  outputDirectory, 
			  dbName,
			  collectionName);
		
		StatusesFilterEndpoint endpoint = new StatusesFilterEndpoint();
		ArrayList<Location> locations = new ArrayList<Location>();
		for(int i = 0; i <= coordinates.length-4;i+=4){
			locations.add( new Location(new Coordinate(coordinates[i], coordinates[i+1]),
						  				new Coordinate(coordinates[i+2], coordinates[i+3])));
		}
		if(terms != null){
			endpoint.trackTerms(terms);
		}
		endpoint.locations(locations);
		setEndpoint(endpoint);
		
	}

}
