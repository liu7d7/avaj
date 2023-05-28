import java.util.List;
import java.util.Scanner;
import java.util.function.Function;

public class Main {
  public static void main(String[] args) {
    String path = new Scanner(System.in).nextLine();
    try {
      List<Lexer.Token> tokens = Lexer.lex(path);
      Parser.Node ast = Parser.parse(tokens);
      Interpreter.SymbolTable global = new Interpreter.SymbolTable();

      global.addBuiltinFunc("int", List.of(new Pair<>("value", Interpreter.Value.Type.String)), (context) -> context.get("value").toIntValue());
      global.addBuiltinFunc("int", List.of(new Pair<>("value", Interpreter.Value.Type.Integer)), (context) -> context.get("value").toIntValue());

      global.addBuiltinFunc("string", List.of(new Pair<>("value", Interpreter.Value.Type.String)), (context) -> context.get("value").toStringValue());
      global.addBuiltinFunc("string", List.of(new Pair<>("value", Interpreter.Value.Type.Integer)), (context) -> context.get("value").toStringValue());

      Function<Interpreter.SymbolTable, Interpreter.Value> printBody = (context) -> {
        System.out.println(context.get("value").toString());
        return Interpreter.VoidValue.VOID;
      };
      global.addBuiltinFunc("print", List.of(new Pair<>("value", Interpreter.Value.Type.Integer)), printBody);
      global.addBuiltinFunc("print", List.of(new Pair<>("value", Interpreter.Value.Type.String)), printBody);
      global.addBuiltinFunc("print", List.of(new Pair<>("value", Interpreter.Value.Type.Function)), printBody);
      global.addBuiltinFunc("print", List.of(new Pair<>("value", Interpreter.Value.Type.Void)), printBody);

      global.addBuiltinFunc("length", List.of(new Pair<>("value", Interpreter.Value.Type.String)), (context) -> {
        return new Interpreter.IntegerValue(((Interpreter.StringValue) context.get("value")).value.length());
      });

      global.add("true", new Interpreter.IntegerValue(1));
      global.add("false", new Interpreter.IntegerValue(0));

      Interpreter.interpret(global, ast);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}