import syntaxtree.*;
import visitor.GJNoArguDepthFirst;

import java.util.*;
import java.lang.*;

public class MJSymbolVisitor extends GJNoArguDepthFirst<MJEntity> {
    public MJSymbolTable sym_table;
    public MJMethodSymbolVisitor visitor = new MJMethodSymbolVisitor(this);

    public MJSymbolVisitor(MJSymbolTable sym_table) {
        this.sym_table = sym_table;
    }
    /** Goal **/
    /**
     * f0 -> MainClass()
     * f1 -> ( TypeDeclaration() )*
     * f2 -> <EOF>
     */
    public MJEntity visit(Goal n) {
        MJEntity err = n.f0.accept(this);
        if (err != null) {
            return err;
        }

        if (n.f1.present()) {
            for (Enumeration<Node> e = n.f1.elements(); e.hasMoreElements(); ) {
                err = e.nextElement().accept(this);
                if (err != null) {
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
    public MJEntity visit(MainClass n) {
        MJClass main_class = new MJClass(n.f1.f0.toString(), null); //main class name
        MJMethod main_meth = new MJMethod("main", sym_table.main_method_type);
        main_meth.set_cls(main_class);
        main_class.add_method(main_meth);
        MJVariable main_arg = new MJVariable(n.f11.f0.toString(), sym_table.string_array_type);
        main_meth.add_arg(main_arg);

        if (n.f14.present()) {
            for (Enumeration<Node> e_var = n.f14.elements(); e_var.hasMoreElements(); ) {
                MJVariable decl = (MJVariable) e_var.nextElement().accept(this);
                if (!main_meth.add_decl(decl)) {
                    return new MJError("Main Method:Duplicate declaration of variable " + decl.get_name() + " in main method.");
                } else if (main_meth.args_contains(decl.get_name())) {
                    return new MJError("Main Method:Variable " + decl.get_name() + " both in parameters and declarations.");
                }
            }
        }
        this.sym_table.add_class(main_class);

        return null;
    }

    /** TypeDeclaration **/
    /**
     * f0 -> ClassDeclaration()
     * | ClassExtendsDeclaration()
     */
    public MJEntity visit(TypeDeclaration n) {
        return n.f0.accept(this);
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
    public MJEntity visit(ClassDeclaration n) {
        String class_name = n.f1.f0.toString();
        if (sym_table.contains(class_name)) {
            return new MJError("Duplicate declaration of class " + class_name + ".");
        }

        MJClass cls = new MJClass(class_name, null);

        if (n.f3.present()) {
            for (Enumeration<Node> e_field = n.f3.elements(); e_field.hasMoreElements(); ) {
                MJVariable f = (MJVariable) e_field.nextElement().accept(this);
                if (!cls.add_variable(f)) {
                    return new MJError("Class " + cls.get_name() + ":Duplicate declaration of field " + f.get_name() + ".");
                }
            }
        }

        if (n.f4.present()) {
            for (Enumeration<Node> e_method = n.f4.elements(); e_method.hasMoreElements(); ) {
                MJEntity m = e_method.nextElement().accept(this);
                MJMethod meth = null;
                if (m instanceof MJError) {
                    MJError err = (MJError) m;
                    return new MJError("Class " + cls.get_name() + ":" + err.get_error());
                }
                else{
                    meth = (MJMethod) m;
                }

                if (!cls.add_method(meth)) {
                    return new MJError("Class " + cls.get_name() + ":Duplicate declaration of method " + meth.get_name() + ".");
                }
                meth.set_cls(cls);
            }
        }

        sym_table.add_class(cls);
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
    public MJEntity visit(ClassExtendsDeclaration n) {
        MJClass ext = sym_table.get(n.f3.f0.toString());
        if (ext == null) {
            return new MJError("Class " + n.f1.f0.toString() + " extends a non-declared class.");
        }

        String class_name = n.f1.f0.toString();

        if (sym_table.contains(class_name)) {
            return new MJError("Duplicate declaration of class " + class_name + ".");
        }

        MJClass parent = ext.get_extending();

        while (parent != null) {
            String parent_name = parent.get_name();
            if (parent_name.equals(class_name)) {
                return new MJError("Cyclic inheritance in class " + class_name + ".");
            }
            parent = parent.get_extending();
        }

        MJClass cls = new MJClass(class_name, ext);

        if (n.f5.present()) {
            for (Enumeration<Node> e_var_field = n.f5.elements(); e_var_field.hasMoreElements(); ) {
                MJVariable v = (MJVariable) e_var_field.nextElement().accept(this);
                if (!cls.add_variable(v)) {
                    return new MJError("Class " + cls.get_name() + ":Duplicate declaration of field " + v.get_name() + ".");
                }
            }
        }

        if (n.f6.present()) {
            for (Enumeration<Node> e_meth_field = n.f6.elements(); e_meth_field.hasMoreElements(); ) {
                MJEntity m = e_meth_field.nextElement().accept(this);
                MJMethod meth = null;
                if (m instanceof MJError) {
                    MJError err = (MJError) m;
                    return new MJError("Class " + cls.get_name() + ":" + err.get_error());
                }
                else{
                    meth = (MJMethod) m;
                }
                if (!cls.add_method(meth)) {
                    return new MJError("Class " + cls.get_name() + ":Duplicate declaration of field " + meth.get_name() + ".");
                }
                meth.set_cls(cls);
            }
        }

        sym_table.add_class(cls);
        return null;
    }

    /** VarDeclaration **/
    /**
     * f0 -> Type()
     * f1 -> Identifier()
     * f2 -> ";"
     */
    public MJEntity visit(VarDeclaration n) {
        MJType t = (MJType) n.f0.accept(this);

        MJVariable f = new MJVariable(n.f1.f0.toString(), t);
        return f;
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
    public MJEntity visit(MethodDeclaration n) {
        MJType t = (MJType) n.f1.accept(this);

        MJMethod m = new MJMethod(n.f2.f0.toString(), t);

        if (n.f4.present()) {
            MJError err = null;
            err = n.f4.accept(this.visitor, m);
            if (err != null) {
                return new MJError("Method " + m.get_name() + ":" + err.get_error());
            }
        }

        if (n.f7.present()) {
            MJError err = null;
            for (Enumeration<Node> e_var = n.f7.elements(); e_var.hasMoreElements(); ) {
                err = e_var.nextElement().accept(this.visitor, m);
                if (err != null) {
                    return new MJError("Method " + m.get_name() + ":" + err.get_error());
                }
            }
        }

        return m;
    }

    /**
     * f0 -> ArrayType()
     * | BooleanType()
     * | IntegerType()
     * | Identifier()
     */
    public MJEntity visit(Type n) {
        return n.f0.accept(this);
    }

    /**
     * f0 -> "int"
     * f1 -> "["
     * f2 -> "]"
     */
    public MJEntity visit(ArrayType n) {
        return sym_table.array_type;
    }

    /**
     * f0 -> "boolean"
     */
    public MJEntity visit(BooleanType n) {
        return sym_table.boolean_type;
    }

    /**
     * f0 -> "int"
     */
    public MJEntity visit(IntegerType n) {
        return sym_table.integer_type;
    }

    //this visit function is only used in variable/methods type declarations, because it returns classes and not strings (identifiers)

    /**
     * f0 -> <IDENTIFIER>
     */
    public MJEntity visit(Identifier n) {
        return sym_table.get(n.f0.toString());
    }
}
