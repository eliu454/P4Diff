package p4Diff;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Created by i860940 on 7/14/17.
 */

public class P4HTMLWriter{
  private PrintWriter writer;

  public P4HTMLWriter(String fileName){
    try {
      File file = new File(fileName);
      if(!file.exists()) {
        file.getParentFile().mkdirs();
        file.createNewFile();
      }
      writer = new PrintWriter(file);
    }
    catch (IOException e){
      e.printStackTrace();
    }
  }
  //write the html file header
  public void writeHeader(boolean includeButtons){
    writer.println("<!DOCTYPE html>");
    writer.println("<html lang = \"en\">");
    writer.println("<head>");
    writer.println("  <link rel=\"stylesheet\" href=\"styles.css\">");
    writer.println("</head>");


    writer.println("<body>");

    if(includeButtons) {
      writer.println("<div>");
      writer.println("  <a href=\"p4Diff.html\">All</a>");
      writer.println("  <a href=\"redP4Diff.html\">Lines changed but not covered after commit</a>");
      writer.println("  <a href=\"greenP4Diff.html\">Lines changed and covered after commit</a>");
      writer.println("</div>");
    }

    writer.println("<table>");

    writer.println("  <colgroup>");
    writer.println("    <col style=\"width:10%\">");
    writer.println("    <col style=\"width:55%\">");
    writer.println("    <col style=\"width:35%\">");
    writer.println("  </colgroup>");

    writer.println("  <tr>");
    writer.println("    <th class=\"small_header\">Changelist #</th>");
    writer.println("    <th>File</th>");
    writer.println("    <th> Lines added or changed</th>");
    writer.println("  </tr>");
  }


  public void closeHTMLFile(){
    writer.println("</table>");
    writer.println("<script src=\"script.js\"></script>");
    writer.println("</body>");
    writer.close();
  }

  //write a table row
  public void writeTableRow(String changeListNum,
                            String fileName, String diff){
    writer.println("  <tr>");
    writer.println("    <td>" + changeListNum + "</td>");
    writer.println("    <td>" + fileName + "</td>");
    writer.println("    <td>" + diff + "</td>");
    writer.println("  </tr>");
  }

  public void print(String str){
    writer.print(str);
  }
  public void println(String str){
    writer.println(str);
  }
}
