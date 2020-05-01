import java.io.* ;
import java.util.* ;

/**
 * A reader tailored for binary extensional CSPs.
 * It is created from a FileReader and a StreamTokenizer
 */
public final class BinaryCSPReader {
  private FileReader inFR ;
  private StreamTokenizer in ;

  /**
   * Main (for testing)
   */
  public static void main(String[] args) {
    if (args.length != 3) {
      System.out.println("Usage: ./run.sh BinaryCSPReader <file.csp> <algorithm> <heuristic>") ;
      return ;
    }
    BinaryCSPReader reader = new BinaryCSPReader() ;
    System.out.println(reader.readBinaryCSP(args[0])) ;

    //Uses args to select algorithm and heuristic to use
    if (args[1].equals("fc")) {
        if (args[2].equals("a")) {
            new FCSolver(reader.readBinaryCSP(args[0]), Heuristic.ASCENDING).solve();
        } else if (args[2].equals("s")) {
            new FCSolver(reader.readBinaryCSP(args[0]), Heuristic.SMALLEST_DOMAIN_FIRST).solve();
        }
    } else if (args[1].equals("mac")) {
        if (args[2].equals("a")) {
            new MACSolver(reader.readBinaryCSP(args[0]), Heuristic.ASCENDING).solve();
        } else if (args[2].equals("s")) {
            new MACSolver(reader.readBinaryCSP(args[0]), Heuristic.SMALLEST_DOMAIN_FIRST).solve();
        }
    }
  }

  /**
   * File format:
   * <no. vars>
   * NB vars indexed from 0
   * We assume that the domain of all vars is specified in terms of bounds
   * <lb>, <ub> (one per var)
   * Then the list of constraints
   * c(<varno>, <varno>)
   * binary tuples
   * <domain val>, <domain val>
   */
  public BinaryCSP readBinaryCSP(String fn) {
    try {
      inFR = new FileReader(fn) ;
      in = new StreamTokenizer(inFR) ;
      in.ordinaryChar('(') ;
      in.ordinaryChar(')') ;
      in.nextToken() ;                                         // n
      int n = (int)in.nval ;
      int[][] domainBounds = new int[n][2] ;
      for (int i = 0; i < n; i++) {
	      in.nextToken() ;                                  // ith ub
	      domainBounds[i][0] = (int)in.nval ;
		    in.nextToken() ;                                   // ','
		    in.nextToken() ;
	      domainBounds[i][1] = (int)in.nval ;
      }
      ArrayList<BinaryConstraint> constraints = readBinaryConstraints() ;
      BinaryCSP csp = new BinaryCSP(domainBounds, constraints) ;
      // TESTING:
      // System.out.println(csp) ;
      inFR.close() ;
      return csp ;
    }
    catch (FileNotFoundException e) {System.out.println(e);}
    catch (IOException e) {System.out.println(e);}
    return null ;
  }

  /**
   *
   */
  private ArrayList<BinaryConstraint> readBinaryConstraints() {
    ArrayList<BinaryConstraint> constraints = new ArrayList<BinaryConstraint>() ;
	
    try {
      in.nextToken() ;                                  //'c' or EOF
      while(in.ttype != in.TT_EOF) {
	      // scope
	      in.nextToken() ;                                       //'('
		    in.nextToken() ;                                       //var
	      int var1 = (int)in.nval ;
		    in.nextToken() ;                                       //','
		    in.nextToken() ;                                       //var
        int var2 = (int)in.nval ;
		    in.nextToken() ;                                       //')'

        //tuples
		    ArrayList<BinaryTuple> tuples = new ArrayList<BinaryTuple>() ;
        in.nextToken() ;              //1st allowed val of 1st tuple
        while (!"c".equals(in.sval) && (in.ttype != in.TT_EOF)) {
          int val1 = (int)in.nval ;
	        in.nextToken() ;                                   //','
	        in.nextToken() ;                               //2nd val
		      int val2 = (int)in.nval ;
		      tuples.add(new BinaryTuple(val1, val2)) ;
		      in.nextToken() ;      //1stallowed val of next tuple/c/EOF
		    }
        BinaryConstraint c = new BinaryConstraint(var1, var2, tuples) ;
        constraints.add(c) ;
      }
	  
      return constraints ;
    }
    catch (IOException e) {System.out.println(e);}
    return null ;  
  }
}
