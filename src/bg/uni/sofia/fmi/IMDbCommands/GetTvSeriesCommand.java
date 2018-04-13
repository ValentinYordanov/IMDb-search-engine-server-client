package bg.uni.sofia.fmi.IMDbCommands;

import bg.uni.sofia.fmi.IMDbSearch.exceptions.UnknownMovieName;
import bg.uni.sofia.fmi.IMDbSearch.server.IMDbSearchServer;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;

public class GetTvSeriesCommand implements Command {


    @Override
    public void run(String stringFromBuffer, SocketChannel socketChannel) {

        String nameOfSeries;
        try {
            nameOfSeries = StringManipulation.getName(stringFromBuffer);
        } catch (UnknownMovieName e) {
            IMDbSearchServer.sendBufferMessage(socketChannel, e.getMessage());
            IMDbSearchServer.sendEndOfReadingMessage(socketChannel);
            return;
        }

        int seasonNumber = StringManipulation.getSeriesSeason(stringFromBuffer);

        if (!FileManager.alreadyDownloaded(nameOfSeries, SERIES_FOLDER, seasonNumber)) {
            try {
                FileManager.downloadInformationForSeriesFromApi(nameOfSeries, seasonNumber);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            if (!FileManager.isValidSeriesAfterDownload(nameOfSeries, seasonNumber)) {
                IMDbSearchServer.sendBufferMessage(socketChannel, "There is no such series!");
                IMDbSearchServer.sendEndOfReadingMessage(socketChannel);
                return;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }

        List<String> listOfEpisodes = null;
        try {
            try {
                listOfEpisodes = new ArrayList<>(StringManipulation.parseJSONForSeries(nameOfSeries + StringManipulation.getSeriesSeason(stringFromBuffer)));
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (ParseException e1) {

            IMDbSearchServer.sendBufferMessage(socketChannel, "There was a problem, please try again");
            IMDbSearchServer.sendEndOfReadingMessage(socketChannel);
            return;

        }

        for (String episode : listOfEpisodes) {
            IMDbSearchServer.sendBufferMessage(socketChannel, episode);

        }

        IMDbSearchServer.sendEndOfReadingMessage(socketChannel);


    }
}
