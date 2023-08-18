package de.wwu.mulib.transformations;

/**
 * Writes class files from bytecode framework representations
 * @param <T>
 */
public interface MulibClassFileWriter<T> {
    /**
     * Validates the class node
     * @param classNode The bytecode format representation of a class
     */
    void validateClassNode(T classNode);

    /**
     * Writes the class to a file
     * @param generatedClassesPathPattern The path to write it to
     * @param includePackageName Whether to include the package name in the path
     * @param classNode The bytecode format representation of a class
     */
    void writeClassToFile(String generatedClassesPathPattern, boolean includePackageName, T classNode);

    /**
     * @param classNode The bytecode format representation of a class
     * @return A byte array representing the class
     */
    byte[] toByteArray(T classNode);
}
