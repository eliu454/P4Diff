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

  public static void main(String[] args) {
    int numOfCoverageArgs = 3;
    int numOfDiffArgs = 2;
    String coverageFile;
    String outputDir;
    String[] changeLists;
    String help = "Usage: java -jar P4Diff.jar <coverage.xml> <output_dir> <changelist1,changelist2,...>\n" +
      "If you just want the diff report without coverage, java -jar P4Diff.jar <output_directory> <changelist1," +
      "changelist2,...>";
    if(args.length != numOfCoverageArgs && args.length != numOfDiffArgs){
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

      coverageFile = args[coverageFileIndex];
      outputDir =  args[outputDirIndex];
      changeLists = args[changeListIndex].split(",");
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
        log("\tProcessing the command \"p4 describe " + changeListNum + "\"");
        //parse the changes
        String changeListDiff = getShellCmdOutput(new String[]{"p4", "describe", changeListNum});
        HashMap<String, FileDiff> fileDiffMap = getChangesFromDiff(changeListDiff);
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
    }
    //generate only a html report on the diff
    else{
      int outputDirIndex = 0;
      int changeListIndex = 1;
      outputDir =  args[outputDirIndex];
      changeLists = args[changeListIndex].split(",");

      writer = new P4HTMLWriter(outputDir + "/p4Diff.html");
      writer.writeHeader(false);
      //loop over each changelist
      for (String changeListNum : changeLists){

        log("Currently processing changelist: " + changeListNum);
        log("\tProcessing the command \"p4 describe " + changeListNum + "\"");

        //parse the changes
        String changeListDiff = getShellCmdOutput(new String[]{"p4", "describe", changeListNum});
        //generate the map and the report
        log("\tProcessing the coverage file");
        HashMap<String, FileDiff> fileDiffMap = getChangesFromDiff(changeListDiff);
        log("\tGenerating the html report");
        generateDiffHTMLReport(changeListNum, fileDiffMap);
      }
      writer.closeHTMLFile();
    }

    //copy the css file over
    try {
      Files.copy(P4Diff.class.getResourceAsStream("/styles.css"),
        new File(outputDir + "/styles.css").toPath(), REPLACE_EXISTING);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  //generates the html report table part for a specific changelist
  public static void generateCoverageHTMLReport(String changeListNum, HashMap<String, FileDiff> fileDiffMap){
    for(Map.Entry<String, FileDiff> fileDiffEntry: fileDiffMap.entrySet()){
      writer.writeTableRow(changeListNum, fileDiffEntry.getKey(), fileDiffEntry.getValue().toHTMLFormat(true, true));
      redWriter.writeTableRow(changeListNum, fileDiffEntry.getKey(), fileDiffEntry.getValue().toHTMLFormat(false, true));
      greenWriter.writeTableRow(changeListNum, fileDiffEntry.getKey(), fileDiffEntry.getValue().toHTMLFormat(true, false));
    }
  }

  public static void generateDiffHTMLReport(String changeListNum, HashMap<String, FileDiff> fileDiffMap){
    for(Map.Entry<String, FileDiff> fileDiff: fileDiffMap.entrySet()){
      writer.writeTableRow(changeListNum, fileDiff.getKey(), fileDiff.getValue().toString());
    }
  }

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
        HashSet<Integer> linesNotTested = new HashSet<>();
        HashSet<Integer> linesTested = new HashSet<>();

        public void startElement(String uri, String localName,String qName,
                                 Attributes attributes) throws SAXException {

          if(qName.equalsIgnoreCase("class")) {
            fileName = attributes.getValue("filename");
            //if this file is in the changelist
            if(fileDiffHashMap.containsKey(fileName)){
              fileChangedInDiff = true;
              linesNotTested = new HashSet<>();
              linesTested = new HashSet<>();
            }
          }
          //if the file is in the changelist, and the line has been hit in prod
          //add the line to list
          else if(qName.equalsIgnoreCase("line") && fileChangedInDiff) {
            int lineNum = Integer.parseInt(attributes.getValue("number"));
            int lineHits = Integer.parseInt(attributes.getValue("hits"));
            //if the line is not being tested
            if(lineHits == 0) {
              linesNotTested.add(lineNum);
            }
            else{
              linesTested.add(lineNum);
            }
          }
        }
        public void endElement(String uri, String localName,
                               String qName) throws SAXException {
          //if the file is ending
          if(qName.equalsIgnoreCase("class")){
            if(fileChangedInDiff) {
              fileDiffHashMap.get(fileName).addLinesNotTested(linesNotTested);
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
    }
  }

  //get each range of lines changed in each file
  private static HashMap<String, FileDiff> getChangesFromDiff(String changeListDiff){
    HashMap<String, FileDiff> fileDiffMap = new HashMap<>();
    String methodSignature = "";
    //go over each line of the change list diff
    for(String line: changeListDiff.split("\n")){
      if(line.contains("====")){
        methodSignature = getKeyFromDiffFileLine(line);
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
      }
    }
    return fileDiffMap;
  }

  //generate the package/filename key from a line
  private static String getKeyFromDiffFileLine(String line){
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
    String grepForPackage = getShellCmdOutput(new String[] {"p4", "grep", "-n", "-e", "package", filePath});
    //package line is after the last colon in path
    String packageLine = grepForPackage.substring(grepForPackage.lastIndexOf(':') + 1);
    //package name is after the first space, hopefully
    String[] packageLineArr = packageLine.split(" ");
    if(packageLineArr.length == 2){
      //format the package name to return
      String packageName = packageLineArr[1];
      packageName = packageName.substring(0, packageName.length() - 2);
      packageName = packageName.replaceAll("\\.", "/");
      return packageName + "/" + fileName;
    }
    else{
      System.out.println("Error finding package in file: " + fileName);
      return "";

    }
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
      p.waitFor();
      BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
      String line;
      //read the input while there is still more to read
      while ((line = reader.readLine()) != null) {
        output.append(line);
        output.append("\n");
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
