import syntaxtree.*;
import visitor.GJDepthFirst;

import java.util.*;
import java.lang.*;

public class MJTypeCheckVisitor extends GJDepthFirst<MJEntity, MJEntity> {
    public MJSymbolVisitor sym_visitor;

    public MJTypeCheckVisitor(MJSymbolVisitor sym_visitor) {
        this.sym_visitor = sym_visitor;
    }

    /** Goal **/
    /**
     * f0 -> MainClass()
     * f1 -> ( TypeDeclaration() )*
     * f2 -> <EOF>
     */

    public MJEntity visit(Goal n, MJEntity e) {
        MJEntity err = null;
        err = n.f0.accept(this, null);
        if (err != null) {
            return err;
        }

        if (n.f1.present()){
            err = null;
            for (Enumeration<Node> e_tdecl = n.f1.elements(); e_tdecl.hasMoreElements(); ){
                err = e_tdecl.nextElement().accept(this, null);
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

        //for variables that had undefined type because it was a until-then undeclared class
        if (n.f14.present()){
            for (Enumeration<Node> e_var = n.f14.elements(); e_var.hasMoreElements(); ){
                VarDeclaration var_decl = (VarDeclaration) e_var.nextElement();
                String var_name = var_decl.f1.f0.toString();
                MJVariable v = main_meth.get_decl(var_name);
                if (v.get_type() == null) {
                    MJType t = (MJType) var_decl.f0.accept(this.sym_visitor);
                    if (t == null) {
                        return new MJError("Main method:Variable " + var_name + " has an undefined type.");
                    }
                    v.set_type(t);
                }
            }
        }

        //here all variables of the main method have a type

        if (n.f15.present()) {
            for (Enumeration<Node> e_stmt = n.f15.elements(); e_stmt.hasMoreElements(); ) {
                MJEntity m = e_stmt.nextElement().accept(this, main_meth);
                if (m != null) {
                    MJError err = (MJError) m;
                    return new MJError("Main method:" + err.get_error());
                }
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
        String class_name = n.f1.f0.toString();
        MJClass cls = this.sym_visitor.sym_table.get(class_name);

        if (n.f3.present()) {
            for (Enumeration<Node> e_var = n.f3.elements(); e_var.hasMoreElements(); ) {
                VarDeclaration var = (VarDeclaration) e_var.nextElement();
                String var_name = var.f1.f0.toString();
                MJVariable v = cls.get_variable(var_name);
                if (v.get_type() == null) {
                    MJType t = (MJType) var.f0.accept(this.sym_visitor);
                    if (t == null){
                        return new MJError("Class " + class_name + ":Variable " + var_name + " has an undefined type.");
                    }
                    v.set_type(t);
                }
            }
        }

        //here all variables have a type

        if (n.f4.present()) {
            for (Enumeration<Node> e_meth = n.f4.elements(); e_meth.hasMoreElements(); ) {
                MJEntity m = e_meth.nextElement().accept(this, cls);
                if (m != null) {
                    MJError err = (MJError) m;
                    return new MJError("Class " + class_name + ":" + err.get_error());
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
        String class_name = n.f1.f0.toString();
        MJClass cls = this.sym_visitor.sym_table.get(class_name);

        if (n.f5.present()) {
            for (Enumeration<Node> e_var = n.f5.elements(); e_var.hasMoreElements(); ) {
                VarDeclaration var = (VarDeclaration) e_var.nextElement();
                String var_name = var.f1.f0.toString();
                MJVariable v = cls.get_variable(var_name);
                if (v.get_type() == null) {
                    MJType t = (MJType) var.f0.accept(this.sym_visitor);
                    if (t == null){
                        return new MJError("Class " + class_name + ":Variable " + var_name + " has an undefined type.");
                    }
                    v.set_type(t);
                }
            }
        }

        //here all variables have a type

        if (n.f6.present()) {
            for (Enumeration<Node> e_meth = n.f6.elements(); e_meth.hasMoreElements(); ) {
                MJEntity m = e_meth.nextElement().accept(this, cls);
                if (m != null) {
                    MJError err = (MJError) m;
                    return new MJError("Class " + class_name + ":" + err.get_error());
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
        String method_name = n.f2.f0.toString();
        MJMethod self = cls.get_method(method_name);

        if (self.get_type() == null){
            MJType type = (MJType) n.f1.accept(this.sym_visitor);
            if (type == null){
                return new MJError("Method " + method_name + ":Undeclared return type.");
            }
            self.set_type(type);
        }

        if (n.f7.present()){
            for (Enumeration<Node> e_var = n.f7.elements(); e_var.hasMoreElements(); ) {
                VarDeclaration var = (VarDeclaration) e_var.nextElement();
                MJVariable v = self.get_decl(var.f1.f0.toString());
                if (v.get_type() == null) {
                    MJType type = (MJType) var.f0.accept(this.sym_visitor);
                    if (type == null) {
                        return new MJError("Method " + method_name + ":Variable " + var.f1.f0.toString() + " has an undefined type.");
                    }
                    v.set_type((MJType) var.f0.accept(this.sym_visitor));
                }
            }
        }

        //here all variable declarations have a type

        if (n.f8.present()){
            for (Enumeration<Node> e_stmt = n.f8.elements(); e_stmt.hasMoreElements(); ) {
                Node stmt = e_stmt.nextElement();
                MJEntity s = stmt.accept(this, self);
                if (s != null) {
                    MJError err = (MJError) s;
                    return new MJError("Method " + method_name + ":" + err.get_error());
                }
            }
        }

        MJEntity exp = n.f10.accept(this, self);
        if (exp instanceof MJError){
            MJError err = (MJError) exp;
            return new MJError("Method " + method_name + ":" + err.get_error());
        }
        else if (!(exp.get_name().equals(self.get_type().get_name()))){
            if (exp instanceof MJClass) {
                MJClass type = (MJClass) exp;
                type = type.get_extending();
                MJClass expected = (MJClass) self.get_type();
                boolean flag = false;

                while (type != null) {
                    if (type.get_name().equals(expected.get_name())) {
                        flag = true;
                        break;
                    }
                    type = type.get_extending();
                }
                if (!flag) {
                    return new MJError("Method " + method_name + ":Wrong return type. Expected " + self.get_type().get_name() + ".");
                }
            }
            else{
                return new MJError("Method " + method_name + ":Wrong return type. Expected " + self.get_type().get_name() + ".");
            }
        }

        return null;
    }

    /** Statement **/
    /**
     * f0 -> Block()
     * | AssignmentStatement()
     * | ArrayAssignmentStatement()
     * | IfStatement()
     * | WhileStatement()
     * | PrintStatement()
     */
    public MJEntity visit(Statement n, MJEntity e) {
        return n.f0.accept(this, e);
    }

    /** Block **/
    /**
     * f0 -> "{"
     * f1 -> ( Statement() )*
     * f2 -> "}"
     */
    public MJEntity visit(Block n, MJEntity e) {
        if (n.f1.present()) {
            for (Enumeration<Node> e_stmt = n.f1.elements(); e_stmt.hasMoreElements(); ) {
                MJEntity s = e_stmt.nextElement().accept(this, e);
                if (s != null) {
                    MJError err = (MJError) s;
                    return new MJError("Block statement:" + err.get_error());
                }
            }
        }

        return null;
    }

    /** AssignmentStatement **/
    /**
     * f0 -> Identifier()
     * f1 -> "="
     * f2 -> Expression()
     * f3 -> ";"
     */
    public MJEntity visit(AssignmentStatement n, MJEntity e) {
        String id = n.f0.f0.toString();
        MJMethod meth = (MJMethod) e;
        MJVariable v = meth.get_variable_definition(id);
        if (v == null) {
            return new MJError("Assignment:Variable " + id + " is undeclared.");
        }

        MJEntity exp = n.f2.accept(this, e);

        if (exp instanceof MJError) {
            return exp;
        }
        else if (!(exp.get_name().equals(v.get_type().get_name()))){
            if (exp instanceof MJClass) {
                MJClass type = (MJClass) exp;
                type = type.get_extending();
                MJClass expected = (MJClass) v.get_type();
                boolean flag = false;

                while (type != null) {
                    if (type.get_name().equals(expected.get_name())) {
                        flag = true;
                        break;
                    }
                    type = type.get_extending();
                }
                if (!flag) {
                    return new MJError("Assignment:Wrong type of expression assigned to variable " + v.get_name() + ". Expected " + v.get_type().get_name() + ".");
                }
            }
            else {
                return new MJError("Assignment:Wrong type of expression assigned to variable " + v.get_name() + ". Expected " + v.get_type().get_name() + ".");
            }
        }

        return null;
    }

    /** ArrayAssignmentStatement **/
    /**
     * f0 -> Identifier()
     * f1 -> "["
     * f2 -> Expression()
     * f3 -> "]"
     * f4 -> "="
     * f5 -> Expression()
     * f6 -> ";"
     */
    public MJEntity visit(ArrayAssignmentStatement n, MJEntity e) {
        MJMethod meth = (MJMethod) e;
        String id = n.f0.f0.toString();
        MJVariable v = meth.get_variable_definition(id);

        if (v == null) {
            return new MJError("Array assignment:Variable " + id + " is undeclared.");
        }

        if (!(v.get_type().get_name().equals(this.sym_visitor.sym_table.array_type.get_name()))){
            return new MJError("Array assignment:Variable " + id + " not an array.");
        }

        MJEntity exp = n.f2.accept(this, e);
        if (exp instanceof MJError){
            return exp;
        }
        else if (!(exp instanceof MJInteger)){
            return new MJError("Array assignment:Dereference expression not an integer.(" + exp.get_name() + ")");
        }

        exp = null;
        exp = n.f5.accept(this, e);
        if (exp instanceof MJError){
            return exp;
        }
        else if (!(exp instanceof MJInteger)){
            return new MJError("Array assignment:Assignment expression not an integer.(" + exp.get_name() + ")");
        }

        return null;
    }

    /** IfStatement **/
    /**
     * f0 -> "if"
     * f1 -> "("
     * f2 -> Expression()
     * f3 -> ")"
     * f4 -> Statement()
     * f5 -> "else"
     * f6 -> Statement()
     */
    public MJEntity visit(IfStatement n, MJEntity e) {
        MJEntity exp = n.f2.accept(this, e);
        if (exp instanceof MJError){
            return exp;
        }
        else if (!(exp instanceof MJBoolean)){
            return new MJError("If condition not boolean.(" + exp.get_name() + ")");
        }

        MJEntity stmt = n.f4.accept(this, e);
        if (stmt != null){
            MJError err = (MJError) stmt;
            return new MJError("If statement:" + err.get_error());
        }

        stmt = null;
        stmt = n.f6.accept(this, e);
        if (stmt != null){
            MJError err = (MJError) stmt;
            return new MJError("If statement:" + err.get_error());
        }

        return null;
    }

    /** WhileStatement **/
    /**
     * f0 -> "while"
     * f1 -> "("
     * f2 -> Expression()
     * f3 -> ")"
     * f4 -> Statement()
     */
    public MJEntity visit(WhileStatement n, MJEntity e) {
        MJEntity exp = n.f2.accept(this, e);
        if (exp instanceof MJError){
            return exp;
        }
        else if (!(exp instanceof MJBoolean)){
            return new MJError("While condition not boolean.(" + exp.get_name() + ")");
        }

        MJEntity stmt = n.f4.accept(this, e);
        if (stmt != null){
            MJError err = (MJError) stmt;
            return new MJError("While statement:" + err.get_error());
        }

        return null;
    }

    /** PrintStatement **/
    /**
     * f0 -> "System.out.println"
     * f1 -> "("
     * f2 -> Expression()
     * f3 -> ")"
     * f4 -> ";"
     */
    public MJEntity visit(PrintStatement n, MJEntity e) {
        MJEntity exp = n.f2.accept(this, e);
        if (exp instanceof MJError){
            return exp;
        }
        else if (!(exp instanceof MJInteger) && !(exp instanceof MJBoolean)){
            return new MJError("Print statement:Cannot print expression of type " + exp.get_name() + ".");
        }

        return null;
    }

    /** Expression **/
    /**
     * f0 -> AndExpression()
     * | CompareExpression()
     * | PlusExpression()
     * | MinusExpression()
     * | TimesExpression()
     * | ArrayLookup()
     * | ArrayLength()
     * | MessageSend()
     * | Clause()
     */
    public MJEntity visit(Expression n, MJEntity e) {
        return n.f0.accept(this, e);
    }

    /** AndExpression **/
    /**
     * f0 -> Clause()
     * f1 -> "&&"
     * f2 -> Clause()
     */
    public MJEntity visit(AndExpression n, MJEntity e) {
        MJEntity cl = n.f0.accept(this, e);
        if (cl instanceof MJError){
            MJError err = (MJError) cl;
            return new MJError("Logical And expression:" + err.get_error());
        }

        cl = n.f2.accept(this, e);
        if (cl instanceof MJError){
            MJError err = (MJError) cl;
            return new MJError("Logical And expression:" + err.get_error());
        }

        return this.sym_visitor.sym_table.boolean_type;
    }

    /** CompareExpression **/
    /**
     * f0 -> PrimaryExpression()
     * f1 -> "<"
     * f2 -> PrimaryExpression()
     */
    public MJEntity visit(CompareExpression n, MJEntity e) {
        MJEntity exp = n.f0.accept(this, e);
        if (exp instanceof MJError){
            MJError err = (MJError) exp;
            return new MJError("Compare expression:" + err.get_error());
        }
        else if (!(exp instanceof MJInteger)){
            return new MJError("Compare expression:First term of expression not an integer.(" + exp.get_name() + ")");
        }

        exp = n.f2.accept(this, e);
        if (exp instanceof MJError){
            MJError err = (MJError) exp;
            return new MJError("Compare expression:" + err.get_error());
        }
        else if (!(exp instanceof MJInteger)){
            return new MJError("Compare expression:Second term of expression not an integer.(" + exp.get_name() + ")");
        }

        return this.sym_visitor.sym_table.boolean_type;
    }

    /** PlusExpression **/
    /**
     * f0 -> PrimaryExpression()
     * f1 -> "+"
     * f2 -> PrimaryExpression()
     */
    public MJEntity visit(PlusExpression n, MJEntity e) {
        MJEntity exp = n.f0.accept(this, e);
        if (exp instanceof MJError){
            MJError err = (MJError) exp;
            return new MJError("Plus expression:" + err.get_error());
        }
        else if (!(exp instanceof MJInteger)){
            return new MJError("Plus expression:First term of expression not an integer.(" + exp.get_name() + ")");
        }

        exp = n.f2.accept(this, e);
        if (exp instanceof MJError){
            MJError err = (MJError) exp;
            return new MJError("Plus expression:" + err.get_error());
        }
        else if (!(exp instanceof MJInteger)){
            return new MJError("Plus expression:Second term of expression not an integer.(" + exp.get_name() + ")");
        }

        return this.sym_visitor.sym_table.integer_type;
    }

    /** MinusExpression **/
    /**
     * f0 -> PrimaryExpression()
     * f1 -> "-"
     * f2 -> PrimaryExpression()
     */
    public MJEntity visit(MinusExpression n, MJEntity e) {
        MJEntity exp = n.f0.accept(this, e);
        if (exp instanceof MJError){
            MJError err = (MJError) exp;
            return new MJError("Minus expression:" + err.get_error());
        }
        else if (!(exp instanceof MJInteger)){
            return new MJError("Minus expression:First term of expression not an integer.(" + exp.get_name() + ")");
        }

        exp = n.f2.accept(this, e);
        if (exp instanceof MJError){
            MJError err = (MJError) exp;
            return new MJError("Minus expression:" + err.get_error());
        }
        else if (!(exp instanceof MJInteger)){
            return new MJError("Minus expression:Second term of expression not an integer.(" + exp.get_name() + ")");
        }

        return this.sym_visitor.sym_table.integer_type;
    }

    /** TimesExpression **/
    /**
     * f0 -> PrimaryExpression()
     * f1 -> "*"
     * f2 -> PrimaryExpression()
     */
    public MJEntity visit(TimesExpression n, MJEntity e) {
        MJEntity exp = n.f0.accept(this, e);
        if (exp instanceof MJError){
            MJError err = (MJError) exp;
            return new MJError("Times expression:" + err.get_error());
        }
        else if (!(exp instanceof MJInteger)){
            return new MJError("Times expression:First term of expression not an integer.(" + exp.get_name() + ")");
        }

        exp = n.f2.accept(this, e);
        if (exp instanceof MJError){
            MJError err = (MJError) exp;
            return new MJError("Times expression:" + err.get_error());
        }
        else if (!(exp instanceof MJInteger)){
            return new MJError("Times expression:Second term of expression not an integer.(" + exp.get_name() + ")");
        }

        return this.sym_visitor.sym_table.integer_type;
    }

    /** ArrayLookup **/
    /**
     * f0 -> PrimaryExpression()
     * f1 -> "["
     * f2 -> PrimaryExpression()
     * f3 -> "]"
     */
    public MJEntity visit(ArrayLookup n, MJEntity e) {
        MJEntity exp = n.f0.accept(this, e);
        if (exp instanceof MJError){
            MJError err = (MJError) exp;
            return new MJError("Array Lookup:Before array dereference:" + err.get_error());
        }
        else if (!(exp instanceof MJArray)){
            return new MJError("Array Lookup:Not an array.(" + exp.get_name() + ")");
        }

        exp = n.f2.accept(this, e);
        if (exp instanceof MJError){
            MJError err = (MJError) exp;
            return new MJError("Array Lookup:Array dereference:" + err.get_error());
        }
        else if (!(exp instanceof MJInteger)){
            return new MJError("Array Lookup:Array dereference expression not an integer.(" + exp.get_name() + ")");
        }

        return this.sym_visitor.sym_table.integer_type;
    }

    /** ArrayLength **/
    /**
     * f0 -> PrimaryExpression()
     * f1 -> "."
     * f2 -> "length"
     */
    public MJEntity visit(ArrayLength n, MJEntity e) {
        MJEntity exp = n.f0.accept(this, e);
        if (exp instanceof MJError){
            MJError err = (MJError) exp;
            return new MJError("Array Length:" + err.get_error());
        }
        else if (!(exp instanceof MJArray)){
            return new MJError("Array Length:Not an array.(" + exp.get_name() + ")");
        }

        return this.sym_visitor.sym_table.integer_type;
    }

    /** MessageSend **/
    /**
     * f0 -> PrimaryExpression()
     * f1 -> "."
     * f2 -> Identifier()
     * f3 -> "("
     * f4 -> ( ExpressionList() )?
     * f5 -> ")"
     */
    public MJEntity visit(MessageSend n, MJEntity e) {
        MJEntity exp = n.f0.accept(this, e);
        if (exp instanceof MJError){
            MJError err = (MJError) exp;
            return new MJError("Message Send:" + err.get_error());
        }
        else if (!(exp instanceof MJVariable) && !(exp instanceof MJClass)) {
            return new MJError("Message Send:Not instance of a defined class.(" + exp.get_name() + ")");
        }

        MJClass t = null;
        if (exp instanceof MJVariable){
            MJVariable v = (MJVariable) exp;
            t = (MJClass) v.get_type();
        }
        else{
            t = (MJClass) exp;
        }

        String method_name = n.f2.f0.toString();

        MJMethod meth = t.get_method(method_name);
        if (meth == null){
            return new MJError("Message send:" + method_name + " not a known method.");
        }

        //the accept methods for ExpressionList() and ExpressionTail are superfluous (Easter Egg: I love that word)
        //because they would need an array of MJEntity-s for the expected arguments and the method we are in for type checking
        if (n.f4.present()){
            int num_of_args = meth.args_size();
            if (num_of_args == 0){
                return new MJError("Expression List:No arguments expected.");
            }
            MJVariable[] args = meth.get_args();
            ExpressionList exp_list = (ExpressionList) n.f4.node;

            exp = exp_list.f0.accept(this, e);
            if (exp instanceof MJError){
                MJError err = (MJError) exp;
                return new MJError("Expression List:" + err.get_error());
            }
            else if (!(exp.get_name().equals(args[0].get_type().get_name()))){
                if (exp instanceof MJClass){
                    MJClass type = (MJClass) exp;
                    MJClass cls = type.get_extending();
                    boolean flag = false;
                    while (cls != null) {
                        if (cls.get_name().equals(args[0].get_type().get_name())){
                            flag = true;
                            break;
                        }
                        cls = cls.get_extending();
                    }
                    if (!flag) {
                        return new MJError("Expression List:Wrong type of argument.(" + exp.get_name() + ") Expected " + args[0].get_type().get_name() + ".");
                    }
                }
                else {
                    return new MJError("Expression List:Wrong type of argument.(" + exp.get_name() + ") Expected " + args[0].get_type().get_name() + ".");
                }
            }

            ExpressionTail exp_tail = exp_list.f1;
            if (exp_tail.f0.present()){
                int arg_num = 1;
                for (Enumeration<Node> e_expt = exp_tail.f0.elements(); e_expt.hasMoreElements(); ){
                    MJEntity ar = e_expt.nextElement().accept(this, e);
                    if (ar instanceof MJError){
                        MJError err = (MJError) ar;
                        return new MJError("Expression Tail:" + err.get_error());
                    }
                    else if (!(ar.get_name().equals(args[arg_num].get_type().get_name()))){
                        if (ar instanceof MJClass) {
                            MJClass type = (MJClass) ar;
                            MJClass cls = type.get_extending();
                            boolean flag = false;
                            while (cls != null) {
                                if (cls.get_name().equals(args[arg_num].get_type().get_name())) {
                                    flag = true;
                                    break;
                                }
                                cls = cls.get_extending();
                            }
                            if (!flag) {
                                return new MJError("Expression Tail:Wrong type of argument.(" + ar.get_name() + ") Expected " + args[arg_num].get_type().get_name() + ".");
                            }
                        }
                        else{
                            return new MJError("Expression Tail:Wrong type of argument.(" + ar.get_name() + ") Expected " + args[arg_num].get_type().get_name() + ".");
                        }
                    }
                    arg_num++;
                }
            }
        }

        return meth.get_type();
    }

    /** ExpressionTerm **/
    /**
     * f0 -> ","
     * f1 -> Expression()
     */
    public MJEntity visit(ExpressionTerm n, MJEntity e) {
        return n.f1.accept(this, e);
    }

    /** Clause **/
    /**
     * f0 -> NotExpression()
     * | PrimaryExpression()
     */
    public MJEntity visit(Clause n, MJEntity e) {
        MJEntity exp = n.f0.accept(this, e);
        if (exp instanceof MJError){
            return exp;
        }

        return exp;
    }

    /** PrimaryExpression **/
    /**
     * f0 -> IntegerLiteral()
     * | TrueLiteral()
     * | FalseLiteral()
     * | Identifier()
     * | ThisExpression()
     * | ArrayAllocationExpression()
     * | AllocationExpression()
     * | BracketExpression()
     */
    public MJEntity visit(PrimaryExpression n, MJEntity e) {
        return n.f0.accept(this, e);
    }

    /** IntegerLiteral **/
    /**
     * f0 -> <INTEGER_LITERAL>
     */
    public MJEntity visit(IntegerLiteral n, MJEntity e) {
        return this.sym_visitor.sym_table.integer_type;
    }

    /** TrueLiteral **/
    /**
     * f0 -> "true"
     */
    public MJEntity visit(TrueLiteral n, MJEntity e) {
        return this.sym_visitor.sym_table.boolean_type;
    }

    /** FalseLiteral **/
    /**
     * f0 -> "false"
     */
    public MJEntity visit(FalseLiteral n, MJEntity e) {
        return this.sym_visitor.sym_table.boolean_type;
    }

    /** Identifier **/
    /**
     * f0 -> <IDENTIFIER>
     */
    public MJEntity visit(Identifier n, MJEntity e) {
        MJMethod meth = (MJMethod) e;
        MJVariable ret = meth.get_variable_definition(n.f0.toString());
        if (ret == null){
            return new MJError("Identifier " + n.f0.toString() + " undeclared.");
        }

        return ret.get_type();
    }

    /** ThisExpression **/
    /**
     * f0 -> "this"
     */
    public MJEntity visit(ThisExpression n, MJEntity e) {
        MJMethod meth = (MJMethod) e;
        return meth.get_cls();
    }

    /** ArrayAllocationExpression **/
    /**
     * f0 -> "new"
     * f1 -> "int"
     * f2 -> "["
     * f3 -> Expression()
     * f4 -> "]"
     */
    public MJEntity visit(ArrayAllocationExpression n, MJEntity e) {
        MJEntity exp = n.f3.accept(this, e);
        if (exp instanceof MJError){
            MJError err = (MJError) exp;
            return new MJError("Array Allocation:" + err.get_error());
        }
        else if (!(exp instanceof MJInteger)){
          return new MJError("Array Allocation:Number of elements of array not integer.(" + exp.get_name() + ")");
        }

        return this.sym_visitor.sym_table.array_type;
    }

    /** AllocationExpression **/
    /**
     * f0 -> "new"
     * f1 -> Identifier()
     * f2 -> "("
     * f3 -> ")"
     */
    public MJEntity visit(AllocationExpression n, MJEntity e) {
        String id = n.f1.f0.toString();
        MJType integer_t = this.sym_visitor.sym_table.integer_type;
        MJType array_t = this.sym_visitor.sym_table.array_type;
        MJType boolean_t = this.sym_visitor.sym_table.boolean_type;
        if (id.equals(integer_t.get_name())){
            return integer_t;
        }
        else if (id.equals(array_t.get_name())){
          return array_t;
        }
        else if (id.equals(boolean_t.get_name())){
          return boolean_t;
        }
        else {
          MJClass cls = this.sym_visitor.sym_table.get(id);
          if (cls == null){
            return new MJError("Allocation:Identifier not a type.");
          }

          return cls;
        }
    }

    /** NotExpression **/
    /**
     * f0 -> "!"
     * f1 -> Clause()
     */
    public MJEntity visit(NotExpression n, MJEntity e) {
        MJEntity cl = n.f1.accept(this, e);
        if (cl instanceof MJError){
            MJError err = (MJError) cl;
            return new MJError("Not expression:" + err.get_error());
        }
        else {
            return this.sym_visitor.sym_table.boolean_type;
        }
    }


    /** BracketExpression **/
    /**
     * f0 -> "("
     * f1 -> Expression()
     * f2 -> ")"
     */
    public MJEntity visit(BracketExpression n, MJEntity e) {
        MJEntity exp = n.f1.accept(this, e);
        if (exp instanceof MJError){
            MJError err = (MJError) exp;
            return new MJError("Brackets:" + err.get_error());
        }
        else {
            return exp;
        }
    }
}
