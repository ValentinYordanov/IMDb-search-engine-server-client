package bg.uni.sofia.fmi.IMDbCommands;

import bg.uni.sofia.fmi.IMDbSearch.exceptions.UnknownMovieName;
import bg.uni.sofia.fmi.IMDbSearch.server.IMDbSearchServer;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.nio.channels.SocketChannel;
/*
* TO DO:
*   REFACTOR MULTIPLE TRY/CATCHES
*
*
* */
public class GetMovieCommand implements Command {

    @Override
    public void run(String stringFromBuffer, SocketChannel socketChannel) {

        String nameOfMovie;
        try {
            nameOfMovie = StringManipulation.getName(stringFromBuffer);
        } catch (UnknownMovieName e) {
            try {
                IMDbSearchServer.sendBufferMessage(socketChannel, e.getMessage() + NEW_LINE_MARKER + END_OF_READING_MARKER, BUFFER_SIZE);
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            return;
        }

        if (!FileManager.alreadyDownloaded(nameOfMovie, MOVIES_FOLDER)) {
            try {
                FileManager.downloadInformationForMoviesFromApi(nameOfMovie);
            } catch (Exception e) {
                System.out.println("Problem with connecting to the api and downloading information");
            }
        }

        try {
            if (!FileManager.isValidMovieAfterDownload(nameOfMovie)) {
                IMDbSearchServer.sendBufferMessage(socketChannel, "There is no such movie!\n" + END_OF_READING_MARKER, BUFFER_SIZE);
                return;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }

        if (!StringManipulation.checkIfHaveFields(stringFromBuffer)) {

            String message = null;
            try {
                message = FileManager.getEveryThingFromFile(nameOfMovie, MOVIES_FOLDER) + NEW_LINE_MARKER
                        + END_OF_READING_MARKER;
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                IMDbSearchServer.sendBufferMessage(socketChannel, message, BUFFER_SIZE);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {

            String[] fields = StringManipulation.getFields(stringFromBuffer);
            for (String field : fields) {
                String message = null;
                try {
                    message = StringManipulation.parseJSONForMovies(field, nameOfMovie, MOVIES_FOLDER) + NEW_LINE_MARKER;
                } catch (ParseException e) {

                    try {
                        IMDbSearchServer.sendBufferMessage(socketChannel,
                                "There was a problem, please try again" + NEW_LINE_MARKER + END_OF_READING_MARKER,
                                BUFFER_SIZE);
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                    break;

                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    IMDbSearchServer.sendBufferMessage(socketChannel, message, BUFFER_SIZE);
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }

            try {
                IMDbSearchServer.sendBufferMessage(socketChannel, END_OF_READING_MARKER, BUFFER_SIZE);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}
