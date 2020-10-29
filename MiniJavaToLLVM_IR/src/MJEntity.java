import java.util.*;

//a MiniJava program consists of these entities, all of them have a name
public abstract class MJEntity {
    private String name;

    /**** constructor ****/
    public MJEntity(String name) {
        this.name = name;
    }

    /*********************/
    public String get_name() {
        return this.name;
    }
}

//an auxiliary class for error handling
class MJError extends MJEntity {
    /**** constructor ****/
    public MJError(String error) {
        super(error);
    }

    /**********************/
    public String get_error() {
        return this.get_name();
    }
}

abstract class MJType extends MJEntity {
    private int size;

    /**** constructor ****/
    public MJType(String name, int size) {
        super(name);
        this.size = size;
    }

    /*********************/
    public int get_size() {
        return this.size;
    }
}

class MJArray extends MJType {
    /**** constructor ****/
    public MJArray() {
        super("int[]", 8);
    }
    /*********************/
}

class MJBoolean extends MJType {
    /**** constructor ****/
    public MJBoolean() {
        super("boolean", 1);
    }
    /*********************/
}

class MJInteger extends MJType {
    /**** constructor ****/
    public MJInteger() {
        super("int", 4);
    }
    /*********************/
}

//the type of the unique argument of main method
class MJMainStringArray extends MJType {
    /**** constructor ****/
    public MJMainStringArray() {
        super("String[]", 0);
    }
    /**********************/
}

class MJMainMethodType extends MJType {
    /**** constructor ****/
    public MJMainMethodType() {
        super("static void", 0);
    }
    /**********************/
}

//every class of a MiniJava program has zero or more fields and methods. They are the class's content
abstract class MJClassContent extends MJEntity {
    private MJType type;
    private int offset;

    /**** constructor ****/
    public MJClassContent(String name, MJType type) {
        super(name);
        this.type = type;
        offset = 0;
    }

    /**********************/
    public MJType get_type() {
        return this.type;
    }

    public void set_type(MJType t) {
        this.type = t;
    }
    public void set_offset(int offset) {
        this.offset = offset;
    }
}

class MJVariable extends MJClassContent {
    /**** constructor ****/
    public MJVariable(String name, MJType type) {
        super(name, type);
    }
    /*********************/
}

//a field can be a method
class MJMethod extends MJClassContent {
    //this method's arguments
    public LinkedHashMap<String, MJVariable> args = new LinkedHashMap<String, MJVariable>();
    //this method's variable declarations
    public LinkedHashMap<String, MJVariable> decls = new LinkedHashMap<String, MJVariable>();
    private MJClass cls;

    /**** constructor ****/
    public MJMethod(String name, MJType type) {
        super(name, type);
    }

    /*********************/
    public void set_cls(MJClass cls) {
        this.cls = cls;
    }

    public MJClass get_cls() {
        return this.cls;
    }

    public MJType[] get_arg_types() {
        MJVariable[] tmp = {};
        tmp = this.args.values().toArray(tmp);
        MJType[] ret = new MJType[tmp.length];
        int index = 0;
        for (MJVariable v : tmp) {
            ret[index] = v.get_type();
            index++;
        }

        return ret;
    }
    public boolean add_arg(MJVariable arg) {
        if (args.containsKey(arg.get_name())) {
            return false;
        }
        args.put(arg.get_name(), arg);
        return true;
    }

    public boolean add_decl(MJVariable v) {
        if (decls.containsKey(v.get_name())) {
            return false;
        }
        decls.put(v.get_name(), v);
        return true;
    }

    public boolean args_contains(String str) {
        return this.args.containsKey(str);
    }

    public int args_size() {
        return this.args.size();
    }

    public MJVariable[] get_args() {
        MJVariable[] tmp = {};
        return this.args.values().toArray(tmp);
    }

    public MJVariable get_arg(String id) {
        return this.args.get(id);
    }

    public boolean decls_contains(String str) {
        return this.decls.containsKey(str);
    }

    public int decls_size() {
        return this.decls.size();
    }

    public MJVariable[] get_decls() {
        MJVariable[] tmp = {};
        return this.decls.values().toArray(tmp);
    }

    public MJVariable get_decl(String str) {
        return this.decls.get(str);
    }

    public MJVariable get_variable_definition(String id) {
        MJVariable ret = this.get_decl(id);
        if (ret != null){
            return ret;
        }
        ret = this.get_arg(id);
        if (ret != null) {
            return ret;
        }
        MJClass cls = this.cls;

        while (cls != null) {
          ret = cls.get_variable(id);
          if (ret != null){
              return ret;
          }
          cls = cls.get_extending();
        }

        return null;
    }

    //methods may have a return type, but their size is 8
    public int get_size() {
        return 8;
    }
}

//a MiniJava program consists only of classes
class MJClass extends MJType {
    //every class has variables or methods
    //LinkedHashMap for order (offsets)
    public LinkedHashMap<String, MJVariable> variables = new LinkedHashMap<String, MJVariable>();
    public LinkedHashMap<String, MJMethod> methods = new LinkedHashMap<String, MJMethod>();
    //a class can inherit from another one
    private MJClass extending = null;
    private int[] offsets = {0, 0};     //offsets[0] is for variables and offsets[1] is for methods
    //offsets[0] will also be used in order to allocate the space need for an object. Also, contains the v_table pointer size
    public LinkedHashMap<String, Integer> variables_offsets = new LinkedHashMap<String, Integer>();
    public LinkedHashMap<String, Integer> methods_offsets = new LinkedHashMap<String, Integer>();

    private int size;

    /**** constructor ****/
    public MJClass(String name, MJClass ext) {
        super(name, 8);
        if (ext != null) {
            this.extending = ext;
            this.methods = null;
            this.methods = new LinkedHashMap<String, MJMethod>(this.extending.methods);
        }
    }
    /********************/

    public boolean existsOverridesInSuper(MJMethod m) {
        MJClass parent = this.extending;
        while (parent != null) {
            MJMethod ov = null;     //overriden function of superclass
            if ( (ov = parent.get_method(m.get_name())) != null) {      //if there is such a class
                if (ov.get_type().get_name().equals(m.get_type().get_name())) {     //methods have same return type
                    MJType[] ovtypes = ov.get_arg_types();
                    MJType[] mtypes = m.get_arg_types();

                    if (ovtypes.length != mtypes.length) {          //if the overriding function has wrong number of arguments
                        return false;
                    }

                    int index = 0;
                    for (MJType t : ovtypes) {
                        if (!(t.get_name().equals(mtypes[index].get_name()))){
                            return false;
                        }
                        index++;
                    }
                    return true;
                }
                return false;
            }

            parent = parent.extending;
        }

        return true;
    }

    public boolean add_variable(MJVariable v) {
        if (variables.containsKey(v.get_name())) {
            return false;
        }
        variables.put(v.get_name(), v);
        return true;
    }

    public int variables_size() {
        return this.variables.size();
    }

    public MJVariable[] get_variables() {
        MJVariable[] tmp = {null};
        return this.variables.values().toArray(tmp);
    }

    public MJVariable get_variable(String var_name) {
        return this.variables.get(var_name);
    }

    public boolean add_method(MJMethod m) {
        if (methods.containsKey(m.get_name())) {
            MJMethod overriden = this.methods.get(m.get_name());
            MJType[] ov_args = overriden.get_arg_types();
            MJType[] args = m.get_arg_types();

            if (ov_args.length != args.length){
                return false;
            }

            int ov_index = 0;
            int index = 0;

            for (ov_index = 0; ov_index < ov_args.length; ov_index++) {
                if ( !( (ov_args[ov_index].get_name()).equals( (args[index].get_name()) ) ) ) {
                    return false;
                }
            }
        }
        /*** If the overriden method has the same arguments and arguments' order as the new method, override it ***/
        methods.put(m.get_name(), m);
        return true;
    }

    public MJMethod get_method(String method_name) {
        MJMethod meth = this.methods.get(method_name);
        if (meth != null){
            return meth;
        }
        MJClass ext = this.extending;
        while (ext != null){
            meth = ext.methods.get(method_name);
            if (meth != null){
                return meth;
            }
            ext = ext.extending;
        }

        return null;
    }

    public int methods_size() {
        return this.methods.size();
    }

    public StringBuilder get_offsets() {
        StringBuilder sbuilder = new StringBuilder();
        Set<String> set = this.variables.keySet();
        String[] keys = new String[set.size()];

        this.offsets[0] = (this.extending != null) ? (this.extending.offsets[0] - 8) : (0);
        for (String string : keys = set.toArray(keys)) {
            sbuilder.append(this.get_name() + "." + string + " : " + offsets[0] + "\n");
            variables_offsets.put(string, this.offsets[0]);
            this.offsets[0] += this.variables.get(string).get_type().get_size();
        }

        this.offsets[0] += 8;       //v_table

        set = null; set = this.methods.keySet();
        keys = null; keys = new String[set.size()];

        if (this.extending == null) {
            this.offsets[1] = 0;
            for (String string : keys = set.toArray(keys)) {
                sbuilder.append(this.get_name() + "." + string + " : " + offsets[1] + "\n");
                methods_offsets.put(string, this.offsets[1]);
                this.offsets[1] += this.methods.get(string).get_size();
            }
        }
        else {
            this.offsets[1] = this.extending.offsets[1];
            Set<String> parent_set = this.extending.methods_offsets.keySet();
            String[] parent_keys = new String[parent_set.size()];
            for (String string : parent_keys = parent_set.toArray(parent_keys)) {
                this.methods_offsets.put(string, this.extending.methods_offsets.get(string));
            }

            for (String string : keys = set.toArray(keys)) {
                if (!(this.methods_offsets.containsKey(string))) {
                    sbuilder.append(this.get_name() + "." + string + " : " + offsets[1] + "\n");
                    methods_offsets.put(string, this.offsets[1]);
                    this.offsets[1] += this.methods.get(string).get_size();
                }
            }
        }

        return sbuilder;
    }

    public int get_var_offset(String var_name) {
        if (this.variables_offsets.containsKey(var_name)) {
            return this.variables_offsets.get(var_name);
        }
        else {
            MJClass parent = this.extending;
            while (parent != null) {
                if (parent.variables_offsets.containsKey(var_name)) {
                    return parent.variables_offsets.get(var_name);
                }
                parent = parent.extending;
            }
        }

        return -1;      //should never reach this point after successful type checking
    }

    public int get_meth_offset(String meth_name) {
      return this.methods_offsets.get(meth_name);
    }

    public MJClass get_extending() {
        return this.extending;
    }

    public int get_cls_size() {
        return this.offsets[0];
    }
}
