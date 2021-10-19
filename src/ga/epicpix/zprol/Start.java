package ga.epicpix.zprol;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class Start {

    public static void main(String[] args) throws IOException {
        if(args.length == 0) {
            throw new IllegalArgumentException("File not specified");
        }
        String fileName = String.join(" ", args);
        File file = new File(fileName);
        if(!file.exists()) {
            throw new FileNotFoundException(fileName);
        }
    }

}
