/*
 * ******************************************************************************
 *  * Copyright 2015 See AUTHORS file.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *   http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *  *****************************************************************************
 */

package games.rednblack.editor.renderer.utils;

import com.artemis.Component;
import com.artemis.ComponentMapper;
import com.artemis.World;
import com.badlogic.gdx.utils.ObjectMap;
import games.rednblack.editor.renderer.SceneLoader;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * Component Retriever is a utility class that maintains access to registered {@link ComponentMapper}.
 * <BR />
 * All runtime's inbuilt {@link Component} are already registered by default.
 * <BR />
 * To register external components, call {@link ComponentRetriever#addComponentMappers(String, World)} after getting the World instance from {@link SceneLoader#getEngine()}.
 * <BR />
 *
 * @author azakhary on 5/19/2015.
 * @author Minecraftian14 on 8/2/2021. Ps. it's M/D/YY  xD
 */
public class ComponentRetriever {

    /**
     * A simple Map object from {@link Component}'s Class to corresponding {@link ComponentMapper}.
     * <p>
     * {@link ComponentMapper} is not used with Type data, "Because it's not really fixated to a single type!"
     * Well, it may still use "? extends Component" but why bother unnecessarily?
     */
    @SuppressWarnings("rawtypes")
    private static final ObjectMap<Class<? extends Component>, ComponentMapper> componentMappers = new ObjectMap<>();

    /**
     * Private constructor, dude, it's not even a singleton... Well, good ol' conventions.
     */
    private ComponentRetriever() {
    }

    public static void addComponentMapper(Class<? extends Component> clazz, World engine) {
        componentMappers.put(clazz, engine.getMapper(clazz));
    }

    public static void addComponentMappers(String packageName, World engine) {
        try {
            for (Class<? extends Component> componentClass : getComponentClasses(packageName))
                addComponentMapper(componentClass, engine);

        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Retrieves a list of all the classes in a given package which might be instances of Component.
     *
     * @param packageName The lower most package which may contain Component classes.
     * @return A list consisting of Class objects for all the Component classes found.
     */
    private static ArrayList<Class<? extends Component>> getComponentClasses(String packageName) throws ClassNotFoundException, IOException {

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        assert classLoader != null;

        String path = packageName.replace('.', '/');
        Enumeration<URL> resources = classLoader.getResources(path);

        ArrayList<Class<? extends Component>> classes = new ArrayList<>();

        while (resources.hasMoreElements()) {
            List<Class<? extends Component>> classList = findComponentClasses(new File(resources.nextElement().getFile()), packageName);

            if (classList != null) classes.addAll(classList);
        }

        return classes;
    }

    private static List<Class<? extends Component>> findComponentClasses(File directory, String packageName) throws ClassNotFoundException {
        if (!directory.exists()) return null;

        List<Class<? extends Component>> classes = new ArrayList<>();

        File[] files = directory.listFiles();
        assert files != null : "directory.listFiles() is null for directory = " + directory;

        for (File file : files) {

            if (file.isDirectory()) {

                assert !file.getName().contains(".") : "That came as a surprise! Why does file.getName() has a '.' when it's supposed to be a directory!";

                List<Class<? extends Component>> classList = findComponentClasses(file, packageName + "." + file.getName());
                if (classList != null)
                    classes.addAll(classList);

            } else if (file.getName().endsWith(".class")) {

                Class<?> clazz = Class.forName(packageName + '.' + file.getName().substring(0, file.getName().length() - 6));
                if (Component.class.isAssignableFrom(clazz)) {

                    Class<? extends Component> componentClazz = clazz.asSubclass(Component.class);
                    classes.add(componentClazz);

                }
            }
        }
        return classes;
    }

    /**
     * Returns the {@link ComponentMapper} associated with the required {@link Component}.
     */
    public static <T extends Component> ComponentMapper<T> getMapper(Class<T> mapper) {
        return componentMappers.get(mapper);
    }

    /**
     * Returns the specified {@link Component} associated with the given entity.
     * Returns null if the component was not added before.
     *
     * @see ComponentMapper#get(int)
     */
    public static <T extends Component> T get(int entity, Class<T> type) {
        return getMapper(type).get(entity);
    }

    /**
     * Returns the specified {@link Component} associated with the given entity.
     * If the {@link Component} is not present, creates a new {@link Component}.
     *
     * @see ComponentMapper#create(int)
     */
    public static <T extends Component> T create(int entity, Class<T> type) {
        return getMapper(type).create(entity);
    }

    /**
     * Removes the specified {@link Component} associated with the given entity.
     *
     * @see ComponentMapper#remove(int)
     */
    public static <T extends Component> void remove(int entity, Class<T> type) {
        getMapper(type).remove(entity);
    }

    /**
     * Checks if the specified {@link Component} is present in the given entity.
     *
     * @see ComponentMapper#has(int)
     */
    public static <T extends Component> boolean has(int entity, Class<T> type) {
        return getMapper(type).has(entity);
    }
}
