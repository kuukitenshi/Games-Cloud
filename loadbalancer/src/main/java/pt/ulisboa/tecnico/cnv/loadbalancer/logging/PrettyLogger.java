package pt.ulisboa.tecnico.cnv.loadbalancer.logging;

import java.text.SimpleDateFormat;
import java.util.Date;

public class PrettyLogger implements ILogger {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("HH:mm:ss.SSS");

    private final String name;

    public PrettyLogger(String name) {
        this.name = name;
    }

    @Override
    public void log(LogLevel level, String format, Object... args) {
        Date now = new Date();
        String formattedDate = DATE_FORMAT.format(now);
        String message = String.format(format, args);
        String threadName = Thread.currentThread().getName();
        String color = ANSIColors.RESET;
        switch (level) {
            case DEBUG:
                color = ANSIColors.MAGENTA_FG;
                break;
            case INFO:
                color = ANSIColors.GREEN_FG;
                break;
            case WARNING:
                color = ANSIColors.YELLOW_FG;
                break;
            case ERROR:
                color = ANSIColors.RED_FG;
                break;
            default:
                color = ANSIColors.RESET;
                break;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(color);
        message.lines()
                .map(line -> String.format("[%s] (%s) %s %s > %s", formattedDate, threadName, level, this.name, line))
                .forEach(line -> sb.append(line).append(System.lineSeparator()));
        sb.append(ANSIColors.RESET);
        System.out.print(sb.toString());
    }

}
