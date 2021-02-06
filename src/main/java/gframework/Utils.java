package gframework;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class Utils {
    
    public static int randInt(int bound) {
        if (bound == 0)
            return 0;
        return ThreadLocalRandom.current().nextInt(bound);
    }

    public static <E> E randGet(List<E> input, boolean replace) {
        if (replace) {
            return input.get(randInt(input.size()));
        } else {
            return input.remove(randInt(input.size()));
        }
    }

    /**
     * Samples an individual from a population using tournament without selection
     * @param pop Population to sample from
     * @param tourSize Size of the tournament, bigger tournaments means higher selective pressure
     * @return
     */
    public static Gram tournamentSelect(List<Gram> pop, int tourSize) {
        // System.err.println("Performing tournament selection on a population of " +
        // pop.size());
        List<Gram> tour = new LinkedList<Gram>();
        while (tour.size() < tourSize) {
            tour.add(pop.get(randInt(pop.size())));
        }
        Gram out = tour.stream().max(Comparator.comparing(Gram::getScore)).get();
        pop.remove(out);
        return out;
    }

    /**
     * Removes all files in target directory, as well as the directory itself
     * 
     * @param directory
     */
    public static void deepCleanDirectory(File directory) {
        assert directory.isDirectory();
        List<File> toDelete = getDirectoryFiles(directory);
    
        toDelete.forEach(File::delete);
        directory.delete();
    }


     /**
     * Recurses directory and returns list files.
     *
     * @param directory Input directory.
     * @return ArrayList of class files.
     */
    public static List<File> getDirectoryFiles(File directory) {
        List<File> fileList = new ArrayList<>();
        if (directory.isDirectory()) {
            for (File file : directory.listFiles()) {
                fileList.addAll(getDirectoryFiles(file));
            }
        } else {
            return Collections.singletonList(directory);
        }
        return fileList;
    }
}
