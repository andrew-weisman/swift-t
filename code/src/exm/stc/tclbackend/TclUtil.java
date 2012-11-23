package exm.stc.tclbackend;

import java.util.List;

import exm.stc.common.exceptions.STCRuntimeError;
import exm.stc.common.lang.Arg;
import exm.stc.common.lang.Var;
import exm.stc.tclbackend.tree.Expression;
import exm.stc.tclbackend.tree.LiteralFloat;
import exm.stc.tclbackend.tree.LiteralInt;
import exm.stc.tclbackend.tree.TclList;
import exm.stc.tclbackend.tree.TclString;
import exm.stc.tclbackend.tree.Value;

public class TclUtil {

  public static Expression opargToExpr(Arg in) {
    switch (in.getKind()) {
    case INTVAL:
      return new LiteralInt(in.getIntLit());
    case BOOLVAL:
      return new LiteralInt(in.getBoolLit() ? 1 : 0);
    case STRINGVAL:
      return new TclString(in.getStringLit(), true);
    case VAR:
      return new Value(TclNamer.prefixVar(in.getVar().name()));
    case FLOATVAL:
      return new LiteralFloat(in.getFloatLit());
    default:
      throw new STCRuntimeError("Unknown oparg type: "
          + in.getKind().toString());
    }
  }

  public static Value varToExpr(Var v) {
    return new Value(TclNamer.prefixVar(v.name()));
  }


  public static TclList tclListOfVariables(List<Var> inputs) {
    TclList result = new TclList();
    for (Var v : inputs)
      result.add(varToExpr(v));
    return result;
  }
}
