package ga.epicpix.zprol.errors;

import java.util.ArrayList;
import java.util.Stack;

public class ErrorStorage {

    private final ArrayList<ErrorInfo> errors = new ArrayList<>();

    public void addError(ErrorCodes err, Object... format) {
        ErrorInfo info = new ErrorInfo(err, err.getMessage(), format);
        if(capturedErrors.size() != 0) {
            capturedErrors.peek().add(info);
            return;
        }
        errors.add(info);
        if(err.getType() == ErrorType.ERROR || err.getType() == ErrorType.CRITICAL) {
            System.err.println(err.getCode() + ": " + String.format(err.getMessage(), format));
            if(err.getType() == ErrorType.CRITICAL) {
                throw new CriticalErrorException();
            }
        }else {
            System.out.println(err.getCode() + ": " + String.format(err.getMessage(), format));
        }
    }

    public void addError(ErrorInfo err) {
        if(capturedErrors.size() != 0) {
            capturedErrors.peek().add(err);
            return;
        }
        errors.add(err);
        if(err.code.getType() == ErrorType.ERROR || err.code.getType() == ErrorType.CRITICAL) {
            System.err.println(err.code.getCode() + ": " + String.format(err.message, err.format));
            if(err.code.getType() == ErrorType.CRITICAL) {
                throw new CriticalErrorException();
            }
        }else {
            System.out.println(err.code.getCode() + ": " + String.format(err.message, err.format));
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
