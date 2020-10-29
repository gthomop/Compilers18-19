import syntaxtree.*;
import visitor.*;
import java.io.*;
import java.util.*;
import java.lang.*;

class Main {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Usage: java Main <inputFile1> <inputFile2> ... <inputFileN>");
            System.exit(1);
        }
        FileInputStream fis = null;
        for (String s : args) {
            System.err.println("**********************");
            try {
                fis = new FileInputStream(s);
                MiniJavaParser parser = new MiniJavaParser(fis);
                System.err.println("Program parsed successfully.");
                System.err.println("$Starting Semantic Analysis$");
                //the syntaxtree entry Goal
                Goal goal = parser.Goal();
                //symbol table and the visitor to fill it
                MJSymbolTable sym_table = new MJSymbolTable();
                MJSymbolVisitor sym_visitor = new MJSymbolVisitor(sym_table);
                MJEntity ret = null;
                ret = goal.accept(sym_visitor);
                if (ret != null) {
                    MJError err = (MJError) ret;
                    System.err.println("Error in file " + s + ":" + err.get_error());
                    try {
                        if (fis != null) fis.close();
                        continue;
                    } catch (IOException ex) {
                        System.err.println(ex.getMessage());
                    }
                }

                MJTypeFillVisitor t_fill_visitor = new MJTypeFillVisitor(sym_visitor);
                ret = goal.accept(t_fill_visitor, null);
                if (ret != null){
                    MJError err = (MJError) ret;
                    System.err.println("Error in file " + s + ":" + err.get_error());
                    try {
                        if (fis != null) fis.close();
                        continue;
                    } catch (IOException ex) {
                        System.err.println(ex.getMessage());
                    }
                }

                MJTypeCheckVisitor tc_visitor = new MJTypeCheckVisitor(sym_visitor);
                ret = goal.accept(tc_visitor, null);
                if (ret != null){
                    MJError err = (MJError) ret;
                    System.err.println("Error in file " + s + ":" + err.get_error());
                    try {
                        if (fis != null) fis.close();
                        continue;
                    } catch (IOException ex) {
                        System.err.println(ex.getMessage());
                    }
                }

                System.err.println("File " + s + " has no errors.");

                Set<String> set = sym_table.sym_table.keySet();
                String[] keys = new String[set.size()];

                System.out.println("Going to print offsets...");
                for (String cls_name : keys = set.toArray(keys)) {
                    System.out.println(sym_table.get(cls_name).get_offsets());
                }

                /*** split the filename in tokens separated by "." in order to delete the file extension ***/
                String[] sp = s.split("\\.");
                int sp_length = sp.length;
                StringBuilder filename = new StringBuilder();
                for (int i = 0; i < sp_length - 1 ; i++) {
                    filename.append(sp[i] + ".");
                }
                File f = new File("./" + filename + "ll");
                PrintWriter printer = new PrintWriter(f);

                MJIRVisitor ir_visitor = new MJIRVisitor(sym_table);
                goal.accept(ir_visitor, null);
                printer.print(ir_visitor.get_output());
                printer.flush();
                System.out.println("Finished writing output.");



            } catch (ParseException ex) {
                System.out.println(ex.getMessage());
            } catch (FileNotFoundException ex) {
                System.err.println(ex.getMessage());
            } finally {
                try {
                    if (fis != null) fis.close();
                } catch (IOException ex) {
                    System.err.println(ex.getMessage());
                }
            }
        }
    }
}
