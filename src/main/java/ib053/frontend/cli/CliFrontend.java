package ib053.frontend.cli;

import ib053.core.Action;
import ib053.core.Event;
import ib053.core.GameCore;
import ib053.core.Player;
import ib053.frontend.Frontend;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static ib053.frontend.cli.Ansi.*;
import static ib053.frontend.cli.Ansi.TextAttribute.*;

/**
 *
 */
public class CliFrontend implements Frontend {

    private GameCore core;
    private Player player;

    private final BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(FileDescriptor.in), StandardCharsets.UTF_8));
    private final PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(FileDescriptor.out), StandardCharsets.UTF_8), false);

    private final List<Action> availableActions = new ArrayList<>();

    private final List<Event> lastEvents = new ArrayList<>();

    @Override
    public void initialize(GameCore core) {
        this.core = core;
    }

    private static int parseInt(String from, int defaultValue) {
        try {
            return Integer.parseInt(from);
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    @Override
    public void begin() {
        player = core.createNewPlayer("Conan the Librarian");
        assert player != null;
        core.initNewPlayer(player);

        new Thread(() -> {
            while (true) {
                final String line;
                try {
                    line = in.readLine();
                } catch (IOException e) {
                    throw new RuntimeException("Console read failure", e);
                }
                if (line == null || line.equalsIgnoreCase("quit") || line.equalsIgnoreCase("q") || line.equalsIgnoreCase("exit")) {
                    break;
                } else {
                    final int action = parseInt(line, -1);
                    core.eventLoop.execute(() -> {
                        if (action < 1 || action > availableActions.size()) {
                            System.out.println("Invalid action");
                        } else {
                            availableActions.get(action-1).perform(player);
                        }
                    });
                }
            }

            core.shutdown();
        }, "Console Input Thread").start();
    }

    private void redraw() {
        // \n for dumb terminals, ERASE_DISPLAY for clever terminals
        out.append("\n\n\n\n\n\n\n\n\n\n").append(ERASE_DISPLAY);

        out.append(attribute(BOLD)).append("Events:\n");
        while (lastEvents.size() > 5) {
            lastEvents.remove(0);
        }

        for (Event event : lastEvents) {
            out.append(attribute(FG_BLUE)).append(event.message).append('\n');
        }
        out.append('\n');
        out.append(attribute(FG_CYAN)).append(player.getActivity().getDescription(player)).append(attribute()).append('\n');
        out.append(attribute(BOLD)).append("Actions:").append(attribute()).append('\n');

        for (int i = 0; i < availableActions.size(); i++) {
            out.append(attribute(BOLD, FG_BLUE)).print(i+1);
            final Action action = availableActions.get(i);

            out.append(' ').append(attribute());
            if (action.group != null) {
                out.append(action.group).append(": ");
            }
            out.append(action.name).append('\n');
        }

        out.flush();
    }

    @Override
    public void playerActivityChanged(Player player) {
        if (player != this.player) return;
        availableActions.clear();
        availableActions.addAll(player.getActivity().getActions());
        redraw();
    }

    @Override
    public void playerReceiveEvent(Player player, Event event) {
        if (player != this.player) return;
        lastEvents.add(event);
        redraw();
    }
}
