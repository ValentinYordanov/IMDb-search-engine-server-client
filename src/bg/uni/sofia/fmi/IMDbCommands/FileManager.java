package bg.uni.sofia.fmi.IMDbCommands;

import org.json.simple.parser.ParseException;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static bg.uni.sofia.fmi.IMDbCommands.Command.*;

public class FileManager {

    public static boolean alreadyDownloaded(String nameOfMovie, String fileFolder) {

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

    public static boolean alreadyDownloaded(String nameOfMovie, String fileFolder, int seasonNumber) {

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



    public static void downloadInformationForMoviesFromApi(String nameOfMovie) {

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

    public static void downloadInformationForSeriesFromApi(String seriesName, int seasonNumber) throws IOException {

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

    public static boolean isValidMovieAfterDownload(String nameOfMovie) throws IOException, ParseException {

        if (getEveryThingFromFile(nameOfMovie, MOVIES_FOLDER).contains("Error")) {
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
        if (getEveryThingFromFile(nameOfSeries, SERIES_FOLDER).contains("Error")) {
            if (StringManipulation.parseJSONForMovies("Error", nameOfSeries, SERIES_FOLDER).equals("Series or season not found!")) {

                File currentFile = new File(SERIES_FOLDER + nameOfSeries + FILE_EXTENSION);
                currentFile.delete();

                return false;
            }
        }

        return true;
    }

    public static String getEveryThingFromFile(String fileName, String FolderType) throws IOException {

        BufferedReader br = new BufferedReader(new FileReader(new File(FolderType + fileName + FILE_EXTENSION)));
        String line;
        StringBuilder result = new StringBuilder();
        while ((line = br.readLine()) != null) {
            result.append(line);
        }
        br.close();
        return result.toString().replace("\n", "").replace("\r", "");
    }

    public static Map<String, Double> browseMoviesAndGetMapFromTitlesAndRatings(String[] actorsCriterias,
                                                                         String[] genresCriteria, boolean isThereGenre) {

        Map<String, Double> resultMovies = new HashMap<>();

        Path serverFolder = Paths.get(MOVIES_FOLDER);
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(serverFolder)) {
            for (Path file : stream) {
                String fileName = file.getFileName().toString().replaceAll(FILE_EXTENSION, "");
                if (StringManipulation.checkIfInfoMatchesTheCriteria(actorsCriterias,
                        StringManipulation.parseJSONForMovies("Actors", fileName, MOVIES_FOLDER))) {
                    if (isThereGenre) {
                        if (StringManipulation.checkIfInfoMatchesTheCriteria(genresCriteria,
                                StringManipulation.parseJSONForMovies("Genre", fileName, MOVIES_FOLDER))) {
                            String tmp = StringManipulation.parseJSONForMovies("Title", fileName, MOVIES_FOLDER);
                            Double rating = Double
                                    .parseDouble(StringManipulation.parseJSONForMovies("imdbRating", fileName, MOVIES_FOLDER));
                            resultMovies.put(tmp, rating);
                        }
                    } else {
                        String tmp = StringManipulation.parseJSONForMovies("Title", fileName, MOVIES_FOLDER);
                        Double rating = Double.parseDouble(StringManipulation.parseJSONForMovies("imdbRating", fileName, MOVIES_FOLDER));
                        resultMovies.put(tmp, rating);
                    }
                }
            }
        } catch (IOException | ParseException e) {
            System.out.println("There was a problem while putting movies into order");
        }

        return resultMovies;
    }

    public static void downloadImage(String fileName) throws ParseException, IOException {

        String posterUrl = StringManipulation.parseJSONForMovies("Poster", fileName, MOVIES_FOLDER);

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
}
