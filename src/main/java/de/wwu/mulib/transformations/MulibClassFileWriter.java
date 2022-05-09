package de.wwu.mulib.transformations;

public interface MulibClassFileWriter<T> {
    public abstract void validateClassNode(T classNode);

    public abstract void writeClassToFile(String generatedClassesPathPattern, boolean includePackageName, T classNode);

    byte[] toByteArray(T classNode);
}
