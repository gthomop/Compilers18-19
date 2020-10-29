import syntaxtree.*;
import visitor.GJDepthFirst;
import java.lang.*;
import java.util.*;

class MJIRVisitor extends GJDepthFirst<String, String> {
    private int reg_counter = 0;
    private int loop_counter = 0;
    private int if_counter = 0;
    private int oob_counter = 0;
    private int arr_alloc_counter = 0;
    private int and_clause_counter = 0;
    private MJSymbolTable sym_table;
    private StringBuilder output = new StringBuilder();
    private String class_to_find = null;

    public MJIRVisitor(MJSymbolTable sym_table) {
        this.sym_table = sym_table;

        Set<String> set = this.sym_table.sym_table.keySet();
        String[] keys = new String[set.size()];
        keys = set.toArray(keys);

        emit("@." + keys[0] + "_vt = global [0 x i8*] []\n");

        if (keys.length > 1) {
            int cls_number = keys.length;
            int methods_number;
            for (int cls_index = 1; cls_index < cls_number; cls_index++) {      /*** skip main class ***/
                String cls_name = keys[cls_index];
                MJClass cls = this.sym_table.get(cls_name);
                methods_number = cls.methods.size();
                emit("@." + cls_name + "_vt = global [" + methods_number + " x i8*] [");

                if (methods_number == 0) {
                    emit("]\n");
                    continue;
                }
                Set<String> meth_set = cls.methods.keySet();
                String[] meth_keys = new String[meth_set.size()]; meth_keys = meth_set.toArray(meth_keys);
                MJMethod meth = cls.get_method(meth_keys[0]);
                MJClass meth_cls = meth.get_cls();
                String meth_cls_name = meth_cls.get_name();
                String method_name = meth.get_name();
                String ret_type = this.get_type(meth.get_type().get_name());

                emit("i8* bitcast (" + ret_type + " (i8*" );

                MJType[] args_types = meth.get_arg_types();
                int args_count = args_types.length;


                for (int j = 0; j < args_count; j++) {
                    emit(", " + this.get_type(args_types[j].get_name()));
                }

                emit(")* @" + meth_cls_name + "." + method_name + " to i8*)");

                for (int i = 1; i < methods_number; i++) {      /*** skip first method ***/
                    meth = cls.get_method(meth_keys[i]);
                    meth_cls = meth.get_cls();
                    meth_cls_name = meth_cls.get_name();
                    method_name = meth.get_name();
                    ret_type = this.get_type(meth.get_type().get_name());

                    emit(",\n\t\t\t i8* bitcast (" + ret_type + " (i8*");

                    args_types = meth.get_arg_types();
                    args_count = args_types.length;

                    for (int j = 0; j < args_count; j++) {
                        emit(", " + this.get_type(args_types[j].get_name()));
                    }

                    emit(")* @" + meth_cls_name + "." + method_name + " to i8*)");
                }
                emit("]\n");
            }
        }

        emit("\n\ndeclare i8* @calloc(i32, i32)\ndeclare i32 @printf(i8*, ...)\ndeclare void @exit(i32)\n");
        emit("@_cint = constant [4 x i8] c\"%d\\0a\\00\"\n@_cOOB = constant [15 x i8] c\"Out of bounds\\0a\\00\"\n");
        emit("define void @print_int(i32 %i) {\n\t%_str = bitcast [4 x i8]* @_cint to i8*\n\tcall i32 (i8*, ...) @printf(i8* %_str, i32 %i)\n\tret void\n}\n\n");
        emit("define void @throw_oob() {\n\t%_str = bitcast [15 x i8]* @_cOOB to i8*\n\tcall i32 (i8*, ...) @printf(i8* %_str)\n\tcall void @exit(i32 1)\n\tret void\n}\n");
    }

    public String get_output() {
        return this.output.toString();
    }

    public void emit(String str) {
        this.output.append(str);
    }

    public void reset_registers() {
        this.reg_counter = 0;
    }

    public String new_register() {
        String ret = new String("%_" + this.reg_counter);
        this.reg_counter++;
        return ret;
    }

    public String new_loop() {
        String ret = new String("loop" + this.loop_counter);
        this.loop_counter++;
        return ret;
    }

    public String new_if() {
        String ret = new String("if" + this.if_counter);
        if_counter++;
        return ret;
    }

    public String new_oob() {
        String ret = new String("oob" + this.oob_counter);
        oob_counter++;
        return ret;
    }

    public String new_arr_alloc() {
        String ret = new String("arr_alloc" + this.arr_alloc_counter);
        arr_alloc_counter++;
        return ret;
    }

    public String new_and_clause() {
        String ret = new String("andclause" + this.and_clause_counter);
        and_clause_counter++;
        return ret;
    }

    public String get_type(String t) {
        if (t == "int") {
            return "i32";
        }
        else if (t == "int[]") {
            return "i32*";
        }
        else if (t == "boolean") {
            return "i1";
        }
        else {      /*** class type ***/
            return "i8*";
        }

    }

/** Goal **/
/**
     * f0 -> MainClass()
     * f1 -> ( TypeDeclaration() )*
     * f2 -> <EOF>
     */

    public String visit(Goal n, String argu) {
        n.f0.accept(this, null);

        if (n.f1.present()) {
            for (Enumeration<Node> e_cls = n.f1.elements(); e_cls.hasMoreElements(); ) {
                e_cls.nextElement().accept(this, null);
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

    public String visit(MainClass n, String argu) {
        this.emit("\ndefine i32 @main() {\n");
        this.reset_registers();

        MJClass m_cls = this.sym_table.get(n.f1.accept(this, null));
        MJMethod main_meth = m_cls.get_method("main");

        if (n.f14.present()) {
            Set<String> set = main_meth.decls.keySet();
            String[] keys = new String[set.size()];
            keys = set.toArray(keys);

            for (String decl_name : keys) {
                String decl_type = this.get_type(main_meth.get_decl(decl_name).get_type().get_name());
                emit("\t%" + decl_name + " = alloca " + decl_type + "\n");
            }
        }

        if (n.f15.present()){
            n.f15.accept(this, null);
        }

        emit("\tret i32 0\n}\n");

        return null;
    }

/** TypeDeclaration **/
/**
     * f0 -> ClassDeclaration()
     *       | ClassExtendsDeclaration()
     */

    public String visit(TypeDeclaration n, String argu) {
        n.f0.accept(this, null);
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

    public String visit(ClassDeclaration n, String argu) {
        String cls_name = n.f1.accept(this, null);
        if (n.f4.present()) {
            for (Enumeration<Node> e_meth = n.f4.elements(); e_meth.hasMoreElements(); ) {
                e_meth.nextElement().accept(this, cls_name);
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

    public String visit(ClassExtendsDeclaration n, String argu) {
        if (n.f6.present()) {
            n.f6.accept(this, n.f1.accept(this, null));
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

    public String visit(MethodDeclaration n, String argu) {
        this.reset_registers();
        String method_name = n.f2.accept(this, null);
        MJMethod meth = this.sym_table.get(argu).get_method(method_name);
        String method_type = this.get_type((n.f1.accept(this, null)));

        emit("\ndefine " + method_type + " @" + argu + "." + method_name);
        emit("(i8* %this");

        Set<String> set = meth.args.keySet();
        String[] keys = new String[set.size()];
        keys = set.toArray(keys);

        for (String arg_name : keys) {
            String arg_type = this.get_type(meth.get_arg(arg_name).get_type().get_name());
            emit(", " + arg_type + " %." + arg_name);
        }
        emit(") {\n");

        for (String arg_name : keys) {
            String arg_type = this.get_type(meth.get_arg(arg_name).get_type().get_name());
            emit("\t%" + arg_name + " = alloca " + arg_type + "\n");
            emit("\tstore " + arg_type + " %." + arg_name + ", " + arg_type + "* %" + arg_name + "\n");
        }

        if (n.f7.present()) {
            set = meth.decls.keySet();
            keys = new String[set.size()];
            keys = set.toArray(keys);

            for (String decl_name : keys) {
                String decl_type = this.get_type(meth.get_decl(decl_name).get_type().get_name());
                emit("\t%" + decl_name + " = alloca " + decl_type + "\n");
            }
        }

        if (n.f8.present()) {
            for (Enumeration<Node> e_stmt = n.f8.elements(); e_stmt.hasMoreElements(); ) {
                e_stmt.nextElement().accept(this, argu + "." + method_name);
            }
        }

        String ret = n.f10.accept(this, argu + "." + method_name).split("\\.")[0];
        emit("\tret " + method_type + " " + ret + "\n}\n");

        return null;
    }

/** Type **/
/**
     * f0 -> ArrayType()
     *       | BooleanType()
     *       | IntegerType()
     *       | Identifier()
     */

    public String visit(Type n, String argu) {
        return n.f0.accept(this, null);
    }

/** ArrayType **/
/**
     * f0 -> "int"
     * f1 -> "["
     * f2 -> "]"
     */

    public String visit(ArrayType n, String argu) {
        return "int[]";
    }


/** BooleanType **/
/**
     * f0 -> "boolean"
     */

    public String visit(BooleanType n, String argu) {
        return "boolean";
    }

/** IntegerType **/
/**
     * f0 -> "int"
     */

    public String visit(IntegerType n, String argu) {
        return "int";
    }

/** Statement **/
/**
     * f0 -> Block()
     *       | AssignmentStatement()
     *       | ArrayAssignmentStatement()
     *       | IfStatement()
     *       | WhileStatement()
     *       | PrintStatement()
     */

    public String visit(Statement n, String argu) {
        return n.f0.accept(this, argu);
    }

/** Block **/
/**
     * f0 -> "{"
     * f1 -> ( Statement() )*
     * f2 -> "}"
     */

    public String visit(Block n, String argu) {
        if (n.f1.present()) {
            for (Enumeration<Node> e_stmt = n.f1.elements(); e_stmt.hasMoreElements(); ) {
                e_stmt.nextElement().accept(this, argu);
            }
        }

        emit("\n\n");
        return null;
    }

/** AssignmentStatement **/
/**
     * f0 -> Identifier()
     * f1 -> "="
     * f2 -> Expression()
     * f3 -> ";"
     */

    public String visit(AssignmentStatement n, String argu) {
        String[] sp = argu.split("\\.");
        MJClass cls = this.sym_table.get(sp[0]);
        MJMethod meth = cls.get_method(sp[1]);
        MJVariable var = meth.get_variable_definition(n.f0.accept(this, null));

        String toStore = n.f2.accept(this, argu).split("\\.")[0];
        String typeToStore = this.get_type(var.get_type().get_name());
        String var_name = var.get_name();
        String storeTo = null;
        if ( (meth.get_arg(var_name) == null) && (meth.get_decl(var_name) == null) ) {        //variable is class field
            String tmp = new_register();
            int offset = cls.get_var_offset(var_name) + 8;       //the v_table is at the start in the IR language
            emit("\t" + tmp + " = getelementptr i8, i8* %this, i32 " + offset + "\n");
            storeTo = new_register();
            emit("\t" + storeTo + " = bitcast i8* " + tmp + " to " + typeToStore + "*\n");
        }
        else {          //local method variable
            storeTo = new String("%" + var_name);
        }

        emit("\tstore " + typeToStore + " " + toStore + ", " + typeToStore + "* " + storeTo + "\n\n");

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

    public String visit(ArrayAssignmentStatement n, String argu) {
        String[] sp = argu.split("\\.");
        MJClass cls = this.sym_table.get(sp[0]);
        MJMethod meth = cls.get_method(sp[1]);

        String branch1 = null, branch2 = null, ending_label = null;
        String tmp_reg1 = null, tmp_reg2 = null, array_register = null;
        String exp1_res = null, exp2_res = null;

        String array_name = n.f0.accept(this, null);

        exp1_res = n.f2.accept(this, argu).split("\\.")[0];

        if ( (meth.get_arg(array_name) == null) && (meth.get_decl(array_name) == null) ) {        //variable is class field
            tmp_reg1 = new_register();
            int offset = cls.get_var_offset(array_name) + 8;
            emit("\t" + tmp_reg1 + " = getelementptr i8, i8* %this, i32 " + offset + "\n");
            tmp_reg2 = new_register();
            emit("\t" + tmp_reg2 + " = bitcast  i8* " + tmp_reg1 + " to i32**\n");
            tmp_reg1 = new_register();
            emit("\t" + tmp_reg1 + " = load i32*, i32** " + tmp_reg2 + "\n");
            array_register = new String(tmp_reg1);
            tmp_reg2 = new_register();
            emit("\t" + tmp_reg2 + " = load i32, i32* " + tmp_reg1 + "\n");
            tmp_reg1 = new_register();
            emit("\t" + tmp_reg1 + " = icmp ult i32 " + exp1_res + ", " + tmp_reg2 + "\n");
            branch1 = new_arr_alloc();
            branch2 = new_arr_alloc();
            emit("\tbr i1 " + tmp_reg1 + ", label %" + branch1 + ", label %" + branch2 + "\n");
        }
        else {
            tmp_reg1 = new_register();
            emit("\t" + tmp_reg1 + " = load i32, i32* %" + array_name + "\n");
            tmp_reg2 = new_register();
            emit("\t" + tmp_reg2 + " = icmp ult i32 " + exp1_res + ", " + tmp_reg1 + "\n");
            emit("\tbr i1 " + tmp_reg2 + ", label %" + branch1 + ", label %" + branch2 + "\n");
        }

        emit(branch1 + ":\n");
        tmp_reg1 = new_register();
        emit("\t" + tmp_reg1 + " = add i32 " + exp1_res + ", 1\n");
        tmp_reg2 = new_register();
        emit("\t" + tmp_reg2 + " = getelementptr i32, i32* " + array_register + ", i32 " + tmp_reg1 + "\n");
        exp2_res = n.f5.accept(this, argu).split("\\.")[0];
        emit("\tstore i32 " + exp2_res + ", i32* " + tmp_reg2 + "\n");
        ending_label = new_oob();
        emit("\tbr label %" + ending_label + "\n");
        emit(branch2 + ":\n");
        emit("\tcall void @throw_oob()\n");
        emit("\tbr label %" + ending_label + "\n");
        emit(ending_label + ":\n");

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

    public String visit(IfStatement n, String argu) {
        String exp_res = n.f2.accept(this, argu).split("\\.")[0];

        String branch1 = this.new_if();
        String branch2 = this.new_if();
        String exit_label = this.new_if();
        emit("\tbr i1 " + exp_res + ", label %" + branch1 + ", label %" + branch2 + "\n");
        emit(branch1 + ":\n");

        n.f4.accept(this, argu);

        emit("\tbr label %" + exit_label + "\n");

        emit(branch2 + ":\n");

        n.f6.accept(this, argu);

        emit("\tbr label %" + exit_label + "\n");
        emit(exit_label + ":\n");

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

    public String visit(WhileStatement n, String argu) {
        String loop_start = this.new_loop();

        emit("\tbr label %" + loop_start + "\n");
        emit(loop_start + ":\n");

        String exp_res = n.f2.accept(this, argu).split("\\.")[0];
        String loop_body = this.new_loop();
        String loop_end = this.new_loop();
        emit("\tbr i1 " + exp_res + ", label %" + loop_body + ", label %" + loop_end + "\n");
        emit(loop_body + ":\n");

        n.f4.accept(this, argu);

        emit("\t br label %" + loop_start + "\n");
        emit(loop_end + ":\n");

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

    public String visit(PrintStatement n, String argu) {
        String exp_res = n.f2.accept(this, argu).split("\\.")[0];

        emit("\tcall void (i32) @print_int(i32 " + exp_res + ")\n");

        return null;
    }

/** Expression **/
/**
     * f0 -> AndExpression()
     *       | CompareExpression()
     *       | PlusExpression()
     *       | MinusExpression()
     *       | TimesExpression()
     *       | ArrayLookup()
     *       | ArrayLength()
     *       | MessageSend()
     *       | Clause()
     */

    public String visit(Expression n, String argu) {
        return n.f0.accept(this, argu);
    }

/** AndExpression **/
/**
     * f0 -> Clause()
     * f1 -> "&&"
     * f2 -> Clause()
     */

    public String visit(AndExpression n, String argu) {
        String clause1 = n.f0.accept(this, argu);

        String clause_label1 = this.new_and_clause();

        emit("\tbr label %" + clause_label1 + "\n");
        emit(clause_label1 + ":\n");

        String clause_label2 = this.new_and_clause();
        String clause_label3 = this.new_and_clause();
        String clause_label4 = this.new_and_clause();
        emit("\tbr i1 " + clause1 + ", label %" + clause_label2 + ", label %" + clause_label4 + "\n");

        emit(clause_label2 + ":\n");

        String clause2 = n.f2.accept(this, argu);

        emit("\tbr label %" + clause_label3 + "\n");
        emit(clause_label3 + ":\n");
        emit("\tbr label %" + clause_label4 + "\n");
        emit(clause_label4 + ":\n");

        String ret = this.new_register();
        emit("\t" + ret + " = phi i1 [0, %" + clause_label1 + "], [" + clause2 + ", %" + clause_label3 + "]\n");

        return ret;
    }

/** CompareExpression **/
/**
     * f0 -> PrimaryExpression()
     * f1 -> "<"
     * f2 -> PrimaryExpression()
     */

    public String visit(CompareExpression n, String argu) {
        String acc1 = n.f0.accept(this, argu);
        String[] sp1 = acc1.split("\\.");
        String acc2 = n.f2.accept(this, argu);
        String[] sp2 = acc2.split("\\.");

        String prim_exp1 = sp1[0];
        String prim_exp2 = sp2[0];

        String ret = this.new_register();
        emit("\t" + ret + " = icmp slt i32 " + prim_exp1 + ", " + prim_exp2 + "\n");

        return ret;
    }

/** PlusExpression **/
/**
     * f0 -> PrimaryExpression()
     * f1 -> "+"
     * f2 -> PrimaryExpression()
     */

    public String visit(PlusExpression n, String argu) {
        String acc1 = n.f0.accept(this, argu);
        String[] sp1 = acc1.split("\\.");
        String acc2 = n.f2.accept(this, argu);
        String[] sp2 = acc2.split("\\.");

        String prim_exp1 = sp1[0];
        String prim_exp2 = sp2[0];

        String ret = this.new_register();
        emit("\t" + ret + " = add i32 " + prim_exp1 + ", " + prim_exp2 + "\n");

        return ret;
    }

/** MinusExpression **/
/**
     * f0 -> PrimaryExpression()
     * f1 -> "-"
     * f2 -> PrimaryExpression()
     */

    public String visit(MinusExpression n, String argu) {
        String acc1 = n.f0.accept(this, argu);
        String[] sp1 = acc1.split("\\.");
        String acc2 = n.f2.accept(this, argu);
        String[] sp2 = acc2.split("\\.");

        String prim_exp1 = sp1[0];
        String prim_exp2 = sp2[0];

        String ret = this.new_register();
        emit("\t" + ret + " = sub i32 " + prim_exp1 + ", " + prim_exp2 + "\n");

        return ret;
    }

/** TimesExpression **/
/**
     * f0 -> PrimaryExpression()
     * f1 -> "*"
     * f2 -> PrimaryExpression()
     */

    public String visit(TimesExpression n, String argu) {
        String acc1 = n.f0.accept(this, argu);
        String[] sp1 = acc1.split("\\.");
        String acc2 = n.f2.accept(this, argu);
        String[] sp2 = acc2.split("\\.");

        String prim_exp1 = sp1[0];
        String prim_exp2 = sp2[0];

        String ret = this.new_register();
        emit("\t" + ret + " = mul i32 " + prim_exp1 + ", " + prim_exp2 + "\n");

        return ret;
    }

/** ArrayLookup **/
/**
     * f0 -> PrimaryExpression()
     * f1 -> "["
     * f2 -> PrimaryExpression()
     * f3 -> "]"
     */

    public String visit(ArrayLookup n, String argu) {
        String acc1 = n.f0.accept(this, argu);
        String[] sp1 = acc1.split("\\.");
        String acc2 = n.f2.accept(this, argu);
        String[] sp2 = acc2.split("\\.");

        String prim_exp1 = sp1[0];
        String prim_exp2 = sp2[0];

        String tmp_reg1 = this.new_register();
        String tmp_reg2 = this.new_register();

        emit("\t" + tmp_reg1 + " = load i32, i32* " + prim_exp1 + "\n");
        emit("\t" + tmp_reg2 + " = icmp ult i32 " + prim_exp2 + ", " + tmp_reg1 + "\n");

        String oob_label1 = this.new_oob(), oob_label2 = this.new_oob(), ending_label = this.new_oob();

        emit("\tbr i1 " + tmp_reg2 + ", label %" + oob_label1 + ", label %" + oob_label2 + "\n");
        emit(oob_label1 + ":\n");

        tmp_reg1 = this.new_register();
        emit("\t" + tmp_reg1 + " = add i32" + prim_exp2 + ", 1\n");
        tmp_reg2 = this.new_register();
        emit("\t" + tmp_reg2 + " = getelementptr i32, i32* " + prim_exp1 + ", i32 " + tmp_reg1 + "\n");
        tmp_reg1 = this.new_register();
        emit("\t" + tmp_reg1 + " = load i32, i32* " + tmp_reg2 + "\n");

        String ret = tmp_reg1;

        emit("\tbr label %" + ending_label + "\n");
        emit(oob_label2 + ":\n");
        emit("\tcall void @throw_oob()\n");
        emit("\tbr label %" + ending_label + "\n");
        emit(ending_label + ":\n");

        return ret;
    }

/** ArrayLength **/
/**
     * f0 -> PrimaryExpression()
     * f1 -> "."
     * f2 -> "length"
     */

    public String visit(ArrayLength n, String argu) {
        String acc = n.f0.accept(this, argu);
        String[] sp = acc.split("\\.");

        String prim_exp = sp[0];
        String ret = this.new_register();
        emit("\t" + ret + " = load i32, i32* " + prim_exp + "\n");

        return ret;
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

    public String visit(MessageSend n, String argu) {

        String exp_list = null;
        if (n.f4.present()) {
            exp_list = n.f4.accept(this, argu);
        }

        String prim_exp = n.f0.accept(this, argu);
        String[] sp = prim_exp.split("\\.");

        MJClass cls = this.sym_table.get(sp[1]);
        int offset = cls.get_meth_offset(n.f2.f0.toString())/8;

        MJMethod meth = cls.get_method(n.f2.f0.toString());

        String tmp_reg1 = this.new_register();
        String tmp_reg2 = this.new_register();

        emit("\t" + tmp_reg1 + " = bitcast i8* " + sp[0] + " to i8***\n");
        emit("\t" + tmp_reg2 + " = load i8**, i8*** " + tmp_reg1 + "\n");
        tmp_reg1 = this.new_register();
        emit("\t" + tmp_reg1 + " = getelementptr i8*, i8** " + tmp_reg2 + ", i32 " + offset + "\n");
        tmp_reg2 = this.new_register();
        emit("\t" + tmp_reg2 + " = load i8*, i8** " + tmp_reg1 + "\n");
        tmp_reg1 = this.new_register();

        String method_type = this.get_type(meth.get_type().get_name());
        emit("\t" + tmp_reg1 + " = bitcast i8* " + tmp_reg2 + " to " + method_type + "(i8*");

        MJType[] arg_types = meth.get_arg_types();
        for (MJType arg : arg_types) {
            String arg_t = this.get_type(arg.get_name());
            emit(", " + arg_t);
        }
        emit(")*\n");

        String ret = this.new_register();
        emit("\t" + ret + " = call " + method_type + " " + tmp_reg1 + "(i8* " + sp[0]);

        if (exp_list != null) {
            String[] sp2 = exp_list.split("\\.");
            int length = sp2.length;

            for (int i = 0; i < length; i++) {
                String arg_t = this.get_type(arg_types[i].get_name());
                emit(", " + arg_t + " " + sp2[i]);
            }
        }
        emit(")\n");

        return ret + "." + meth.get_type().get_name();
    }

/** ExpressionList **/
    /**
     * f0 -> Expression()
     * f1 -> ExpressionTail()
     */
    public String visit(ExpressionList n, String argu) {
       StringBuilder sb = new StringBuilder(n.f0.accept(this, argu).split("\\.")[0]);
       sb.append(".");
       String exp_tail = n.f1.accept(this, argu);
       if ( exp_tail != null ) {
          sb.append(n.f1.accept(this, argu));
       }

       String ret = sb.toString();

       return ret;
    }

/** ExpressionTail **/
    /**
     * f0 -> ( ExpressionTerm() )*
     */
    public String visit(ExpressionTail n, String argu) {
       StringBuilder sb = new StringBuilder();
       if (n.f0.present()) {
          for (Enumeration<Node> e_term = n.f0.elements(); e_term.hasMoreElements(); ) {
              sb.append(e_term.nextElement().accept(this, argu));
              sb.append(".");
          }
          String ret = sb.toString();
          return ret;
       }

       return null;
    }

/** ExpressionTerm **/
    /**
     * f0 -> ","
     * f1 -> Expression()
     */
    public String visit(ExpressionTerm n, String argu) {
       String ret = n.f1.accept(this, argu).split("\\.")[0];

       return ret;
    }

/** Clause **/
/**
     * f0 -> NotExpression()
     *       | PrimaryExpression()
     */

    public String visit(Clause n, String argu) {
        String acc = n.f0.accept(this, argu);
        String[] sp = acc.split("\\.");
        String ret = sp[0];
        return sp[0];
    }

/** PrimaryExpression **/
/**
     * f0 -> IntegerLiteral()
     *       | TrueLiteral()
     *       | FalseLiteral()
     *       | Identifier()
     *       | ThisExpression()
     *       | ArrayAllocationExpression()
     *       | AllocationExpression()
     *       | BracketExpression()
     */

    public String visit(PrimaryExpression n, String argu) {
        return n.f0.accept(this, argu);
    }

/** IntegerLiteral **/
/**
     * f0 -> <INTEGER_LITERAL>
     */

    public String visit(IntegerLiteral n, String argu) {
        return n.f0.toString();
    }

/** TrueLiteral **/
/**
     * f0 -> "true"
     */

    public String visit(TrueLiteral n, String argu) {
        return "1";
    }

/** FalseLiteral **/
/**
     * f0 -> "false"
     */

    public String visit(FalseLiteral n, String argu) {
        return "0";
    }

/** Identifier **/
/**
     * f0 -> <IDENTIFIER>
     */

    public String visit(Identifier n, String argu) {
        String name = n.f0.toString();
        if (argu == null) {     //if just a name is needed
            return name;
        }
        String ret = null;

        String[] sp = argu.split("\\.");
        MJClass cls = this.sym_table.get(sp[0]);
        MJMethod meth = cls.get_method(sp[1]);
        MJVariable var = meth.get_variable_definition(name);
        String type = this.get_type(var.get_type().get_name());

        if ( (meth.get_decl(name) == null) && (meth.get_arg(name) == null) ) {      //class field
            String tmp_reg1 = this.new_register();
            String tmp_reg2 = this.new_register();

            int offset = cls.get_var_offset(name) + 8;

            emit("\t" + tmp_reg1 + " = getelementptr i8, i8* %this, i32 " + offset + "\n");
            emit("\t" + tmp_reg2 + " = bitcast i8* " + tmp_reg1 + " to " + type + "*\n");
            ret = this.new_register();
            emit("\t" + ret + " = load " + type + ", " + type + "* " + tmp_reg2 + "\n");
        }
        else {
            ret = this.new_register();
            emit("\t" + ret + " = load " + type + ", " + type + "* %" + name + "\n");
        }

        return ret + "." + var.get_type().get_name();       //this helps MessageSend know which object's function has to be called
    }

/** ThisExpression **/
/**
     * f0 -> "this"
     */

    public String visit(ThisExpression n, String argu) {
        String ret = new String("%this");
        String[] sp = argu.split("\\.");

        return ret + "." + sp[0];
    }

/** ArrayAllocationExpression **/
/**
     * f0 -> "new"
     * f1 -> "int"
     * f2 -> "["
     * f3 -> Expression()
     * f4 -> "]"
     */

    public String visit(ArrayAllocationExpression n, String argu) {
        String size = n.f3.accept(this, argu).split("\\.")[0];

        String tmp_reg1 = this.new_register(), tmp_reg2 = null;
        emit("\t" + tmp_reg1 + " = icmp slt i32 " + size + ", 0\n");
        String branch1 = this.new_arr_alloc(), branch2 = this.new_arr_alloc();
        emit("\tbr i1 " + tmp_reg1 + ", label %" + branch1 + ", label %" + branch2 + "\n");

        emit(branch1 + ":\n");
        emit("\tcall void @throw_oob()\n");
        emit("\tbr label %" + branch2 + "\n");
        emit(branch2 + ":\n");

        tmp_reg1 = this.new_register();
        emit("\t" + tmp_reg1 + " = add i32 " + size + ", 1\n");
        tmp_reg2 = this.new_register();
        emit("\t" + tmp_reg2 + " = call i8* @calloc(i32 4, i32 " + tmp_reg1 + ")\n");
        tmp_reg1 = this.new_register();
        emit("\t" + tmp_reg1 + " = bitcast i8* " + tmp_reg2 + " to i32*\n");
        emit("\tstore i32 " + size + ", i32* " + tmp_reg1 + "\n");

        String ret = tmp_reg1;
        return ret;
    }

/** AllocationExpression **/
/**
     * f0 -> "new"
     * f1 -> Identifier()
     * f2 -> "("
     * f3 -> ")"
     */

    public String visit(AllocationExpression n, String argu) {
        String ret = this.new_register();
        String cls_name = n.f1.accept(this, null);
        MJClass cls = this.sym_table.get(cls_name);
        int cls_size = cls.get_cls_size();
        emit("\t" + ret + " = call i8* @calloc(i32 1, i32 " + cls_size + ")\n");

        String tmp_reg1 = this.new_register();
        emit("\t" + tmp_reg1 + " = bitcast i8* " + ret + " to i8***\n");
        String tmp_reg2 = this.new_register();
        int methods_size = cls.methods_size();
        emit("\t" + tmp_reg2 + " = getelementptr [" + methods_size + " x i8*], [" + methods_size + " x i8*]* @." + cls_name + "_vt, i32 0, i32 0\n");
        emit("\tstore i8** " + tmp_reg2 + ", i8*** " + tmp_reg1 + "\n");

        return ret + "." + cls_name;
    }

/** NotExpression **/
/**
     * f0 -> "!"
     * f1 -> Clause()
     */

    public String visit(NotExpression n, String argu) {
        String clause = n.f1.accept(this, argu);
        String ret = this.new_register();
        emit("\t" + ret + " = xor i1 1, " + clause + "\n");

        return ret;
    }

/** BracketExpression **/
/**
     * f0 -> "("
     * f1 -> Expression()
     * f2 -> ")"
     */

    public String visit(BracketExpression n, String argu) {
        return n.f1.accept(this, argu);
    }

}
