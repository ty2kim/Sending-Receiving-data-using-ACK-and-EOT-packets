.phony: all
.phony: clean

all: receiver sender

receiver:
	javac receiver.java

sender:
	javac sender.java

clean:
	rm *.class
