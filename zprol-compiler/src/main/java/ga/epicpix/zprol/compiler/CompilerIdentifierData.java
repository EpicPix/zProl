package ga.epicpix.zprol.compiler;

import ga.epicpix.zprol.parser.exceptions.TokenLocatedException;
import ga.epicpix.zprol.parser.tree.*;

import java.util.ArrayList;

public class CompilerIdentifierData {

    public final ITree location;

    public CompilerIdentifierData(ITree location) {
        this.location = location;
    }

    public static CompilerIdentifierData[] accessorToData(AccessorTree accessor) {
        IAccessorElement[] tokens = accessor.accessorElements;
        ArrayList<CompilerIdentifierData> list = new ArrayList<CompilerIdentifierData>();
        for(IAccessorElement element : tokens) {
            if(element instanceof FieldAccessTree) {
                FieldAccessTree fa = (FieldAccessTree) element;
                list.add(new CompilerIdentifierDataField(fa, fa.name.toStringRaw()));
            }else if(element instanceof FunctionCallTree) {
                FunctionCallTree fc = (FunctionCallTree) element;
                list.add(new CompilerIdentifierDataFunction(fc, fc.name.toStringRaw(), fc.arguments));
            }else if(element instanceof ArrayAccessTree) {
                ArrayAccessTree aa = (ArrayAccessTree) element;
                list.add(new CompilerIdentifierDataArray(aa, aa.index));
            }else {
                throw new TokenLocatedException("Cannot handle accessor element: " + element);
            }
        }
        return list.toArray(new CompilerIdentifierData[0]);
    }
}
