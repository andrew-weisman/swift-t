package exm.stc.ic.opt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import exm.stc.common.lang.Arg;
import exm.stc.common.lang.Var;
import exm.stc.common.lang.Var.DefType;
import exm.stc.ic.tree.ICContinuations.Continuation;
import exm.stc.ic.tree.ICTree.Block;
import exm.stc.ic.tree.ICTree.Function;
import exm.stc.ic.tree.ICTree.Program;

public class Flattener {

    public static void flattenNestedBlocks(Block block) {


    List<Continuation> originalContinuations =
          new ArrayList<Continuation>(block.getContinuations());
    // Stick any nested blocks instructions into the main thing
    for (Continuation c: originalContinuations) {
      switch (c.getType()) {
      case NESTED_BLOCK:
        assert(c.getBlocks().size() == 1);
        if (!c.runLast()) {
          Block inner = c.getBlocks().get(0);
          flattenNestedBlocks(inner);
          c.inlineInto(block, inner);
        }
        break;
      default:
        // Recursively flatten any blocks inside the continuation
        for (Block b: c.getBlocks()) {
          flattenNestedBlocks(b);
        }
      }

    }
  }

  /**
   * Remove all nested blocks from program
   * Precondition: all variable names in functions should be unique
   * @param in
   * @return
   */
  public static Program flattenNestedBlocks(Program in) {
    for (Function f: in.getFunctions()) {
      flattenNestedBlocks(f.getMainblock());
    }
    return in;
  }

  /**
   * Make all names in block unique
   *
   * @param in
   * @param usedNames Names already used
   */
  private static void makeVarNamesUnique(Block in, Set<String> usedNames) {
    HashMap<String, Arg> renames = new HashMap<String, Arg>();
    for (Var v: in.getVariables()) {
      if (v.defType() == DefType.GLOBAL_CONST) {
        continue;
      }
      if (usedNames.contains(v.name())) {
        int counter = 1;
        String newName;
        // try x_1 x_2 x_3, etc until we find something
        do {
          newName = v.name() + "_" + counter;
          counter++;
        } while(usedNames.contains(newName));
        renames.put(v.name(),
            Arg.createVar(new Var(v.type(), newName,
                            v.storage(), v.defType(), v.mapping())));
        usedNames.add(newName);
      } else {
        usedNames.add(v.name());
      }
    }

    // Rename variables in Block (and nested blocks) according to map
    in.renameVars(renames, false);

    // Recurse through nested blocks, making sure that all used variable
    // names are added to the usedNames
    for (Continuation c: in.getContinuations()) {
      for (Block b: c.getBlocks()) {
        makeVarNamesUnique(b, usedNames);
      }
    }
  }

  public static void makeVarNamesUnique(Function in,
            Set<String> globals) {
    Set<String> usedNames = new HashSet<String>(globals);
    for (Var v: in.getInputList()) {
      usedNames.add(v.name());
    }
    for (Var v: in.getOutputList()) {
      usedNames.add(v.name());
    }

    makeVarNamesUnique(in.getMainblock(), usedNames);
  }

  /**
   * Make all of variable names in functions completely
   * unique within the function
   * @param in
   */
  public static void makeVarNamesUnique(Program in) {
    for (Function f: in.getFunctions()) {
      makeVarNamesUnique(f, in.getGlobalConsts().keySet());
    }
  }

}
