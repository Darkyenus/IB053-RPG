package ib053;

import ib053.core.GameCore;
import ib053.frontend.cli.CliFrontend;

/**
 *
 */
public class ServerMain {
    public static void main(String[] args) {
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                System.err.println("Thread "+t.getName()+" crashed with exception: "+e);
                e.printStackTrace();
            }
        });

        new GameCore(new CliFrontend());
    }
}
