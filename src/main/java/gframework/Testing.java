package gframework;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.ListTokenSource;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.Tool;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.tool.Grammar;
import org.antlr.v4.tool.ast.GrammarRootAST;


import static java.lang.String.format;

//Based on code gotten from Chelsea Barraball 19768125@sun.ac.za 
public class Testing {
    public static final String ANTLR_DIR = "./antlrOut";
    public static final String POS_TEST_SUITE_PATH = String.join("/", List.of("tests", Gram.CURR_GRAMMAR, "pos"));
    public static final String POS_SUITE_NAME = "positive tests";
    public static final String NEG_TEST_SUITE_PATH = String.join("/", List.of("tests", Gram.CURR_GRAMMAR, "neg"));
    public static final String NEG_SUITE_NAME = "negative tests";

    // Test suites
    static Map<String, List<String>> testSuites = new HashMap<>();

    /**
     * Loads testSuite located in suitePath and names it suiteName
     * @param suiteName
     * @param suitePath
     * @return
     */
    public static boolean loadTests(String suiteName, String suitePath) {

        if (testSuites.containsKey(suiteName))
            testSuites.remove(suiteName);
        List<String> toAdd = new LinkedList<>();

        try {
            for (Path path : Files.walk(Paths.get(suitePath)).filter(Files::isRegularFile)
                    .collect(Collectors.toList())) {

                StringBuilder rawContent = new StringBuilder(Files.readString(path));

                while (rawContent.indexOf("/*") != -1) {
                    rawContent.delete(rawContent.indexOf("/*"), rawContent.indexOf("*/") + 2);
                }

                String content = rawContent.toString().trim().replaceAll(" ", "");
                toAdd.add(content);

            }
            testSuites.put(suiteName, toAdd);
        } catch (Exception e) {
            System.err.println("Could not load tests in " + suitePath);
            return false;
        }

        return true;
    }

    /**
     * Generates the source files to be used by the following call to runTests
     * 
     * @param in
     */
    public static void generateSources(Gram in) {
        String finName = "default";
        try {
            // System.err.println("Generating sources for " + grammar);

            // Name of the file, for example ampl.g4
            finName = in.getName();
            // Setting up the arguments for the ANTLR Tool. outputDir is in this case
            // generated-sources/

            // args arrays for writing for normal testing and for writing to localiser
            String[] args = { "-o", ANTLR_DIR + "/" + finName };

            // Creating a new Tool object with org.antlr.v4.Tool

            Tool tool = new Tool(args);
            GrammarRootAST grast = tool.parseGrammarFromString(in.toString());

            // Create a new Grammar object from the tool
            Grammar g = tool.createGrammar(grast);

            g.fileName = in.getName();

            tool.process(g, true);
            // System.err.println("Tool done");
            if (tool.getNumErrors() != 0) {
                in.flagForRemoval();
                return;
            }

            // Compile source files
            DynamicClassCompiler dynamicClassCompiler = new DynamicClassCompiler();
            dynamicClassCompiler.compile(getOutputDir(in));

        } catch (Exception e) {
            e.printStackTrace();
            in.flagForRemoval();
        }
        // addToSlowGrammars(new retainedGrammar(stopwatch.elapsedTime(),
        // grammar.toString()));

    }

    public static int[] runTestcases(String suiteName, Gram in) throws IOException, IllegalAccessException,
            InvocationTargetException, InstantiationException, NoSuchMethodException {

        // output array {numPasses, numTests}
        if(!testSuites.containsKey(suiteName)) {
            System.err.println("Could not find testsuite with name " + suiteName + " in " + testSuites.keySet());
            return new int[]{0};
        }

        List<String> tests = testSuites.get(suiteName);

        int[] out = { 0, tests.size() };
        Stack<String> passingTests = new Stack<String>();
        Stack<String> failingTests = new Stack<String>();
        

        try {
            Map<String, Class<?>> hm = new DynamicClassLoader().load(getOutputDir(in));

            // Manually creates lexer.java file in outputDir
            Class<?> lexerC = hm.get(in.getName() + "Lexer");
            // Manually creates the lexerConstructor for use later
            // Is initialized as Constructor<?> lexerConstructor
            Constructor<?> lexerConstructor = lexerC.getConstructor(CharStream.class);
            String.class.getConstructor(String.class);

            Class<?> parserC = hm.get(in.getName() + "Parser");

            // Manually creates the parserConstructor for use later
            // Is initialized as Constructor<?> parserConstructor
            Constructor<?> parserConstructor = parserC.getConstructor(TokenStream.class);
            for (String test : tests) {
                Lexer lexer = (Lexer) lexerConstructor.newInstance(CharStreams.fromString(test));
                CommonTokenStream tokens = new CommonTokenStream(lexer);
                Parser parser = (Parser) parserConstructor.newInstance(tokens);

                //Uncomment this line if you want to see ANTLR ouput
                parser.removeErrorListeners();
                parser.setErrorHandler(new BailErrorStrategy());
                Method parseEntrypoint = parser.getClass().getMethod("program");
                try {
                    parseEntrypoint.invoke(parser);

                    passingTests.push(test);
                    out[0]++;
                    // If this code is reached the test case was accepeted and numPassing
                    // should be incremented
                } catch (Exception e) {
                    failingTests.push(test);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return out;

    }

    public static File getOutputDir(Gram in) {
        return new File(ANTLR_DIR + "/" + in.getName());
    }

    /**
     * Computes positive score based off of the fraction of tests accepted
     * @param testResult
     * @param GUT
     */
    public static void normalPositiveScoring(int[] testResult, Gram GUT) {
        int totalTests = testResult[1];

        double numPass = testResult[0];
        double out = numPass * 1.0 / totalTests;
        GUT.setPosScore(out);
    }
    /**
     * Computes negative score based off of the fraction of tests rejected
     * @param testResult
     * @param GUT
     */
    public static void normalNegativeScoring(int[] testResult, Gram GUT) {
        int totalTests = testResult[1];

        double numPass = totalTests - testResult[0];
        double out = numPass * 1.0 / totalTests;
        GUT.setNegScore(out);
    };
}
