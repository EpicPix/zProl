package ga.epicpix.zprol;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

public class Parser {

    public static final char[] specialCharacters = "{".toCharArray();

    public static void parse(String fileName) throws IOException {
        File file = new File(fileName);
        if(!file.exists()) {
            throw new FileNotFoundException(fileName);
        }

        List<String> listLines = Files.readAllLines(file.toPath());
        listLines.removeIf((line) -> line.trim().startsWith("//"));
        listLines.removeIf((line) -> line.trim().isEmpty());
        String[] lines = new String[listLines.size()];
        for(int i = 0; i<lines.length; i++) {
            lines[i] = listLines.get(i).trim();
        }

        DataParser parser = new DataParser(lines);
        System.out.println(parser.nextWord()); // should be "structure"

    }

}
