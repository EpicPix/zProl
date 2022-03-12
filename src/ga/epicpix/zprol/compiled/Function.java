package ga.epicpix.zprol.compiled;

import ga.epicpix.zprol.compiled.bytecode.IBytecodeStorage;

public record Function(String name, FunctionSignature signature, IBytecodeStorage code) {}
