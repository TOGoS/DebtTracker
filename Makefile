.PHONY: clean all src

all: DebtTracker.jar

src:

bin: src
	rm -rf bin
	find src -name *.java >.java-src.lst
	mkdir -p bin
	javac -source 1.6 -target 1.6 -sourcepath src -d bin @.java-src.lst

DebtTracker.jar: bin
	jar -ce togos.debttracker.Parser -C bin . >DebtTracker.jar

debttracker: DebtTracker.jar
	gcj -o "$@" --main=togos.debttracker.Parser DebtTracker.jar

clean:
	rm -rf bin DebtTracker.jar
