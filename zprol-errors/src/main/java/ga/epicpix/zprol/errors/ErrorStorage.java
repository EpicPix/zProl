package ga.epicpix.zprol.errors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Stack;
import java.util.stream.Collectors;

import static ga.epicpix.zprol.errors.LineMode.*;

public class ErrorStorage {

    private final ArrayList<ErrorInfo> errors = new ArrayList<>();

    private final boolean otherInfo;

    public ErrorStorage(boolean otherInfo) {
        this.otherInfo = otherInfo;
    }

    public void addError(ErrorCodes err, Object... format) {
        addError(err, null, format);
    }

    public void addError(ErrorCodes err, ErrorLocation location, Object... format) {
        ErrorInfo info = new ErrorInfo(err, location, err.getMessage(), format);
        if(capturedErrors.size() != 0) {
            capturedErrors.peek().add(info);
            return;
        }
        errors.add(info);
        showError(info);
    }

    public void addError(ErrorInfo err) {
        if(capturedErrors.size() != 0) {
            capturedErrors.peek().add(err);
            return;
        }
        errors.add(err);
        showError(err);
    }

    private void showError(ErrorInfo err) {
        String formatted;
        if(!otherInfo) {
            Object[] format = new Object[0];
            String location = err.location == null ? "???:??:??" : (err.location.filename + ":" + (err.location.startLine + 1) + ":" + (err.location.startRow + 1));
            format = err.format;
            if(err.location != null) {
                LineMode mode = err.code.getMode();
                if(mode == LINE_HIGHLIGHT) {
                    Object[] newFormat = new Object[format.length + 3];
                    System.arraycopy(format, 0, newFormat, 0, format.length);
                    String line = err.location.lines[err.location.startLine];
                    newFormat[format.length] = line.substring(0, err.location.startRow);
                    newFormat[format.length + 1] = line.substring(err.location.startRow, err.location.endRow);
                    newFormat[format.length + 2] = line.substring(err.location.endRow);
                    format = newFormat;
                } else if(mode == LINE_REPLACE_UNKNOWN) {
                    Object[] newFormat = new Object[format.length + 3];
                    System.arraycopy(format, 0, newFormat, 0, format.length);
                    String line = err.location.lines[err.location.startLine];
                    newFormat[format.length] = line;
                    newFormat[format.length + 1] = line.substring(0, err.location.startRow);
                    newFormat[format.length + 2] = line.substring(err.location.endRow);
                    format = newFormat;
                } else if(mode == LINE_START_UNKNOWN) {
                    Object[] newFormat = new Object[format.length + 3];
                    System.arraycopy(format, 0, newFormat, 0, format.length);
                    String line = err.location.lines[err.location.startLine];
                    newFormat[format.length] = line;
                    newFormat[format.length + 1] = line.substring(0, err.location.startRow);
                    newFormat[format.length + 2] = line.substring(err.location.startRow);
                    format = newFormat;
                } else if(mode == LINE_END_UNKNOWN) {
                    Object[] newFormat = new Object[format.length + 3];
                    System.arraycopy(format, 0, newFormat, 0, format.length);
                    String line = err.location.lines[err.location.startLine];
                    newFormat[format.length] = line;
                    newFormat[format.length + 1] = line.substring(0, err.location.endRow);
                    newFormat[format.length + 2] = line.substring(err.location.endRow);
                    format = newFormat;
                } else if(mode == LINE_REPLACE) { // last value must be the replacement
                    Object[] newFormat = new Object[format.length + 3];
                    System.arraycopy(format, 0, newFormat, 0, format.length);
                    String line = err.location.lines[err.location.startLine];
                    newFormat[format.length - 1] = line;
                    newFormat[format.length] = line.substring(0, err.location.startRow);
                    newFormat[format.length + 1] = format[format.length - 1];
                    newFormat[format.length + 2] = line.substring(err.location.endRow);
                    format = newFormat;
                } else if(mode == LINE_REPLACE_FULL) { // last value is the line replacement
                    Object[] newFormat = new Object[format.length + 1];
                    System.arraycopy(format, 0, newFormat, 0, format.length);
                    String line = err.location.lines[err.location.startLine];
                    newFormat[format.length - 1] = line;
                    newFormat[format.length] = format[format.length - 1];
                    format = newFormat;
                }
            }
            formatted = err.code.getCode() + ": " + (err.code.getMode() == LineMode.NONE ? "" : (ErrorStrings.ANSI_CYAN + location + ErrorStrings.ANSI_RESET + " ")) + String.format(err.message + (err.code.getMode() != LineMode.NONE ? ("\n" + err.code.getMode().display) : ""), format);

            if(err.code.getType() == ErrorType.ERROR || err.code.getType() == ErrorType.CRITICAL) {
                System.err.println(formatted);
            } else {
                System.out.println(formatted);
            }
        } else {
            formatted = "--- " +
                err.code.getType() + ", " +
                err.code.getCode() + ", " +
                (err.location == null ? -1 : err.location.startRow) + ", " +
                (err.location == null ? -1 : err.location.startLine) + ", " +
                (err.location == null ? -1 : err.location.endRow) + ", " +
                (err.location == null ? -1 : err.location.endLine) + ", " +
                (err.location == null ? -1 : err.location.filename) + ", " +
                (err.format.length != 0 ? (", " + Arrays.stream(err.format).map(Object::toString).collect(Collectors.joining(", "))) : "");
            System.err.println(formatted);
        }
        if(err.code.getType() == ErrorType.CRITICAL) {
            throw new CriticalErrorException();
        }
    }

    private final Stack<ArrayList<ErrorInfo>> capturedErrors = new Stack<>();

    public void startCapturingErrors() {
        capturedErrors.push(new ArrayList<>());
    }

    public void stopCapturingErrors(boolean add) {
        ArrayList<ErrorInfo> err = capturedErrors.pop();
        if(add) {
            for(ErrorInfo e : err) {
                addError(e);
            }
        }
    }

    public int getErrorCount(ErrorType type) {
        if(type == null) {
            return errors.size();
        }
        int count = 0;
        for(ErrorInfo err : errors) {
            if(err.code.getType() == type) {
                count++;
            }
        }
        return count;
    }

    public boolean hasCapturedErrors(ErrorType ofType) {
        if(capturedErrors.size() == 0) {
            if(ofType == null) {
                return errors.size() != 0;
            }
            for(ErrorInfo err : errors) {
                if(err.code.getType() == ofType) {
                    return true;
                }
            }
            return false;
        }
        if(ofType == null) return capturedErrors.peek().size() != 0;
        for(ErrorInfo i : capturedErrors.peek()) {
            if(i.code.getType() == ofType) {
                return true;
            }
        }
        return false;
    }
}
