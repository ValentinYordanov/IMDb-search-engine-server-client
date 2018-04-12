package bg.uni.sofia.fmi.IMDbCommands;

import bg.uni.sofia.fmi.IMDbSearch.exceptions.UnknownMovieName;
import bg.uni.sofia.fmi.IMDbSearch.server.IMDbSearchServer;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.SocketChannel;

public class GetMoviePosterCommand implements Command {


    @Override
    public void run(String stringFromBuffer, SocketChannel socketChannel) {

        try {
            String nameOfMovie;
            try {
                nameOfMovie = StringManipulation.getName(stringFromBuffer);
            } catch (UnknownMovieName e) {
                IMDbSearchServer.sendBufferMessage(socketChannel, e.getMessage() + END_OF_READING_MARKER, IMAGE_BUFFER_SIZE);
                return;
            }

            if (!FileManager.alreadyDownloaded(nameOfMovie, MOVIES_FOLDER)) {
                try {
                    FileManager.downloadInformationForMoviesFromApi(nameOfMovie);
                } catch (Exception e) {
                    System.out.println("Problem with connecting to the api and downloading information");
                }
            }

            if (!FileManager.isValidMovieAfterDownload(nameOfMovie)) {
                IMDbSearchServer.sendBufferMessage(socketChannel, "There is no such movie!" + END_OF_READING_MARKER, IMAGE_BUFFER_SIZE);
                return;
            }

            FileManager.downloadImage(nameOfMovie);

            byte[] nameBytes = (nameOfMovie.toLowerCase() + IMAGE_EXTENSION + NEW_LINE_MARKER).getBytes("UTF-8");
            IMDbSearchServer.sendBufferMessage(socketChannel, nameBytes, IMAGE_BUFFER_SIZE);

            InputStream initialStream = new FileInputStream(
                    new File(POSTER_FOLDER + nameOfMovie.toLowerCase() + IMAGE_EXTENSION));
            byte[] imageBuffer = new byte[initialStream.available()];
            initialStream.read(imageBuffer);

            IMDbSearchServer.sendBufferMessage(socketChannel, imageBuffer, IMAGE_BUFFER_SIZE);
            initialStream.close();
        } catch (ParseException exc) {

    } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
