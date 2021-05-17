package com.web.plan.bheft;
/*
https://www.logicbig.com/how-to/code-snippets/jcode-java-cmd-command-line-table.html
Changed so addRow accepts any number of objects and formats them to readable strings.
-------------------------------------------------------------------------------------
Licensed under Creative Commons Attribution-ShareAlike 3.0 Unported
 */


import java.lang.reflect.Array;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CommandLineTable {
    private static final String HORIZONTAL_SEP = "-";
    private String verticalSep;
    private String joinSep;
    private String[] headers;
    private List<String[]> rows = new ArrayList<>();
    private boolean rightAlign;
    private int[] maxWidths;

    public CommandLineTable() {
        setShowVerticalLines(false);
    }

    public void setRightAlign(boolean rightAlign) {
        this.rightAlign = rightAlign;
    }

    public void setShowVerticalLines(boolean showVerticalLines) {
        verticalSep = showVerticalLines ? "|" : "";
        joinSep = showVerticalLines ? "+" : " ";
    }

    public void setHeaders(String... headers) {
        this.headers = headers;
    }

    public void addRow(Object... cellsS) {
        ArrayList<String> row = new ArrayList<>();
        DecimalFormat df = new DecimalFormat();
        df.setMaximumFractionDigits(2);

        for (Object item: cellsS) {
            if (item instanceof Double) {
                row.add(df.format(item));
            }
            if (item instanceof Integer) {
                row.add(df.format(item));
            }
            if (item instanceof res) {
                row.add(df.format(((res) item).getID()));
            }
            if (item instanceof ArrayList) {
                String tempResString = "";
                if (!((ArrayList<?>) item).isEmpty()) {
                    for (res resource : (ArrayList<res>) item) {
                        tempResString += df.format(resource.getID()) + ",";
                    }
                    tempResString = tempResString.substring(0, tempResString.length() -1);
                }

                row.add(tempResString);
            }
            if (item instanceof String) {
                row.add((String)item);
            }
        }
        rows.add(row.toArray(new String[0]));

        maxWidths = headers != null ?
                Arrays.stream(headers).mapToInt(String::length).toArray() : null;

        for (String[] cells : rows) {
            if (maxWidths == null) {
                maxWidths = new int[cells.length];
            }
            if (cells.length != maxWidths.length) {
                throw new IllegalArgumentException("Number of row-cells and headers should be consistent");
            }
            for (int i = 0; i < cells.length; i++) {
                maxWidths[i] = Math.max(maxWidths[i], cells[i].length());
            }
        }

    }

    private void printLine(int[] columnWidths) {
        for (int i = 0; i < columnWidths.length; i++) {
            String line = String.join("", Collections.nCopies(columnWidths[i] +
                    verticalSep.length() + 1, HORIZONTAL_SEP));
            System.out.print(joinSep + line + (i == columnWidths.length - 1 ? joinSep : ""));
        }
        System.out.println();
    }

    private void printRow(String[] cells, int[] maxWidths) {
        for (int i = 0; i < cells.length; i++) {
            String s = cells[i];
            String verStrTemp = i == cells.length - 1 ? verticalSep : "";
            if (rightAlign) {
                System.out.printf("%s %" + maxWidths[i] + "s %s", verticalSep, s, verStrTemp);
            } else {
                System.out.printf("%s %-" + maxWidths[i] + "s %s", verticalSep, s, verStrTemp);
            }
        }
        System.out.println();
    }

    public void printTable(){
        if (headers != null) {
            printLine(maxWidths);
            printRow(headers, maxWidths);
            printLine(maxWidths);
        }
        for (String[] cells : rows) {
            printRow(cells, maxWidths);
        }
        if (headers != null) {
            printLine(maxWidths);
        }
    }
}
