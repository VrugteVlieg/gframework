package gframework;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class Rule {

    public static final int MAX_RULE_NAME_LEN = 5;

    public String name;
    ArrayList<LinkedList<Rule>> alternatives;
    private StringBuilder sb = new StringBuilder();
    private boolean optional;
    private boolean iterative;
    private boolean isEpsilon = false;

    private Rule() {

    }

    public Rule copyOf() {
        Rule out = new Rule();
        out.name = name;
        return out;
    }

    private static Rule rhsRule(String name) {
        Rule out = new Rule();
        out.name = name;
        return out;
    }

    /**
     * Constructs a rule given a name and a RHS according to ANTLR specifcation
     * 
     * @param ruleName
     * @param ruleText
     */
    public Rule(String ruleName, String ruleText) {
        name = ruleName;
        alternatives = parseRule(ruleText);
    }

    public Rule terminalRule(String name) {
        Rule out = new Rule();
        out.name = name;
        return out;
    }

    public static Rule EPSILON() {
        Rule out = new Rule();
        out.isEpsilon = true;

        return out;
    }

    public static ArrayList<LinkedList<Rule>> parseRule(String ruleText) {
        ArrayList<LinkedList<Rule>> out = new ArrayList<>();
        LinkedList<Rule> currAlternative = new LinkedList<>();
        int index = 0;
        char currChar;
        StringBuilder currRule = new StringBuilder();

        while (index < ruleText.length()) {
            currChar = ruleText.charAt(index);

            switch (currChar) {
                case '|':
                    if (currAlternative.isEmpty()) {
                        currAlternative.add(Rule.EPSILON());
                    } else if (currRule.length() == 0) {
                        out.add(currAlternative);
                        currAlternative = new LinkedList<>();
                    } else {
                        System.err.println("Encountered rogue | when parsing " + ruleText.substring(0, index) + ">"
                                + currChar + "<" + ruleText.substring(index + 1));
                    }
                    break;

                case ';':
                    if (currAlternative.isEmpty()) {
                        currAlternative.add(Rule.EPSILON());
                    } else if (currRule.length() > 0) {
                        currAlternative.add(rhsRule(currRule.toString()));
                    } else if(!currAlternative.isEmpty()) {
                        out.add(currAlternative);
                        currAlternative = new LinkedList<>();
                    } else {
                        System.err.println(out.size());
                        System.err.println("Encountered rogue ; when parsing " + ruleText.substring(0, index) + ">"
                                + currChar + "<" + ruleText.substring(index + 1));
                    }
                    break;

                case '*':
                    currAlternative.add(rhsRule(currRule.toString()));
                    currAlternative.getLast().iterative = true;
                    currAlternative.getLast().optional = true;
                    break;

                case '?':
                    currAlternative.add(rhsRule(currRule.toString()));
                    currAlternative.getLast().optional = true;
                    break;

                case '+':
                    currAlternative.add(rhsRule(currRule.toString()));
                    currAlternative.getLast().iterative = true;
                    break;

                case ' ':
                    if (currRule.length() > 0) {
                        // System.err.println("adding rhs from " + currRule.toString());
                        currAlternative.add(rhsRule(currRule.toString()));
                        currRule.setLength(0);
                    }
                    break;

                default:
                    if (Character.isAlphabetic(currChar)) {
                        // System.err.println("Appending " + currChar + " to " + currRule.toString());
                        currRule.append(currChar);
                    } else {
                        System.err.println("Read " + currChar + " when parsing " + ruleText.substring(0, index) + ">"
                                + currChar + "<" + ruleText.substring(index + 1));
                    }

            }

            index++;
        }
        return out;

    }

    public void flipOptional() {
        optional = !optional;
    }

    public void flipIterative() {
        iterative = !iterative;
    }

    public boolean isEps() {
        return isEpsilon;
    }

    @Override
    public String toString() {
        if (isEpsilon)
            return " ";

        sb.setLength(0);
        if (alternatives == null) {
            sb.append(name);
            if (optional && iterative) {
                sb.append("*");
            } else if (optional) {
                sb.append("?");
            } else if (iterative) {
                sb.append("+");
            }
            return sb.toString();
        } else {
            sb.append(name + " : ");
            sb.append(alternatives.stream().map(l -> l.stream()
                        .map(Rule::toString)
                        .collect(Collectors.joining(" ")))
                    .collect(Collectors.joining(" | ")));
            sb.append(";");
            return sb.toString();
        }
    }
}
