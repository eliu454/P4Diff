package p4Diff;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class P4Diff {

  private static P4HTMLWriter writer;
  private static P4HTMLWriter greenWriter;
  private static P4HTMLWriter redWriter;
  private static boolean logging;

  //main driver method
  public static void main(String[] args) {
    int numOfCoverageArgs = 3;
    int numOfGitArgs = 4;
    String help = "Usage: java -jar P4Diff.jar <coverage.xml> <output_dir> <changelist_1,changelist_2,...>\n" +
      "For git, usage: java -jar P4Diff.jar git <coverage.xml> <output_dir> <commitID_1,commitID_2,...>\n";
    if(args.length != numOfCoverageArgs && args.length != numOfGitArgs){
      System.err.println(help);
      System.exit(1);
    }

    File propertiesFile = new File("file.properties");
    if(propertiesFile.exists()){
      try {
        BufferedReader propertyReader = new BufferedReader(new FileReader(propertiesFile));
        String input;
        while ((input = propertyReader.readLine()) != null){
          if(input.trim().equals("logging=true")){
            logging = true;
          }
        }
      } catch (IOException e){
        e.printStackTrace();
      }
    }

    if(args.length == numOfCoverageArgs) {

      int coverageFileIndex = 0;
      int outputDirIndex = 1;
      int changeListIndex = 2;

      String coverageFile = args[coverageFileIndex];
      String outputDir =  args[outputDirIndex];
      String[] changeLists = args[changeListIndex].split(",");
      //initialize writers, and write headers
      writer = new P4HTMLWriter(outputDir + "/p4Diff.html");
      greenWriter = new P4HTMLWriter(outputDir + "/greenP4Diff.html");
      redWriter = new P4HTMLWriter(outputDir + "/redP4Diff.html");
      writer.writeHeader(true);
      greenWriter.writeHeader(true);
      redWriter.writeHeader(true);

      //loop over the changelists
      for (String changeListNum : changeLists) {
        log("Currently processing changelist: " + changeListNum);
        log("\tCalling the command \"p4 describe " + changeListNum + "\"");
        //parse the changes
        String p4DiffOutput = getShellCmdOutput(new String[]{"p4", "describe", changeListNum});
        log("\tProcessing the command \"p4 describe " + changeListNum + "\"");
        HashMap<String, FileDiff> fileDiffMap = getChangesFromP4Diff(p4DiffOutput);
        log("\tProcessing the coverage file");
        //generate the map and the report
        getCoverageForFileDiffMap(fileDiffMap, coverageFile);
        log("\tGenerating the html report");
        generateCoverageHTMLReport(changeListNum, fileDiffMap);
      }

      //close the writers
      writer.closeHTMLFile();
      redWriter.closeHTMLFile();
      greenWriter.closeHTMLFile();
      //copy the css file over
      try {
        Files.copy(P4Diff.class.getResourceAsStream("/styles.css"),
          new File(outputDir + "/styles.css").toPath(), REPLACE_EXISTING);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    else if(args[0].equals("git")){
      int coverageFileIndex = 1;
      int outputDirIndex = 2;
      int changeListIndex = 3;

      String coverageFile = args[coverageFileIndex];
      String outputDir =  args[outputDirIndex];
      String[] commits = args[changeListIndex].split(",");

      //initialize writers, and write headers
      writer = new P4HTMLWriter(outputDir + "/p4Diff.html");
      greenWriter = new P4HTMLWriter(outputDir + "/greenP4Diff.html");
      redWriter = new P4HTMLWriter(outputDir + "/redP4Diff.html");
      writer.writeHeader(true);
      greenWriter.writeHeader(true);
      redWriter.writeHeader(true);

      for (String commit: commits){
        log("Currently processing changelist: " + commit);
        log("\tCalling the command \"git diff " + commit + "\"");
        String gitDiffOutput = readGitDiff(commit);
        HashMap<String, FileDiff> fileDiffHashMap = getChangesFromGitDiff(gitDiffOutput);
        log("\tProcessing the coverage file");
        getCoverageForFileDiffMap(fileDiffHashMap, coverageFile);
        log("\tgenerating the html report");
        generateCoverageHTMLReport(commit, fileDiffHashMap);
      }

      //close the writers
      writer.closeHTMLFile();
      redWriter.closeHTMLFile();
      greenWriter.closeHTMLFile();
      //copy the css file over
      try {
        Files.copy(P4Diff.class.getResourceAsStream("/styles.css"),
          new File(outputDir + "/styles.css").toPath(), REPLACE_EXISTING);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    else{
      System.err.println(help);
      System.exit(1);
    }
  }

  //generates the html report table part for a specific changelist
  public static void generateCoverageHTMLReport(String changeListNum, HashMap<String, FileDiff> fileDiffMap){

    //loop over each file
    for(Map.Entry<String, FileDiff> fileDiffEntry: fileDiffMap.entrySet()){
      FileDiff currFileDiff = fileDiffEntry.getValue();
      //if the file has not been covered, don't loop over the lines changed
      if(!currFileDiff.isCovered()){
        writer.writeTableRow("", changeListNum, fileDiffEntry.getKey(), "N/A", "File not covered!");
        continue;
      }
      boolean isComment = false;
      //loop over each line changed, and print it out
      for(Range range: currFileDiff.getRanges()){

        int startLineNum = range.getStart();
        int endLineNum = range.getEnd();
        for(int currLineNum = startLineNum; currLineNum <= endLineNum; currLineNum++){
          String currLine = currFileDiff.getLine(currLineNum).trim();
          //filter out comments and such
          if(currLine.startsWith("/*") || currLine.startsWith("//") || currLine.startsWith("*")){
            isComment = true;
          }
          else if(currLine.contains("*/")){
            isComment = false;
          }
          if(isComment || currLine.trim().isEmpty()){
            if(currLine.startsWith("//")){
              isComment = false;
            }
            continue;
          }


          //print out the green rows
          if(currFileDiff.getLinesTested().contains(currLineNum)){
            writer.writeTableRow("green",
              changeListNum, fileDiffEntry.getKey(), Integer.toString(currLineNum),
              currFileDiff.getLine(currLineNum));
            greenWriter.writeTableRow("green",
              changeListNum, fileDiffEntry.getKey(), Integer.toString(currLineNum),
              currFileDiff.getLine(currLineNum));
          }
          //print out the red rows
          else{
            writer.writeTableRow("red",
              changeListNum, fileDiffEntry.getKey(), Integer.toString(currLineNum),
              currFileDiff.getLine(currLineNum));
            redWriter.writeTableRow("red",
              changeListNum, fileDiffEntry.getKey(), Integer.toString(currLineNum),
              currFileDiff.getLine(currLineNum));

          }
        }
      }
    }
  }

  //goes to each filediff and adds which lines are covered
  private static void getCoverageForFileDiffMap
    (HashMap<String, FileDiff> fileDiffHashMap, String testFileName){

    try{
      //ignore the DTD
      //step is only necessary if program is not connected to internet
      SAXParserFactory saxfac = SAXParserFactory.newInstance();
      saxfac.setValidating(false);
      saxfac.setFeature("http://xml.org/sax/features/validation", false);
      saxfac.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
      saxfac.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
      saxfac.setFeature("http://xml.org/sax/features/external-general-entities", false);
      saxfac.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
      //create a new SAX parser
      SAXParser saxParser = saxfac.newSAXParser();
      //our handler keeps track of lines covered per method
      DefaultHandler handler = new DefaultHandler() {

        String fileName;
        boolean fileChangedInDiff = false;
        HashSet<Integer> linesTested = new HashSet<>();

        public void startElement(String uri, String localName,String qName,
                                 Attributes attributes) throws SAXException {

          if(qName.equalsIgnoreCase("class")) {
            fileName = attributes.getValue("filename");
            //if this file is in the changelist
            if(fileDiffHashMap.containsKey(fileName)){
              fileChangedInDiff = true;
              linesTested = new HashSet<>();
            }
          }
          //if the file is in the changelist, and the line has been hit in prod
          //add the line to list
          else if(qName.equalsIgnoreCase("line") && fileChangedInDiff) {
            int lineNum = Integer.parseInt(attributes.getValue("number"));
            int lineHits = Integer.parseInt(attributes.getValue("hits"));
            //if the line is not being tested
            if(lineHits != 0) {
              linesTested.add(lineNum);
            }
          }
        }
        public void endElement(String uri, String localName,
                               String qName) throws SAXException {
          //if the file is ending
          if(qName.equalsIgnoreCase("class")){
            if(fileChangedInDiff) {
              fileDiffHashMap.get(fileName).addLinesTested(linesTested);
              fileDiffHashMap.get(fileName).setCovered(true);
            }
            fileChangedInDiff = false;
          }
        }
      };
      saxParser.parse(testFileName, handler);
    }

    catch(Exception e){
      System.err.println("Error parsing the xml file.");
      System.exit(1);
    }
  }

  //get each range of lines changed in each file
  private static HashMap<String, FileDiff> getChangesFromP4Diff(String diffOutput){
    HashMap<String, FileDiff> fileDiffMap = new HashMap<>();
    String methodSignature = "";
    int currLineNum = -1;
    //go over each line of the change list diff
    for(String line: diffOutput.split("\n")){
      if(line.contains("====")){
        methodSignature = getSignatureFromP4(line);
        log("\tProcessing file: " + methodSignature);
      }
      //if the key was properly generated, e.g. if the file was actually
      // a java file and there was no error
      if(!methodSignature.isEmpty()){
        //if this line describes the range of lines changed
        if(!line.isEmpty() && Character.isDigit(line.charAt(0))){
          //we only care about lines added, not deleted
          if(!line.contains("d")){
            //create the range
            String[] rangeStr = line.split("[ac]")[1].split(",");
            Range currRange;
            if(rangeStr.length == 1){
              currRange = new Range(Integer.parseInt(rangeStr[0]));
            }
            else {
              currRange = new Range(Integer.parseInt(rangeStr[0]),
                Integer.parseInt(rangeStr[1]));
            }
            currLineNum = Integer.parseInt(rangeStr[0]);
            //add range to the fileDiff in hashmap if the key exists
            if(fileDiffMap.containsKey(methodSignature)){
              fileDiffMap.get(methodSignature).addRange(currRange);
            }
            //if the key doesn't exist, initialize it
            else{
              FileDiff fileDiff = new FileDiff();
              fileDiff.addRange(currRange);
              fileDiffMap.put(methodSignature, fileDiff);
            }
          }
        }
        else if(!line.isEmpty() && line.charAt(0) == '>'){
          //process the line and add it to the map
          fileDiffMap.get(methodSignature).addLine(currLineNum++, line.substring(1, line.length()).trim());
        }
      }
    }
    return fileDiffMap;
  }

  //need a method to read all the output for git diff because git commands are interactive
  private static String readGitDiff(String commitID){
    StringBuilder output = new StringBuilder();
    try {
      //call the command
      ProcessBuilder pb = new ProcessBuilder(new String[]{"git", "diff", commitID});
      //for speeding up grep
      Process p = pb.start();
      //p.waitFor();
      //BufferedReader errorReader = new BufferedReader(new InputStreamReader(p.getErrorStream()));
      BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
      String line;
      String errorLine;
      //read the input while there is still more to read
      boolean changed = true;
      while(changed) {
        changed = false;
        while ((line = reader.readLine()) != null) {
          changed = true;
          output.append(line);
          output.append("\n");
          /*
          while ((errorLine = errorReader.readLine()) != null) {
            System.err.println(errorLine);
          }
          */
        }
        System.out.println(" ");
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return output.toString();
  }

  private static HashMap<String, FileDiff> getChangesFromGitDiff(String diffOutput){
    HashMap<String, FileDiff> fileDiffMap = new HashMap<>();
    String methodSignature = "";
    int currLine = -1;
    for(String line: diffOutput.split("\n")){
      if(line.startsWith("+++")){
        String fileName = line.substring(line.indexOf("/") + 1);
        if(!new File(fileName).equals(new File("dev/null"))){
          methodSignature = fileName;
        }
      }
      else if(line.startsWith("@@")){
        //will look like @@ -0,0 +1,40 @@, or may have a line from the file after the @@
        String unifiedDiffRange = line.split(" ")[2];
        //get rid of the plus
        unifiedDiffRange = unifiedDiffRange.substring(1, unifiedDiffRange.length());
        int rangeStart = Integer.parseInt(unifiedDiffRange.split(",")[0]);
        int rangeOffset = Integer.parseInt(unifiedDiffRange.split(",")[1]);
        currLine = rangeStart;
        Range currRange = new Range(rangeStart, rangeStart + rangeOffset - 1);

        //if there is stuff after the last @@, that's a line in the pushed file
        //which has not been changed
        if(line.split(" ").length > 4){
          currLine++;
        }

        //now add the file diff to the map
        if(fileDiffMap.containsKey(methodSignature)){
          fileDiffMap.get(methodSignature).addRange(currRange);
        }
        else{
          FileDiff fileDiff = new FileDiff();
          fileDiff.addRange(currRange);
          fileDiffMap.put(methodSignature, fileDiff);
        }
      }
      //it's a line that is in the pushed file
      else if((line.startsWith(" ") || line.startsWith("+")) && !methodSignature.equals("")){
        fileDiffMap.get(methodSignature).addLine(currLine++, line.substring(1, line.length()));
      }
    }
    return fileDiffMap;
  }


  //generate the package/filename key from a line
  private static String getSignatureFromP4(String line){
    String filePath;
    String fileName;
    //describes the file that was changed
    //gets rid of the ends so only the path is remaining
    filePath = line.replace("====", "").trim().split(" ")[0];
    //get the file name
    Path p = Paths.get(filePath);
    fileName = p.getFileName().toString();
    //get rid of the change #
    fileName = fileName.split("#")[0];
    //grep the file for which package it is in
    String grepForPackage = getShellCmdOutput(new String[]{"p4", "grep", "-n", "-e", "package", filePath});
    String[] grepLines = grepForPackage.split("\n");

    //loop over each line to find the correct package line
    for(String grepLine: grepLines){
      //get rid of file info from grep
      String lineInFile = grepLine.substring(grepLine.lastIndexOf(':') + 1);
      //match to package regex
      if(lineInFile.matches("package .*;")){
        String packageName = grepLine.split(" ")[1];
        packageName = packageName.substring(0, packageName.length() - 1);
        packageName = packageName.replaceAll("\\.", "/");
        return packageName + "/" + fileName;
      }
    }

    log("\tError parsing for package name in file: " + fileName);
    log("\tIf this isn't a .java file, an error is expected");
    return "";
  }

  //execute a command in shell
  private static String getShellCmdOutput(String[] shellCmd) {
    StringBuilder output = new StringBuilder();
    try {
      //call the command
      ProcessBuilder pb = new ProcessBuilder(shellCmd);
      //for speeding up grep
      pb.environment().put("LANG", "C");
      Process p = pb.start();
      //for some reason, p.waitFor() causes errors on windows. the errorReader might fix this issue,
      //but has not been tested
      //p.waitFor();
      //BufferedReader errorReader = new BufferedReader(new InputStreamReader(p.getErrorStream()));
      BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
      String line;
      String errorLine;
      //read the input while there is still more to read
      while ((line = reader.readLine()) != null) {
        output.append(line);
        output.append("\n");
        /*
        while((errorLine = errorReader.readLine()) != null){
          System.err.println(errorLine);
        }
        */
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return output.toString();
  }

  //prints to stdout if logging is true
  private static void log(String str){
    if(logging){
      System.out.println(str);
    }
  }
}
