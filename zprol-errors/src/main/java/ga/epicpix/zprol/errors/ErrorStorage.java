package ga.epicpix.zprol.errors;

import java.util.ArrayList;
import java.util.Stack;

import static ga.epicpix.zprol.errors.LineMode.*;

public class ErrorStorage {

    private final ArrayList<ErrorInfo> errors = new ArrayList<>();

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
        String location = err.location == null ? "???:??:??" : (err.location.filename + ":" + (err.location.startLine+1) + ":" + (err.location.startRow+1));
        Object[] format = err.format;
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
            }else if(mode == LINE_REPLACE_UNKNOWN) {
                Object[] newFormat = new Object[format.length + 3];
                System.arraycopy(format, 0, newFormat, 0, format.length);
                String line = err.location.lines[err.location.startLine];
                newFormat[format.length] = line;
                newFormat[format.length + 1] = line.substring(0, err.location.startRow);
                newFormat[format.length + 2] = line.substring(err.location.endRow);
                format = newFormat;
            }else if(mode == LINE_REPLACE) { // last value must be the replacement
                Object[] newFormat = new Object[format.length + 3];
                System.arraycopy(format, 0, newFormat, 0, format.length);
                String line = err.location.lines[err.location.startLine];
                newFormat[format.length - 1] = line;
                newFormat[format.length] = line.substring(0, err.location.startRow);
                newFormat[format.length + 1] = format[format.length - 1];
                newFormat[format.length + 2] = line.substring(err.location.endRow);
                format = newFormat;
            }else if(mode == LINE_REPLACE_FULL) { // last value is the line replacement
                Object[] newFormat = new Object[format.length + 1];
                System.arraycopy(format, 0, newFormat, 0, format.length);
                String line = err.location.lines[err.location.startLine];
                newFormat[format.length - 1] = line;
                newFormat[format.length] = format[format.length - 1];
                format = newFormat;
            }
        }
        String formatted = err.code.getCode() + ": " + (err.code.getMode() == LineMode.NONE ? "" : (ErrorStrings.ANSI_CYAN + location + ErrorStrings.ANSI_RESET + " ")) + String.format(err.message + (err.code.getMode() != LineMode.NONE ? ("\n" + err.code.getMode().display) : ""), format);
        if(err.code.getType() == ErrorType.ERROR || err.code.getType() == ErrorType.CRITICAL) {
            System.err.println(formatted);
            if(err.code.getType() == ErrorType.CRITICAL) {
                throw new CriticalErrorException();
            }
        }else {
            System.out.println(formatted);
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