package bg.uni.sofia.fmi.IMDbSearch.client;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;
import java.util.Set;

public class IMDbSearchClient {

   // private static final Set<String> listOfCommands = Set.of("get-movie", "get-movies", "get-tv-series",
     //       "get-movie-poster");
    public static final int SERVER_PORT = 4444;
    public static final String IMAGE_EXTENSION = ".jpg";
    private static final String CLIENT_FOLDER = "src\\bg\\uni\\sofia\\fmi\\IMDbSearch\\client\\";
    private static final int IMAGE_BUFFER_SIZE = 400000;
    private static final String END_OF_READING = "xxx";

    public IMDbSearchClient(int port) throws UnknownHostException, IOException {
        try (Socket s = new Socket("localhost", port);
             BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream()))) {

            Scanner sc = new Scanner(System.in);

            while (true) {
                PrintWriter pw = new PrintWriter(s.getOutputStream());
                String line_tmp;
                line_tmp = commandReader(sc);
                pw.print(line_tmp);
                pw.flush();

                String line;

                while (!(line = br.readLine()).equals(END_OF_READING)) {

                    if (line.endsWith(IMAGE_EXTENSION)) {

                        readImageFromInputStream(s.getInputStream(), line);
                        break;

                    }

                    System.out.println(line);
                }
            }
        }
    }

    private String commandReader(Scanner sc) {

        String inputLine = null;
        String[] listOfWords;
        System.out.println("Enter command, please");
            inputLine = sc.nextLine();
            listOfWords = inputLine.split(" ");

        return inputLine;

    }

    public static void readImageFromInputStream(InputStream s, String line) throws IOException {

        String nameOfMoviePoster = line.replaceAll(IMAGE_EXTENSION, "").replaceAll("\n", "");

        ByteArrayOutputStream into = new ByteArrayOutputStream();
        byte[] buf = new byte[IMAGE_BUFFER_SIZE];
        int n = 0;
        n = s.read(buf);
        into.write(buf, 0, n);

        File targetFile = new File(CLIENT_FOLDER + nameOfMoviePoster + IMAGE_EXTENSION);
        OutputStream outStream = new FileOutputStream(targetFile);
        outStream.write(buf);
        outStream.close();
        into.close();

    }

    public static void main(String[] args) {

        try {
            IMDbSearchClient client = new IMDbSearchClient(SERVER_PORT);
        } catch (IOException e) {
            System.out.println("error occured with the client");
        }

    }

}