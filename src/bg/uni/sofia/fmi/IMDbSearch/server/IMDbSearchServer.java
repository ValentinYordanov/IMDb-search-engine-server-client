package bg.uni.sofia.fmi.IMDbSearch.server;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import bg.uni.sofia.fmi.IMDbSearch.exceptions.UnknownMovieName;

import java.util.stream.Collectors;

public class IMDbSearchServer implements AutoCloseable {

	private static final String SERVER_FOLDER_ADDRESS = "C:\\Users\\valen\\Desktop\\Java\\Java Project\\IMDbSearchEngine\\src\\bg\\uni\\sofia\\fmi\\IMDbSearch\\server\\";
	public static final int SERVER_PORT = 4444;
	private static final String POSTER_FOLDER = SERVER_FOLDER_ADDRESS + "posters\\";
	private static final String MOVIES_FOLDER = SERVER_FOLDER_ADDRESS + "movies\\";
	private static final String SERIES_FOLDER = SERVER_FOLDER_ADDRESS + "series\\";
	private static final String GENRE_TAG = "--genres=";
	private static final String ACTORS_TAG = "--actors=";
	private static final String ORDER_TAG = "--order=";
	private static final String SEASON_TAG = "--season=";
	private static final String FIELDS_TAG = "--fields=";
	private static final String API_ADDRESS = "http://www.omdbapi.com/?t=";
	private static final String API_KEY = "&apikey=3124849e";
	private static final String FILE_EXTENSION = ".json";
	private static final String IMAGE_EXTENSION = ".jpg";
	private static final String END_OF_READING_MARKER = "xxx\n"; // marks when the clients has to stop reading otherwise
																	// it gets
																	// in an infinite loop
	private static final int BUFFER_SIZE = 10000;
	private static final int IMAGE_BUFFER_SIZE = 400000;
	private static final String NEW_LINE_MARKER = "\n";
	private Selector selector;

	public IMDbSearchServer(int port) throws IOException {

		ServerSocketChannel ssc = ServerSocketChannel.open();
		ssc.configureBlocking(false);
		ServerSocket socket = ssc.socket();
		InetSocketAddress addr = new InetSocketAddress(port);

		socket.bind(addr);

		selector = Selector.open();
		ssc.register(selector, SelectionKey.OP_ACCEPT);

	}

	public void start() throws IOException, ParseException {

		while (true) {
			int readyChannels = selector.select();
			if (readyChannels == 0)
				continue;
			Set<SelectionKey> selectedKeys = selector.selectedKeys();
			Iterator<SelectionKey> keyIterator = selectedKeys.iterator();
			while (keyIterator.hasNext()) {
				SelectionKey key = keyIterator.next();
				if (key.isAcceptable()) {

					this.accept(key);

				} else if (key.isReadable()) {
					SocketChannel sc = (SocketChannel) key.channel();
					while (true) {
						ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
						buffer.clear();
						int r = sc.read(buffer);
						if (r <= 0) {
							break;
						}

						buffer.flip();

						CharBuffer cb = StandardCharsets.UTF_8.decode(buffer);
						String whatsInsideBuffer = cb.toString();

						buffer.flip();

						String command = getCommand(whatsInsideBuffer);

						buffer.flip();

						commandOperator(whatsInsideBuffer, command, sc, buffer);
					}

				}
				keyIterator.remove();
			}
		}

	}

	private void accept(SelectionKey key) throws IOException {

		ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
		SocketChannel sc = ssc.accept();
		sc.configureBlocking(false);
		sc.register(selector, SelectionKey.OP_READ);

		System.out.println("Client " + sc + " is connected!");

	}

	public void connectToApiAndDownloadInformationForMovies(String nameOfMovie) {

		String URLNameOfMovie = nameOfMovie.replaceAll(" ", "+");
		URL url = null;
		try {
			url = new URL(API_ADDRESS + URLNameOfMovie + API_KEY);
		} catch (MalformedURLException e) {
			System.out.println("Wrong URL address");
		}

		String inputLine;
		try (FileWriter fw = new FileWriter(MOVIES_FOLDER + nameOfMovie.toLowerCase() + FILE_EXTENSION);
				BufferedWriter output = new BufferedWriter(fw);
				BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()))) {
			while ((inputLine = br.readLine()) != null) {
				output.write(inputLine);
			}
		} catch (IOException e) {
			System.out.println("There was an IOException while trying to write the API info to the file");
		}

	}

	private void connectToApiAndDownloadInformationForSeries(String seriesName, int seasonNumber) throws IOException {

		final String SEASON_TAG = "&Season=";
		String URLNameOfMovie = seriesName.replaceAll(" ", "+");
		URL url = new URL(API_ADDRESS + URLNameOfMovie + SEASON_TAG + seasonNumber + API_KEY);

		BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));

		String inputLine;
		FileWriter fw = new FileWriter(SERIES_FOLDER + seriesName.toLowerCase() + seasonNumber + FILE_EXTENSION);

		BufferedWriter output = new BufferedWriter(fw);
		while ((inputLine = br.readLine()) != null) {
			output.write(inputLine);
		}
		br.close();
		output.close();

	}

	public void downloadImage(String fileName) throws ParseException, IOException {

		String posterUrl = parseJSONForMovies("Poster", fileName, MOVIES_FOLDER);

		URL url = new URL(posterUrl);
		InputStream in = new BufferedInputStream(url.openStream());
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] buf = new byte[1024];
		int n = 0;
		while (-1 != (n = in.read(buf))) {
			out.write(buf, 0, n);
		}
		out.close();
		in.close();
		byte[] response = out.toByteArray();

		FileOutputStream fos = new FileOutputStream(POSTER_FOLDER + fileName.toLowerCase() + IMAGE_EXTENSION);
		fos.write(response);
		fos.close();

	}

	@Override
	public void close() throws Exception {

		try {
			if (selector != null) {
				selector.close();
			}
		} catch (IOException e) {
			// nothing to do here
		}

	}

	public String getCommand(String buffer) {

		String[] wordsInBuffer = buffer.split(" ");
		return wordsInBuffer[0];

	}

	public String getName(String buffer) throws UnknownMovieName {

		String resultName = buffer;
		int i = 0;
		while (resultName.charAt(i) != ' ') {
			i++;
		}
		i++;
		int startIndex = i;
		int endIndex = resultName.length();
		for (int j = i; j < resultName.length(); j++) {
			if (resultName.charAt(j) == '-' && resultName.charAt(j + 1) == '-') {
				endIndex = j - 1;
			}
		}

		if (endIndex <= startIndex) {
			throw new UnknownMovieName("This is not a valid movie name!");
		}

		return resultName.substring(startIndex, endIndex).replaceAll("[<>|:\"\\/?]", "_");
	}

	public boolean alreadyDownloaded(String nameOfMovie, String fileFolder) {

		Path serverFolder = Paths.get(fileFolder);
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(serverFolder)) {
			for (Path file : stream) {

				if (file.getFileName().toString().toLowerCase().equals(nameOfMovie.toLowerCase() + FILE_EXTENSION)) {
					return true;
				}

			}

		} catch (Exception e) {
			System.out.println("There is a problem with the directory search");
		}

		return false;
	}

	public boolean alreadyDownloaded(String nameOfMovie, String fileFolder, int seasonNumber) {

		Path serverFolder = Paths.get(fileFolder);
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(serverFolder)) {
			for (Path file : stream) {

				if (file.getFileName().toString().toLowerCase()
						.equals(nameOfMovie.toLowerCase() + seasonNumber + FILE_EXTENSION)) {
					return true;
				}

			}

		} catch (Exception e) {
			System.out.println("There is a problem with the directory search");
		}

		return false;
	}

	public boolean checkFields(String buffer) {

		if (buffer.contains(FIELDS_TAG)) {
			return true;
		}
		return false;

	}

	public String parseJSONForMovies(String whatToParse, String fileName, String folderType)
			throws ParseException, IOException {

		String parseResult = "";
		JSONParser parser = new JSONParser();
		Object obj = parser.parse(getEveryThingFromFile(fileName, folderType));
		JSONObject jsonObj = (JSONObject) obj;

		parseResult = (String) jsonObj.get(whatToParse);

		return parseResult;
	}

	public List<String> parseJSONForSeries(String fileName) throws ParseException, IOException {

		List<String> resultList = new ArrayList<>();

		JSONParser parser = new JSONParser();
		Object obj = parser.parse(getEveryThingFromFile(fileName, SERIES_FOLDER));
		JSONObject jsonObj = (JSONObject) obj;

		JSONArray slideContent = (JSONArray) jsonObj.get("Episodes");

		Iterator i = slideContent.iterator();
		while (i.hasNext()) {
			JSONObject slide = (JSONObject) i.next();
			String title = (String) slide.get("Title");
			resultList.add(title);
		}

		return resultList;

	}

	public String getEveryThingFromFile(String fileName, String FolderType) throws IOException {

		BufferedReader br = new BufferedReader(new FileReader(new File(FolderType + fileName + FILE_EXTENSION)));
		String line;
		StringBuilder result = new StringBuilder();
		while ((line = br.readLine()) != null) {
			result.append(line);
		}
		br.close();
		return result.toString().replace("\n", "").replace("\r", "");
	}

	public String[] getFields(String message) {

		int i = message.indexOf(FIELDS_TAG) + FIELDS_TAG.length();

		message = message.substring(i);

		String[] result = message.split(",");

		return result;

	}

	public int getSeriesSeason(String message) {

		int i = message.indexOf(SEASON_TAG) + SEASON_TAG.length();
		message = message.substring(i);
		return Integer.parseInt(message);

	}

	// Returns a list of movies according to the criteria in the message
	public List<String> getMovies(String message) {

		boolean isThereOrder = false;
		boolean isThereGenre = false;
		String genresCriteria = null;
		String orderCriteria = null;
		String[] actorsCriterias = null;
		String[] genresCriterias = null;

		List<String> resultList = new ArrayList<>();
		Map<String, Double> resultMovies = new HashMap<>();

		if (message.contains(GENRE_TAG)) {

			isThereGenre = true;
			int startIndex = message.indexOf(GENRE_TAG) + GENRE_TAG.length();
			int endIndex = message.indexOf(ACTORS_TAG) - 1;

			genresCriteria = message.substring(startIndex, endIndex);
			genresCriterias = genresCriteria.split(",");

		}

		if (message.contains(ORDER_TAG)) {

			isThereOrder = true;
			int startIndex = message.indexOf(ORDER_TAG) + ORDER_TAG.length();
			orderCriteria = message.substring(startIndex, startIndex + 3);

		}
		int startIndexForActors = message.indexOf(ACTORS_TAG) + ACTORS_TAG.length();
		String actorsCriteria = message.substring(startIndexForActors);

		actorsCriterias = actorsCriteria.split(",");

		resultMovies = browseMoviesAndGetMapFromTitlesAndRatings(actorsCriterias, genresCriterias, isThereGenre);

		if (isThereOrder) {
			return putInOrder(resultMovies, orderCriteria);
		}

		resultList.addAll(resultMovies.keySet());

		return resultList;
	}

	public List<String> putInOrder(Map<String, Double> map, String order) {

		if (order.equals("asc")) {
			return map.entrySet().stream().sorted((e1, e2) -> Double.compare(e1.getValue(), e2.getValue()))
					.map(Map.Entry::getKey).collect(Collectors.toList());
		}

		return map.entrySet().stream().sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()))
				.map(Map.Entry::getKey).collect(Collectors.toList());

	}

	public Map<String, Double> browseMoviesAndGetMapFromTitlesAndRatings(String[] actorsCriterias,
			String[] genresCriteria, boolean isThereGenre) {

		Map<String, Double> resultMovies = new HashMap<>();

		Path serverFolder = Paths.get(MOVIES_FOLDER);
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(serverFolder)) {
			for (Path file : stream) {
				String fileName = file.getFileName().toString().replaceAll(FILE_EXTENSION, "");
				if (checkIfInfoMatchesTheCriteria(actorsCriterias,
						parseJSONForMovies("Actors", fileName, MOVIES_FOLDER))) {
					if (isThereGenre) {
						if (checkIfInfoMatchesTheCriteria(genresCriteria,
								parseJSONForMovies("Genre", fileName, MOVIES_FOLDER))) {
							String tmp = parseJSONForMovies("Title", fileName, MOVIES_FOLDER);
							Double rating = Double
									.parseDouble(parseJSONForMovies("imdbRating", fileName, MOVIES_FOLDER));
							resultMovies.put(tmp, rating);
						}
					} else {
						String tmp = parseJSONForMovies("Title", fileName, MOVIES_FOLDER);
						Double rating = Double.parseDouble(parseJSONForMovies("imdbRating", fileName, MOVIES_FOLDER));
						resultMovies.put(tmp, rating);
					}
				}
			}
		} catch (IOException | ParseException e) {
			System.out.println("There was a problem while putting movies into order");
		}

		return resultMovies;
	}

	public boolean checkIfInfoMatchesTheCriteria(String[] criterias, String JSONinfo) {

		JSONinfo = JSONinfo.toLowerCase();

		for (String actor : criterias) {
			if (actor.charAt(0) == ' ') {
				actor = actor.substring(1);
			}

			if (!JSONinfo.contains(actor.toLowerCase())) {
				return false;
			}
		}

		return true;
	}

	public boolean checkIfValidMovieAfterDownload(String nameOfMovie) throws IOException, ParseException {

		if (getEveryThingFromFile(nameOfMovie, MOVIES_FOLDER).contains("Error")) {
			if (parseJSONForMovies("Error", nameOfMovie, MOVIES_FOLDER).equals("Movie not found!")) {

				File currentFile = new File(MOVIES_FOLDER + nameOfMovie + FILE_EXTENSION);
				currentFile.delete();

				return false;
			}
		}

		return true;
	}

	public boolean checkIfValidSeriesAfterDownload(String nameOfSeries, int seasonNumber)
			throws IOException, ParseException {

		nameOfSeries = nameOfSeries.toLowerCase() + seasonNumber;
		if (getEveryThingFromFile(nameOfSeries, SERIES_FOLDER).contains("Error")) {
			if (parseJSONForMovies("Error", nameOfSeries, SERIES_FOLDER).equals("Series or season not found!")) {

				File currentFile = new File(SERIES_FOLDER + nameOfSeries + FILE_EXTENSION);
				currentFile.delete();

				return false;
			}
		}

		return true;
	}

	private void getMovieCommand(String whatsInsideBuffer, SocketChannel sc, ByteBuffer buffer)
			throws IOException, ParseException {

		String nameOfMovie;
		try {
			nameOfMovie = getName(whatsInsideBuffer);
		} catch (UnknownMovieName e) {
			sendBufferMessage(sc, e.getMessage() + NEW_LINE_MARKER + END_OF_READING_MARKER, BUFFER_SIZE);
			return;
		}

		if (!alreadyDownloaded(nameOfMovie, MOVIES_FOLDER)) {
			try {
				connectToApiAndDownloadInformationForMovies(nameOfMovie);
			} catch (Exception e) {
				System.out.println("Problem with connecting to the api and downloading information");
			}
		}

		if (!checkIfValidMovieAfterDownload(nameOfMovie)) {
			sendBufferMessage(sc, "There is no such movie!\n" + END_OF_READING_MARKER, BUFFER_SIZE);
			return;
		}

/*		ByteBuffer buffer2 = ByteBuffer.allocate(10000);
		buffer2.clear();
		buffer2.flip();*/

		if (!checkFields(whatsInsideBuffer)) {

			String message = getEveryThingFromFile(nameOfMovie, MOVIES_FOLDER) + NEW_LINE_MARKER
					+ END_OF_READING_MARKER;
			sendBufferMessage(sc, message, BUFFER_SIZE);
		} else {

			String[] fields = getFields(whatsInsideBuffer);
			for (String field : fields) {
				String message = null;
				try {
					message = parseJSONForMovies(field, nameOfMovie, MOVIES_FOLDER) + NEW_LINE_MARKER;
				} catch (ParseException e) {

					sendBufferMessage(sc,
							"There was a problem, please try again" + NEW_LINE_MARKER + END_OF_READING_MARKER,
							BUFFER_SIZE);
					break;

				}
				sendBufferMessage(sc, message, BUFFER_SIZE);

			}

			sendBufferMessage(sc, END_OF_READING_MARKER, BUFFER_SIZE);
		}

	}

	private void getMoviesCommand(String whatsInsideBuffer, SocketChannel sc) throws IOException {

		List<String> movies = getMovies(whatsInsideBuffer);
		for (String movie : movies) {
			sendBufferMessage(sc, movie + NEW_LINE_MARKER, BUFFER_SIZE);
		}
		sendBufferMessage(sc, END_OF_READING_MARKER, BUFFER_SIZE);

	}

	private void getTvSeriesCommand(String whatsInsideBuffer, SocketChannel sc) throws IOException, ParseException {

		String nameOfSeries;
		try {
			nameOfSeries = getName(whatsInsideBuffer);
		} catch (UnknownMovieName e) {
			sendBufferMessage(sc, e.getMessage() + NEW_LINE_MARKER + END_OF_READING_MARKER, BUFFER_SIZE);
			return;
		}

		int seasonNumber = getSeriesSeason(whatsInsideBuffer);

		if (!alreadyDownloaded(nameOfSeries, SERIES_FOLDER, seasonNumber)) {
			connectToApiAndDownloadInformationForSeries(nameOfSeries, seasonNumber);
		}

		if (!checkIfValidSeriesAfterDownload(nameOfSeries, seasonNumber)) {
			sendBufferMessage(sc, "There is no such series!\n" + END_OF_READING_MARKER, BUFFER_SIZE);
			return;
		}

		List<String> listOfEpisodes;
		try {
			listOfEpisodes = new ArrayList<>(parseJSONForSeries(nameOfSeries + getSeriesSeason(whatsInsideBuffer)));
		} catch (ParseException e1) {

			sendBufferMessage(sc, "There was a problem, please try again\n" + END_OF_READING_MARKER, BUFFER_SIZE);
			return;

		}

		for (String episode : listOfEpisodes) {
			sendBufferMessage(sc, episode + NEW_LINE_MARKER, BUFFER_SIZE);
		}

		sendBufferMessage(sc, END_OF_READING_MARKER, BUFFER_SIZE);

	}

	public void getMoviePosterCommand(String whatsInsideBuffer, SocketChannel sc, ByteBuffer buffer)
			throws IOException, ParseException {

		String nameOfMovie;
		try {
			nameOfMovie = getName(whatsInsideBuffer);
		} catch (UnknownMovieName e) {
			sendBufferMessage(sc, e.getMessage() + NEW_LINE_MARKER + END_OF_READING_MARKER, IMAGE_BUFFER_SIZE);
			return;
		}

		if (!alreadyDownloaded(nameOfMovie, MOVIES_FOLDER)) {
			try {
				connectToApiAndDownloadInformationForMovies(nameOfMovie);
			} catch (Exception e) {
				System.out.println("Problem with connecting to the api and downloading information");
			}
		}

		if (!checkIfValidMovieAfterDownload(nameOfMovie)) {
			sendBufferMessage(sc, "There is no such movie!\n" + END_OF_READING_MARKER, IMAGE_BUFFER_SIZE);
			return;
		}

		downloadImage(nameOfMovie);

		byte[] nameBytes = (nameOfMovie.toLowerCase() + IMAGE_EXTENSION + NEW_LINE_MARKER).getBytes("UTF-8");
		sendBufferMessage(sc, nameBytes, IMAGE_BUFFER_SIZE);

		InputStream initialStream = new FileInputStream(
				new File(POSTER_FOLDER + nameOfMovie.toLowerCase() + IMAGE_EXTENSION));
		byte[] imageBuffer = new byte[initialStream.available()];
		initialStream.read(imageBuffer);

		sendBufferMessage(sc, imageBuffer, IMAGE_BUFFER_SIZE);
		initialStream.close();

	}

	void commandOperator(String whatsInsideBuffer, String command, SocketChannel sc, ByteBuffer buffer)
			throws IOException, ParseException {

		switch (command) {
		case "get-movie":
			getMovieCommand(whatsInsideBuffer, sc, buffer);
			break;
		case "get-movies":
			getMoviesCommand(whatsInsideBuffer, sc);
			break;
		case "get-tv-series":
			getTvSeriesCommand(whatsInsideBuffer, sc);
			break;
		case "get-movie-poster":
			getMoviePosterCommand(whatsInsideBuffer, sc, buffer);
			break;
		}

	}

	public void sendBufferMessage(SocketChannel sc, String message, int bufferSize) throws IOException {

		ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
		buffer.clear();
		buffer.flip();
		buffer = ByteBuffer.wrap((message).getBytes());
		sc.write(buffer);

	}

	public void sendBufferMessage(SocketChannel sc, byte[] message, int bufferSize) throws IOException {

		ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
		buffer.clear();
		buffer.flip();
		buffer = ByteBuffer.wrap(message);
		sc.write(buffer);

	}

	public static void main(String[] args) {

		try (IMDbSearchServer server = new IMDbSearchServer(SERVER_PORT)) {
			server.start();
		} catch (ParseException e) {
			System.out.println("error occured while trying to parse some JSON");
		} catch (IOException e1) {
			System.out.println("There was IOException, trying to start the server");
		} catch (Exception e1) {
			System.out.println("There was a problem while trying to close the server");
		}
	}
}
