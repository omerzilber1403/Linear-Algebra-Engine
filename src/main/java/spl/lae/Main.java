package spl.lae;
import java.io.IOException;
import java.text.ParseException;

import parser.*;

public class Main {
    public static void main(String[] args) throws IOException {
        // TODO: main
        LinearAlgebraEngine LAE=new LinearAlgebraEngine(10);
        InputParser inputParser=new InputParser();
        try {
            ComputationNode root=inputParser.parse("example.json");
            LAE.run(root);
            OutputWriter.write(root.getMatrix(), "My_out.json");
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }finally {
            try {
                LAE.shutdown();
            }
            catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

    }
}