package bg.uni.sofia.fmi.IMDbCommands;

import bg.uni.sofia.fmi.IMDbSearch.server.IMDbSearchServer;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.List;

public class GetMoviesCommand implements Command {


    @Override
    public void run(String stringFromBuffer, SocketChannel socketChannel) {
        try {
            List<String> movies = StringManipulation.getMovies(stringFromBuffer);
            for (String movie : movies) {
                IMDbSearchServer.sendBufferMessage(socketChannel, movie + NEW_LINE_MARKER, BUFFER_SIZE);
            }
            IMDbSearchServer.sendBufferMessage(socketChannel, END_OF_READING_MARKER, BUFFER_SIZE);
        } catch (IOException e) {
            System.err.println("IOException with getMoviesCommand");
        }

    }
}
