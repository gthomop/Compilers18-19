import java.util.*;

class MJSymbolTable {
    public LinkedHashMap< String, MJClass > sym_table = new LinkedHashMap< String, MJClass >();
    public static final MJArray array_type = new MJArray();
    public static final MJBoolean boolean_type = new MJBoolean();
    public static final MJInteger integer_type = new MJInteger();
    public static final MJMainStringArray string_array_type = new MJMainStringArray();
    public static final MJMainMethodType main_method_type = new MJMainMethodType();

    public void add_class(MJClass c) {
        sym_table.put(c.get_name(), c);
    }
    public boolean contains(String c) {
        if ( sym_table.containsKey(c) ) {
            return true;
        }
        return false;
    }
    public MJClass get(String name) {
        return sym_table.get(name);
    }
}
