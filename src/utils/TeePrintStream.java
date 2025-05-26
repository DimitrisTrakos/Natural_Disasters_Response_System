package utils;

import java.io.PrintStream;

public class TeePrintStream extends PrintStream {
    private final PrintStream second;

    public TeePrintStream(PrintStream main, PrintStream second) {
        super(main);
        this.second = second;
    }

    @Override
    public void println(String x) {
        super.println(x);
        second.println(x);
    }

    @Override
    public void print(String x) {
        super.print(x);
        second.print(x);
    }

    @Override
    public void flush() {
        super.flush();
        second.flush();
    }
}
