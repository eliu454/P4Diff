# Overview:
This is a command line tool that compares the coverage before and after a p4 commit.
It produces an html report, with data described in a table.
Each table row has the changelist num, the file name, and the lines changed.

# Prerequisites:
This needs to be run in a bash shell that has p4 installed and logged in.
Java 7 or later is also required.
Ant is required for building.

# How to build:
This project uses ant to build. Running ant -d with the source files in src/P4Diff
will generate the .class files in /build/P4Diff/, and a .jar file in /dist/lib/ .

# How to run:
This program takes in 2 arguments. To run, enter the command
java -jar P4Diff.jar <coverage.xml> <output_dir> <changelist1,changelist2,...>
Ex: java -jar P4Diff.jar coverage.xml htmlreport 3823093
coverage.xml is the cobertura coverage file.
output_dir is the directory that you would like the output to be in.
changelist1,changelist2,... is a comma separated list of each changelist to diff.

It is also possible to run this on git commits. To run, enter the command
java -jar P4Diff.jar git <coverage.xml> <output_dir> <commitID_1,commitID_2,...>

# Reports:
This program will generate an HTML file in a specified directory.

# Classes:
P4Diff is the main driver class. It parses the XML files, greps the P4Diff itself, and uses
the P4HTMLwriter to write the summary.

P4HTMLWriter is a helper class which wraps around the BufferedWriter. It is used to print out the html files.

Range is a helper class which keeps track of the data for ranges of lines changed in the diff.

FileDiff is a helper class which keeps track of the data of diff changes for each file.

styles.css is for the css.

# Other files:
If there is a properties file called "file.properties", the program will check to see if
the user specifies logging=true, in which case it will print logging statements.