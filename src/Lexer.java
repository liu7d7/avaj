import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Lexer {
  private static final HashMap<String, Token.Type> KEYWORDS = makeKeywords();

  private static HashMap<String, Token.Type> makeKeywords() {
    HashMap<String, Token.Type> keywords = new HashMap<>();
    keywords.put("else", Token.Type.Else);
    keywords.put("if", Token.Type.If);
    keywords.put("end", Token.Type.End);
    keywords.put("fun", Token.Type.Fun);
    keywords.put("for", Token.Type.For);
    keywords.put("do", Token.Type.Do);
    keywords.put("var", Token.Type.Var);
    keywords.put("then", Token.Type.Then);
    return keywords;
  }

  private static char escape(State s) {
    if (s.cur() != '\\') throw new IllegalStateException("Lexer::escape - Nothing to escape at " + s.pos.toString());
    s.adv();
    if (s.cur() == 'n') {
      s.adv();
      return '\n';
    } else if (s.cur() == 'r') {
      s.adv();
      return '\r';
    } else if (s.cur() == 'b') {
      s.adv();
      return '\b';
    } else if (s.cur() == '0') {
      s.adv();
      return '\0';
    } else if (s.cur() == '"') {
      s.adv();
      return '"';
    }
    throw new IllegalStateException("Lexer::escape - Unexpected escape character '" + s.cur() + "' at " + s.pos.toString());
  }

  public static List<Token> lex(String path) {
    State s = new State(path);
    while (s.cur() != 0) {
      s.begin = s.pos.copy();
      if (s.cur() == ' ' /* handle whitespace */) {
        s.adv();
      } else if (s.cur() == ';' /* Type.Semicolon */) {
        s.adv();
        // never put 2 newlines in a row
        if (s.tokens.isEmpty() || s.tokens.get(s.tokens.size() - 1).type == Token.Type.Newline) {
          continue;
        }
        s.add(Token.Type.Newline, ";");
      } else if (s.cur() == '+' /* Type.Add */) {
        s.add(Token.Type.Add, "+");
        s.adv();
      } else if (s.cur() == '-' /* Type.Sub */) {
        s.add(Token.Type.Sub, "-");
        s.adv();
      } else if (s.cur() == '*' /* Type.Mul */) {
        s.add(Token.Type.Mul, "*");
        s.adv();
      } else if (s.cur() == '/' /* Type.Div */) {
        s.adv();
        if (s.cur() == '/') {
          while (s.cur() != '\n') {
            s.adv();
          }
          continue;
        } else if (s.cur() == '*') {
          s.adv();
          while (s.cur() != '*' || s.relative(1) != '/') {
            s.adv();
          }
          s.adv();
          s.adv();
          continue;
        }
        s.add(Token.Type.Div, "/");
        s.adv();
      } else if (s.cur() == '%' /* Type.Mod */) {
        s.add(Token.Type.Mod, "%");
        s.adv();
      } else if (s.cur() == '^' /* Type.Pow */) {
        s.add(Token.Type.Pow, "^");
        s.adv();
      } else if (s.cur() == '(' /* Type.LParen */) {
        s.add(Token.Type.LParen, "(");
        s.adv();
      } else if (s.cur() == ')' /* Type.RParen */) {
        s.add(Token.Type.RParen, ")");
        s.adv();
      } else if (s.cur() == ':' /* Type.Colon */) {
        s.add(Token.Type.Colon, ":");
        s.adv();
      } else if (s.cur() == '\n' /* Type.Newline */) {
        s.adv();
        /* Don't want more than 1 newline token in a row */
        if (s.tokens.isEmpty() || s.tokens.get(s.tokens.size() - 1).type == Token.Type.Newline) {
          continue;
        }
        s.add(Token.Type.Newline, "\\n");
      } else if (s.cur() == '=' /* Type.EqualsEquals */) {
        s.adv();
        if (s.cur() != '=') {
          throw new IllegalStateException("Expected '=', got '" + s.cur() + "' at " + s.pos.toString());
        }
        s.adv();
        s.add(Token.Type.EqualsEquals, "==");
      } else if (s.cur() == '&' /* Type.AndAnd */) {
        s.adv();
        if (s.cur() != '&') {
          throw new IllegalStateException("Expected '&', got '" + s.cur() + "' at " + s.pos.toString());
        }
        s.adv();
        s.add(Token.Type.AndAnd, "&&");
      } else if (s.cur() == '|' /* Type.OrOr */) {
        s.adv();
        if (s.cur() != '|') {
          throw new IllegalStateException("Expected '|', got '" + s.cur() + "' at " + s.pos.toString());
        }
        s.adv();
        s.add(Token.Type.OrOr, "||");
      } else if (s.cur() == '!' /* Type.Not/Type.NotEquals */) {
        s.adv();
        if (s.cur() == '=' /* Type.NotEquals */) {
          s.adv();
          s.add(Token.Type.NotEquals, "!=");
        } else /* Type.Not */ {
          s.add(Token.Type.Not, "!");
        }
      } else if (s.cur() == '>' /* Type.GreaterThan/Type.GreaterThanEquals */) {
        s.adv();
        if (s.cur() == '=' /* Type.GreaterThanEquals */) {
          s.adv();
          s.add(Token.Type.GreaterThanEquals, ">=");
        } else /* Type.GreaterThan */ {
          s.add(Token.Type.GreaterThan, ">");
        }
      } else if (s.cur() == '<' /* Type.LessThan/Type.LessThanEquals/Type.Assign */) {
        s.adv();
        if (s.cur() == '-' /* Type.Assign */) {
          s.adv();
          s.add(Token.Type.Assign, "<-");
        } else if (s.cur() == '=' /* Type.LessThanEquals */) {
          s.adv();
          s.add(Token.Type.LessThanEquals, "<=");
        } else {
          s.add(Token.Type.LessThan, "<");
        }
      } else if (s.cur() == '"') {
        s.adv();
        StringBuilder sb = new StringBuilder();
        while (s.cur() != '"') {
          if (s.cur() == '\\') {
            sb.append(escape(s));
            continue;
          }
          sb.append(s.cur());
          s.adv();
          if (s.cur() == 0) break;
        }
        s.adv();
        s.add(Token.Type.String, sb.toString());
      } else if (isIdentifierStart(s.cur())) {
        StringBuilder id = new StringBuilder();
        while (isIdentifierContinue(s.cur())) {
          id.append(s.cur());
          s.adv();
        }
        String str = id.toString();
        s.add(KEYWORDS.getOrDefault(str, Token.Type.Identifier), str);
      } else if (Character.isDigit(s.cur())) {
        StringBuilder number = new StringBuilder();
        while (Character.isDigit(s.cur())) {
          number.append(s.cur());
          s.adv();
        }
        s.add(Token.Type.Integer, number.toString());
      } else {
        throw new IllegalStateException("found invalid character '" + s.cur() + "' at " + s.pos.toString());
      }
    }

    s.tokens.add(Token.EOF);
    return s.tokens;
  }

  private static boolean isIdentifierStart(char ch) {
    return Character.isLetter(ch) || ch == '_';
  }

  private static boolean isIdentifierContinue(char ch) {
    return isIdentifierStart(ch) || Character.isDigit(ch);
  }

  public static class Token {
    public static final Token EOF = new Token(Type.EndOfFile, "<EOF>", new Position(0, 0, 0, ""));
    public final Type type;
    public final String contents;
    public final Position pos;

    public Token(Type type, String contents, Position pos) {
      this.type = type;
      this.contents = contents;
      this.pos = pos;
    }

    public enum Type {
      LParen,
      RParen,
      End,
      If,
      Else,
      Integer,
      Identifier,
      Var,
      Fun,
      Add,
      Sub,
      Mod,
      Div,
      Mul,
      Pow,
      Colon,
      Assign,
      EqualsEquals,
      NotEquals,
      LessThan,
      LessThanEquals,
      GreaterThan,
      GreaterThanEquals,
      AndAnd,
      OrOr,
      Newline,
      Comma,
      Not,
      EndOfFile,
      String,
      For,
      Do,
      Then
    }
  }

  public static class State {
    public Position pos;
    public Position begin;
    public char[] text;
    public int length;
    public final ArrayList<Token> tokens;

    public State(String path) {
      try {
        String txt = Files.readString(Paths.get(path)).replace("\r\n", "\n");
        this.text = txt.toCharArray();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }

      this.length = text.length;
      this.pos = new Position(0, 1, 1, path);
      this.tokens = new ArrayList<>();
    }

    public void add(Token.Type type, String contents) {
      tokens.add(new Token(type, contents, begin.copy()));
    }

    public char cur() {
      if (pos.index >= length || pos.index < 0) {
        return 0;
      }
      return text[pos.index];
    }

    public char relative(int rel) {
      int index = pos.index + rel;
      if (index >= length || index < 0) {
        return 0;
      }
      return text[index];
    }

    public void adv() {
      pos.index++;
      pos.column++;
      if (pos.index >= length) {
        return;
      }
      if (cur() == '\n') {
        pos.row++;
        pos.column = 0;
      }
    }
  }
}
