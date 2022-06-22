package ga.epicpix.zprol.structures;

import ga.epicpix.zprol.types.Type;

import java.util.EnumSet;

public record Field(String namespace, EnumSet<FieldModifiers> modifiers, String name, Type type) {}
