import java.util.* ;

public final class BinaryConstraint {
  private int firstVar, secondVar ;
  private ArrayList<BinaryTuple> tuples ;
  private boolean reversed = false;
  
  public BinaryConstraint(int fv, int sv, ArrayList<BinaryTuple> t) {
    firstVar = fv ;
    secondVar = sv ;
    tuples = t ;
  }
  
  public String toString() {
    StringBuffer result = new StringBuffer() ;
    result.append("c("+firstVar+", "+secondVar+")\n") ;
    for (BinaryTuple bt : tuples)
      result.append(bt+"\n") ;
    return result.toString() ;
  }

  public int getFirstVar() {
    return firstVar;
  }

  public int getSecondVar() {
    return secondVar;
  }

  public ArrayList<BinaryTuple> getTuples() {
    return tuples;
  }

  /**
   * Returns whether assignments to variables xi and xj satisfy constraint
   * @param xi - first variable in arc
   * @param xj - second variable in arc
   * @return whether (xi, xj) satisfies c
   */
  protected boolean satisfies(int xi, int xj) {
    //For each pair of valid values in the constraint
    for (BinaryTuple tuple : getTuples()) {
      if (!reversed) {
        //If one of them matches the values of the variables
        if (tuple.matches(xi, xj)) {
          //Then the constraint is satisfied
          return true;
        }
      } else {
        //If equivalent reversed tuple
        if (tuple.matches(xj, xi)) {
          return true;
        }
      }
    }

    //If none of the tuples match the variable
    // values then the constraint is not satisfied
    return false;
  }

  /**
   * Swaps firstVar and secondVar for equivalent constraint
   */
  public void reverse() {
    int temp = firstVar;
    firstVar = secondVar;
    secondVar = temp;
    reversed = true;
  }
}
