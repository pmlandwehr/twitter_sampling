import java.util.ArrayList;
import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;

import com.mongodb.MongoClient;
import com.twitter.hbc.core.endpoint.Location;
import com.twitter.hbc.core.endpoint.Location.Coordinate;
import com.twitter.hbc.core.endpoint.StatusesFilterEndpoint;


public class LocationSampler extends Sampler {

	public LocationSampler(
			ValidUser user, 
			String dbName,
			String collectionName,
			MongoClient mongoClient,
			String outputDirectory, 
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
		endpoint.locations(locations);
		setEndpoint(endpoint);
		
	}

}
