package p4Diff;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

//the changes that happened in a file in a given changelist
public class FileDiff {
  private List<Range> ranges;
  private HashMap<Integer, String> linesMap;
  private HashSet<Integer> linesTested;
  private static String red = "#94505a";
  private static String green = "#498849";
  private boolean covered;
  public FileDiff(){
    ranges = new ArrayList<>();
    linesTested = new HashSet<>();
    linesMap = new HashMap<>();
  }
  public void addRange(Range range){
    ranges.add(range);
  }

  public List<Range> getRanges(){
    return ranges;
  }

  public HashSet<Integer> getLinesTested(){
    return linesTested;
  }

  //lines that have hits in production
  public void addLinesTested(HashSet<Integer> linesTested){
    this.linesTested.addAll(linesTested);
  }

  public String getLine(int num){
    return linesMap.get(num);
  }

  public void addLine(int num, String line){
    linesMap.put(num, line);
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
  public void setCovered(boolean isCovered) {
    covered = isCovered;
  }
  public boolean isCovered(){
    return covered;
  }

}
