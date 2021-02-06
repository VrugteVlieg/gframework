package gframework;

import java.util.LinkedList;
import java.util.List;
import static java.lang.String.format;

public class Mutations {

    static boolean DEBUG = false;

    static List<Mutation> activeMutations = List.of(new pointMutation(), new heuristicMutation());

    public interface Mutation {

        /**
         * Whether or not this mutation is applied
         * @param input
         */
        public boolean choice();

        public void apply(Gram input);
    
        /**
         * Check if a grammar is a valid candidate for this mutation
         * @param input
         * @return true if the grammar can undergo this mutation, false otherwise
         */
        public boolean preReqCheck(Gram input);
    
        /**
         * Perform any cleanup actions on a grammar after applying this mutation
         * @param input
         * @return true if cleanup was performed successfully, false otherwise
         */
        public boolean postCheck(Gram input);
    }

    //Standard point mutation
    static class pointMutation implements Mutation {
        public static double choice = 0.2;

        private Rule targetRule;
        private Rule toReplace;
        private int indexToReplace;
        private int targetAlternativeIndex;

        private pointMutation() {

        }

        @Override
        public boolean choice() {
            return Math.random() < choice;
        }

        @Override
        public void apply(Gram input) {
            //Which non-terminal to change
            targetRule = Utils.randGet(input.getNonTerminals(), true);
            List<LinkedList<Rule>> alternatives = targetRule.alternatives;
            //Which alternative in the non terminal to mutate
            targetAlternativeIndex = Utils.randInt(alternatives.size());
            List<Rule> targetAlternative = alternatives.get(targetAlternativeIndex);
            
            indexToReplace = Utils.randInt(targetAlternative.size());
            //Which non terminal to put at the index
            toReplace = Utils.randGet(input.getNonTerminals(), true).copyOf();
            targetAlternative.set(indexToReplace, toReplace);
        }

        @Override
        public boolean preReqCheck(Gram input) {
            return true;
        }

        @Override
        public boolean postCheck(Gram input) {
            if(targetRule.name.equals(toReplace.name) && DEBUG) {
                System.err.println(format(
                    "Point mutation had no effect\ntargetRule: %s\nalternativeIndex %d\nindexToReplace: %d\nnewValue: %s"
                    , targetRule, targetAlternativeIndex, indexToReplace, toReplace));

                return true;
            }
            return false;
        }

    }

    /**
     * Applies the heuristic operator from Mernik et. al to a random symbol on the RHS of a random rule.
     */
    static class heuristicMutation implements Mutation {
        public static double choice = 0.3;


        private Rule targetRule;
        private int indexToChange;
        private int targetAlternativeIndex;
        private double heurChoice;

        private heuristicMutation() {

        }

        @Override
        public boolean choice() {
            return Math.random() < choice;
        }

        @Override
        public void apply(Gram input) {
            targetRule = Utils.randGet(input.getNonTerminals(), true);
            List<LinkedList<Rule>> alternatives = targetRule.alternatives;
            targetAlternativeIndex = Utils.randInt(alternatives.size());
            List<Rule> targetAlternative = alternatives.get(targetAlternativeIndex);
            indexToChange = Utils.randInt(targetAlternative.size());
            targetRule = targetAlternative.get(indexToChange);

            heurChoice = Math.random();
            if(heurChoice < 0.33) {
                targetRule.flipOptional();
            } else if(heurChoice < 0.66) {
                targetRule.flipIterative();
            } else {
                targetRule.flipOptional();
                targetRule.flipIterative();
            }


        }

        @Override
        public boolean preReqCheck(Gram input) {
            return true;
        }

        @Override
        public boolean postCheck(Gram input) {
            return false;
        }

    }
}
