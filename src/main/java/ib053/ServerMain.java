package ib053;

import ib053.core.GameCore;
import ib053.frontend.cli.CliFrontend;

import java.io.File;

/**
 *
 */
public class ServerMain {
    public static void main(String[] args) {
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            System.err.println("Thread "+t.getName()+" crashed with exception: "+e);
            e.printStackTrace();
        });

        new GameCore(new File("locations.json"), new File("items.json"), new File("enemies.json"), new CliFrontend());
    }
}
