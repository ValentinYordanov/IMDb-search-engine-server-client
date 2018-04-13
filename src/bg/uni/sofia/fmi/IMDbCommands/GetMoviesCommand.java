package bg.uni.sofia.fmi.IMDbCommands;

import bg.uni.sofia.fmi.IMDbSearch.server.IMDbSearchServer;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.List;

public class GetMoviesCommand implements Command {

    @Override
    public void run(String stringFromBuffer, SocketChannel socketChannel) {
        List<String> movies = StringManipulation.getMovies(stringFromBuffer);
        for (String movie : movies) {
            IMDbSearchServer.sendBufferMessage(socketChannel, movie);
        }
        IMDbSearchServer.sendEndOfReadingMessage(socketChannel);


    }
}
