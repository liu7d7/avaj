import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.function.Function;

public class Parser {

  /**
   * Throw an error based on the given arguments.
   * @param location The function in which this error occurred (most likely malformed program)
   * @param reason   The reason why this error occurred
   * @throws IllegalStateException, an exception formatted "<location> - <reason>"
   */
  public static void error(String location, String reason) {
    throw new IllegalStateException(location + " - " + reason);
  }

  /**
   * Shorthand for expecting the current token to be a certain type, and calling error if the current token isn't.
   * @param s        The current state of the parser.
   * @param location The function in which this error occurred.
   * @param type     The Lexer.Token.Type we are expecting.
   * @throws IllegalStateException, if the current Lexer.Token's type does not match the expected type.
   */
  public static void expect(State s, String location, Lexer.Token.Type type) {
    if (s.cur().type != type)
      error(location, "Expected <" + type + ">, got <" + s.cur().type + "> at " + s.cur().pos);
  }

  /**
   * @param toks List of Lexer.Token
   * @return The Abstract Syntax Tree (AST) that corresponds to the list of Lexer.Token
   * @throws IllegalStateException, if the parser detects a malformed program.
   */
  public static Node parse(List<Lexer.Token> toks) {
    final String loc = "Parser::parse";

    State s = new State(toks);
    Node program = block(s);
    expect(s, loc, Lexer.Token.Type.EndOfFile);
    return program;
  }

  /**
   * The Lexer.Token.Types which denote the end of a block. A block is a list of expressions.
   */
  private static final HashSet<Lexer.Token.Type> END_BLOCK = new HashSet<>(List.of(
      Lexer.Token.Type.EndOfFile,
      Lexer.Token.Type.End,
      Lexer.Token.Type.Else,
      Lexer.Token.Type.Do
  ));

  /**
   * @param s The current state of the parser.
   * @return A Node object representing the block starting at the parser's current state and ending at the
   *         occurrence of one of the Lexer.Token.Types found in the END_BLOCK set found above.
   */
  public static Node block(State s) {
    List<Node> exprs = new ArrayList<>();
    while (!END_BLOCK.contains(s.cur().type)) {
      Node expr = expr(s);
      exprs.add(expr);
      if (s.cur().type == Lexer.Token.Type.Newline) {
        s.adv();
      }
    }
    return new BlockNode(exprs);
  }

  /**
   * @param s The current state of the parser.
   * @return The Node representing the expression that starts at the parser's current state.
   */
  public static Node expr(State s) {
    return assign(s);
  }

  /**
   * @param s The current state of the parser.
   * @return The Node representing the assignment expression that starts at the parser's current state.
   */
  public static Node assign(State s) {
    return maths(s, Parser::or, Parser::or, new HashSet<>(List.of(Lexer.Token.Type.Assign)));
  }

  /**
   * @param s The current state of the parser.
   * @return The Node representing the || expression that starts at the parser's current state.
   */
  public static Node or(State s) {
    return maths(s, Parser::and, Parser::and, new HashSet<>(List.of(Lexer.Token.Type.OrOr)));
  }

  /**
   * @param s The current state of the parser.
   * @return The Node representing the && expression that starts at the parser's current state.
   */
  public static Node and(State s) {
    return maths(s, Parser::comparison, Parser::comparison, new HashSet<>(List.of(Lexer.Token.Type.AndAnd)));
  }

  /**
   * Represents all the comparison operators this language supports.
   */
  private static final HashSet<Lexer.Token.Type> COMPARISON_OPS = new HashSet<>(Arrays.asList(
      Lexer.Token.Type.EqualsEquals,
      Lexer.Token.Type.NotEquals,
      Lexer.Token.Type.GreaterThan,
      Lexer.Token.Type.GreaterThanEquals,
      Lexer.Token.Type.LessThan,
      Lexer.Token.Type.LessThanEquals
  ));

  /**
   * @param s The current state of the parser.
   * @return The Node representing the comparison expression that starts at the parser's current state.
   */
  public static Node comparison(State s) {
    if (s.cur().type == Lexer.Token.Type.Not) {
      s.adv();
      return new NegationNode(comparison(s), Lexer.Token.Type.Not);
    }

    return maths(s, Parser::addition, Parser::addition, COMPARISON_OPS);
  }

  /**
   * @param s The current state of the parser.
   * @return The Node representing the addition expression that starts at the parser's current state.
   */
  public static Node addition(State s) {
    return maths(s, Parser::multiplication, Parser::multiplication, new HashSet<>(List.of(Lexer.Token.Type.Add, Lexer.Token.Type.Sub)));
  }

  /**
   * @param s The current state of the parser.
   * @return The Node representing the multiplication expression that starts at the parser's current state.
   */
  public static Node multiplication(State s) {
    if (s.cur().type == Lexer.Token.Type.Add || s.cur().type == Lexer.Token.Type.Sub) {
      Lexer.Token.Type op = s.cur().type;
      s.adv();

      return new NegationNode(multiplication(s), op);
    }

    return maths(s, Parser::exponent, Parser::exponent, new HashSet<>(List.of(Lexer.Token.Type.Mul, Lexer.Token.Type.Div, Lexer.Token.Type.Mod)));
  }

  /**
   * @param s The current state of the parser.
   * @return The Node representing the exponent expression that starts at the parser's current state.
   */
  public static Node exponent(State s) {
    return maths(s, Parser::atom, Parser::multiplication, new HashSet<>(List.of(Lexer.Token.Type.Pow)));
  }

  /**
   * @param s The current state of the parser.
   * @param left A Function object that takes the parser's current state and parses the expression that's the "next one down" the order
   *             of operations to the left of the operator.
   * @param right A Function object that takes the parser's current state and parses the expression that's the "next one down" the order
   *             of operations to the right of the operator.
   * @return The Node representing the assignment expression that starts at the parser's current state.
   */
  public static Node maths(State s, Function<State, Node> left, Function<State, Node> right, HashSet<Lexer.Token.Type> operators) {
    Node leftExpr = left.apply(s);
    while (operators.contains(s.cur().type)) {
      Lexer.Token.Type op = s.cur().type;
      s.adv();
      Node rightExpr = right.apply(s);
      leftExpr = new MathNode(leftExpr, rightExpr, op);
    }
    return leftExpr;
  }

  /**
   * Precondition: s.cur().type == Lexer.Token.Type.Fun/Integer/String/If/Var/Identifier
   * Layout:
   * | (FunctionDeclaration)
   * | <LParen> (Expr) <RParen>
   * | <Integer>
   * | <String>
   * | (IfStatement)
   * | (VariableDeclaration)
   * | <Identifier> [<LParen> (Expr) [<Comma> (Expr)]+]?
   * | (ForLoop)
   * @param s The current state of the parser.
   * @return A Node that represents the "atom," or basic value, that starts at the parser's current state.
   */
  public static Node atom(State s) {
    final String loc = "Parser::atom";
    if (s.cur().type == Lexer.Token.Type.Fun) {
      return functionDeclaration(s);
    } else if (s.cur().type == Lexer.Token.Type.LParen) {
      s.adv();
      Node expr = expr(s);
      expect(s, loc, Lexer.Token.Type.RParen);
      s.adv();
      return expr;
    } else if (s.cur().type == Lexer.Token.Type.Integer) {
      Node n = new IntegerNode(s.cur().contents);
      s.adv();
      return n;
    } else if (s.cur().type == Lexer.Token.Type.String) {
      Node n = new StringNode(s.cur().contents);
      s.adv();
      return n;
    } else if (s.cur().type == Lexer.Token.Type.If) {
      return ifStatement(s);
    } else if (s.cur().type == Lexer.Token.Type.Var) {
      return variableDeclaration(s);
    } else if (s.cur().type == Lexer.Token.Type.Identifier) {
      String id = s.cur().contents;
      s.adv();
      if (s.cur().type == Lexer.Token.Type.LParen) {
        s.adv();
        List<Node> args = new ArrayList<>();
        if (s.cur().type != Lexer.Token.Type.RParen) {
          args.add(expr(s));
          while (s.cur().type == Lexer.Token.Type.Comma) {
            s.adv();
            args.add(expr(s));
          }
        }
        expect(s, loc, Lexer.Token.Type.RParen);
        s.adv();
        return new FunctionCallNode(id, args);
      } else {
        return new VariableAccessNode(id);
      }
    } else if (s.cur().type == Lexer.Token.Type.For) {
      return forLoop(s);
    }
    throw new IllegalStateException("Parser::atom - Expected, <Fun>, <Integer>, <String>, <If>, <Var>, or <Identifier>, got <" + s.cur().type + "> at " + s.cur().pos);
  }

  /**
   * Precondition: state.cur().type == Lexer.Token.Type.For
   * Layout: <For> (Block) <Do> <Newline>? (Block) <End>
   * @param s The current state of the Parser.
   * @return The Node object representing the for loop.
   */
  public static Node forLoop(State s) {
    final String loc = "Parser::forLoop";

    expect(s, loc, Lexer.Token.Type.For);
    s.adv();
    Node condition = block(s);
    expect(s, loc, Lexer.Token.Type.Do);
    s.adv();
    if (s.cur().type == Lexer.Token.Type.Newline)
      s.adv();
    Node body = block(s);
    s.adv();

    return new ForNode(condition, body);
  }

  /**
   * Precondition: state.cur().type == Lexer.Token.Type.Var
   * Layout: <Var> <Identifier> <Colon> <Identifier> <Assign> (Expr)
   * @param s The parser's current state.
   * @return The Node representing a variable declaration that starts at the parser's current state.
   */
  public static Node variableDeclaration(State s) {
    final String loc = "Parser::variableDeclaration";

    expect(s, loc, Lexer.Token.Type.Var);
    s.adv();
    expect(s, loc, Lexer.Token.Type.Identifier);
    String id = s.cur().contents;
    s.adv();
    expect(s, loc, Lexer.Token.Type.Colon);
    s.adv();
    expect(s, loc, Lexer.Token.Type.Identifier);
    String type = s.cur().contents;
    s.adv();
    expect(s, loc, Lexer.Token.Type.Assign);
    s.adv();
    Node value = expr(s);
    return new VariableDeclarationNode(id, type, value);
  }

  /**
   * Precondition: state.cur().type == Lexer.Token.Type.If
   * Layout: <If> (Expr) <Then> <Newline>?
   *           (Block)
   *
   * @return A Pair of 2 Nodes that represents the condition for the branch and the branch's body
   */
  public static Pair<Node, Node> ifBranch(State s) {
    final String loc = "Parser::ifBranch";

    expect(s, loc, Lexer.Token.Type.If);
    s.adv();
    Node cond = expr(s);
    expect(s, loc, Lexer.Token.Type.Then);
    s.adv();
    if (s.cur().type == Lexer.Token.Type.Newline)
      s.adv();
    Node body = block(s);
    return new Pair<>(cond, body);
  }

  /**
   * Precondition: s.cur().type == Lexer.Token.Type.If
   * Layout: <If> (Expr) <Then> <Newline>?
   *           (Block)
   *         [<Else> <If> (Expr) <Then> <Newline>?
   *           (Block)]+
   *         [<Else> <Newline>?
   *           (Block)]?
   *
   * @return The IfNode
   */
  public static Node ifStatement(State s) {
    final String loc = "Parser::ifStatement";
    List<Pair<Node, Node>> branches = new ArrayList<>();
    Node otherwise = null;
    branches.add(ifBranch(s));
    while (s.cur().type == Lexer.Token.Type.Else) {
      s.adv();
      if (s.cur().type == Lexer.Token.Type.If) {
        branches.add(ifBranch(s));
      } else {
        if (s.cur().type == Lexer.Token.Type.Newline) s.adv();
        otherwise = block(s);
        break;
      }
    }
    expect(s, loc, Lexer.Token.Type.End);
    s.adv();
    return new IfNode(branches, otherwise);
  }

  /**
   * Precondition: s.cur().type == Lexer.Token.Type.Identifier
   * Layout: <Identifier> <Colon> <Identifier>
   *
   * @return A pair with field a being the name of the argument and field b being the type of the argument.
   */
  private static Pair<String, String> argument(State s) {
    final String loc = "Parser::argument";

    expect(s, loc, Lexer.Token.Type.Identifier);
    String argId = s.cur().contents;
    s.adv();
    expect(s, loc, Lexer.Token.Type.Colon);
    s.adv();
    expect(s, loc, Lexer.Token.Type.Identifier);
    String argType = s.cur().contents;
    s.adv();
    return new Pair<>(argId, argType);
  }

  /**
   * Precondition: s.cur().type == Lexer.Token.Type.Fun
   * Layout: <Fun> <Identifier> <LParen> [<Identifier> <Colon> <Identifier> <Comma>]+ <RParen> <Newline>?
   *           (Block)
   *         <End>
   *
   * @return The FunctionDeclarationNode
   */
  public static Node functionDeclaration(State s) {
    final String loc = "Parser::functionDeclaration";

    expect(s, loc, Lexer.Token.Type.Fun);
    s.adv();
    expect(s, loc, Lexer.Token.Type.Identifier);
    String id = s.cur().contents;
    s.adv();
    expect(s, loc, Lexer.Token.Type.LParen);
    s.adv();
    List<Pair<String, String>> args = new ArrayList<>();
    if (s.cur().type == Lexer.Token.Type.Identifier) {
      args.add(argument(s));
      while (s.cur().type == Lexer.Token.Type.Comma) {
        s.adv();
        args.add(argument(s));
      }
    }

    expect(s, loc, Lexer.Token.Type.RParen);
    s.adv();

    if (s.cur().type == Lexer.Token.Type.Newline) s.adv();
    Node block = block(s);

    expect(s, loc, Lexer.Token.Type.End);
    s.adv();
    return new FunctionDeclarationNode(id, args, block);
  }

  public static class Node {
    public final Type type;

    protected Node(Type type) {
      this.type = type;
    }

    public enum Type {
      Integer,
      String,
      VariableDeclaration,
      VariableAccess,
      FunctionDeclaration,
      FunctionCall,
      Maths,
      Negation,
      Block,
      If,
      For
    }
  }

  public static class IfNode extends Node {
    public List<Pair<Node, Node>> conditions;
    public Node otherwise;

    public IfNode(List<Pair<Node, Node>> conditions, Node otherwise) {
      super(Type.If);
      this.conditions = conditions;
      this.otherwise = otherwise;
    }
  }

  public static class BlockNode extends Node {
    public List<Node> exprs;

    public BlockNode(List<Node> exprs) {
      super(Type.Block);
      this.exprs = exprs;
    }
  }

  public static class IntegerNode extends Node {
    public final int value;

    public IntegerNode(String value) {
      super(Type.Integer);
      this.value = Integer.parseInt(value);
    }
  }

  public static class StringNode extends Node {
    public final String value;

    public StringNode(String value) {
      super(Type.String);
      this.value = value;
    }
  }

  public static class VariableDeclarationNode extends Node {
    public final String id;
    public final String type;
    public final Node value;

    public VariableDeclarationNode(String id, String type, Node value) {
      super(Type.VariableDeclaration);
      this.id = id;
      this.type = type;
      this.value = value;
    }
  }

  public static class VariableAccessNode extends Node {
    public final String id;

    public VariableAccessNode(String id) {
      super(Type.VariableAccess);
      this.id = id;
    }
  }

  public static class FunctionDeclarationNode extends Node {
    public final String id;
    public final List<Pair<String, String>> args;
    public final Node body;

    public FunctionDeclarationNode(String id, List<Pair<String, String>> args, Node body) {
      super(Type.FunctionDeclaration);
      this.id = id;
      this.args = args;
      this.body = body;
    }
  }

  public static class FunctionCallNode extends Node {
    public final String toCall;
    public final List<Node> args;

    public FunctionCallNode(String toCall, List<Node> args) {
      super(Type.FunctionCall);
      this.toCall = toCall;
      this.args = args;
    }
  }

  public static class MathNode extends Node {
    public final Node left;
    public final Node right;
    public final Lexer.Token.Type operation;

    public MathNode(Node left, Node right, Lexer.Token.Type operation) {
      super(Type.Maths);
      this.left = left;
      this.right = right;
      this.operation = operation;
    }
  }

  public static class NegationNode extends Node {
    public final Node acting;
    public final Lexer.Token.Type operation;

    public NegationNode(Node acting, Lexer.Token.Type operation) {
      super(Type.Negation);
      this.acting = acting;
      this.operation = operation;
    }
  }

  public static class ForNode extends Node {

    public final Node condition;
    public final Node body;

    protected ForNode(Node condition, Node body) {
      super(Type.For);
      this.condition = condition;
      this.body = body;
    }
  }

  public static class State {
    public final List<Lexer.Token> toks;
    public int index;

    public State(List<Lexer.Token> toks) {
      this.toks = toks;
      this.index = 0;
    }

    public Lexer.Token cur() {
      if (index >= toks.size()) {
        return Lexer.Token.EOF;
      }
      return toks.get(index);
    }

    public void adv() {
      if (index >= toks.size()) return;
      index++;
    }
  }
}
