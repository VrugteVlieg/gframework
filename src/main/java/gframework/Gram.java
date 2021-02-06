package gframework;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toCollection;

public class Gram implements Comparable<Gram> {

    private static final int GRAM_NAME_LENGTH = 5;
    private static final int MAX_RULE_SIZE = 3;
    public static final String CURR_GRAMMAR = "dyck";
    private static final String TERMINALS_PATH = String.join("/", List.of("grammars", CURR_GRAMMAR,  CURR_GRAMMAR + ".terminals"));


    //List of terminal rule names
    public static List<String> terminals = loadTerminals(TERMINALS_PATH);
    //Constant string for the terminal symbols of the current grammar
    static String terminalString;

    private String name;
    private ArrayList<Rule> nonTerminals = new ArrayList<>();
    private boolean toRemove = false;

    //Fraction of postive test cases passed
    private double posScore = 0.0;
    private double negScore = 0.0;

    /**
     * Constructs a grammar given the path to an ANTLR file
     * 
     * @param sourceFilePath
     */
    public static Gram fromFile(String sourceFilePath) {
        String content = "";
        try (BufferedReader in = new BufferedReader(new FileReader(sourceFilePath));) {
            content = in.lines().collect(Collectors.joining("\n"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new Gram(content);
    }

    /**
     * Constructs a grammar from an ANTLR file.
     * 
     * @param fileContent
     */
    public Gram(String fileContent) {
        List<String> lines = Arrays.stream(fileContent.split("\n")).filter(not(String::isBlank))
                .collect(toCollection(LinkedList<String>::new));
        String titleLine = lines.remove(0);
        name = titleLine.split(" ")[1].replace(";", "");
        readRules(lines);
    }

    public Gram() {

    }

    /**
     * Reads the nonterminals for a grammar line by line
     */
    private void readRules(List<String> fileLines) {
        fileLines.forEach(input -> {
            String[] ruleData = input.split(":");
            String RuleName = ruleData[0].trim();
            String RuleString = ruleData[1].trim(); // cuts off the ; at the end
            Rule currRule = new Rule(RuleName, RuleString);
            // currRule.addRule(RuleString);
            nonTerminals.add(currRule);
        });
    }

    public double getScore() {
        return (posScore + negScore)/2.0;
    }

    @Override
    public int compareTo(Gram other) {
        return Double.compare(getScore(), other.getScore());
    }

    /**
     * Load the terminal symbols shared by all grammars
     * 
     * @param path
     * @return
     */
    public static List<String> loadTerminals(String path) {
        List<String> out = new LinkedList<>();
        StringBuilder terminalBuilder = new StringBuilder();
        try (BufferedReader in = new BufferedReader(new FileReader(path))) {

            in.lines().forEach(l -> {
                String[] data = l.split(":", 2);
                out.add(data[0]);
                terminalBuilder.append(l + "\n");
            });

            terminalString = terminalBuilder.toString();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return out;
    }

    public static String genGramName() {
        StringBuilder nameBuilder = new StringBuilder();
        // Generates gram name by randomly concatting letters

        for (int nameIndex = 0; nameIndex < GRAM_NAME_LENGTH; nameIndex++) {
            nameBuilder.append((char) ('a' + Utils.randInt(26)));
        }
        // Check if the generated ruleName matches an existing parserRule, only
        // parserRules are checked as lowercase letters are used exclusively

        // clears the name builder if it is the reserved word program or a duplicate of
        // an existing ruleName
        if (nameBuilder.toString().equals("program"))
            nameBuilder.setLength(0);

        return nameBuilder.toString();
    }

	public static Gram namedGram(String grammarName) {
        Gram out = new Gram();
        out.name = grammarName;
        return out;
	}

    /**
     * Generates a new non-terminal name for use in this grammar
     * @return
     */
	public String genRuleName() {
		StringBuilder ruleNameBuilder = new StringBuilder();
        // Generates rule name by randomly concatting letters
        while (ruleNameBuilder.length() == 0) {
            for (int nameIndex = 0; nameIndex < Rule.MAX_RULE_NAME_LEN; nameIndex++) {
                ruleNameBuilder.append((char) ('a' + Utils.randInt(26)));
            }

            // Check if the generated ruleName matches an existing non-terminal, only
            // non-terminals are checked as lowercase letters are used exclusively
            String currName = ruleNameBuilder.toString();
            boolean duplicateRuleName = nonTerminals.stream().map(rule -> rule.name)
                    .anyMatch(currName::equals);

            // clears the name builder if it is the reserved word program or a duplicate of
            // an existing ruleName, program is reserved as it is injected as a rule when generating the parser
            if (ruleNameBuilder.toString().equals("program") || duplicateRuleName)
                ruleNameBuilder.setLength(0);
        }
        return ruleNameBuilder.toString();
	}

	public void generateNonTerminal() {
        String ruleName = genRuleName();
        int currRuleLen = 1 + Utils.randInt(MAX_RULE_SIZE - 1);
        StringBuilder currRHS = new StringBuilder();

        //All of the symbols that can appear on the RHS of the new 
        List<String> currentSymbols = new ArrayList<>();
        currentSymbols.addAll(terminals);
        nonTerminals.forEach(r -> currentSymbols.add(r.name));

        //The following may lead to a lot of LR recursion errors but I include it for completeness
        // currentSymbols.add(ruleName);

        for (int i = 0; i < currRuleLen; i++) {
            currRHS.append(Utils.randGet(currentSymbols, true) + " ");
        }
        currRHS.append(";");

        nonTerminals.add(new Rule(ruleName, currRHS.toString()));
        
	}

    /**
     * Applied genetic crossover to 2 parent grammars to produce offspring
     */
	public static List<Gram> crossover(Gram parent1, Gram parent2) {
        List<Gram> out = new LinkedList<>(List.of(parent1, parent2));
        //TODO the crossover operator you want to use
		return out;
    }
    
    /**
     * Perform any mutations 
     */
    public void mutate() {
        Mutations.activeMutations.stream()
        .filter(m -> m.choice() && m.preReqCheck(this))
        .forEach(m -> {
            m.apply(this);
            m.postCheck(this);
        });
    }

    public List<Rule> getNonTerminals() {
        return nonTerminals;
    }


    @Override
    public String toString() {
        StringBuilder out = new StringBuilder("grammar " + name + ";\n");
        nonTerminals.forEach(r -> out.append(r + "\n"));
        out.append(terminalString);

        //skips all whitespaces
        out.append("WS  : [ \\n\\r\\t]+ -> skip;");
        return out.toString();

    }

    public String getName() {
        return name;
    }

    public void flagForRemoval() {
        toRemove = true;
    }

    public boolean toRemove() {
        return toRemove;
    }

	public void injectEOF() {
        Rule wrapperRule = new Rule("program", nonTerminals.get(0).name + " EOF ;");
        nonTerminals.add(0, wrapperRule);
    }

	public void stripEOF() {
        nonTerminals.remove(0);
	}

	public void setPosScore(double out) {
	}

	public void setNegScore(double out) {
	}
    
}
