package org.myspring.core;

import org.myspring.core.util.Assert;
import org.myspring.core.util.LinkedMultiValueMap;
import org.myspring.core.util.MultiValueMap;

import java.util.*;

public abstract class CollectionFactory {

    private static final Set<Class<?>> approximableCollectionTypes = new HashSet<Class<?>>();

    private static final Set<Class<?>> approximableMapTypes = new HashSet<Class<?>>();


    static {
        // Standard collection interfaces
        approximableCollectionTypes.add(Collection.class);
        approximableCollectionTypes.add(List.class);
        approximableCollectionTypes.add(Set.class);
        approximableCollectionTypes.add(SortedSet.class);
        approximableCollectionTypes.add(NavigableSet.class);
        approximableMapTypes.add(Map.class);
        approximableMapTypes.add(SortedMap.class);
        approximableMapTypes.add(NavigableMap.class);

        // Common concrete collection classes
        approximableCollectionTypes.add(ArrayList.class);
        approximableCollectionTypes.add(LinkedList.class);
        approximableCollectionTypes.add(HashSet.class);
        approximableCollectionTypes.add(LinkedHashSet.class);
        approximableCollectionTypes.add(TreeSet.class);
        approximableCollectionTypes.add(EnumSet.class);
        approximableMapTypes.add(HashMap.class);
        approximableMapTypes.add(LinkedHashMap.class);
        approximableMapTypes.add(TreeMap.class);
        approximableMapTypes.add(EnumMap.class);
    }



    public static boolean isApproximableCollectionType(Class<?> collectionType) {
        return (collectionType != null && approximableCollectionTypes.contains(collectionType));
    }


    @SuppressWarnings({ "unchecked", "cast", "rawtypes" })
    public static <E> Collection<E> createApproximateCollection(Object collection, int capacity) {
        if (collection instanceof LinkedList) {
            return new LinkedList<E>();
        }
        else if (collection instanceof List) {
            return new ArrayList<E>(capacity);
        }
        else if (collection instanceof EnumSet) {
            // Cast is necessary for compilation in Eclipse 4.4.1.
            Collection<E> enumSet = (Collection<E>) EnumSet.copyOf((EnumSet) collection);
            enumSet.clear();
            return enumSet;
        }
        else if (collection instanceof SortedSet) {
            return new TreeSet<E>(((SortedSet<E>) collection).comparator());
        }
        else {
            return new LinkedHashSet<E>(capacity);
        }
    }

    /**
     * Create the most appropriate collection for the given collection type.
     * <p>Delegates to {@link #createCollection(Class, Class, int)} with a
     * {@code null} element type.
     * @param collectionType the desired type of the target collection; never {@code null}
     * @param capacity the initial capacity
     * @return a new collection instance
     * @throws IllegalArgumentException if the supplied {@code collectionType}
     * is {@code null} or of type {@link EnumSet}
     */
    public static <E> Collection<E> createCollection(Class<?> collectionType, int capacity) {
        return createCollection(collectionType, null, capacity);
    }

    @SuppressWarnings({ "unchecked", "cast" })
    public static <E> Collection<E> createCollection(Class<?> collectionType, Class<?> elementType, int capacity) {
        Assert.notNull(collectionType, "Collection type must not be null");
        if (collectionType.isInterface()) {
            if (Set.class == collectionType || Collection.class == collectionType) {
                return new LinkedHashSet<E>(capacity);
            }
            else if (List.class == collectionType) {
                return new ArrayList<E>(capacity);
            }
            else if (SortedSet.class == collectionType || NavigableSet.class == collectionType) {
                return new TreeSet<E>();
            }
            else {
                throw new IllegalArgumentException("Unsupported Collection interface: " + collectionType.getName());
            }
        }
        else if (EnumSet.class == collectionType) {
            Assert.notNull(elementType, "Cannot create EnumSet for unknown element type");
            // Cast is necessary for compilation in Eclipse 4.4.1.
            return (Collection<E>) EnumSet.noneOf(asEnumType(elementType));
        }
        else {
            if (!Collection.class.isAssignableFrom(collectionType)) {
                throw new IllegalArgumentException("Unsupported Collection type: " + collectionType.getName());
            }
            try {
                return (Collection<E>) collectionType.newInstance();
            }
            catch (Throwable ex) {
                throw new IllegalArgumentException(
                        "Could not instantiate Collection type: " + collectionType.getName(), ex);
            }
        }
    }

    /**
     * Determine whether the given map type is an <em>approximable</em> type,
     * i.e. a type that {@link #createApproximateMap} can approximate.
     * @param mapType the map type to check
     * @return {@code true} if the type is <em>approximable</em>
     */
    public static boolean isApproximableMapType(Class<?> mapType) {
        return (mapType != null && approximableMapTypes.contains(mapType));
    }


    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <K, V> Map<K, V> createApproximateMap(Object map, int capacity) {
        if (map instanceof EnumMap) {
            EnumMap enumMap = new EnumMap((EnumMap) map);
            enumMap.clear();
            return enumMap;
        }
        else if (map instanceof SortedMap) {
            return new TreeMap<K, V>(((SortedMap<K, V>) map).comparator());
        }
        else {
            return new LinkedHashMap<K, V>(capacity);
        }
    }


    public static <K, V> Map<K, V> createMap(Class<?> mapType, int capacity) {
        return createMap(mapType, null, capacity);
    }


    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <K, V> Map<K, V> createMap(Class<?> mapType, Class<?> keyType, int capacity) {
        Assert.notNull(mapType, "Map type must not be null");
        if (mapType.isInterface()) {
            if (Map.class == mapType) {
                return new LinkedHashMap<K, V>(capacity);
            }
            else if (SortedMap.class == mapType || NavigableMap.class == mapType) {
                return new TreeMap<K, V>();
            }
            else if (MultiValueMap.class == mapType) {
                return new LinkedMultiValueMap();
            }
            else {
                throw new IllegalArgumentException("Unsupported Map interface: " + mapType.getName());
            }
        }
        else if (EnumMap.class == mapType) {
            Assert.notNull(keyType, "Cannot create EnumMap for unknown key type");
            return new EnumMap(asEnumType(keyType));
        }
        else {
            if (!Map.class.isAssignableFrom(mapType)) {
                throw new IllegalArgumentException("Unsupported Map type: " + mapType.getName());
            }
            try {
                return (Map<K, V>) mapType.newInstance();
            }
            catch (Throwable ex) {
                throw new IllegalArgumentException("Could not instantiate Map type: " + mapType.getName(), ex);
            }
        }
    }

    /**
     * Create a variant of {@code java.util.Properties} that automatically adapts
     * non-String values to String representations on {@link Properties#getProperty}.
     * @return a new {@code Properties} instance
     * @since 4.3.4
     */
    @SuppressWarnings("serial")
    public static Properties createStringAdaptingProperties() {
        return new Properties() {
            @Override
            public String getProperty(String key) {
                Object value = get(key);
                return (value != null ? value.toString() : null);
            }
        };
    }

    /**
     * Cast the given type to a subtype of {@link Enum}.
     * @param enumType the enum type, never {@code null}
     * @return the given type as subtype of {@link Enum}
     * @throws IllegalArgumentException if the given type is not a subtype of {@link Enum}
     */
    @SuppressWarnings("rawtypes")
    private static Class<? extends Enum> asEnumType(Class<?> enumType) {
        Assert.notNull(enumType, "Enum type must not be null");
        if (!Enum.class.isAssignableFrom(enumType)) {
            throw new IllegalArgumentException("Supplied type is not an enum: " + enumType.getName());
        }
        return enumType.asSubclass(Enum.class);
    }

}
