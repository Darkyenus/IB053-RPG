package ib053.core;

import com.esotericsoftware.jsonbeans.JsonReader;
import com.esotericsoftware.jsonbeans.JsonValue;
import com.koloboke.collect.map.LongObjMap;
import com.koloboke.collect.map.ObjObjMap;
import com.koloboke.collect.map.hash.HashLongObjMaps;
import com.koloboke.collect.map.hash.HashObjObjMaps;
import com.koloboke.collect.set.ObjSet;
import com.koloboke.collect.set.hash.HashObjSets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

/**
 * Used only in {@link GameCore} to hold instances of Activities.
 */
final class ActivityCache {

    private static final Logger LOG = LoggerFactory.getLogger(ActivityCache.class);

    private final File activityFile;
    private final GameCore gameCore;

    /** Cache of which Activity implementation has which type (determined by its {@link Activity} annotation) */
    private final ObjObjMap<Class<? extends ActivityBase>, ActivityType> activityTypes = HashObjObjMaps.newMutableMap();
    /** Cache of activities that are singletons */
    private final ObjObjMap<Class<? extends ActivityBase>, ActivityBase> singletonActivities = HashObjObjMaps.newMutableMap();
    /** Cache of activities that are singletons per location */
    private final ObjObjMap<Class<? extends ActivityBase>, LongObjMap<ActivityBase>> locationActivities = HashObjObjMaps.newMutableMap();
    /** Contains all custom activities in which players are */
    private final ObjSet<ActivityBase> customActivities = HashObjSets.newMutableSet();

    ActivityCache(File activityFile, GameCore gameCore) {
        this.activityFile = activityFile;
        this.gameCore = gameCore;
    }

    ActivityType getActivityType(Class<? extends ActivityBase> activityClass) {
        final ActivityType result = activityTypes.get(activityClass);
        if (result != null) return result;

        final Activity activity = activityClass.getDeclaredAnnotation(Activity.class);
        if (activity == null) {
            LOG.error("Activity {} has no Activity annotation", activityClass.getName());
            throw new IllegalArgumentException();
        }

        final ActivityType value = activity.value();
        LOG.debug("Activity {} is of type {}", activityClass.getName(), value);
        activityTypes.put(activityClass, value);
        return value;
    }

    static <T> T instantiate(Class<T> type) {
        try {
            final Constructor<T> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
            LOG.error("Can't instantiate {}", type, e);
            throw new IllegalArgumentException();
        }
    }

    private static <T> T instantiate(Class<T> type, Location location) {
        try {
            final Constructor<T> constructor = type.getDeclaredConstructor(Location.class);
            constructor.setAccessible(true);
            return constructor.newInstance(location);
        } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
            LOG.error("Can't instantiate with Location {}", type, e);
            throw new IllegalArgumentException();
        }
    }

    void ensureInitialized(ActivityBase activity) {
        if (activity.core == null) {
            activity.core = gameCore;
            activity.initialize();
        } else if (activity.core != gameCore) {
            throw new IllegalArgumentException("Activity "+activity+" already assigned to a different core!");
        }
    }

    ActivityBase getSingletonActivity(Class<? extends ActivityBase> activity) {
        assert getActivityType(activity) == ActivityType.SINGLETON_ACTIVITY : activity+" is not a singleton activity";

        ActivityBase singleton = singletonActivities.get(activity);
        if (singleton == null) {
            singleton = instantiate(activity);
            ensureInitialized(singleton);
            singletonActivities.put(activity, singleton);
        }
        return singleton;
    }

    private LongObjMap<ActivityBase> getPerLocationActivityMap(Class<? extends ActivityBase> activity) {
        LongObjMap<ActivityBase> locationMap = locationActivities.get(activity);
        if (locationMap == null) {
            locationMap = HashLongObjMaps.newMutableMap();
            locationActivities.put(activity, locationMap);
        }
        return locationMap;
    }

    ActivityBase getLocationActivity(Class<? extends ActivityBase> activity, Location location) {
        assert getActivityType(activity) == ActivityType.PER_LOCATION_ACTIVITY : activity+" is not a location activity";

        final LongObjMap<ActivityBase> locationMap = getPerLocationActivityMap(activity);

        ActivityBase instance = locationMap.get(location.id);
        if (instance == null) {
            instance = instantiate(activity, location);
            ensureInitialized(instance);
            locationMap.put(location.id, instance);
        }

        return instance;
    }

    void refreshPossiblyCustomActivity(ActivityBase activity) {
        if (getActivityType(activity.getClass()) != ActivityType.CUSTOM_ACTIVITY) return;
        int engagedPlayerCount = activity.engagedPlayers.size();
        if (engagedPlayerCount == 0) {
            // Everybody just left
            customActivities.remove(activity);
        } else if (engagedPlayerCount == 1) {
            // Either somebody just entered, or second person just left
            customActivities.add(activity);
        }
    }

    private void loadEngagedPlayers(ActivityBase activity, JsonValue activityJson) {
        JsonValue engagedPlayersJson = activityJson.get("_engagedPlayers");
        if (engagedPlayersJson == null || engagedPlayersJson.size == 0) return;

        for (JsonValue engagedPlayerIdJson : engagedPlayersJson) {
            final Player engagedPlayer = gameCore.findPlayer(engagedPlayerIdJson.asLong());
            if (engagedPlayer == null) {
                LOG.warn("Not adding player {} to activity {}, because player does not exist", engagedPlayerIdJson, activity);
                continue;
            }
            activity.engagedPlayers.add(engagedPlayer);
            engagedPlayer.currentActivity = activity;
        }
    }

    boolean load() {
        if (!activityFile.exists()) {
            LOG.info("Not loading activity file {}, because it doesn't exist", activityFile);
            return true;
        }
        int activitiesLoaded = 0;
        boolean error = false;
        final JsonValue activityJson = new JsonReader().parse(activityFile);

        // Singleton
        for (JsonValue singletonJson : activityJson.get("singleton")) {
            final Class<? extends ActivityBase> singletonClass;
            try {
                //noinspection unchecked
                singletonClass = (Class<? extends ActivityBase>) Class.forName(singletonJson.name());
            } catch (ClassNotFoundException e) {
                LOG.error("Not restoring state of singleton activity {}", singletonJson.name(), e);
                error = true;
                continue;
            }

            final ActivityType activityType = getActivityType(singletonClass);
            if (activityType != ActivityType.SINGLETON_ACTIVITY) {
                LOG.error("Not restoring state of singleton activity {}, because its real type is {}", singletonJson.name(), activityType);
                error = true;
                continue;
            }

            final ActivityBase singleton = instantiate(singletonClass);

            if (singleton instanceof ActivityBase.Serializable) {
                ((ActivityBase.Serializable) singleton).read(gameCore, singletonJson);
            }

            ensureInitialized(singleton);
            loadEngagedPlayers(singleton, singletonJson);

            singletonActivities.put(singletonClass, singleton);
            activitiesLoaded++;
        }

        // Per-location
        for (JsonValue perLocationJson : activityJson.get("per-location")) {
            final Class<? extends ActivityBase> perLocationClass;
            try {
                //noinspection unchecked
                perLocationClass = (Class<? extends ActivityBase>) Class.forName(perLocationJson.name());
            } catch (ClassNotFoundException e) {
                LOG.error("Not restoring state of per-location activity {}", perLocationJson.name(), e);
                error = true;
                continue;
            }

            final ActivityType activityType = getActivityType(perLocationClass);
            if (activityType != ActivityType.PER_LOCATION_ACTIVITY) {
                LOG.error("Not restoring state of per-location activity {}, because its real type is {}", perLocationJson.name(), activityType);
                error = true;
                continue;
            }

            final LongObjMap<ActivityBase> locationMap = getPerLocationActivityMap(perLocationClass);

            for (JsonValue instanceJson : perLocationJson) {
                final long locationId = Long.parseLong(instanceJson.name());
                final Location location = gameCore.findLocation(locationId);
                if (location == null) {
                    LOG.error("Not restoring state of per-location activity {} for location {}, because the location no longer exists", perLocationClass, locationId);
                    error = true;
                    continue;
                }

                final ActivityBase instance = instantiate(perLocationClass, location);

                if (instance instanceof ActivityBase.Serializable) {
                    ((ActivityBase.Serializable) instance).read(gameCore, instanceJson);
                }
                ensureInitialized(instance);
                loadEngagedPlayers(instance, instanceJson);

                locationMap.put(locationId, instance);
                activitiesLoaded++;
            }
        }

        // Custom
        {
            for (JsonValue customActivityJson : activityJson.get("custom")) {
                final Class<? extends ActivityBase> customClass;
                try {
                    //noinspection unchecked
                    customClass = (Class<? extends ActivityBase>) Class.forName(customActivityJson.getString("_class"));
                } catch (ClassNotFoundException e) {
                    LOG.error("Not restoring state of custom activity {}", customActivityJson, e);
                    error = true;
                    continue;
                }

                final ActivityType activityType = getActivityType(customClass);
                if (activityType != ActivityType.CUSTOM_ACTIVITY) {
                    LOG.error("Not restoring state of custom activity {}, because its real type is {}", customClass.getName(), activityType);
                    error = true;
                    continue;
                }

                final ActivityBase instance = instantiate(customClass);

                if (instance instanceof ActivityBase.Serializable) {
                    ((ActivityBase.Serializable) instance).read(gameCore, customActivityJson);
                }
                ensureInitialized(instance);
                loadEngagedPlayers(instance, customActivityJson);

                customActivities.add(instance);
                activitiesLoaded++;
            }
        }

        if (error) {
            LOG.error("Loading of activities failed");
            return false;
        } else {
            LOG.info("Loaded {} stateful activities", activitiesLoaded);
            return true;
        }
    }

    void save() {
        try {
            PersistenceUtil.saveJsonSecurely(activityFile, json -> {
                json.writeObjectStart();
                {
                    json.writeObjectStart("singleton");
                    for (ActivityBase singleton : singletonActivities.values()) {

                        boolean serializable = singleton instanceof ActivityBase.Serializable;
                        boolean hasPlayers = !singleton.engagedPlayers.isEmpty();
                        if (serializable || hasPlayers) {
                            json.writeObjectStart(singleton.getClass().getName());

                            if (hasPlayers) {
                                json.writeArrayStart("_engagedPlayers");
                                for (Player engagedPlayer : singleton.engagedPlayers) {
                                    json.writeValue(engagedPlayer.getId(), long.class);
                                }
                                json.writeArrayEnd();
                            }

                            if (serializable) {
                                ((ActivityBase.Serializable) singleton).write(json);
                            }

                            json.writeObjectEnd();
                        }
                    }
                    json.writeObjectEnd();
                }

                {
                    json.writeObjectStart("per-location");
                    for (Map.Entry<Class<? extends ActivityBase>, LongObjMap<ActivityBase>> entry : locationActivities.entrySet()) {
                        final LongObjMap<ActivityBase> locations = entry.getValue();
                        final Class<? extends ActivityBase> activityType = entry.getKey();
                        boolean serializable = ActivityBase.Serializable.class.isAssignableFrom(activityType);
                        if (locations.isEmpty()) continue;
                        json.writeObjectStart(activityType.getName());

                        for (Map.Entry<Long, ActivityBase> locationEntry : locations.entrySet()) {
                            final Long locationId = locationEntry.getKey();
                            final ActivityBase activity = locationEntry.getValue();

                            json.writeObjectStart(locationId.toString());

                            if (!activity.engagedPlayers.isEmpty()) {
                                json.writeArrayStart("_engagedPlayers");
                                for (Player engagedPlayer : activity.engagedPlayers) {
                                    json.writeValue(engagedPlayer.getId(), long.class);
                                }
                                json.writeArrayEnd();
                            }

                            if (serializable) {
                                ((ActivityBase.Serializable) activity).write(json);
                            }

                            json.writeObjectEnd();
                        }

                        json.writeObjectEnd();
                    }
                    json.writeObjectEnd();
                }

                {
                    json.writeArrayStart("custom");

                    for (ActivityBase customActivity : customActivities) {
                        json.writeObjectStart();

                        json.writeValue("_class", customActivity.getClass().getName(), String.class);

                        json.writeArrayStart("_engagedPlayers");
                        for (Player engagedPlayer : customActivity.engagedPlayers) {
                            json.writeValue(engagedPlayer.getId(), long.class);
                        }
                        json.writeArrayEnd();

                        if (customActivity instanceof ActivityBase.Serializable) {
                            ((ActivityBase.Serializable) customActivity).write(json);
                        }

                        json.writeObjectEnd();
                    }

                    json.writeArrayEnd();
                }
                json.writeObjectEnd();
            });
            LOG.info("Activities saved");
        } catch (PersistenceUtil.PersistenceException e) {
            LOG.error("Failed to save activities", e);
        }
    }
}
