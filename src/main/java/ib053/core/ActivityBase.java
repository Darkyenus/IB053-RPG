package ib053.core;

import com.esotericsoftware.jsonbeans.Json;
import com.esotericsoftware.jsonbeans.JsonValue;
import ib053.core.Action.Perform;

import java.util.*;

/**
 * Represents the activity the player is doing and fully handles interaction with them.
 *
 * Subclassed for each activity.
 * Each concrete subclass must be annotated with {@link Activity} and be implemented according to the ActivityType.
 *
 * Instances may implement {@link Serializable} if they wish to save any state.
 * No other way of serialization is possible.
 */
public abstract class ActivityBase {

    /** List of actions that this activity offers to its players.
     * May be changed only during initialization of activity through
     * {@link #action(String, String, Perform)} and {@link #action(String, String, String, Perform)}. */
    private final ArrayList<Action> actions = new ArrayList<>();
    private final ActionsView actionsView = new ActionsView(actions);

    /** List of players engaged in this activity. Modifiable only through {@link GameCore#changePlayerActivity(Player, ActivityBase)} */
    final List<Player> engagedPlayers = new ArrayList<>();

    /** Core to which this activity is initialized to. Null = not initialized. */
    GameCore core = null;

    protected final Action action(String key, String name, Perform perform) {
        return action(key, null, name, perform);
    }

    protected final Action action(String key, String group, String name, Perform perform) {
        if (core != null) throw new IllegalStateException("Activity is already initialized");
        final Action action = new Action(this, key, group, name, perform);
        actions.add(action);
        return action;
    }

    protected final void allActionsSetEnabled(boolean enabled) {
        for (Action action : actions) {
            action.setEnabled(enabled);
        }
    }

    /** Get core to which this Activity has been initialized.
     * Valid to call only after {@link #initialize()} has been called!!! */
    public final GameCore core() {
        if (core == null) throw new IllegalStateException("Activity not yet initialized");
        return core;
    }

    /** Called before activity is first assigned to any player. */
    public void initialize(){}

    /** Called when player begins this activity. */
    public void beginActivity(Player player) {}

    /** @return Immutable view into currently enabled actions available to players engaged in this activity. */
    public final Collection<Action> getActions() {
        return actionsView;
    }

    /** Called when player asks for description of the action. */
    public abstract String getDescription(Player player);

    /** Called when player ends this activity. */
    public void endActivity(Player player) {}

    /** Implement this interface in Activity to add support for serialization.
     * _underscore names are reserved. */
    public interface Serializable {
        void write(Json json);
        void read(GameCore core, JsonValue json);
    }

    private final static class ActionsView implements Collection<Action> {

        private final ArrayList<Action> collection;

        private ActionsView(ArrayList<Action> collection) {
            this.collection = collection;
        }

        @Override
        public int size() {
            final int baseSize = collection.size();
            int realSize = 0;
            //noinspection ForLoopReplaceableByForEach
            for (int i = 0; i < baseSize; i++) {
                if (collection.get(i).isEnabled()) {
                    realSize++;
                }
            }
            return realSize;
        }

        @Override
        public boolean isEmpty() {
            return size() == 0;
        }

        @Override
        public boolean contains(Object o) {
            if (o instanceof Action) {
                final Action action = (Action) o;
                return action.isEnabled() && collection.contains(action);
            }
            return false;
        }

        @Override
        public Iterator<Action> iterator() {
            return new Iterator<Action>() {

                int nextPossible = 0;
                boolean nextFound = false;

                @Override
                public boolean hasNext() {
                    if (nextFound) {
                        return nextPossible != -1;
                    }

                    nextFound = true;
                    for (;;) {
                        if (nextPossible >= collection.size()) {
                            nextPossible = -1;
                            return false;
                        }
                        if (collection.get(nextPossible).isEnabled()) {
                            return true;
                        }
                        nextPossible++;
                    }
                }

                @Override
                public Action next() {
                    if (!hasNext()) throw new NoSuchElementException();
                    nextFound = false;
                    return collection.get(nextPossible++);
                }
            };
        }

        @Override
        public Action[] toArray() {
            return toArray(null);
        }

        @Override
        public <T> T[] toArray(T[] a) {
            final int size = size();
            Action[] actions;
            if (a != null && a instanceof Action[] && a.length == size) {
                actions = (Action[]) a;
            } else {
                actions = new Action[size];
            }
            int actionsI = 0;
            for (int i = 0; i < collection.size() && actionsI < size; i++) {
                final Action action = collection.get(i);
                if (action.isEnabled()) {
                    actions[actionsI++] = action;
                }
            }
            //noinspection unchecked
            return (T[]) actions;
        }

        @Override
        public boolean add(Action action) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean remove(Object o) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            for (Action action : collection) {
                if (!contains(action)) return false;
            }
            return true;
        }

        @Override
        public boolean addAll(Collection<? extends Action> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }
    }
}
