package ga.epicpix.zprol.compiled;

import ga.epicpix.zprol.compiled.bytecode.IBytecodeStorage;

public record Function(String namespace, String name, FunctionSignature signature, IBytecodeStorage code) {}
