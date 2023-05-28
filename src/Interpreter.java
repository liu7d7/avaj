import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

public class Interpreter {
  public static final HashMap<Parser.Node.Type, BiFunction<SymbolTable, Parser.Node, Value>> CALLS = makeCalls();

  /**
   * @return The HashMap container that maps the Nodes' types to the Interpreter functions that execute them.
   */
  private static HashMap<Parser.Node.Type, BiFunction<SymbolTable, Parser.Node, Value>> makeCalls() {
    HashMap<Parser.Node.Type, BiFunction<SymbolTable, Parser.Node, Value>> calls = new HashMap<>();
    calls.put(Parser.Node.Type.Integer, Interpreter::integer);
    calls.put(Parser.Node.Type.String, Interpreter::string);
    calls.put(Parser.Node.Type.FunctionDeclaration, Interpreter::functionDeclaration);
    calls.put(Parser.Node.Type.Block, Interpreter::block);
    calls.put(Parser.Node.Type.VariableDeclaration, Interpreter::variableDeclaration);
    calls.put(Parser.Node.Type.VariableAccess, Interpreter::variableAccess);
    calls.put(Parser.Node.Type.FunctionCall, Interpreter::functionCall);
    calls.put(Parser.Node.Type.Maths, Interpreter::maths);
    calls.put(Parser.Node.Type.Negation, Interpreter::negation);
    calls.put(Parser.Node.Type.If, Interpreter::ifStatement);
    calls.put(Parser.Node.Type.For, Interpreter::forLoop);
    return calls;
  }

  /**
   * Precondition: node.type == Parser.Node.Type.Integer
   * @param context The interpreter's current Symbol Table.
   * @param node The node to execute.
   * @return The value corresponding to the Parser.IntegerNode passed in.
   */
  private static Value integer(SymbolTable context, Parser.Node node) {
    return new IntegerValue(((Parser.IntegerNode)node).value);
  }

  /**
   * Precondition: node.type == Parser.Node.Type.String
   * @param context The interpreter's current Symbol Table.
   * @param node The node to execute.
   * @return The value corresponding to the Parser.StringNode passed in.
   */
  private static Value string(SymbolTable context, Parser.Node node) {
    return new StringValue(((Parser.StringNode)node).value);
  }

  /**
   * Precondition: node.type == Parser.Node.Type.FunctionDeclaration
   * @param context The interpreter's current Symbol Table.
   * @param node The node to execute.
   * @return Void; This function has the side effect of adding the corresponding function of the
   *         Parser.FunctionDeclarationNode passed in to the Symbol Table.
   */
  public static Value functionDeclaration(SymbolTable context, Parser.Node node) {
    Parser.FunctionDeclarationNode functionDeclarationNode = (Parser.FunctionDeclarationNode) node;
    FunctionValue func = new FunctionValue(functionDeclarationNode);
    context.add(func.id, func);
    return VoidValue.VOID;
  }

  /**
   * Precondition: node.type == Parser.Node.Type.VariableDeclaration
   * @param context The interpreter's current Symbol Table.
   * @param node The node to execute.
   * @return Void; This function has the side effect of adding the variable that the
   *         Parser.VariableDeclarationNode passed in corresponds to to the Symbol Table.
   */
  public static Value variableDeclaration(SymbolTable context, Parser.Node node) {
    Parser.VariableDeclarationNode variableDeclarationNode = (Parser.VariableDeclarationNode) node;
    context.add(variableDeclarationNode.id, interpret(context, variableDeclarationNode.value));
    return VoidValue.VOID;
  }

  /**
   * Precondition: node.type == Parser.Node.Type.VariableAccess
   * @param context The interpreter's current Symbol Table.
   * @param node The node to execute.
   * @return The value corresponding to the Parser.VariableAccessNode passed in.
   */
  public static Value variableAccess(SymbolTable context, Parser.Node node) {
    Parser.VariableAccessNode variableAccessNode = (Parser.VariableAccessNode) node;
    return context.get(variableAccessNode.id);
  }

  /**
   * Precondition: node.type == Parser.Node.Type.FunctionCall
   * @param context The interpreter's current Symbol Table.
   * @param node The node to execute.
   * @return The value returned by the function call that the Parser.FunctionCallNode
   *         passed in corresponds to.
   */
  public static Value functionCall(SymbolTable context, Parser.Node node) {
    Parser.FunctionCallNode functionCallNode = (Parser.FunctionCallNode) node;
    List<Value> args = new ArrayList<>();
    for (Parser.Node it : ((Parser.FunctionCallNode) node).args) {
      args.add(interpret(context, it));
    }
    return context.get(FunctionValue.mangleNameCallerSide(functionCallNode.toCall, args)).call(context, args);
  }

  /**
   * Precondition: node.type == Parser.Node.Type.Maths
   * @param context The interpreter's current Symbol Table.
   * @param node The node to execute.
   * @return The value corresponding to the operation described by the Parser.MathNode passed in.
   * @throws IllegalStateException, if the operation is not one supported by this interpreter.
   */
  public static Value maths(SymbolTable context, Parser.Node node) {
    Parser.MathNode mathNode = (Parser.MathNode) node;
    Value left = interpret(context, mathNode.left);
    Value right = interpret(context, mathNode.right);
    if (mathNode.operation == Lexer.Token.Type.Add) {
      return left.add(right);
    } else if (mathNode.operation == Lexer.Token.Type.Sub) {
      return left.sub(right);
    } else if (mathNode.operation == Lexer.Token.Type.Mul) {
      return left.mul(right);
    } else if (mathNode.operation == Lexer.Token.Type.Div) {
      return left.div(right);
    } else if (mathNode.operation == Lexer.Token.Type.Mod) {
      return left.mod(right);
    } else if (mathNode.operation == Lexer.Token.Type.Pow) {
      return left.pow(right);
    } else if (mathNode.operation == Lexer.Token.Type.EqualsEquals) {
      return left.equalsEquals(right);
    } else if (mathNode.operation == Lexer.Token.Type.NotEquals) {
      return left.equalsEquals(right).not();
    } else if (mathNode.operation == Lexer.Token.Type.LessThan) {
      return left.lessThan(right);
    } else if (mathNode.operation == Lexer.Token.Type.LessThanEquals) {
      return left.lessThanEqualTo(right);
    } else if (mathNode.operation == Lexer.Token.Type.GreaterThan) {
      return left.greaterThan(right);
    } else if (mathNode.operation == Lexer.Token.Type.GreaterThanEquals) {
      return left.greaterThanEqualTo(right);
    } else if (mathNode.operation == Lexer.Token.Type.OrOr) {
      return left.orOr(right);
    } else if (mathNode.operation == Lexer.Token.Type.AndAnd) {
      return left.andAnd(right);
    } else if (mathNode.operation == Lexer.Token.Type.Assign) {
      return left.assign(right);
    }
    throw new IllegalStateException("Interpreter::maths - Invalid operation " + mathNode.operation);
  }

  /**
   * Precondition: node.type == Parser.Node.Type.Negation
   * @param context The interpreter's current Symbol Table.
   * @param node The node to execute.
   * @return The value corresponding to the operation described by the Parser.NegationNode passed in.
   * @throws IllegalStateException, if the operation is not one supported by this interpreter.
   */
  public static Value negation(SymbolTable context, Parser.Node node) {
    Parser.NegationNode negationNode = (Parser.NegationNode) node;
    Value acting = integer(context, negationNode.acting);
    if (negationNode.operation == Lexer.Token.Type.Not) {
      return acting.not();
    } else if (negationNode.operation == Lexer.Token.Type.Sub) {
      return acting.negate();
    } else if (negationNode.operation == Lexer.Token.Type.Add) {
      return acting;
    } else {
      throw new IllegalStateException("Interpreter::negation - Invalid operation " + negationNode.operation);
    }
  }

  /**
   * Precondition: node.type == Parser.Node.Type.Block
   * @param context The interpreter's current Symbol Table.
   * @param node The node to execute.
   * @return The value corresponding to the output of the last statement of the Parser.BlockNode passed in.
   */
  public static Value block(SymbolTable context, Parser.Node node) {
    Parser.BlockNode blockNode = (Parser.BlockNode) node;
    SymbolTable symb = new SymbolTable(context);

    for (int i = 0; i < blockNode.exprs.size() - 1; i++) {
      interpret(symb, blockNode.exprs.get(i));
    }

    return interpret(symb, blockNode.exprs.get(blockNode.exprs.size() - 1));
  }

  /**
   * Precondition: node.type == Parser.Node.Type.If
   * @param context The interpreter's current Symbol Table.
   * @param node The node to execute.
   * @return The value returned by the body of the branch of the executed if statement corresponding to the
   *         Parser.IfNode passed in.
   */
  public static Value ifStatement(SymbolTable context, Parser.Node node) {
    Parser.IfNode ifNode = (Parser.IfNode) node;
    for (Pair<Parser.Node, Parser.Node> it : ifNode.conditions) {
      if (interpret(context, it.a).truthy()) {
        return interpret(context, it.b);
      }
    }

    if (ifNode.otherwise != null) {
      return interpret(context, ifNode.otherwise);
    }

    return VoidValue.VOID;
  }

  /**
   * Precondition: node.type == Parser.Node.Type.For
   * @param context The interpreter's current Symbol Table.
   * @param node The node to execute.
   * @return Void; This function has the side effect of executing the for loop that corresponds to the
   *         ForNode passed in.
   */
  public static Value forLoop(SymbolTable context, Parser.Node node) {
    Parser.ForNode forNode = (Parser.ForNode) node;
    SymbolTable symb = new SymbolTable(context);
    while (interpret(symb, forNode.condition).truthy()) {
      interpret(symb, forNode.body);
    }
    return VoidValue.VOID;
  }

  /**
   * Precondition: node.type == Parser.Node.Type.Integer
   * @param context The interpreter's current Symbol Table.
   * @param node The node to execute.
   * @return The value corresponding to the Parser.IntegerNode passed in.
   */
  public static Value interpret(SymbolTable context, Parser.Node node) {
    return CALLS.get(node.type).apply(context, node);
  }

  /**
   * Represents the variables accessible by the program at any given time.
   */
  public static class SymbolTable {
    public final HashMap<String, Value> values = new HashMap<>();
    public final SymbolTable parent;

    public SymbolTable() {
      parent = null;
    }

    public SymbolTable(SymbolTable parent) {
      this.parent = parent;
    }

    /**
     * Precondition: The variable whose identifier is id is in this Symbol Table or one of its parents.
     * @param id The identifier of the variable that will be returned by this call.
     * @return The value that is held in the variable named id
     * @throws IllegalStateException if the
     */
    public Value get(String id) {
      if (values.containsKey(id)) {
        return values.get(id);
      }
      if (parent == null) {
        throw new IllegalStateException("SymbolTable::get - Was not able to find a value for id \"" + id + "\"");
      }
      Value value = parent.get(id);
      values.put(id, value);
      return value;
    }

    /**
     * Add a new variable to the Symbol Table.
     * @param id The name of the variable to add
     * @param val The value initially held by this variable.
     */
    public void add(String id, Value val) {
      this.values.put(id, val);
    }

    /**
     * A convenience function to construct and add a BuiltinFunctionValue given the arguments.
     * @param id The un-mangled name of the function. Remember, function names are mangled in order to allow overloading.
     * @param args The arguments this function takes.
     * @param body The Java function that will be executed when this builtin function is executed in the language.
     */
    public void addBuiltinFunc(String id, List<Pair<String, Value.Type>> args, Function<SymbolTable, Value> body) {
      BuiltinFunctionValue func = new BuiltinFunctionValue(id, args, body);
      add(func.id, func);
    }
  }

  public static class Value {
    public final Type type;

    protected Value(Type type) {
      this.type = type;
    }

    public Value add(Value other) {
      throw new IllegalStateException(this.getClass().getSimpleName() + "::add - Tried to do " + this.type + " + " + other.type);
    }

    public Value mul(Value other) {
      throw new IllegalStateException(this.getClass().getSimpleName() + "::mul - Tried to do " + this.type + " * " + other.type);
    }

    public Value sub(Value other) {
      throw new IllegalStateException(this.getClass().getSimpleName() + "::sub - Tried to do " + this.type + " - " + other.type);
    }

    public Value div(Value other) {
      throw new IllegalStateException(this.getClass().getSimpleName() + "::div - Tried to do " + this.type + " / " + other.type);
    }

    public Value mod(Value other) {
      throw new IllegalStateException(this.getClass().getSimpleName() + "::mod - Tried to do " + this.type + " % " + other.type);
    }

    public Value pow(Value other) {
      throw new IllegalStateException(this.getClass().getSimpleName() + "::pow - Tried to do " + this.type + " ^ " + other.type);
    }

    public Value equalsEquals(Value other) {
      throw new IllegalStateException(this.getClass().getSimpleName() + "::equalsEquals - Tried to do " + this.type + " == " + other.type);
    }

    public Value lessThan(Value other) {
      throw new IllegalStateException(this.getClass().getSimpleName() + "::lessThan - Tried to do " + this.type + " < " + other.type);
    }

    public Value lessThanEqualTo(Value other) {
      throw new IllegalStateException(this.getClass().getSimpleName() + "::lessThanEqualTo - Tried to do " + this.type + " <= " + other.type);
    }

    public Value greaterThan(Value other) {
      throw new IllegalStateException(this.getClass().getSimpleName() + "::greaterThan - Tried to do " + this.type + " > " + other.type);
    }

    public Value greaterThanEqualTo(Value other) {
      throw new IllegalStateException(this.getClass().getSimpleName() + "::greaterThanEqualTo - Tried to do " + this.type + " >= " + other.type);
    }

    public Value assign(Value other) {
      throw new IllegalStateException(this.getClass().getSimpleName() + "::assign - Tried to do " + this.type + " <- " + other.type);
    }

    public boolean truthy() {
      throw new IllegalStateException(this.getClass().getSimpleName() + "::truthy - Tried to determine the truthiness of " + this.type);
    }

    public Value call(SymbolTable context, List<Value> args) {
      throw new IllegalStateException(this.getClass().getSimpleName() + "::call - Tried to call on " + this.type);
    }

    public Value orOr(Value other) {
      return new IntegerValue(this.truthy() || other.truthy() ? 1 : 0);
    }

    public Value andAnd(Value other) {
      return new IntegerValue(this.truthy() && other.truthy() ? 1 : 0);
    }

    public Value not() {
      return new IntegerValue(this.truthy() ? 0 : 1);
    }

    public Value negate() {
      throw new IllegalStateException(this.getClass().getSimpleName() + "::negate - Tried to negate a " + this.type);
    }

    public IntegerValue toIntValue() {
      throw new IllegalStateException(this.getClass().getSimpleName() + "::toIntValue - Tried to convert a " + this.type + " to int");
    }

    public StringValue toStringValue() {
      throw new IllegalStateException(this.getClass().getSimpleName() + "::toStringValue - Tried to convert a " + this.type + " to string");
    }

    public enum Type {
      Integer("<int>"), String("<string>"), Function("<fun>"), Void("<void>");

      private final String name;

      Type(String name) {
        this.name = name;
      }

      @Override
      public java.lang.String toString() {
        return name;
      }

      public static Type fromString(String s) {
        if ("int".equals(s)) {
          return Integer;
        } else if ("void".equals(s)) {
          return Void;
        } else if ("string".equals(s)) {
          return String;
        } else if ("fun".equals(s)) {
          return Function;
        }
        throw new IllegalArgumentException("Value::Type::fromString - Expected \"int\", \"void\", \"string\", or \"fun\", got " + s);
      }
    }
  }

  public static class VoidValue extends Value {
    public static final VoidValue VOID = new VoidValue();

    protected VoidValue() {
      super(Type.Void);
    }

    @Override
    public String toString() {
      return "<void>";
    }
  }

  public static class IntegerValue extends Value {
    public int value;

    public IntegerValue(int value) {
      super(Type.Integer);
      this.value = value;
    }

    @Override
    public Value add(Value other) {
      if (other.type == Type.Integer) {
        return new IntegerValue(this.value + ((IntegerValue) other).value);
      }
      return super.add(other);
    }

    @Override
    public Value sub(Value other) {
      if (other.type == Type.Integer) {
        return new IntegerValue(this.value - ((IntegerValue) other).value);
      }
      return super.sub(other);
    }

    @Override
    public Value mul(Value other) {
      if (other.type == Type.Integer) {
        return new IntegerValue(this.value * ((IntegerValue) other).value);
      }
      return super.mul(other);
    }

    @Override
    public Value div(Value other) {
      if (other.type == Type.Integer) {
        return new IntegerValue(this.value / ((IntegerValue) other).value);
      }
      return super.div(other);
    }

    @Override
    public Value mod(Value other) {
      if (other.type == Type.Integer) {
        return new IntegerValue(this.value % ((IntegerValue) other).value);
      }
      return super.mod(other);
    }

    @Override
    public Value pow(Value other) {
      if (other.type == Type.Integer) {
        return new IntegerValue((int) Math.pow(this.value, ((IntegerValue) other).value));
      }
      return super.pow(other);
    }

    @Override
    public Value equalsEquals(Value other) {
      if (other.type == Type.Integer) {
        return new IntegerValue(this.value == ((IntegerValue) other).value ? 1 : 0);
      }
      return super.equalsEquals(other);
    }

    @Override
    public Value lessThan(Value other) {
      if (other.type == Type.Integer) {
        return new IntegerValue(this.value < ((IntegerValue) other).value ? 1 : 0);
      }
      return super.lessThan(other);
    }

    @Override
    public Value lessThanEqualTo(Value other) {
      if (other.type == Type.Integer) {
        return new IntegerValue(this.value <= ((IntegerValue) other).value ? 1 : 0);
      }
      return super.lessThanEqualTo(other);
    }

    @Override
    public Value greaterThan(Value other) {
      if (other.type == Type.Integer) {
        return new IntegerValue(this.value > ((IntegerValue) other).value ? 1 : 0);
      }
      return super.greaterThan(other);
    }

    @Override
    public Value greaterThanEqualTo(Value other) {
      if (other.type == Type.Integer) {
        return new IntegerValue(this.value >= ((IntegerValue) other).value ? 1 : 0);
      }
      return super.greaterThanEqualTo(other);
    }

    @Override
    public Value assign(Value other) {
      if (other.type == Type.Integer) {
        this.value = ((IntegerValue)other).value;
        return other;
      }
      return super.assign(other);
    }

    @Override
    public boolean truthy() {
      return this.value != 0;
    }

    @Override
    public Value negate() {
      return new IntegerValue(-this.value);
    }

    @Override
    public String toString() {
      return Integer.toString(value);
    }

    @Override
    public IntegerValue toIntValue() {
      return this;
    }

    @Override
    public StringValue toStringValue() {
      return new StringValue(Integer.toString(value));
    }
  }

  public static class StringValue extends Value {
    public String value;

    public StringValue(String value) {
      super(Type.String);
      this.value = value;
    }

    @Override
    public Value add(Value other) {
      if (other.type == Type.String) {
        return new StringValue(value + ((StringValue) other).value);
      } else {
        return new StringValue(value + ((IntegerValue) other).value);
      }
    }

    @Override
    public Value mul(Value other) {
      if (other.type == Type.Integer) {
        return new StringValue(String.valueOf(value).repeat(Math.max(0, ((IntegerValue) other).value)));
      } else {
        throw new IllegalStateException("StringValue::mul - Tried to do <string> * <string>");
      }
    }

    @Override
    public Value equalsEquals(Value other) {
      if (other.type == Type.String) {
        return new IntegerValue(Objects.equals(this.value, ((StringValue) other).value) ? 1 : 0);
      }
      return super.equalsEquals(other);
    }

    @Override
    public Value lessThan(Value other) {
      if (other.type == Type.String) {
        return new IntegerValue(this.value.compareTo(((StringValue) other).value) < 0 ? 1 : 0);
      }
      return super.lessThan(other);
    }

    @Override
    public Value lessThanEqualTo(Value other) {
      if (other.type == Type.String) {
        return new IntegerValue(this.value.compareTo(((StringValue) other).value) <= 0 ? 1 : 0);
      }
      return super.lessThanEqualTo(other);
    }

    @Override
    public Value greaterThan(Value other) {
      if (other.type == Type.String) {
        return new IntegerValue(this.value.compareTo(((StringValue) other).value) > 0 ? 1 : 0);
      }
      return super.greaterThan(other);
    }

    @Override
    public Value greaterThanEqualTo(Value other) {
      if (other.type == Type.String) {
        return new IntegerValue(this.value.compareTo(((StringValue) other).value) >= 0 ? 1 : 0);
      }
      return super.greaterThanEqualTo(other);
    }

    @Override
    public Value assign(Value other) {
      if (other.type == Type.String) {
        this.value = ((StringValue) other).value;
        return other;
      }
      return super.assign(other);
    }

    @Override
    public boolean truthy() {
      return value.length() != 0;
    }

    @Override
    public String toString() {
      return value;
    }

    @Override
    public StringValue toStringValue() {
      return this;
    }

    @Override
    public IntegerValue toIntValue() {
      return new IntegerValue(Integer.parseInt(value));
    }
  }

  public static class FunctionValue extends Value {

    public final String id;
    public final Parser.Node todo;
    public final List<Pair<String, Value.Type>> args;

    public FunctionValue(Parser.FunctionDeclarationNode funcDeclNode) {
      super(Type.Function);
      this.todo = funcDeclNode.body;
      this.args = new ArrayList<>();
      for (Pair<String, String> arg : funcDeclNode.args) {
        args.add(new Pair<>(arg.a, Type.fromString(arg.b)));
      }
      this.id = mangleNameFunctionSide(funcDeclNode.id, args);
    }

    public static String mangleNameFunctionSide(String id, List<Pair<String, Value.Type>> args) {
      StringBuilder sb = new StringBuilder(id);
      for (Pair<String, Value.Type> arg : args) {
        sb.append(arg.b);
      }
      return sb.toString();
    }

    public static String mangleNameCallerSide(String id, List<Value> args) {
      StringBuilder sb = new StringBuilder(id);
      for (Value arg : args) {
        sb.append(arg.type);
      }
      return sb.toString();
    }

    public static SymbolTable setUpSymbolTable(SymbolTable context, String id, List<Pair<String, Value.Type>> args, List<Value> argsIn) {
      if (args.size() != argsIn.size()) {
        throw new IllegalStateException("FunctionValue::setUpSymbolTable - Tried to call function " + id + ", which takes " + args.size() + " arguments, with " + argsIn.size() + " arguments.");
      }
      SymbolTable symb = new SymbolTable(context);
      for (int i = 0; i < args.size(); i++) {
        Pair<String, Value.Type> arg = args.get(i);
        Value argValue = argsIn.get(i);
        if (argValue.type != arg.b) {
          throw new IllegalStateException("FunctionValue::setUpSymbolTable - Tried to call function " + id + ", whose " + i + "th argument is a " + arg.b + ", with a " + argValue.type);
        }
        symb.add(arg.a, argValue);
      }
      return symb;
    }

    @Override
    public Value call(SymbolTable context, List<Value> argsIn) {
      SymbolTable symb = setUpSymbolTable(context, id, args, argsIn);
      return Interpreter.interpret(symb, todo);
    }

    @Override
    public String toString() {
      return "fun " + id;
    }
  }

  public static class BuiltinFunctionValue extends Value {

    public final String id;
    public final List<Pair<String, Value.Type>> args;
    public final Function<SymbolTable, Value> body;

    public BuiltinFunctionValue(String id, List<Pair<String, Value.Type>> args, Function<SymbolTable, Value> body) {
      super(Type.Function);
      this.body = body;
      this.id = FunctionValue.mangleNameFunctionSide(id, args);
      this.args = args;
    }

    @Override
    public Value call(SymbolTable context, List<Value> argsIn) {
      SymbolTable symb = FunctionValue.setUpSymbolTable(context, id, args, argsIn);
      return body.apply(symb);
    }

    @Override
    public String toString() {
      return "[builtin] fun " + id;
    }
  }
}
