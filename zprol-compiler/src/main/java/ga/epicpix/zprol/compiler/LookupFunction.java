package ga.epicpix.zprol.compiler;

import ga.epicpix.zprol.compiler.precompiled.PreFunction;

public record LookupFunction(boolean isClassMethod, PreFunction func, String namespace) {}
