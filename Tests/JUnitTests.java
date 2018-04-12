/*import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.json.simple.parser.ParseException;
import org.junit.BeforeClass;
import org.junit.Test;

import bg.uni.sofia.fmi.IMDbSearch.client.IMDbSearchClient;
import bg.uni.sofia.fmi.IMDbSearch.exceptions.UnknownMovieName;
import bg.uni.sofia.fmi.IMDbSearch.server.IMDbSearchServer;

public class JUnitTests {
	static IMDbSearchServer server;
	static IMDbSearchClient client;
	@BeforeClass
	public static void run() throws IOException {
		server = new IMDbSearchServer(4444);
	//	client = new IMDbSearchClient(4444);
	}
	
	@Test
	public void method() throws IOException, ParseException, UnknownMovieName {
		String[] testString = {"Leonardo Di Caprio"," Emma Watson"};
		String[] actorsCriteria = {"Daniel Radcliffe"};
		String[] genreCriteria = {"Drama"};
		
		assertTrue("assert valid movie", server.isValidMovieAfterDownload("titanic"));
		assertTrue("assert valid series", server.isValidSeriesAfterDownload("friends", 5));
		assertNotNull(new UnknownMovieName("hello"));
		assertTrue("match criteria test", server.checkIfInfoMatchesTheCriteria(testString,"Leonardo Di Caprio,Emma Watson, James"));
		assertFalse("should not match criteria", server.checkIfInfoMatchesTheCriteria(testString,"James, Paul"));
		assertTrue("map from movies and ratings with genre", server.browseMoviesAndGetMapFromTitlesAndRatings(actorsCriteria, genreCriteria, true).containsKey("Swiss Army Man"));
		assertTrue("map from movies and rating without genre", server.browseMoviesAndGetMapFromTitlesAndRatings(actorsCriteria, null, false).containsKey("Harry Potter and the Prisoner of Azkaban"));
		assertTrue("get movies function test", server.getMovies("get-movies --order=asc --genres=Drama --actors=Daniel Radcliffe").contains("Harry Potter and the Deathly Hallows: Part 2"));
		assertTrue("get movies function test", server.getMovies("get-movies --order=desc --genres=Drama --actors=Daniel Radcliffe").contains("Harry Potter and the Deathly Hallows: Part 2"));
		assertTrue("get movies function test without order", server.getMovies("get-movies --genres=Drama --actors=Daniel Radcliffe").contains("Swiss Army Man"));
		assertTrue("get season number right", server.getSeriesSeason("get-tv-series friends --season=4") == 4);
		assertTrue("get fields right", server.getFields("get-movie titanic --fields=Actors,Year")[0].equals("Actors"));
		assertTrue("get everything from file", server.getEveryThingFromFile("titanic", "C:\\Users\\valen\\Desktop\\Java\\Java Project\\IMDbSearchEngine\\src\\bg\\uni\\sofia\\fmi\\IMDbSearch\\server\\movies\\").equals("{\"Title\":\"Titanic\",\"Year\":\"1997\",\"Rated\":\"PG-13\",\"Released\":\"19 Dec 1997\",\"Runtime\":\"194 min\",\"Genre\":\"Drama, Romance\",\"Director\":\"James Cameron\",\"Writer\":\"James Cameron\",\"Actors\":\"Leonardo DiCaprio, Kate Winslet, Billy Zane, Kathy Bates\",\"Plot\":\"A seventeen-year-old aristocrat falls in love with a kind but poor artist aboard the luxurious, ill-fated R.M.S. Titanic.\",\"Language\":\"English, Swedish\",\"Country\":\"USA\",\"Awards\":\"Won 11 Oscars. Another 111 wins & 75 nominations.\",\"Poster\":\"https://images-na.ssl-images-amazon.com/images/M/MV5BMDdmZGU3NDQtY2E5My00ZTliLWIzOTUtMTY4ZGI1YjdiNjk3XkEyXkFqcGdeQXVyNTA4NzY1MzY@._V1_SX300.jpg\",\"Ratings\":[{\"Source\":\"Internet Movie Database\",\"Value\":\"7.8/10\"},{\"Source\":\"Rotten Tomatoes\",\"Value\":\"88%\"},{\"Source\":\"Metacritic\",\"Value\":\"75/100\"}],\"Metascore\":\"75\",\"imdbRating\":\"7.8\",\"imdbVotes\":\"879,510\",\"imdbID\":\"tt0120338\",\"Type\":\"movie\",\"DVD\":\"10 Sep 2012\",\"BoxOffice\":\"N/A\",\"Production\":\"Paramount Pictures\",\"Website\":\"http://www.titanicmovie.com/\",\"Response\":\"True\"}"));
		assertTrue("get command", server.getCommand("get-movie titanic --fields=Actors,Year").equals("get-movie"));
		assertTrue("get name right", server.getName("get-movie titanic --fields=Actors,Year,Released").equals("titanic"));
		assertTrue("already downloaded movie", server.alreadyDownloaded("titanic", "C:\\Users\\valen\\Desktop\\Java\\Java Project\\IMDbSearchEngine\\src\\bg\\uni\\sofia\\fmi\\IMDbSearch\\server\\movies\\"));
		assertFalse("not downloaded yet movie", server.alreadyDownloaded("tutanic", "C:\\Users\\valen\\Desktop\\Java\\Java Project\\IMDbSearchEngine\\src\\bg\\uni\\sofia\\fmi\\IMDbSearch\\server\\movies\\"));
		assertTrue("already downloaded series season",server.alreadyDownloaded("friends", "C:\\Users\\valen\\Desktop\\Java\\Java Project\\IMDbSearchEngine\\src\\bg\\uni\\sofia\\fmi\\IMDbSearch\\server\\series\\", 5));
		assertFalse("not downloaded yet series season",server.alreadyDownloaded("friends", "C:\\Users\\valen\\Desktop\\Java\\Java Project\\IMDbSearchEngine\\src\\bg\\uni\\sofia\\fmi\\IMDbSearch\\server\\series\\", 2));		
		assertTrue("check fields", server.checkFields("get-movie titanic --fields=Actors,Year"));
		assertFalse("check fields", server.checkFields("get-movie titanic"));
		assertTrue("parse JSON for movies test", server.parseJSONForMovies("Year", "titanic", "C:\\Users\\valen\\Desktop\\Java\\Java Project\\IMDbSearchEngine\\src\\bg\\uni\\sofia\\fmi\\IMDbSearch\\server\\movies\\").equals("1997"));
	}
	
	@Test(expected=UnknownMovieName.class)
	public void exceptionInvoked() throws IOException, ParseException, UnknownMovieName {
		assertFalse(server.isValidMovieAfterDownload("tutanic"));
		assertFalse(server.isValidSeriesAfterDownload("game of throne", 5));
		assertTrue("get name right", server.getName("get-movie --fields=Actors,Year,Released").equals("titanic"));
	}
	
	@Test
	public void voidTests() throws ParseException, IOException {
		
		server.downloadInformationForMoviesFromApi("forever my girl");
		server.downloadImage("forever my girl");
		File f = new File("C:\\Users\\valen\\Desktop\\Java\\Java Project\\IMDbSearchEngine\\src\\bg\\uni\\sofia\\fmi\\IMDbSearch\\server\\posters\\forever my girl.jpg");
		assertTrue("image download test", f.exists());
		assertTrue("image download test", !f.isDirectory());
		f.delete();
		
	}
	
	@Test
	public void readImageTest() throws IOException {
		
		InputStream initialStream = new FileInputStream(
				new File("C:\\Users\\valen\\Desktop\\Java\\Java Project\\IMDbSearchEngine\\src\\bg\\uni\\sofia\\fmi\\IMDbSearch\\server\\posters\\forever my girl2.jpg"));
		IMDbSearchClient.readImageFromInputStream(initialStream, "forever my girl2.jpg");
		File f = new File("C:\\Users\\valen\\Desktop\\Java\\Java Project\\IMDbSearchEngine\\src\\bg\\uni\\sofia\\fmi\\IMDbSearch\\client\\forever my girl2.jpg");
		assertTrue(f.exists());
		assertTrue(!f.isDirectory());
		f.delete();
	}
}
*/