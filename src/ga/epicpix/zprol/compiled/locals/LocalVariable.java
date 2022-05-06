package ga.epicpix.zprol.compiled.locals;

import ga.epicpix.zprol.compiled.PrimitiveType;

public record LocalVariable(String name, PrimitiveType type, int index) {}
