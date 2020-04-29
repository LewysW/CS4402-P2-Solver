import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Stack;

import static java.lang.System.exit;

public class FCSolver extends Solver {
    public FCSolver(BinaryCSP binaryCSP, Heuristic heuristic) {
        super(binaryCSP, heuristic);
    }

    public void solve() {
        forwardChecking();
    }

    private void forwardChecking() {
        if (completeAssignment()) {
            if (solution()) {
                printSolution();
                exit(0);
            }
        } else {
            int var = selectVar();
            int val = selectVal(domain(var));

            branchFCLeft(var, val);
            branchFCRight(var, val);
        }
    }

    private void branchFCLeft(int var, int val) {
        assign(var, val);
        Stack<BinaryTuple> pruned = new Stack<>();
        if (reviseFutureArcs(var, pruned)) {
            forwardChecking();
        }
        undoPruning(pruned);
        unassign(var);
    }

    private void branchFCRight(int var, int val) {
        remove(val, var);
        Stack<BinaryTuple> pruned = new Stack<>();
        if (!empty(domain(var))) {
            if (reviseFutureArcs(var, pruned)) {
                forwardChecking();
            }
            undoPruning(pruned);
        }
        restore(val, var);
    }

    private boolean reviseFutureArcs(int var, Stack<BinaryTuple> pruned) {
        Iterator<Integer> iterator = varList.iterator();

        //For each future variable which is not var
        while (iterator.hasNext()) {
            int futureVar = iterator.next();

            if (!(futureVar == var)) {
                try {
                    //If an arc exists between the two variables
                    if (constraints.containsKey(new Arc(futureVar, var))) {
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

    //Restores pruned value to variable
    private void undoPruning(Stack<BinaryTuple> pruned) {
        while (!pruned.empty()) {
            int val = pruned.peek().getVal2();
            int var = pruned.pop().getVal1();
            restore(val, var);
        }
    }
}
