import java.io.InputStream;
import java.io.IOException;

class CalculatorParser{
  private int lookaheadToken;

  private InputStream input;

  public CalculatorParser(InputStream input) throws IOException{
    this.input = input;
    lookaheadToken = input.read();
  }

  private void consume(int symbol) throws IOException, ParseError{
    if (lookaheadToken != symbol)
      throw new ParseError();

    lookaheadToken = input.read();
  }

  private int exp() throws IOException, ParseError{
    return low(high());
  }

  private int low(int untilnow) throws IOException, ParseError{
    if (lookaheadToken == -1 || lookaheadToken == '\r' || lookaheadToken == ')')
      return untilnow;
    else if (lookaheadToken != '^')
      throw new ParseError();

    consume('^');
    return untilnow ^ low(high());
  }

  private int high() throws IOException, ParseError{
	return high2(paren());
  }

  private int high2(int untilnow) throws IOException, ParseError{
    if (lookaheadToken == '^' || lookaheadToken == -1 || lookaheadToken == '\r' ||
      lookaheadToken == ')')
      return untilnow;
    else if (lookaheadToken != '&')
      throw new ParseError();

    consume('&');

    return untilnow & high2(paren());
  }

  private int paren() throws IOException, ParseError{
    if (lookaheadToken != '(' && (lookaheadToken < '0' || lookaheadToken > '9'))
      throw new ParseError();
      
    if (lookaheadToken == '('){
      consume('(');
      int result = exp();
      
      if (lookaheadToken == ')'){
        consume(')');
        return result;
      }
      else throw new ParseError();

    }
    else
	  return num();
  }

  private int num() throws IOException, ParseError{
    if (lookaheadToken < '0' || lookaheadToken > '9')
      throw new ParseError();
    else{
	  int number = lookaheadToken - 48;
      consume(lookaheadToken);
      return number;
    }
  }

  public int parse() throws IOException, ParseError{
    int result = exp();
    if (lookaheadToken != '\r' && lookaheadToken != -1)
      throw new ParseError();
    return result;
  }

  public static void main(String[] args){
    try{
      CalculatorParser parser = new CalculatorParser(System.in);
      System.out.println(parser.parse());
    }
    catch (IOException except){
      System.err.println(except.getMessage());
    }
    catch (ParseError parserr){
      System.err.println(parserr.getMessage());
    }
  }
}
