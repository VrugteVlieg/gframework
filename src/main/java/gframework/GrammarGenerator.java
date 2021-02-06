package gframework;

import java.util.LinkedList;
import java.util.List;


public class GrammarGenerator {

    //Maximum number of non terminals in a new grammar
    public static int MAX_NT_COUNT = 3;
    //Maximum number of symbols in a new rule
    

    public static List<Gram> generatePopulation(int size) {
        List<Gram> out = new LinkedList<>();
        for (int i = 0; i < size; i++) {
            String grammarName = "Grammar_" + Gram.genGramName();
            Gram currGrammar = Gram.namedGram(grammarName);
            int grammarRuleCount = 1 + Utils.randInt(MAX_NT_COUNT);
            for (int j = 0; j < grammarRuleCount; j++) {
                currGrammar.generateNonTerminal();
            }

            out.add(currGrammar);
        }
        return out;
    }

}
