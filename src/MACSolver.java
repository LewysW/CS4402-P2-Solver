import java.util.*;

import static java.lang.System.exit;

public class MACSolver extends Solver {
    public MACSolver(BinaryCSP binaryCSP, Heuristic heuristic) {
        super(binaryCSP, heuristic);
    }

    public void solve() {
        LinkedHashSet<Integer> varList = new LinkedHashSet<>();

        for (int v = 0; v < binaryCSP.getNoVariables(); v++) {
            varList.add(v);
        }

        MAC3(varList);
    }

    public void MAC3(LinkedHashSet<Integer> varList) {
        int var = selectVar(varList);
        int val = selectVal(domain(var));
        Stack<BinaryTuple> pruned = new Stack<>();
        assign(var, val, pruned);
        if (completeAssignment()) {
            printSolution();
            exit(0);
        } else if (AC3(pruned)) {
            //Pass in value of varList - var
            LinkedHashSet<Integer> subset = (LinkedHashSet<Integer>) varList.clone();
            subset.remove(var);
            MAC3(subset);
        }
        undoPruning(pruned);
        unassign(var);
        remove(val, var);
        if (!empty(domain(var))) {
            if (AC3(pruned)) {
                MAC3(varList);
            }
            undoPruning(pruned);
        }
        restore(val, var);
    }

    public boolean AC3(Stack<BinaryTuple> pruned) {
        Queue<BinaryConstraint> queue = new LinkedList<>(binaryCSP.getConstraints());

        while (!queue.isEmpty()) {
            try {
                BinaryConstraint topConstraint = ((LinkedList<BinaryConstraint>) queue).pop();
                if (revise(topConstraint, pruned)) {
                    int xi = topConstraint.getSecondVar();
                    int xj = topConstraint.getFirstVar();

                    for (int xh = 0; xh < binaryCSP.getNoVariables(); xh++) {
                        if (xh != xj) {
                            if (constraints.containsKey(xh) && constraints.get(xh).containsKey(xi)) {
                                ((LinkedList<BinaryConstraint>) queue).push(constraints.get(xh).get(xi));
                            }
                        }
                    }
                }
            } catch (DomainEmptyException e) {
                return false;
            }
        }

        //TODO - sorted here if using SDF
        return true;
    }
}
