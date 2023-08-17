package de.wwu.mulib.transformations;

public interface MulibClassFileWriter<T> {
    void validateClassNode(T classNode);

    void writeClassToFile(String generatedClassesPathPattern, boolean includePackageName, T classNode);

    byte[] toByteArray(T classNode);
}
