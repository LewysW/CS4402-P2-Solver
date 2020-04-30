import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Stack;

import static java.lang.System.exit;

public class FCSolver extends Solver {
    public FCSolver(BinaryCSP binaryCSP, Heuristic heuristic) {
        super(binaryCSP, heuristic);
    }

    public void solve() {
        LinkedHashSet<Integer> varList = new LinkedHashSet<>();

        for (int v = 0; v < binaryCSP.getNoVariables(); v++) {
            varList.add(v);
        }

        forwardChecking(varList);
    }

    private void forwardChecking(LinkedHashSet<Integer> varList) {
        if (completeAssignment()) {
            printSolution();
            exit(0);
        } else {
            int var = selectVar(varList);
            int val = selectVal(domains.get(var));

            branchFCLeft(varList, var, val);
            branchFCRight(varList, var, val);
        }
    }

    private void branchFCLeft(LinkedHashSet<Integer> varList, int var, int val) {
        Stack<BinaryTuple> pruned = new Stack<>();
        assign(var, val, pruned);
        if (reviseFutureArcs(varList, var, pruned)) {
            //Pass in value of varList - var
            LinkedHashSet<Integer> subset = (LinkedHashSet<Integer>) varList.clone();
            subset.remove(var);
            forwardChecking(subset);
        }
        undoPruning(pruned);
        unassign(var);
    }

    private void branchFCRight(LinkedHashSet<Integer> varList, int var, int val) {
        remove(val, var);
        Stack<BinaryTuple> pruned = new Stack<>();
        if (!domains.get(var).isEmpty()) {
            if (reviseFutureArcs(varList, var, pruned)) {
                forwardChecking(varList);
            }
            undoPruning(pruned);
        }
        restore(val, var);
    }

    private boolean reviseFutureArcs(LinkedHashSet<Integer> varList, int var, Stack<BinaryTuple> pruned) {
        //For each future variable which is not var
        for (int futureVar : varList) {
            if (!(futureVar == var)) {
                try {
                    //If an arc exists between the two variables
                    if (constraints.containsKey(var) && constraints.get(var).containsKey(futureVar)) {
                        revise(arc(futureVar, var), pruned);
                    }
                 //Returns false iff a domain is emptied by a revision
                } catch (DomainEmptyException e) {
                    return false;
                }
            }
        }

        return true;
    }
}
