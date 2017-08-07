package p4Diff;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

//the changes that happened in a file in a given changelist
public class FileDiff {
  private List<Range> ranges;
  private HashSet<Integer> linesNotTested;
  private HashSet<Integer> linesTested;
  private static String red = "#94505a";
  private static String green = "#498849";
  private boolean covered;
  public FileDiff(){
    ranges = new ArrayList<>();
    linesNotTested = new HashSet<>();
    linesTested = new HashSet<>();
  }
  public void addRange(Range range){
    ranges.add(range);
  }

  //lines that don't have hits in production
  public void addLinesNotTested(HashSet<Integer> linesNotTested){
    this.linesNotTested.addAll(linesNotTested);
  }

  //lines that have hits in production
  public void addLinesTested(HashSet<Integer> linesTested){
    this.linesTested.addAll(linesTested);
  }

  //returns string of the row with all line intervals in colors, with the option to specify
  //if user wants to print green or red or both
  public String toHTMLFormat(boolean printGreen, boolean printRed){
    if(!covered){
      return "File not covered!";
    }
    int lastRed = -2;
    int lastGreen = -2;
    Range greenRange = new Range();
    Range redRange = new Range();
    StringBuilder sb = new StringBuilder();
    for(Range range: ranges){
      for(int i = range.getStart(); i <= range.getEnd(); i++){
        if(printRed && (linesNotTested.contains(i) || !linesTested.contains(i))){
          if(lastRed == i - 1){
            redRange.setEnd(i);
          }
          else{
            if(redRange.isInitialized()){
              sb.append("<p style=\"color:" + FileDiff.red + "\">" + redRange + ", </p>");
            }
            redRange = new Range(i);
          }
          lastRed = i;
        }
        else if(printGreen && linesTested.contains(i)){
          if(lastGreen == i - 1){
            greenRange.setEnd(i);
          }
          else{
            if(greenRange.isInitialized()){
              sb.append("<p style=\"color:" + FileDiff.green + "\">" + greenRange + ", </p>");
            }
            greenRange = new Range(i);
          }

          lastGreen = i;
        }
      }
    }
    //print the range
    if(printRed && redRange.isInitialized()){
      sb.append("<p style=\"color:" + FileDiff.red + "\">" + redRange + ", </p>");
    }
    if(printGreen && greenRange.isInitialized()){
      sb.append("<p style=\"color:" + FileDiff.green + "\">" + greenRange + ", </p>");
    }
    if(sb.length() != 0) {
      sb.deleteCharAt(sb.lastIndexOf(","));
    }
    return sb.toString();
  }

  public String toString(){
    StringBuilder sb = new StringBuilder();
    for(Range range: ranges){
      sb.append(range);
      sb.append(", ");
    }
    if(sb.length() > 0) {
      sb.deleteCharAt(sb.lastIndexOf(","));
    }
    return sb.toString();
  }
  public void setCovered(boolean isCovered){
    covered = isCovered;
  }

}
