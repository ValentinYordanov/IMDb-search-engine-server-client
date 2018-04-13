package bg.uni.sofia.fmi.IMDbCommands;

import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static bg.uni.sofia.fmi.IMDbCommands.Command.*;

public class CheckerCommands {

    public static boolean alreadyDownloaded(String nameOfMovie, String fileFolder) {
        return checkIfDuplicateFiles(nameOfMovie.toLowerCase() + FILE_EXTENSION, fileFolder);
    }

    public static boolean alreadyDownloaded(String nameOfMovie, String fileFolder, int seasonNumber) {
        return checkIfDuplicateFiles(nameOfMovie.toLowerCase() + seasonNumber + FILE_EXTENSION, fileFolder);
    }

    private static boolean checkIfDuplicateFiles(String stringToCheck, String fileFolder) {

        Path serverFolder = Paths.get(fileFolder);
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(serverFolder)) {
            for (Path file : stream) {

                if (file.getFileName().toString().toLowerCase()
                        .equals(stringToCheck)) {
                    return true;
                }

            }

        } catch (Exception e) {
            System.out.println("There is a problem with the directory search");
        }
        return false;
    }

    public static boolean isValidMovieAfterDownload(String nameOfMovie) throws IOException, ParseException {

        if (FileManager.getEveryThingFromFile(nameOfMovie, MOVIES_FOLDER).contains("Error")) {
            if (StringManipulation.parseJSONForMovies("Error", nameOfMovie, MOVIES_FOLDER).equals("Movie not found!")) {
                File currentFile = new File(MOVIES_FOLDER + nameOfMovie + FILE_EXTENSION);
                currentFile.delete();
                return false;
            }
        }

        return true;
    }

    public static boolean isValidSeriesAfterDownload(String nameOfSeries, int seasonNumber)
            throws IOException, ParseException {

        nameOfSeries = nameOfSeries.toLowerCase() + seasonNumber;
        if (FileManager.getEveryThingFromFile(nameOfSeries, SERIES_FOLDER).contains("Error")) {
            if (StringManipulation.parseJSONForMovies("Error", nameOfSeries, SERIES_FOLDER).equals("Series or season not found!")) {
                File currentFile = new File(SERIES_FOLDER + nameOfSeries + FILE_EXTENSION);
                currentFile.delete();
                return false;
            }
        }

        return true;
    }

    public static boolean haveFields(String buffer) {

        if (buffer.contains(FIELDS_TAG)) {
            return true;
        }
        return false;

    }

    public static boolean checkIfInfoMatchesTheCriteria(String[] criterias, String JSONinfo) {

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
}
