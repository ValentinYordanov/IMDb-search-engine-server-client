package bg.uni.sofia.fmi.IMDbCommands;

import bg.uni.sofia.fmi.IMDbSearch.exceptions.UnknownMovieName;
import bg.uni.sofia.fmi.IMDbSearch.server.IMDbSearchServer;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.nio.channels.SocketChannel;

public class GetMovieCommand implements Command {

    @Override
    public void run(String stringFromBuffer, SocketChannel socketChannel) {

        String nameOfMovie;
        try {
            nameOfMovie = StringManipulation.getName(stringFromBuffer);
        } catch (UnknownMovieName e) {
            IMDbSearchServer.sendBufferMessage(socketChannel, e.getMessage());
            IMDbSearchServer.sendEndOfReadingMessage(socketChannel);
            return;
        }


        if (!FileManager.alreadyDownloaded(nameOfMovie, MOVIES_FOLDER)) {
            FileManager.downloadInformationForMoviesFromApi(nameOfMovie);
        }

        try {
            if (!FileManager.isValidMovieAfterDownload(nameOfMovie)) {
                IMDbSearchServer.sendBufferMessage(socketChannel, "There is no such movie!");
                IMDbSearchServer.sendEndOfReadingMessage(socketChannel);
                return;
            }
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


        if (!StringManipulation.haveFields(stringFromBuffer)) {

            String message = null;
            try {
                message = FileManager.getEveryThingFromFile(nameOfMovie, MOVIES_FOLDER);
            } catch (IOException e) {
                e.printStackTrace();
            }
            IMDbSearchServer.sendBufferMessage(socketChannel, message);
            IMDbSearchServer.sendEndOfReadingMessage(socketChannel);
        } else {

            String[] fields = StringManipulation.getFields(stringFromBuffer);
            for (String field : fields) {
                String message = null;
                try {
                    message = StringManipulation.parseJSONForMovies(field, nameOfMovie, MOVIES_FOLDER);
                } catch (ParseException e) {
                    e.printStackTrace();
                    break;
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
                IMDbSearchServer.sendBufferMessage(socketChannel, message);

            }

            IMDbSearchServer.sendEndOfReadingMessage(socketChannel);
        }

    }
}
