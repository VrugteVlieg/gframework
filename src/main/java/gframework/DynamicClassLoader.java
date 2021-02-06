package gframework;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Dynamic class loader for instrumented directory.
 * Obtained from rom Chelsea Barraball 19768125@sun.ac.za
 */
public class DynamicClassLoader {

    /**
     * Recurses output instrumented directory and returns list of class files.
     *
     * @param directory Input directory.
     * @return ArrayList of class files.
     */
    private List<File> getDirectoryFiles(File directory) {
        List<File> fileList = new ArrayList<>();
        if (directory.isDirectory()) {
            for (File file : directory.listFiles()) {
                fileList.addAll(getDirectoryFiles(file));
            }
        } else {
            if (directory.getName().endsWith(".class")) {
                return Collections.singletonList(directory);
            }
        }
        return fileList;
    }


    /**
     * Responsible for the actual loading of classes.
     * Loads classes from output directory. Caters for presence of packages in directory.
     *
     * @param directory Input directory.
     * @return Hashmap of class names and their loaded versions.
     * @throws ClassNotFoundException
     * @throws MalformedURLException
     */
    public Map<String, Class<?>> load(File directory) throws ClassNotFoundException, MalformedURLException {
        assert directory.isDirectory();

        URL url = directory.toURI().toURL();
        URL[] urls = new URL[]{url};
        ClassLoader cl = new URLClassLoader(urls);

        Map<String, Class<?>> classList = new HashMap<>();
        Path absPath = Paths.get(directory.getAbsolutePath());
        // System.err.println(getDirectoryFiles(directory).stream().map(File::toString).collect(Collectors.joining("\n")));

        for (File javaFile : getDirectoryFiles(directory)) {

            Path relPath = absPath.relativize(Paths.get(javaFile.getParentFile().getAbsolutePath()));
            String classPackage = relPath.toString().replace("/", ".");
            if (!classPackage.isEmpty()) {
                classPackage = classPackage.concat(".");
            }
            String className = classPackage + javaFile.getName().replace(".class", "");
            Class<?> targetClass = cl.loadClass(className);
            classList.put(className, targetClass);
        }
        
        return classList;
    }


    

}
