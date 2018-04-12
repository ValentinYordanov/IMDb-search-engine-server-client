package bg.uni.sofia.fmi.IMDbCommands;

import java.nio.channels.SocketChannel;

public interface Command {

    public static final String POSTER_FOLDER = "src\\bg\\uni\\sofia\\fmi\\IMDbSearch\\server\\posters\\";
    public static final String MOVIES_FOLDER = "src\\bg\\uni\\sofia\\fmi\\IMDbSearch\\server\\movies\\";
    public static final String SERIES_FOLDER = "src\\bg\\uni\\sofia\\fmi\\IMDbSearch\\server\\series\\";
    public static final String GENRE_TAG = "--genres=";
    public static final String ACTORS_TAG = "--actors=";
    public static final String ORDER_TAG = "--order=";
    public static final String SEASON_TAG = "--season=";
    public static final String FIELDS_TAG = "--fields=";
    public static final String API_ADDRESS = "http://www.omdbapi.com/?t=";
    public static final String API_KEY = "&apikey=3124849e";
    public static final String FILE_EXTENSION = ".json";
    public static final String IMAGE_EXTENSION = ".jpg";
    public static final String END_OF_READING_MARKER = "xxx\n";
    public static final String NEW_LINE_MARKER = "\n";
    public static final int BUFFER_SIZE = 10000;
    public static final int IMAGE_BUFFER_SIZE = 400000;


    public void run(String stringFromBuffer, SocketChannel socketChannel);

}
