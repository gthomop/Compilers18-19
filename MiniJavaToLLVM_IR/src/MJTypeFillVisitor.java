import syntaxtree.*;
import visitor.GJDepthFirst;
import java.util.*;
import java.lang.*;

class MJTypeFillVisitor extends GJDepthFirst<MJEntity, MJEntity> {
    private MJSymbolVisitor sym_visitor;

    public MJTypeFillVisitor(MJSymbolVisitor sym_visitor) {
        this.sym_visitor = sym_visitor;
    }

    /** Goal **/
    /**
     * f0 -> MainClass()
     * f1 -> ( TypeDeclaration() )*
     * f2 -> <EOF>
     */
    public MJEntity visit(Goal n, MJEntity e) {
        MJError err = null;
        err = (MJError) n.f0.accept(this, null);
        if (err != null){
            return err;
        }

        if (n.f1.present()){
            for (Enumeration<Node> e_tdecl = n.f1.elements(); e_tdecl.hasMoreElements(); ){
                err = (MJError) e_tdecl.nextElement().accept(this, null);
                if (err != null){
                    return err;
                }
            }
        }

        return null;
    }

    /** MainClass **/
    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "{"
     * f3 -> "public"
     * f4 -> "static"
     * f5 -> "void"
     * f6 -> "main"
     * f7 -> "("
     * f8 -> "String"
     * f9 -> "["
     * f10 -> "]"
     * f11 -> Identifier()
     * f12 -> ")"
     * f13 -> "{"
     * f14 -> ( VarDeclaration() )*
     * f15 -> ( Statement() )*
     * f16 -> "}"
     * f17 -> "}"
     */
    public MJEntity visit(MainClass n, MJEntity e) {
        MJClass main_cls = this.sym_visitor.sym_table.get(n.f1.f0.toString());
        MJMethod main_meth = main_cls.get_method("main");

        MJVariable[] decls = main_meth.get_decls();
        int decls_size = main_meth.decls_size();
        int index = 0;

        if (n.f14.present()) {
            for (Enumeration<Node> e_var = n.f14.elements(); e_var.hasMoreElements(); ) {
                if (decls[index].get_type() == null){
                    MJType t = (MJType) e_var.nextElement().accept(this, null);
                    if (t == null) {
                        return new MJError("Main Method:Variable " + decls[index].get_name() + " has undeclared type.");
                    }

                    decls[index].set_type(t);
                }
                else {
                    e_var.nextElement();
                }
                index++;
            }
        }

        return null;
    }

    /** ClassDeclaration **/
    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "{"
     * f3 -> ( VarDeclaration() )*
     * f4 -> ( MethodDeclaration() )*
     * f5 -> "}"
     */
    public MJEntity visit(ClassDeclaration n, MJEntity e) {
        MJClass cls = this.sym_visitor.sym_table.get(n.f1.f0.toString());

        MJVariable[] vars = cls.get_variables();

        int index = 0;
        if (n.f3.present()) {
            for (Enumeration<Node> e_vars = n.f3.elements(); e_vars.hasMoreElements(); ) {
                if (vars[index].get_type() == null) {
                    MJType t = (MJType) e_vars.nextElement().accept(this, null);
                    if (t == null) {
                        return new MJError("Class " + cls.get_name() + ":Variable " + vars[index].get_name() + " has undeclared type.");
                    }
                    vars[index].set_type(t);
                }
                else{
                    e_vars.nextElement();
                }
                index++;
            }
        }

        if (n.f4.present()) {
            for (Enumeration<Node> e_meths = n.f4.elements(); e_meths.hasMoreElements(); ) {
                MJEntity ret = e_meths.nextElement().accept(this, cls);
                if (ret instanceof MJError) {
                    MJError err = (MJError) ret;
                    return new MJError("Class " + cls.get_name() + ":" + err.get_error());
                }
            }
        }
        return null;
    }

    /** ClassExtendsDeclaration **/
    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "extends"
     * f3 -> Identifier()
     * f4 -> "{"
     * f5 -> ( VarDeclaration() )*
     * f6 -> ( MethodDeclaration() )*
     * f7 -> "}"
     */
    public MJEntity visit(ClassExtendsDeclaration n, MJEntity e) {
        MJClass cls = this.sym_visitor.sym_table.get(n.f1.f0.toString());

        MJVariable[] vars = cls.get_variables();

        int index = 0;
        if (n.f5.present()) {
            for (Enumeration<Node> e_vars = n.f5.elements(); e_vars.hasMoreElements(); ) {
                if (vars[index].get_type() == null) {
                    MJType t = (MJType) e_vars.nextElement().accept(this, null);
                    if (t == null) {
                        return new MJError("Class " + cls.get_name() + ":Variable " + vars[index].get_name() + " has undeclared type.");
                    }
                    vars[index].set_type(t);
                }
                else{
                    e_vars.nextElement();
                }
                index++;
            }
        }

        if (n.f6.present()) {
            for (Enumeration<Node> e_meths = n.f6.elements(); e_meths.hasMoreElements(); ) {
                MJEntity ret = e_meths.nextElement().accept(this, cls);
                if (ret instanceof MJError) {
                    MJError err = (MJError) ret;
                    return new MJError("Class " + cls.get_name() + ":" + err.get_error());
                }
            }
        }

        return null;
    }

    /** MethodDeclaration **/
    /**
     * f0 -> "public"
     * f1 -> Type()
     * f2 -> Identifier()
     * f3 -> "("
     * f4 -> ( FormalParameterList() )?
     * f5 -> ")"
     * f6 -> "{"
     * f7 -> ( VarDeclaration() )*
     * f8 -> ( Statement() )*
     * f9 -> "return"
     * f10 -> Expression()
     * f11 -> ";"
     * f12 -> "}"
     */
    public MJEntity visit(MethodDeclaration n, MJEntity e) {
        MJClass cls = (MJClass) e;
        MJMethod self = cls.get_method(n.f2.f0.toString());

        if (self.get_type() == null) {
            MJType t = (MJType) n.f1.accept(this, null);
            if (t == null) {
                return new MJError("Method " + self.get_name() + " has an undeclared return type.");
            }
            self.set_type(t);
        }

        if (n.f4.present()){
            MJError err = (MJError) n.f4.accept(this, self);
            if (err != null) {
                return new MJError("Method " + self.get_name() + err.get_error());
            }
        }

        if (!(cls.existsOverridesInSuper(self))){
            return new MJError("Method " + self.get_name() + " has the same name as a method from one of its superclasses.");
        }

        MJVariable[] decls = self.get_decls();

        int index = 0;
        if (n.f7.present()) {
            for (Enumeration<Node> e_decls = n.f7.elements(); e_decls.hasMoreElements(); ) {
                if (decls[index].get_type() == null) {
                    MJType type = (MJType) e_decls.nextElement().accept(this, null);
                    if (type == null) {
                        return new MJError("Method " + self.get_name() + ":Variable " + decls[index].get_name() + " has undeclared type.");
                    }
                    decls[index].set_type(type);
                }
                else{
                    e_decls.nextElement().accept(this, null);
                }
                index++;
            }
        }

        return null;
    }

    /** FormalParameterList **/
    /**
     * f0 -> FormalParameter()
     * f1 -> FormalParameterTail()
     */
    public MJEntity visit(FormalParameterList n, MJEntity e) {
        MJError err = null;
        err = (MJError) n.f0.accept(this, e);
        if ( err != null ){
            return err;
        }
        err = (MJError) n.f1.accept(this, e);

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
    public MJEntity visit(FormalParameter n , MJEntity e) {
        MJMethod meth = (MJMethod) e;
        String var_name = n.f1.f0.toString();
        MJVariable v = meth.get_arg(var_name);
        if (v.get_type() == null){
            MJType t = (MJType) n.f0.accept(this, null);
            if (t == null){
                return new MJError("Parameter " + var_name + " has an undeclared type.");
            }

            v.set_type(t);
        }

        return null;
    }

    /** FormalParameterTail **/
    /**
     * f0 -> ( FormalParameterTerm() )*
     */
    public MJEntity visit(FormalParameterTail n, MJEntity e) {
        if ( n.f0.present() ){
            for (Enumeration<Node> e_args = n.f0.elements(); e_args.hasMoreElements(); ){
                MJError err = (MJError) e_args.nextElement().accept(this, e);
                if (err != null) {
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
    public MJEntity visit(FormalParameterTerm n, MJEntity e) {
        return n.f1.accept(this, e);
    }

    /** VarDeclaration **/
    /**
     * f0 -> Type()
     * f1 -> Identifier()
     * f2 -> ";"
     */
    public MJEntity visit(VarDeclaration n, MJEntity e) {
        return n.f0.accept(this, null);
    }

    /** Type **/
    /**
     * f0 -> ArrayType()
     *       | BooleanType()
     *       | IntegerType()
     *       | Identifier()
     */
    public MJEntity visit(Type n, MJEntity e) {
        return n.f0.accept(this, null);
    }

    public MJEntity visit(ArrayType n, MJEntity e) {
        return this.sym_visitor.sym_table.array_type;
    }

    public MJEntity visit(BooleanType n, MJEntity e) {
        return this.sym_visitor.sym_table.boolean_type;
    }

    public MJEntity visit(IntegerType n, MJEntity e) {
        return this.sym_visitor.sym_table.integer_type;
    }

    public MJEntity visit(Identifier n, MJEntity e) {
        return this.sym_visitor.sym_table.get(n.f0.toString());
    }
}