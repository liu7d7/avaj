public class Position {
  public int index;
  public int column;
  public int row;
  public String fileName;

  public Position(int index, int column, int row, String fileName) {
    this.index = index;
    this.column = column;
    this.row = row;
    this.fileName = fileName;
  }

  public Position copy() {
    return new Position(index, column, row, fileName);
  }

  @Override
  public String toString() {
    return "Position{" +
        "row=" + row +
        ", column=" + column +
        ", fileName='" + fileName + '\'' +
        '}';
  }
}
