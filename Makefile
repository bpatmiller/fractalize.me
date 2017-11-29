JC=javac
SOURCES=*.java

# Clear default make targets for java files
.SUFFIXES: .java .class

# Set default target
default: all

# Builds everything
all: $(SOURCES:.java=.class) in out

# Directory targets
in: ; mkdir -p in
out: ; mkdir -p out

# Removes build results
.PHONY: clean
clean:
	-rm -rf in out
	-rm -rf *.class

# Uses suffix rule syntax to build any given java source file
.java.class:
	$(JC) $*.java

# Runs a demo
demo: all
	cp examples/demo.png in/in.png
	java Fractalize
