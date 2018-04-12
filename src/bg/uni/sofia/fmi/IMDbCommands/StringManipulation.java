package bg.uni.sofia.fmi.IMDbCommands;

import bg.uni.sofia.fmi.IMDbSearch.exceptions.UnknownMovieName;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static bg.uni.sofia.fmi.IMDbCommands.Command.*;

public class StringManipulation {

    public static String getName(String buffer) throws UnknownMovieName {

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

    public static String parseJSONForMovies(String whatToParse, String fileName, String folderType)
            throws ParseException, IOException {

        String parseResult = "";
        JSONParser parser = new JSONParser();
        Object obj = parser.parse(FileManager.getEveryThingFromFile(fileName, folderType));
        JSONObject jsonObj = (JSONObject) obj;

        parseResult = (String) jsonObj.get(whatToParse);

        return parseResult;
    }

    public static List<String> parseJSONForSeries(String fileName) throws ParseException, IOException {

        List<String> resultList = new ArrayList<>();

        JSONParser parser = new JSONParser();
        Object obj = parser.parse(FileManager.getEveryThingFromFile(fileName, SERIES_FOLDER));
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

    public static boolean checkIfHaveFields(String buffer) {

        if (buffer.contains(FIELDS_TAG)) {
            return true;
        }
        return false;

    }

    public static String[] getFields(String message) {

        int i = message.indexOf(FIELDS_TAG) + FIELDS_TAG.length();

        message = message.substring(i);

        String[] result = message.split(",");

        return result;

    }

    public static int getSeriesSeason(String message) {

        int i = message.indexOf(SEASON_TAG) + SEASON_TAG.length();
        message = message.substring(i);
        return Integer.parseInt(message);

    }

    public static List<String> getMovies(String message) {

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

        resultMovies = FileManager.browseMoviesAndGetMapFromTitlesAndRatings(actorsCriterias, genresCriterias, isThereGenre);

        if (isThereOrder) {
            return putInOrder(resultMovies, orderCriteria);
        }

        resultList.addAll(resultMovies.keySet());

        return resultList;
    }

    public static List<String> putInOrder(Map<String, Double> map, String order) {

        if (order.equals("asc")) {
            return map.entrySet().stream().sorted((e1, e2) -> Double.compare(e1.getValue(), e2.getValue()))
                    .map(Map.Entry::getKey).collect(Collectors.toList());
        }

        return map.entrySet().stream().sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()))
                .map(Map.Entry::getKey).collect(Collectors.toList());

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
