all: compile

compile:
	java -jar ./jtb132di.jar minijava.jj
	java -jar ./javacc5.jar minijava-jtb.jj
	javac Main.java MJEntity.java MJSymbolTable.java MJMethodSymbolVisitor.java MJSymbolVisitor.java MJIRVisitor.java

clean:
	rm -f *.class *~
