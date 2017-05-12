package ib053.core;

import com.koloboke.collect.map.LongObjMap;
import com.koloboke.collect.map.ObjObjMap;
import com.koloboke.collect.map.hash.HashLongObjMaps;
import com.koloboke.collect.map.hash.HashObjObjMaps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Used only in {@link GameCore} to hold instances of Activities.
 */
final class ActivityCache {

    private static final Logger LOG = LoggerFactory.getLogger(ActivityCache.class);

    private final File activityFile;

    /** Cache of which Activity implementation has which type (determined by its {@link Activity} annotation) */
    private final ObjObjMap<Class<? extends ActivityBase>, ActivityType> activityTypes = HashObjObjMaps.newMutableMap();
    /** Cache of activities that are singletons */
    private final ObjObjMap<Class<? extends ActivityBase>, ActivityBase> singletonActivities = HashObjObjMaps.newMutableMap();
    /** Cache of activities that are singletons per location */
    private final ObjObjMap<Class<? extends ActivityBase>, LongObjMap<ActivityBase>> locationActivities = HashObjObjMaps.newMutableMap();

    ActivityCache(File activityFile) {
        this.activityFile = activityFile;
    }

    ActivityType getActivityType(Class<? extends ActivityBase> activityClass) {
        final ActivityType result = activityTypes.get(activityClass);
        if (result != null) return result;

        final Activity activity = activityClass.getDeclaredAnnotation(Activity.class);
        if (activity == null) {
            LOG.error("Activity {} has no Activity annotation", activityClass.getCanonicalName());
            throw new IllegalArgumentException();
        }

        final ActivityType value = activity.value();
        LOG.debug("Activity {} is of type {}", activityClass.getCanonicalName(), value);
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

    ActivityBase getSingletonActivity(Class<? extends ActivityBase> activity) {
        assert getActivityType(activity) == ActivityType.SINGLETON_ACTIVITY : activity+" is not a singleton activity";

        ActivityBase singleton = singletonActivities.get(activity);
        if (singleton == null) {
            singleton = instantiate(activity);
            singletonActivities.put(activity, singleton);
        }
        return singleton;
    }

    ActivityBase getLocationActivity(Class<? extends ActivityBase> activity, Location location) {
        assert getActivityType(activity) == ActivityType.PER_LOCATION_ACTIVITY : activity+" is not a location activity";

        LongObjMap<ActivityBase> locationMap = locationActivities.get(activity);
        if (locationMap == null) {
            locationMap = HashLongObjMaps.newMutableMap();
            locationActivities.put(activity, locationMap);
        }

        ActivityBase instance = locationMap.get(location.id);
        if (instance == null) {
            instance = instantiate(activity, location);
            locationMap.put(location.id, instance);
        }

        return instance;
    }

    void load() {}

    void save() {}
}
