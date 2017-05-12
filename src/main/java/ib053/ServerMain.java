package ib053;

import ib053.core.GameCore;
import ib053.frontend.cli.CliFrontend;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Entry point for the server.
 */
public class ServerMain {

    private static final Map<String, String> ARGS = new HashMap<>();

    public static void main(String[] args) {
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            System.err.println("Thread "+t.getName()+" crashed with exception: "+e);
            e.printStackTrace();
        });

        for (String arg : args) {
            int splitIndex = arg.indexOf(':');
            if (splitIndex == -1) {
                ARGS.put(arg, null);
            } else {
                ARGS.put(arg.substring(0, splitIndex), arg.substring(splitIndex + 1));
            }
        }

        new GameCore(
                new File(ARGS.getOrDefault("resources", "resources")),
                new File(ARGS.getOrDefault("state", "state")),
                new CliFrontend());
    }
}
