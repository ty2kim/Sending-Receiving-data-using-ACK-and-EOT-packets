# Sending-Receiving-data-using-ACK-and-EOT-packets
Demonstrations of the data transaction using ACK and EOT packets


1. How to compile
- compile everything : type "make" or "make all"
- compile receiver : type "make receiver"
- compile sender : type "make sender"
- erase *.class files : type "make clean"

2. How to run
I did my testing this way:
a. in ubuntu1204-002.student.cs.uwaterloo.ca, 
    ./nEmulator-linux386 3331 ubuntu1204-004 3334 3333 ubuntu1204-006 3332 1 0.1 1
b. in ubuntu1204-004.student.cs.uwaterloo.ca,
    java receiver ubuntu1204-002 3333 3334 <output_file_name>
c. in ubuntu1204-006.student.cs.uwaterloo.ca,
    java sender ubuntu1204-002 3331 3332 <input_file_name>

* note that input_file should be created before running the program

3. What undergrad machine my program was built and tested on
emulator : ubuntu1204-002.student.cs.uwaterloo.ca
receiver : ubuntu1204-004.student.cs.uwaterloo.ca
sender : ubuntu1204-006.student.cs.uwaterloo.ca

4. What version of make and compiler I am using
GNU Make 3.81
java version "1.6.0_31"
