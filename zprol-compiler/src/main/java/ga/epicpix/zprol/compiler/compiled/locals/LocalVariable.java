package ga.epicpix.zprol.compiler.compiled.locals;

import ga.epicpix.zprol.types.Type;

public record LocalVariable(String name, Type type, int index) {}
