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

import bg.uni.sofia.fmi.IMDbCommands.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import bg.uni.sofia.fmi.IMDbSearch.exceptions.UnknownMovieName;

import java.util.stream.Collectors;

import static bg.uni.sofia.fmi.IMDbCommands.Command.BUFFER_SIZE;
import static bg.uni.sofia.fmi.IMDbCommands.Command.END_OF_READING_MARKER;

public class IMDbSearchServer implements AutoCloseable {

    private static final int SERVER_PORT = 4444;
    private Selector selector;
    private Map<String, Command> mapOfSupportedCommands;

    public IMDbSearchServer(int port) throws IOException {

        ServerSocketChannel ssc = ServerSocketChannel.open();
        ssc.configureBlocking(false);
        ServerSocket socket = ssc.socket();
        InetSocketAddress addr = new InetSocketAddress(port);

        socket.bind(addr);

        selector = Selector.open();
        ssc.register(selector, SelectionKey.OP_ACCEPT);

        mapOfSupportedCommands = new HashMap<>();
        mapOfSupportedCommands.put("get-movie", new GetMovieCommand());
        mapOfSupportedCommands.put("get-movies", new GetMoviesCommand());
        mapOfSupportedCommands.put("get-tv-series", new GetTvSeriesCommand());
        mapOfSupportedCommands.put("get-movie-poster", new GetMoviePosterCommand());
    }

    public void addNewCommand(String commandName, Command command) {
        mapOfSupportedCommands.put(commandName, command);
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
                    SocketChannel socketChannel = (SocketChannel) key.channel();
                    while (true) {
                        ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
                        buffer.clear();
                        int r = socketChannel.read(buffer);
                        if (r <= 0) {
                            break;
                        }

                        buffer.flip();

                        CharBuffer cb = StandardCharsets.UTF_8.decode(buffer);
                        String whatsInsideBuffer = cb.toString();

                        buffer.flip();

                        String command = StringManipulation.getCommand(whatsInsideBuffer);

                        buffer.flip();

                        commandOperator(whatsInsideBuffer, command, socketChannel, buffer);
                    }
                }

            }
            keyIterator.remove();
        }
    }


    private void accept(SelectionKey key) throws IOException {

        ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
        SocketChannel sc = ssc.accept();
        sc.configureBlocking(false);
        sc.register(selector, SelectionKey.OP_READ);

        System.out.println("Client " + sc + " is connected!");

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


    void commandOperator(String stringFromBuffer, String commandName, SocketChannel socketChannel, ByteBuffer buffer)
            throws IOException, ParseException {

        if(mapOfSupportedCommands.get(commandName) == null) {
            IMDbSearchServer.sendBufferMessage(socketChannel, "Invalid command, please try again!", BUFFER_SIZE);
            IMDbSearchServer.sendBufferMessage(socketChannel, END_OF_READING_MARKER, BUFFER_SIZE);
        } else {
            mapOfSupportedCommands.get(commandName).run(stringFromBuffer, socketChannel);
        }
    }

    /*
    * SENDING TOO MUCH WHITE SPACES, MUST FIX!
    *
    *
    *
    * */

    public static void sendBufferMessage(SocketChannel socketChannel, String message, int bufferSize) throws IOException {

        message = message + '\n';
        ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
        buffer.clear();
        buffer.flip();
        buffer = ByteBuffer.wrap((message).getBytes());
        socketChannel.write(buffer);

    }

    public static void sendBufferMessage(SocketChannel sc, byte[] message, int bufferSize) throws IOException {

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
