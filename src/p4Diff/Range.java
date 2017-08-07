package p4Diff;

//range of integers, inclusive
public class Range{
  private int start;
  private int end;
  private boolean initialized = true;
  public Range(){
    initialized = false;
  }
  public Range(int start){
    this.start = start;
    this.end = start;
  }
  public Range(int start, int end){
    this.start = start;
    this.end = end;
  }
  public boolean contains(int num){
    return start <= num && num <= end;
  }
  public String toString(){
    StringBuilder sb = new StringBuilder();
    if(start == end){
      return Integer.toString(start);
    }
    else {
      sb.append(start);
      sb.append("-");
      sb.append(end);
    }
    return sb.toString();
  }
  public int getStart(){
    return start;
  }
  public int getEnd() {
    return end;
  }
  public void setEnd(int end){
    this.end = end;
  }
  public boolean isInitialized(){
    return initialized;
  }
}
