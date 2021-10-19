package ga.epicpix.zprol;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Parser {

    public static void parse(String fileName) throws IOException {
        File file = new File(fileName);
        if(!file.exists()) {
            throw new FileNotFoundException(fileName);
        }

        List<String> lines = Files.readAllLines(file.toPath());
        lines.removeIf((line) -> line.trim().startsWith("//"));
        lines.removeIf((line) -> line.trim().isEmpty());

        ArrayList<String> words = new ArrayList<>();
        lines.forEach(line -> words.addAll(Arrays.asList(line.trim().split(" "))));

        System.out.println("Words: " + words);
    }

}
