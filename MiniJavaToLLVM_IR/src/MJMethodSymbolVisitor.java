import syntaxtree.*;
import visitor.GJDepthFirst;
import java.util.*;
import java.lang.*;

public class MJMethodSymbolVisitor extends GJDepthFirst< MJError, MJMethod > {
    MJSymbolVisitor visitor;
    public MJMethodSymbolVisitor(MJSymbolVisitor v) {
    this.visitor = v;
  }

    /** FormalParameterList **/
    /**
     * f0 -> FormalParameter()
     * f1 -> FormalParameterTail()
     */
    public MJError visit(FormalParameterList n, MJMethod meth) {
        MJError err = null;
        err = n.f0.accept(this, meth);
        if ( err != null ){
            return err;
        }
        err = n.f1.accept(this, meth);

        if ( err != null ){
            return err;
        }

        return null;
    }

    /** FormalParameter **/
    /**
     * f0 -> Type()
     * f1 -> Identifier()
     */

    public MJError visit(FormalParameter n , MJMethod meth) {
        MJType t = (MJType) n.f0.accept(this.visitor);
        String var_name = n.f1.f0.toString();
        if ( meth.args_contains(var_name) ){
            return new MJError("Variable " + var_name + " passed as parameter twice.");
        }
        MJVariable v = new MJVariable(var_name, t);
        meth.add_arg(v);
        return null;
    }

   /** FormalParameterTail **/
   /**
    * f0 -> ( FormalParameterTerm() )*
    */
   public MJError visit(FormalParameterTail n, MJMethod meth) {
       MJError err = null;
       if ( n.f0.present() ){
           for (Enumeration<Node> e_args = n.f0.elements(); e_args.hasMoreElements(); ){
               err = e_args.nextElement().accept(this, meth);
               if ( err != null ) {
                   return err;
               }
           }
       }
       return null;
   }

   /** FormalParameterTerm **/
   /**
    * f0 -> ","
    * f1 -> FormalParameter()
    */
   public MJError visit(FormalParameterTerm n, MJMethod meth) {
       return n.f1.accept(this, meth);
   }

   /**
    *   MethodDeclaration:
    *  f7 -> ( VarDeclaration() )*
    */
   /**
    * f0 -> Type()
    * f1 -> Identifier()
    * f2 -> ";"
    */
   public MJError visit(VarDeclaration n, MJMethod meth) {
       MJType t = (MJType) n.f0.accept(this.visitor);
       String var_name = n.f1.f0.toString();

       if ( meth.decls_contains(var_name) ){
            return new MJError("Variable " + var_name + " declared twice.");
       }
       else if ( meth.args_contains(var_name) ) {
            return new MJError("Variable " + var_name + " is passed as an argument and is redeclared.");
       }
       MJVariable v = new MJVariable(var_name, t);
       meth.add_decl(v);

       return null;
   }
}
