/**
 * @author kjoseph
 * A valid user requires a consumerKey, a consumerSecret, an accessToken and an accessTokenSecret
 * As soon as I get the time, I'll make a command line interface so you don't have to have all of this stuff
 * before you start using the tool
 */
package user;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;



public class ValidUser {

	public String consumerKey = "";
	public String consumerSecret = "";
	public String accessToken = "";
	public String accessTokenSecret = "";
	public String name = "";
	public ValidUser(String name, String cKey, String cSecret, String aToken, String aSecret){
		this.name = name;
		consumerKey = cKey;
		consumerSecret = cSecret;
		accessToken = aToken;
		accessTokenSecret = aSecret;
	}
	public static ArrayList<ValidUser> getUsersFromConfig(String configFile) throws IOException {
		ArrayList<ValidUser> users = new ArrayList<ValidUser>();
		BufferedReader reader = new BufferedReader(new FileReader(configFile));
		String line= "";
		//TODO: Add some error checking
		while((line = reader.readLine()) != null){
			String[] splitLine = line.split(",");
			users.add(new ValidUser(splitLine[0], splitLine[1],splitLine[2],splitLine[3],splitLine[4]));
			System.out.println("created user: " + splitLine[0]);
			reader.readLine();
		}
		reader.close();
		return users;
	}
}
