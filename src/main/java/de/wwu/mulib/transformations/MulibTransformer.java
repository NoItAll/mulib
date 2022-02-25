package de.wwu.mulib.transformations;

import de.wwu.mulib.Mulib;
import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.exceptions.MulibRuntimeException;
import de.wwu.mulib.exceptions.NotYetImplementedException;
import de.wwu.mulib.search.executors.SymbolicExecution;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.util.CheckClassAdapter;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.stream.Collectors;

import static de.wwu.mulib.transformations.StringConstants.*;
import static de.wwu.mulib.transformations.TransformationUtility.*;
import static org.objectweb.asm.Opcodes.*;

/**
 * Core piece of Mulib. The MulibTransformer accepts classes and generates partner classes for them, according
 * to a specified configuration.
 */
public class MulibTransformer {
    // For synchronizing file writes
    private static final Object syncObject = new Object();
    private static final int _API_VERSION = ASM9;
    private final String generatedClassesPathPattern;
    private final boolean writeToFile;
    private final boolean validate;
    private final boolean tryUseSystemClassLoader;
    private final boolean includePackageName;
    private final List<String> ignoreFromPackages;
    private final List<Class<?>> ignoreClasses;
    private final List<Class<?>> ignoreSubclassesOf;
    private final List<Class<?>> regardSpecialCase;
    private final Queue<Class<?>> classesToTransform = new ArrayDeque<>();
    private final Set<Class<?>> explicitlyAddedClasses = new HashSet<>();
    private final List<Class<?>> concretizeFor;
    private final List<Class<?>> generalizeMethodCallsFor;

    // original class -> class transformed for symbolic execution
    private final Map<String, Class<?>> transformedClasses = new HashMap<>();
    private final Map<String, ClassNode> transformedClassNodes = new HashMap<>();
    private final MulibConfig config;

    private final ClassLoader classLoader;

    /**
     * Constructs an instance of MulibTranformer according to the configuration.
     * @param config The configuration.
     */
    public MulibTransformer(MulibConfig config) {
        this.classLoader = config.TRANSF_LOAD_WITH_SYSTEM_CLASSLOADER ?
                ClassLoader.getSystemClassLoader() :
                new MulibClassLoader(this);
        this.tryUseSystemClassLoader = config.TRANSF_LOAD_WITH_SYSTEM_CLASSLOADER;
        this.writeToFile = config.TRANSF_WRITE_TO_FILE;
        this.validate = config.TRANSF_VALIDATE_TRANSFORMATION;
        String generatedClassesPath = config.TRANSF_GENERATED_CLASSES_PATH;
        this.generatedClassesPathPattern = generatedClassesPath + "%s.class";
        this.ignoreFromPackages = config.TRANSF_IGNORE_FROM_PACKAGES;
        this.ignoreClasses = config.TRANSF_IGNORE_CLASSES;
        this.ignoreSubclassesOf = config.TRANSF_IGNORE_SUBCLASSES_OF;
        this.regardSpecialCase = config.TRANSF_REGARD_SPECIAL_CASE;
        this.concretizeFor = config.TRANSF_CONCRETIZE_FOR;
        this.generalizeMethodCallsFor = config.TRANSF_TRY_USE_MORE_GENERAL_METHOD_FOR;
        this.includePackageName = config.TRANSF_INCLUDE_PACKAGE_NAME;
        this.config = config;
    }

    /* PUBLIC METHODS */

    /**
     * Transforms the specified classes. Even if the passed classes are ignored according to the configuration,
     * they will be transformed as they have been explicitly stated to be transformed.
     * These classes that are dependencies for the specified classes are transformed according to the configuration
     * of the MulibTransformer instance.
     * @param toTransform Those classes that are transformed, even if they have been set to be ignored.
     */
    public void transformAndLoadClasses(Class<?>... toTransform) {
        List<Class<?>> definitelyTransform = Arrays.asList(toTransform);
        explicitlyAddedClasses.addAll(definitelyTransform);
        classesToTransform.addAll(definitelyTransform);

        while (!classesToTransform.isEmpty()) {
            transformClass(classesToTransform.poll());
        }

        // Define classes
        for (Map.Entry<String, ClassNode> entry : transformedClassNodes.entrySet()) {
            if (transformedClasses.get(entry.getKey()) != null) {
                continue;
            }

            try {
                synchronized (syncObject) {
                    Class<?> result = classLoader.loadClass(entry.getValue().name.replace("/", "."));
                    transformedClasses.put(entry.getKey(), result);
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                throw new MulibRuntimeException(e);
            }
        }

        maybeCheckAreValidInitializedClasses(transformedClasses.values());
    }

    /**
     * Returns the partner class for the given class.
     * @param beforeTransformation The class the partner class of which is to be returned.
     * @return The partner class.
     */
    public final Class<?> getTransformedClass(Class<?> beforeTransformation) {
        Class<?> result = transformedClasses.get(beforeTransformation.getName());
        if (result == null) {
            throw new MulibRuntimeException("Class has not been transformed: " + beforeTransformation);
        }
        return result;
    }

    /**
     * @param beforeTransformation The class before transforming it into the partner class.
     * @return Either the partner class, or, if not available, the original class.
     */
    public final Class<?> getPossiblyTransformedClass(Class<?> beforeTransformation) {
        Class<?> result = transformedClasses.get(beforeTransformation.getName());
        if (result == null) {
            return beforeTransformation;
        }
        return result;
    }

    public final void setPartnerClass(Class<?> original, Class<?> partnerClass) {
        transformedClasses.put(original.getName(), partnerClass);
    }

    /* METHODS FOR CHECKING HOW CLASSES SHOULD BE TREATED. */

    /**
     * Checks whether, given the configuration, the method of the class represented by the given path, should be concretized.
     * @param methodOwner The path of the class.
     * @return true, if the method should be concretized, else false.
     */
    boolean shouldBeConcretizedFor(String methodOwner) {
        Class<?> c = getClassForPath(methodOwner);
        return shouldBeConcretizedFor(c);
    }

    /**
     * Checks whether, given the configuration, the method of the class represented by the given path, should be generalized.
     * @param methodOwner The path of the class.
     * @return true, if the method should be generalized, else false.
     */
    boolean shouldTryToUseGeneralizedMethodCall(String methodOwner) {
        Class<?> c = getClassForPath(methodOwner);
        return shouldTryToUseGeneralizedMethodCall(c);
    }

    /**
     * Checks whether, given the configuration, the method of the class, should be concretized.
     * @param methodOwner The class.
     * @return true, if the method should be concretized, else false.
     */
    boolean shouldBeConcretizedFor(Class<?> methodOwner) {
        return concretizeFor.contains(methodOwner);
    }

    /**
     * Checks whether, given the configuration, the method of the class, should be generalized.
     * @param methodOwner The class.
     * @return true, if the method should be generalized, else false.
     */
    boolean shouldTryToUseGeneralizedMethodCall(Class<?> methodOwner) {
        return generalizeMethodCallsFor.contains(methodOwner);
    }

    /**
     * Checks whether, according to the configuration, a class should be transformed, i.e., whether a partner
     * class should be generated.
     * @param classAsPath The path of the class.
     * @return true, if the class should be transformed, otherwise false.
     */
    boolean shouldBeTransformed(String classAsPath) {
        Class<?> c = getClassForPath(classAsPath);
        return !isIgnored(c);
    }

    boolean shouldBeTransformedFromDesc(String desc) {
        if (desc.startsWith("[")) {
            int last = desc.lastIndexOf('[');
            desc = desc.substring(last + 1);
        }
        if (desc.length() == 1 && primitiveTypes.contains(desc)) {
            return true;
        }
        assert (desc.startsWith("L") && desc.endsWith(";"));
        return shouldBeTransformed(desc.substring(1, desc.length() - 1));
    }

    // Check if the class, represented by the path, is about to be transformed or has already been transformed.
    private boolean isAlreadyTransformedOrToBeTransformedPath(String classAsPath) {
        Class<?> c = getClassForPath(classAsPath);
        return classesToTransform.contains(c) // Should not be already enqueued for transformation
                || transformedClasses.containsKey(c.getName()); // Should not already be transformed
    }

    // Checks if a class is ignored according to some configuration. Ignored classes are not transformed.
    private boolean isIgnored(Class<?> toTransform) {
        return shouldBeConcretizedFor(toTransform)
                || shouldTryToUseGeneralizedMethodCall(toTransform)
                || (
                !explicitlyAddedClasses.contains(toTransform)
                        && regardSpecialCase.stream().noneMatch(x -> x.equals(toTransform))
                        && (ignoreFromPackages.stream().anyMatch(x -> toTransform.getPackageName().startsWith(x)) // (Multi-dimensional) arrays of primitives are treated separately in the transformation
                        || ignoreClasses.stream().anyMatch(x -> x.equals(toTransform))
                        || ignoreSubclassesOf.stream().anyMatch(x -> x.isAssignableFrom(toTransform)))
        );
    }

    /* METHODS FOR INTERACTING WITH CUSTOM CLASSLOADER */
    // Gets the partner class to the class represented by its name.
    Class<?> getTransformedClass(String className) {
        return transformedClasses.get(className);
    }

    // Gets the ASM representation of the partner class of the class represented by its name.
    ClassNode getTransformedClassNode(String className) {
        return transformedClassNodes.get(className);
    }

    // Adds the transformed class to the map with <class name, partner class>.
    void addTransformedClass(String className, Class<?> c) {
        transformedClasses.put(className, c);
    }

    // Transforms one class. Checks whether the class is ignored and whether the class has already been transformed.
    private void transformClass(Class<?> toTransform) {
        if (!(classLoader instanceof MulibClassLoader)) {
            try {
                getClass().getClassLoader().loadClass(addPrefix(toTransform.getName()));
                return;
            } catch (ClassNotFoundException ignored) {
            }
        }
        if (isIgnored(toTransform) || transformedClassNodes.containsKey(toTransform.getName())) {
            return;
        }
        ClassNode result = transformedClassNodes.get(toTransform.getName());
        if (result != null) {
            return;
        }

        try {
            ClassReader cr = new ClassReader(toTransform.getName());
            ClassNode classNode = new ClassNode(_API_VERSION);
            cr.accept(classNode, 0);
            ClassNode transformedClass = transform(classNode);
            transformedClassNodes.put(toTransform.getName(), transformedClass);
        } catch (IOException e) {
            throw new MulibRuntimeException("The class to be read could not be found: " + toTransform.getName(), e);
        }
    }

    /* TRANSFORMING CLASS TO PARTNER CLASS */
    // Transforms the ASM representation of the original class to the ASM representation of the partner class.
    private ClassNode transform(ClassNode originalCn) {
        ClassNode result = new ClassNode(MulibTransformer._API_VERSION);
        result.name = addPrefix(originalCn.name);
        result.access = originalCn.access;
        result.version = originalCn.version;
        throwExceptionIfNotEmpty(originalCn.recordComponents);
        throwExceptionIfNotEmpty(originalCn.attrs);
        result.innerClasses = new ArrayList<>();
        for (InnerClassNode icn : originalCn.innerClasses) {
            if (icn.outerName == null || shouldBeTransformed(icn.outerName)) {
                icn.outerName = icn.outerName != null ? decideOnReplaceName(icn.outerName) : null;
                icn.innerName = addPrefix(icn.innerName);
                icn.name = decideOnReplaceName(icn.name); // Adds to queue if needed
                result.innerClasses.add(icn);
            }
        }
        throwExceptionIfNotEmpty(originalCn.invisibleAnnotations);
        throwExceptionIfNotEmpty(originalCn.invisibleTypeAnnotations);
        if (originalCn.nestHostClass != null) {
            result.nestHostClass = decideOnReplaceName(originalCn.nestHostClass);
        }
        if (originalCn.nestMembers != null) {
            result.nestMembers = new ArrayList<>();
            for (String ic : originalCn.nestMembers) {
                result.nestMembers.add(addPrefix(ic));
            }
        }
        throwExceptionIfNotEmpty(originalCn.visibleAnnotations);
        throwExceptionIfNotEmpty(originalCn.visibleTypeAnnotations);
        throwExceptionIfNotEmpty(originalCn.outerMethod);
        throwExceptionIfNotEmpty(originalCn.outerClass);
        throwExceptionIfNotEmpty(originalCn.outerMethodDesc);
        throwExceptionIfNotEmpty(originalCn.permittedSubclasses);

        // Adjust type name of super class and add to queue where needed.
        result.superName = decideOnReplaceName(originalCn.superName);
        result.interfaces = new ArrayList<>();
        for (String i : originalCn.interfaces) {
            result.interfaces.add(decideOnReplaceName(i));
        }
        result.interfaces.add(partnerClassCp);
        result.recordComponents = originalCn.recordComponents;
        result.attrs = originalCn.attrs;
        result.innerClasses = originalCn.innerClasses;
        result.invisibleAnnotations = originalCn.invisibleAnnotations;
        result.invisibleTypeAnnotations = originalCn.invisibleTypeAnnotations;
        result.visibleAnnotations = originalCn.visibleAnnotations;
        result.visibleTypeAnnotations = originalCn.visibleTypeAnnotations;
        result.outerMethod = originalCn.outerMethod;
        result.outerClass = originalCn.outerClass;
        result.outerMethodDesc = originalCn.outerMethodDesc;
        result.permittedSubclasses = originalCn.permittedSubclasses;

        result.fields = new ArrayList<>();
        for (FieldNode fn : originalCn.fields) {
            String resultName = fn.name;
            throwExceptionIfNotEmpty(fn.attrs);
            throwExceptionIfNotEmpty(fn.invisibleAnnotations);
            throwExceptionIfNotEmpty(fn.invisibleTypeAnnotations);
            int access = fn.access;
            String newSignature = calculateSignatureForSarrayIfNecessary(fn.desc);
            result.fields.add(new FieldNode(
                    MulibTransformer._API_VERSION,
                    access,
                    resultName,
                    decideOnReplaceDesc(fn.desc),
                    newSignature,
                    /* value */ null
            ));
        }

        List<MethodNode> resultMethods = new ArrayList<>();
        MethodNode clinitMethodNode = null;
        List<MethodNode> initNodes = new ArrayList<>();
        for (MethodNode mn : originalCn.methods) {
            MethodNode resultingNode = transformMethodNode(mn, result.name, originalCn.name);
            if (resultingNode.name.equals(clinit)) {
                assert clinitMethodNode == null;
                clinitMethodNode = resultingNode;
            }
            if (resultingNode.name.equals(init)) {
                initNodes.add(resultingNode);
            }
            // Additionally, if we have to, e.g., apply concretizations, we now postprocess this intermediate method node.
            resultMethods.add(resultingNode);
        }
        for (MethodNode initNode : initNodes) {
            // We assure that in each init-Method all previously primitive fields are initialized with a
            // Mulib library-type, i.e., they must not be null.
            ensureInitializedLibraryTypeFields(result, initNode);
        }

        if (!Modifier.isInterface(originalCn.access)) {
            // Synthesize additional constructors
            // Constructor for free object-initialization:
            // We do not need to ensure that all previously primitive fields are properly initialized since
            // each field is set to its symbolic value.
            result.methods.add(generateSymbolicExecutionConstructor(originalCn, result));
            // Constructor for translating input objects to the Mulib-representation
            // We do not need to ensure that all previously primitive fields are properly initialized since we get the
            // values from primitive fields which, per default, have the value 0
            result.methods.add(generateTransformationConstructor(originalCn, result));
            // Constructor for copying input objects, transformed by the transformationConstructor
            // We do not need to ensure that all previously primitive fields are properly initialized since this should
            // already hold for the copied object.
            result.methods.add(generateCopyConstructor(originalCn, result));
            // Method for copying called-upon object
            result.methods.add(generateCopyMethod(originalCn, result));
            // Method for labeling original object
            result.methods.add(generateLabelTypeMethod(originalCn, result));
            // Method for returning original type's class
            result.methods.add(generateOriginalClassMethod(originalCn));
            // Method for setting static fields, if any,
            generateOrEnhanceClinit(result, clinitMethodNode);
        }

        result.methods.addAll(resultMethods);

        // Optionally, conduct some checks and write class node to class file
        maybeWriteToFile(result);
        maybeCheckIsValidAsmWrittenClass(result);
        return result;
    }

    private String calculateSignatureForSarrayIfNecessary(String desc) {
        // Check if is array of arrays or array of objects, in both cases, we want to set a parameter
        if (desc.startsWith("[[") || desc.startsWith("[L")) {
            String result = _calculateSignatureForSarrayIfNecessary(desc);
            return result;
        }
        return null;
    }

    private String _calculateSignatureForSarrayIfNecessary(String fieldNodeDesc) {
        // Check if is array of arrays or array of objects, in both cases, we want to set a parameter
        if (fieldNodeDesc.startsWith("[[")) {
            // Is a nested sarray
            return "L" + sarraySarrayCp + "<" + _calculateSignatureForSarrayIfNecessary(fieldNodeDesc.substring(1)) +  ">;";
        } else if (fieldNodeDesc.startsWith("[L")) {
            // Is a PartnerClassSarray
            return "L" + partnerClassSarrayCp + "<" + decideOnReplaceDesc(fieldNodeDesc.substring(1)) + ">;";
        }
        return decideOnReplaceDesc(fieldNodeDesc);
    }

    private void ensureInitializedLibraryTypeFields(ClassNode result, MethodNode initNode) {
        enhanceClinitOrInitWithNullConditionalDefaults(initNode, result.name, result.fields, false);
    }

    // Ensure that all static primitives are initialized to the default element for fields
    private void generateOrEnhanceClinit(ClassNode result, MethodNode clinitMethodNode) {
        if (clinitMethodNode == null) {
            clinitMethodNode = new MethodNode(ACC_PUBLIC | ACC_STATIC, clinit, "()V", null, null);
            result.methods.add(clinitMethodNode);
            clinitMethodNode.instructions.add(new LabelNode()); // start
            InsnList insns = clinitMethodNode.instructions;
            insns.add(new InsnNode(RETURN));
            insns.add(new LabelNode()); // end
        }
        enhanceClinitOrInitWithNullConditionalDefaults(clinitMethodNode, result.name, result.fields, true);
    }

    private void enhanceClinitOrInitWithNullConditionalDefaults(MethodNode clinitOrInit, String owner, List<FieldNode> fieldNodes, boolean forStaticField) {
        Set<AbstractInsnNode> returnNodes = Arrays.stream(clinitOrInit.instructions.toArray())
                .filter(ain -> ain.getOpcode() == RETURN)
                .collect(Collectors.toSet());
        for (AbstractInsnNode returnNode : returnNodes) {
            InsnList inits = nullConditionalDefaultValueInitializations(owner, fieldNodes, forStaticField);
            clinitOrInit.instructions.insertBefore(returnNode, inits);
        }
    }

    private InsnList nullConditionalDefaultValueInitializations(String ownerName, List<FieldNode> fieldNodes, boolean forStaticFields) {
        InsnList result = new InsnList();
        for (FieldNode fn : fieldNodes) {
            if (forStaticFields != Modifier.isStatic(fn.access) || Modifier.isFinal(fn.access)) {
                // If we do not want to set (non-)static fields, we continue with the next field.
                // If the field is final, it must have a value assigned to it already, so we do not have to regard it.
                continue;
            }
            String[] nameAndDescForConcMethod;
            String libraryClassOwner;
            AbstractInsnNode constantPush;
            if (sintDesc.equals(fn.desc)) {
                nameAndDescForConcMethod = getNameAndDescriptorForConcMethodOfSPrimitiveSubclass(WR_INT, false);
                libraryClassOwner = sintCp;
                constantPush = new InsnNode(ICONST_0);
            } else if (slongDesc.equals(fn.desc)) {
                nameAndDescForConcMethod = getNameAndDescriptorForConcMethodOfSPrimitiveSubclass(WR_LONG, false);
                libraryClassOwner = slongCp;
                constantPush = new InsnNode(LCONST_0);
            } else if (sdoubleDesc.equals(fn.desc)) {
                nameAndDescForConcMethod = getNameAndDescriptorForConcMethodOfSPrimitiveSubclass(WR_DOUBLE, false);
                libraryClassOwner = sdoubleCp;
                constantPush = new InsnNode(DCONST_0);
            } else if (sfloatDesc.equals(fn.desc)) {
                nameAndDescForConcMethod = getNameAndDescriptorForConcMethodOfSPrimitiveSubclass(WR_FLOAT, false);
                libraryClassOwner = sfloatCp;
                constantPush = new InsnNode(FCONST_0);
            } else if (sshortDesc.equals(fn.desc)) {
                nameAndDescForConcMethod = getNameAndDescriptorForConcMethodOfSPrimitiveSubclass(WR_SHORT, false);
                libraryClassOwner = sshortCp;
                constantPush = new InsnNode(ICONST_0);
            } else if (sbyteDesc.equals(fn.desc)) {
                nameAndDescForConcMethod = getNameAndDescriptorForConcMethodOfSPrimitiveSubclass(WR_BYTE, false);
                libraryClassOwner = sbyteCp;
                constantPush = new InsnNode(ICONST_0);
            } else if (sboolDesc.equals(fn.desc)) {
                nameAndDescForConcMethod = getNameAndDescriptorForConcMethodOfSPrimitiveSubclass(WR_BOOL, false);
                libraryClassOwner = sboolCp;
                constantPush = new InsnNode(ICONST_0);
            } else {
                continue;
            }
            assert nameAndDescForConcMethod != null;
            if (!forStaticFields) {
                result.add(loadThis());
            }
            result.add(new FieldInsnNode(forStaticFields ? GETSTATIC : GETFIELD, ownerName, fn.name, fn.desc));
            LabelNode afterInitialization = new LabelNode();
            result.add(new JumpInsnNode(IFNONNULL, afterInitialization));
            // Initialize
            if (!forStaticFields) {
                result.add(loadThis());
            }
            result.add(constantPush);
            result.add(new MethodInsnNode(INVOKESTATIC, libraryClassOwner, nameAndDescForConcMethod[0], nameAndDescForConcMethod[1]));
            result.add(new FieldInsnNode(forStaticFields ? PUTSTATIC : PUTFIELD, ownerName, fn.name, fn.desc));
            result.add(afterInitialization);
        }
        return result;
    }

    private MethodNode generateSymbolicExecutionConstructor(ClassNode originalCn, ClassNode result) {
        // Check if is inner class:
        boolean isInnerNonStaticClass = originalCn.nestHostClass != null;
        // Further check whether is not only an inner class, but an inner non-static class:
        String nestHostFieldName = null;
        if (isInnerNonStaticClass) {
            // Determine into which field to set this$X, if any
            nestHostFieldName = determineNestHostFieldName(originalCn.name);
            // If there is no such field, it is a static inner class
            isInnerNonStaticClass = nestHostFieldName != null;
        }

        // Generate <init>(SymbolicExecution)V.
        MethodNode mnInit;
        int seIndexForNewConstructor;
        int hostClassIndex = -1;
        if (isInnerNonStaticClass) {
            seIndexForNewConstructor = 2; // 0=this, 1=this$X, 2=se
            hostClassIndex = 1;
            mnInit = new MethodNode(ACC_PUBLIC, init, toMethodDesc("L" + result.nestHostClass + ";" + seDesc, "V"), null, null);
        } else {
            seIndexForNewConstructor = 1; // 0=this, 1=se
            mnInit = new MethodNode(ACC_PUBLIC, init, toMethodDesc(seDesc, "V"), null, null);
        }
        InsnList mnInsns = mnInit.instructions;
        LabelNode initStart = new LabelNode();
        mnInsns.add(initStart);
        mnInsns.add(loadThis());

        if (isInnerNonStaticClass) {
            // For inner non-static classes, we need to PUTFIELD "this$X"
            mnInsns.add(loadThis()); // this
            mnInsns.add(new VarInsnNode(ALOAD, hostClassIndex)); // this$X, i.e., object of nest host class
            mnInsns.add(new FieldInsnNode(PUTFIELD, result.name, nestHostFieldName, "L" + result.nestHostClass + ";"));
        }

        // Either the super class is Object, Exception, or a transformed class.
        String constructorDescOfSuperclass;
        if (isIgnored(getClassForPath(originalCn.superName))) {
            // TODO We assume an empty constructor for ignored superclasses (e.g. Exception.class and Object.class)
            constructorDescOfSuperclass = toMethodDesc("", "V");
        } else {
            boolean superClassIsInnerNonStatic = determineNestHostFieldName(originalCn.superName) != null;
            if (superClassIsInnerNonStatic) {
                // TODO Rather theoretical issue: if a superclass is an inner class, we need to know the object
                //  it belongs to.
                throw new NotYetImplementedException();
//                    String superClassSuperNestHostClass = "";
//                    constructorDescOfSuperclass = toMethodDesc("L" + superClassSuperNestHostClass + ";" + seDesc, "V");
            } else {
                constructorDescOfSuperclass = toMethodDesc(seDesc, "V");
            }
            // Load se for constructor call of superclass
            mnInsns.add(new VarInsnNode(ALOAD, seIndexForNewConstructor));
        }
        mnInsns.add(new MethodInsnNode(INVOKESPECIAL, decideOnReplaceName(originalCn.superName), init, constructorDescOfSuperclass, false));
        LabelNode beforeReturn = new LabelNode();
        mnInsns.add(new VarInsnNode(ALOAD, seIndexForNewConstructor));
        mnInsns.add(new JumpInsnNode(IFNULL, beforeReturn));
        // Set all fields to symbolic variables, if this constructor is used
        for (FieldNode fn : result.fields) {
            if (Modifier.isStatic(fn.access)) {
                // We do not set the static field each time.
                continue;
            }
            if (fn.name.equals(nestHostFieldName)) {
                continue;
            }
            mnInsns.add(loadThis());
            if (sintDesc.equals(fn.desc)) {
                mnInsns.add(newVirtualSeCall(symSint, toMethodDesc("", sintDesc), seIndexForNewConstructor));
            } else if (sdoubleDesc.equals(fn.desc)) {
                mnInsns.add(newVirtualSeCall(symSdouble, toMethodDesc("", sdoubleDesc), seIndexForNewConstructor));
            } else if (sfloatDesc.equals(fn.desc)) {
                mnInsns.add(newVirtualSeCall(symSfloat, toMethodDesc("", sfloatDesc), seIndexForNewConstructor));
            } else if (sboolDesc.equals(fn.desc)) {
                mnInsns.add(newVirtualSeCall(symSbool, toMethodDesc("", sboolDesc), seIndexForNewConstructor));
            } else if (sshortDesc.equals(fn.desc)) {
                mnInsns.add(newVirtualSeCall(symSshort, toMethodDesc("", sshortDesc), seIndexForNewConstructor));
            } else if (slongDesc.equals(fn.desc)) {
                mnInsns.add(newVirtualSeCall(symSlong, toMethodDesc("", slongDesc), seIndexForNewConstructor));
            } else if (sbyteDesc.equals(fn.desc)) {
                mnInsns.add(newVirtualSeCall(symSbyte, toMethodDesc("", sbyteDesc), seIndexForNewConstructor));
            } else if (fn.desc.charAt(0) == '[') {
                // TODO Add support for symbolic arrays and other types; SymbolicExecution would then be needed.
                mnInsns.add(new InsnNode(ACONST_NULL));
            } else {
                mnInsns.add(new InsnNode(ACONST_NULL));
            }
            mnInsns.add(new FieldInsnNode(PUTFIELD, result.name, fn.name, fn.desc));
        }
        mnInsns.add(beforeReturn);
        mnInsns.add(new InsnNode(RETURN));
        LabelNode initEnd = new LabelNode();
        mnInsns.add(initEnd);
        mnInit.localVariables.add(new LocalVariableNode(thisDesc, "L" + result.name + ";", null, initStart, initEnd, 0));
        return mnInit;
    }


    // <init>(LobjectOfPartnerClass;LMulibValueTransformer;)
    private MethodNode generateCopyConstructor(ClassNode originalCn, ClassNode result) {
        // Check if is inner class:
        boolean isInnerNonStaticClass = originalCn.nestHostClass != null;
        // Further check whether is not only an inner class, but an inner non-static class:
        String nestHostFieldName = null;
        if (isInnerNonStaticClass) {
            // Determine into which field to set this$X, if any
            nestHostFieldName = determineNestHostFieldName(originalCn.name);
            // If there is no such field, it is a static inner class
            isInnerNonStaticClass = nestHostFieldName != null;
        }

        String constructorDesc = toMethodDesc(
                (isInnerNonStaticClass ? "L" + result.nestHostClass + ";" : "") +
                        "L" + result.name + ";"
                        + mulibValueTransformerDesc, "V");
        MethodNode mnInit = new MethodNode(ACC_PUBLIC, init, constructorDesc, null, null);
        LabelNode initStart = new LabelNode();
        LabelNode initEnd = new LabelNode();
        int indexOffset = 0;
        if (isInnerNonStaticClass) {
            indexOffset = 1;
            mnInit.localVariables.add(new LocalVariableNode(nestHostFieldName, "L" + result.nestHostClass + ";",
                    null, initStart, initEnd, 1));
        }
        int toCopyIndex = 1 + indexOffset;
        int valueTransformerIndex = 2 + indexOffset;
        mnInit.localVariables.add(new LocalVariableNode(_TRANSFORMATION_PREFIX + "toCopy", "L" + result.name + ";",
                null, initStart, initEnd, toCopyIndex));
        mnInit.localVariables.add(new LocalVariableNode(_TRANSFORMATION_PREFIX + "valueTransformer", mulibValueTransformerDesc,
                null, initStart, initEnd, valueTransformerIndex));
        InsnList mnInsns = mnInit.instructions;

        mnInsns.add(initStart);
        mnInsns.add(loadThis());

        // Either the super class is Object, Exception, or a transformed class.
        String constructorDescOfSuperclass;
        if (!result.superName.startsWith(_TRANSFORMATION_PREFIX)) {
            constructorDescOfSuperclass = toMethodDesc("", "V");
        } else {
            constructorDescOfSuperclass = toMethodDesc("L" + result.superName + ";" + mulibValueTransformerDesc, "V"); /// TODO account for non-static inner classes in all methods.
            mnInsns.add(loadObjVar(toCopyIndex));
            mnInsns.add(loadObjVar(valueTransformerIndex));
        }

        mnInsns.add(new MethodInsnNode(INVOKESPECIAL, result.superName, init, constructorDescOfSuperclass));

        // Register copy at MulibValueTransformer:
        mnInsns.add(loadObjVar(valueTransformerIndex));
        mnInsns.add(loadObjVar(toCopyIndex));
        mnInsns.add(loadThis());
        mnInsns.add(newVirtualCall("registerCopy", toMethodDesc(objectDesc + objectDesc, "V"), mulibValueTransformerCp));

        for (FieldNode fn : result.fields) {
            if (Modifier.isStatic(fn.access)) {
                // We do not set the static field each time.
                continue;
            }
            mnInsns.add(loadThis());
            LabelNode putfield = new LabelNode();
            if (fn.desc.startsWith("[")) { // TODO Remove as soon as Sarray is used
                // TODO Enhance with proper array-treatment
                mnInsns.add(loadObjVar(toCopyIndex));
                mnInsns.add(new FieldInsnNode(GETFIELD, result.name, fn.name, fn.desc));
            } else if (!getMulibPrimitiveWrapperDescs.contains(fn.desc)) { // Object
                String fieldType = fn.desc.substring(1, fn.desc.length() - 1);
                LabelNode isNull = new LabelNode();
                LabelNode alreadyCreated = new LabelNode();

                mnInsns.add(loadObjVar(toCopyIndex));
                mnInsns.add(new FieldInsnNode(GETFIELD, result.name, fn.name, fn.desc));
                mnInsns.add(new JumpInsnNode(IFNULL, isNull));
                mnInsns.add(loadObjVar(valueTransformerIndex));
                mnInsns.add(loadObjVar(toCopyIndex));
                mnInsns.add(new FieldInsnNode(GETFIELD, result.name, fn.name, fn.desc));
                mnInsns.add(newVirtualCall("alreadyCreated", toMethodDesc(objectDesc, "Z"), mulibValueTransformerCp));
                mnInsns.add(new JumpInsnNode(IFNE, alreadyCreated));

                mnInsns.add(new TypeInsnNode(NEW, fieldType));
                mnInsns.add(new InsnNode(DUP));
                mnInsns.add(loadObjVar(toCopyIndex));
                mnInsns.add(new FieldInsnNode(GETFIELD, result.name, fn.name, fn.desc));
                if (shouldBeTransformed(fieldType.replace(_TRANSFORMATION_PREFIX, ""))) {
                    mnInsns.add(loadObjVar(valueTransformerIndex));
                    mnInsns.add(new MethodInsnNode(INVOKESPECIAL, fieldType, init, toMethodDesc(fn.desc + mulibValueTransformerDesc, "V")));
                } else {
                    mnInsns.add(new MethodInsnNode(INVOKESPECIAL, fieldType, init, toMethodDesc(fn.desc, "V")));
                }
                mnInsns.add(new JumpInsnNode(GOTO, putfield));
                mnInsns.add(isNull);
                mnInsns.add(new InsnNode(ACONST_NULL));
                mnInsns.add(new JumpInsnNode(GOTO, putfield));
                mnInsns.add(alreadyCreated);
                mnInsns.add(loadObjVar(valueTransformerIndex));
                mnInsns.add(loadObjVar(toCopyIndex));
                mnInsns.add(new FieldInsnNode(GETFIELD, result.name, fn.name, fn.desc));
                mnInsns.add(newVirtualCall("getCopy", toMethodDesc(objectDesc, objectDesc), mulibValueTransformerCp));
                mnInsns.add(new TypeInsnNode(CHECKCAST, fn.desc.substring(1, fn.desc.length() - 1)));
            } else {
                mnInsns.add(loadObjVar(toCopyIndex));
                mnInsns.add(new FieldInsnNode(GETFIELD, result.name, fn.name, fn.desc));
            }
            mnInsns.add(putfield);
            mnInsns.add(new FieldInsnNode(PUTFIELD, result.name, fn.name, fn.desc));
        }
        mnInsns.add(new InsnNode(RETURN));
        mnInsns.add(initEnd);
        return mnInit;
    }

    // public Object copy(MulibValueTransformer mulibValueTransformer) { return new T(this, mulibValueTransformer); }
    private MethodNode generateCopyMethod(ClassNode originalCn, ClassNode result) {
        // Check if is inner class:
        boolean isInnerNonStaticClass = originalCn.nestHostClass != null;
        // Further check whether is not only an inner class, but an inner non-static class:
        String nestHostFieldName = null;
        if (isInnerNonStaticClass) {
            // Determine into which field to set this$X, if any
            nestHostFieldName = determineNestHostFieldName(originalCn.name);
            // If there is no such field, it is a static inner class
            isInnerNonStaticClass = nestHostFieldName != null;
        }

        MethodNode copyMethod = new MethodNode(
                ACC_PUBLIC,
                "copy",
                toMethodDesc(mulibValueTransformerDesc, objectDesc),
                null,
                null
        );
        LabelNode start = new LabelNode();
        LabelNode end = new LabelNode();
        int mvtIndex = 1;

        InsnList insns = copyMethod.instructions;
        insns.add(start);
        insns.add(new TypeInsnNode(NEW, result.name));
        insns.add(new InsnNode(DUP));
        if (isInnerNonStaticClass) {
            insns.add(loadThis());
            insns.add(new FieldInsnNode(GETFIELD, result.name, nestHostFieldName, "L" + result.nestHostClass + ";"));
        }
        insns.add(loadThis());
        insns.add(loadObjVar(mvtIndex));
        insns.add(new MethodInsnNode(
                INVOKESPECIAL,
                result.name,
                init,
                toMethodDesc((isInnerNonStaticClass ? "L" + result.nestHostClass + ";" : "") +
                        "L" + result.name + ";"
                        + mulibValueTransformerDesc, "V"),
                false
        ));
        insns.add(new InsnNode(ARETURN));
        insns.add(end);
        return copyMethod;
    }

    private boolean calculateReflectionRequiredForFieldInNonStaticMethod(FieldNode fn) {
        if (Modifier.isStatic(fn.access)) {
            return false;
        }
        if (tryUseSystemClassLoader) {
            // Otherwise, it is in another module and friendly as well as protected fields cannot be accessed
            // without reflection
            return Modifier.isPrivate(fn.access) || Modifier.isFinal(fn.access);
        } else {
            return !Modifier.isPublic(fn.access) || Modifier.isFinal(fn.access);
        }
    }

    // <init>(LobjectOfOriginalClass;LMulibValueTransformer;)
    private MethodNode generateTransformationConstructor(ClassNode originalCn, ClassNode result) {
        // Check if is inner class:
        boolean isInnerNonStaticClass = originalCn.nestHostClass != null;
        // Further check whether is not only an inner class, but an inner non-static class:
        String nestHostFieldName = null;
        if (isInnerNonStaticClass) {
            // Determine into which field to set this$X, if any
            nestHostFieldName = determineNestHostFieldName(originalCn.name);
            // If there is no such field, it is a static inner class
            isInnerNonStaticClass = nestHostFieldName != null;
        }
        boolean reflectionRequired = false;
        for (FieldNode fn : originalCn.fields) {
            if (calculateReflectionRequiredForFieldInNonStaticMethod(fn)) { // We need to set private fields with reflection.
                reflectionRequired = true;
                break;
            }
        }
        // Generate <init>(LOriginalClass;LSymbolicExecution;)V.
        MethodNode mnInit = new MethodNode(ACC_PUBLIC, init, toMethodDesc(
                (isInnerNonStaticClass ? "L" + result.nestHostClass + ";" : "") +
                        "L" + originalCn.name + ";" +
                        mulibValueTransformerDesc, "V"), null, null);
        LabelNode initStart = new LabelNode();
        LabelNode initEnd = new LabelNode();
        int indexOffset = 0;
        if (isInnerNonStaticClass) {
            indexOffset = 1;
            mnInit.localVariables.add(new LocalVariableNode(nestHostFieldName, "L" + result.nestHostClass + ";",
                    null, initStart, initEnd, 1));
        }
        int originalObjectIndexForNewConstructor = 1 + indexOffset;
        int valueTransformerIndex = 2 + indexOffset; // 0 = this, 1 = original, 2 = SymbolicExecution
        // These indexes are only set if reflection is used
        int fieldIndex = -1;
        int fieldValIndex = -1;
        int exceptionIndex = -1;
        int classIndex = -1;
        LabelNode throwExceptionPartStart = null;
        LabelNode throwExceptionPartEnd = null;
        LabelNode throwExceptionHandler = null;
        if (reflectionRequired) {
            classIndex = 3 + indexOffset;
            fieldIndex = 4 + indexOffset;
            fieldValIndex = 5 + indexOffset;
            exceptionIndex = 6 + indexOffset;
            throwExceptionPartStart = new LabelNode();
            throwExceptionPartEnd = new LabelNode();
            throwExceptionHandler = new LabelNode();
            mnInit.localVariables.add(new LocalVariableNode(_TRANSFORMATION_PREFIX + "field",
                    cdescForClass(Field.class), null, initStart, initEnd, fieldIndex));
            mnInit.localVariables.add(new LocalVariableNode(_TRANSFORMATION_PREFIX + "fieldVal",
                    objectDesc, null, initStart, initEnd, fieldValIndex));
            mnInit.tryCatchBlocks.add(new TryCatchBlockNode(throwExceptionPartStart, throwExceptionPartEnd,
                    throwExceptionHandler, cpForClass(ReflectiveOperationException.class)));
        }
        LabelNode beforeReturn = new LabelNode();
        mnInit.localVariables.add(new LocalVariableNode(thisDesc, "L" + result.name + ";", null, initStart, initEnd, 0));
        mnInit.localVariables.add(new LocalVariableNode(_TRANSFORMATION_PREFIX + "originalObject", "L" + originalCn.name + ";", null, initStart, initEnd, originalObjectIndexForNewConstructor));

        InsnList mnInsns = mnInit.instructions;
        mnInsns.add(initStart);
        mnInsns.add(loadThis());

        // Either the super class is Object, Exception, or a transformed class.
        String constructorDescOfSuperclass;
        if (isIgnored(getClassForPath(originalCn.superName))) {
            constructorDescOfSuperclass = toMethodDesc("", "V");
        } else {
            constructorDescOfSuperclass = toMethodDesc("L" + originalCn.superName + ";" + mulibValueTransformerDesc, "V");
            mnInsns.add(loadObjVar(originalObjectIndexForNewConstructor));
            mnInsns.add(loadObjVar(valueTransformerIndex));
        }
        mnInsns.add(new MethodInsnNode(INVOKESPECIAL, decideOnReplaceName(originalCn.superName), init, constructorDescOfSuperclass, false));

        // Register copy at MulibValueTransformer:
        mnInsns.add(loadObjVar(valueTransformerIndex));
        mnInsns.add(loadObjVar(originalObjectIndexForNewConstructor));
        mnInsns.add(loadThis());
        mnInsns.add(newVirtualCall("registerCopy", toMethodDesc(objectDesc + objectDesc, "V"), mulibValueTransformerCp));

        if (reflectionRequired) {
            mnInsns.add(throwExceptionPartStart);
            mnInsns.add(loadObjVar(originalObjectIndexForNewConstructor));
            mnInsns.add(new MethodInsnNode(INVOKEVIRTUAL, objectCp, "getClass", toMethodDesc("", classDesc)));
            mnInsns.add(new VarInsnNode(ASTORE, classIndex));
        }

        for (FieldNode fn : result.fields) {
            if (Modifier.isStatic(fn.access)) {
                // We do not set the static field each time.
                continue;
            }
            boolean reflectionRequiredForField = calculateReflectionRequiredForFieldInNonStaticMethod(fn);
            if (fn.desc.charAt(0) == '[') { // Is array
                mnInsns.add(loadThis());
                mnInsns.add(new InsnNode(ACONST_NULL));
                mnInsns.add(new FieldInsnNode(PUTFIELD, result.name, fn.name, fn.desc));
                // TODO Free arrays
            } else if (!getMulibPrimitiveWrapperDescs.contains(fn.desc)) { // Is object
                String originalDesc = fn.desc.replace(_TRANSFORMATION_PREFIX, "");
                String transformedOwnerName = fn.desc.substring(1, fn.desc.length() - 1);
                LabelNode isNull = new LabelNode();
                LabelNode alreadyCreated = new LabelNode();
                LabelNode putfieldLabel = new LabelNode();
                mnInsns.add(loadThis());

                if (reflectionRequiredForField) {
                    mnInsns.add(loadObjVar(classIndex));
                    mnInsns.add(new LdcInsnNode(fn.name));
                    mnInsns.add(new MethodInsnNode(INVOKEVIRTUAL, classCp, "getDeclaredField", toMethodDesc(stringDesc, cdescForClass(Field.class))));
                    mnInsns.add(new InsnNode(DUP));
                    mnInsns.add(new VarInsnNode(ASTORE, fieldIndex));
                    mnInsns.add(new InsnNode(ICONST_1));
                    mnInsns.add(new MethodInsnNode(INVOKEVIRTUAL, cpForClass(Field.class), "setAccessible", toMethodDesc("Z", "V")));

                    mnInsns.add(loadObjVar(fieldIndex));
                    mnInsns.add(loadObjVar(originalObjectIndexForNewConstructor));
                    mnInsns.add(new MethodInsnNode(INVOKEVIRTUAL, cpForClass(Field.class), "get", toMethodDesc(objectDesc, objectDesc)));
                    mnInsns.add(new InsnNode(DUP));
                    mnInsns.add(new VarInsnNode(ASTORE, fieldValIndex));
                    // Due to DUP we do not need to load anything for the null check.
                } else {
                    mnInsns.add(loadObjVar(originalObjectIndexForNewConstructor));
                    mnInsns.add(new FieldInsnNode(GETFIELD, originalCn.name, fn.name, originalDesc));
                }
                mnInsns.add(new JumpInsnNode(IFNULL, isNull));

                mnInsns.add(loadObjVar(valueTransformerIndex));
                if (reflectionRequiredForField) {
                    mnInsns.add(loadObjVar(fieldValIndex));
                } else {
                    mnInsns.add(loadObjVar(originalObjectIndexForNewConstructor));
                    mnInsns.add(new FieldInsnNode(GETFIELD, originalCn.name, fn.name, originalDesc));
                }
                mnInsns.add(newVirtualCall("alreadyCreated", toMethodDesc(objectDesc, "Z"), mulibValueTransformerCp));
                mnInsns.add(new JumpInsnNode(IFNE, alreadyCreated));

                mnInsns.add(new TypeInsnNode(NEW, transformedOwnerName));
                mnInsns.add(new InsnNode(DUP));
                if (reflectionRequiredForField) {
                    mnInsns.add(loadObjVar(fieldValIndex));
                    mnInsns.add(new TypeInsnNode(Opcodes.CHECKCAST, originalDesc.substring(1, originalDesc.length() - 1)));
                } else {
                    mnInsns.add(loadObjVar(originalObjectIndexForNewConstructor));
                    mnInsns.add(new FieldInsnNode(GETFIELD, originalCn.name, fn.name, originalDesc));
                }
                if (shouldBeTransformed(transformedOwnerName.replace(_TRANSFORMATION_PREFIX, ""))) {
                    mnInsns.add(loadObjVar(valueTransformerIndex));
                    mnInsns.add(new MethodInsnNode(
                            INVOKESPECIAL,
                            transformedOwnerName,
                            init,
                            toMethodDesc(originalDesc + mulibValueTransformerDesc, "V"), false));
                } else {
                    /// TODO copyMethod for non-transformed methods?
                    mnInsns.add(new MethodInsnNode(
                            INVOKESPECIAL,
                            transformedOwnerName,
                            init,
                            // transformedOwnerName here is the usual owner.
                            toMethodDesc("L" + transformedOwnerName + ";", "V"), false
                    ));
                }
                mnInsns.add(new JumpInsnNode(GOTO, putfieldLabel));
                mnInsns.add(isNull);
                mnInsns.add(new InsnNode(ACONST_NULL));
                mnInsns.add(new JumpInsnNode(GOTO, putfieldLabel));
                mnInsns.add(alreadyCreated);
                mnInsns.add(loadObjVar(valueTransformerIndex));
                if (reflectionRequiredForField) {
                    mnInsns.add(loadObjVar(fieldValIndex));
                } else {
                    mnInsns.add(loadObjVar(originalObjectIndexForNewConstructor));
                    mnInsns.add(new FieldInsnNode(GETFIELD, originalCn.name, fn.name, originalDesc));
                }
                mnInsns.add(newVirtualCall("getCopy", toMethodDesc(objectDesc, objectDesc), mulibValueTransformerCp));
                mnInsns.add(new TypeInsnNode(CHECKCAST, fn.desc.substring(1, fn.desc.length() - 1)));
                mnInsns.add(putfieldLabel);
                mnInsns.add(new FieldInsnNode(PUTFIELD, result.name, fn.name, fn.desc));
            } else { // Is primitive
                mnInsns.add(loadThis());
                String[] nameAndDescAndOriginalDesc = getOwnerAndNameAndDescAndOriginalDescOfFieldForConcreteSprimitive(fn);
                if (reflectionRequiredForField) {
                    mnInsns.add(loadObjVar(classIndex));
                    mnInsns.add(new LdcInsnNode(fn.name));
                    mnInsns.add(new MethodInsnNode(INVOKEVIRTUAL, classCp, "getDeclaredField", toMethodDesc(stringDesc, cdescForClass(Field.class))));
                    mnInsns.add(new VarInsnNode(ASTORE, fieldIndex));

                    mnInsns.add(loadObjVar(fieldIndex));
                    mnInsns.add(new InsnNode(ICONST_1));
                    mnInsns.add(new MethodInsnNode(INVOKEVIRTUAL, cpForClass(Field.class), "setAccessible", toMethodDesc("Z", "V")));

                    mnInsns.add(loadObjVar(fieldIndex));
                    mnInsns.add(loadObjVar(originalObjectIndexForNewConstructor));
                    mnInsns.add(new MethodInsnNode(INVOKEVIRTUAL, cpForClass(Field.class), "get", toMethodDesc(objectDesc, objectDesc)));

                    mnInsns.add(newCastNode(getPrimitiveAwareJavaWrapperClass(nameAndDescAndOriginalDesc[3])));
                    MethodInsnNode getPrimitiveValue = getValueFromJavaWrapperMethodNode(nameAndDescAndOriginalDesc[3]);
                    mnInsns.add(getPrimitiveValue);
                    mnInsns.add(newStaticCall(nameAndDescAndOriginalDesc[1], nameAndDescAndOriginalDesc[2], nameAndDescAndOriginalDesc[0]));
                } else {
                    mnInsns.add(loadObjVar(originalObjectIndexForNewConstructor));
                    mnInsns.add(new FieldInsnNode(GETFIELD, originalCn.name, fn.name, nameAndDescAndOriginalDesc[3]));
                    mnInsns.add(newStaticCall(nameAndDescAndOriginalDesc[1], nameAndDescAndOriginalDesc[2], nameAndDescAndOriginalDesc[0]));
                }
                mnInsns.add(new FieldInsnNode(PUTFIELD, result.name, fn.name, fn.desc));
            }
        }
        if (reflectionRequired) {
            mnInsns.add(throwExceptionPartEnd);
            mnInsns.add(new JumpInsnNode(GOTO, beforeReturn));
            mnInsns.add(throwExceptionHandler);
            mnInsns.add(new VarInsnNode(ASTORE, exceptionIndex));
            mnInsns.add(new TypeInsnNode(NEW, cpForClass(MulibRuntimeException.class)));
            mnInsns.add(new InsnNode(DUP));
            mnInsns.add(loadObjVar(exceptionIndex));
            mnInsns.add(new MethodInsnNode(INVOKESPECIAL, cpForClass(MulibRuntimeException.class), init, toMethodDesc(cdescForClass(Exception.class), "V")));
            mnInsns.add(new InsnNode(ATHROW));
        }
        mnInsns.add(beforeReturn);
        mnInsns.add(new InsnNode(RETURN));
        mnInsns.add(initEnd);
        return mnInit;
    }

    private MethodNode generateOriginalClassMethod(ClassNode originalCn) {
        MethodNode getOriginalClassMethod = new MethodNode(
                ACC_PUBLIC,
                "getOriginalClass",
                toMethodDesc("", classDesc),
                null,
                null
        );
        LabelNode start = new LabelNode();
        LabelNode end = new LabelNode();
        getOriginalClassMethod.instructions.add(start);
        getOriginalClassMethod.instructions.add(new LdcInsnNode(Type.getObjectType(originalCn.name)));
        getOriginalClassMethod.instructions.add(new InsnNode(ARETURN));
        getOriginalClassMethod.instructions.add(end);
        return getOriginalClassMethod;
    }

    // Label-method for the type of an object.
    // We assume all symbolic variables have already been substituted with constant values, we also
    // assume that an empty object of the original class has been initialized which is to be filled with values,
    // if necessary, using reflection.
    // label(LObject;LMulibValueTransformer;)LObject;
    private MethodNode generateLabelTypeMethod(ClassNode originalCn, ClassNode result) {
        /// TODO Constructor checks for inner classes!
        // Check if is inner class:
        boolean isInnerNonStaticClass = originalCn.nestHostClass != null;
        // Further check whether is not only an inner class, but an inner non-static class:
        String nestHostFieldName = null;
        if (isInnerNonStaticClass) {
            // Determine into which field to set this$X, if any
            nestHostFieldName = determineNestHostFieldName(originalCn.name);
            // If there is no such field, it is a static inner class
            isInnerNonStaticClass = nestHostFieldName != null;
        }
        boolean reflectionRequired = false;
        for (FieldNode fn : originalCn.fields) {
            if (calculateReflectionRequiredForFieldInNonStaticMethod(fn)) { // We need to set private fields with reflection.
                reflectionRequired = true;
                break;
            }
        }

        MethodNode labelMethod = new MethodNode(
                ACC_PUBLIC,
                "label",
                toMethodDesc(objectDesc + mulibValueTransformerDesc + solverManagerDesc,
                        objectDesc),
                null,
                null
        );
        int nonCastedOriginalObjectIndex = 1;
        int valueTransformerIndex = 2;
        int solverManagerIndex = 3;
        int originalObjectIndex = 4;
        LabelNode start = new LabelNode();
        LabelNode end = new LabelNode();
        // These indexes are only set if reflection is used
        int fieldIndex = -1;
        int fieldValIndex;
        int exceptionIndex = -1;
        int classIndex = -1;
        LabelNode throwExceptionPartStart = null;
        LabelNode throwExceptionPartEnd = null;
        LabelNode throwExceptionHandler = null;
        if (reflectionRequired) {
            classIndex = 5;
            fieldIndex = 6;
            fieldValIndex = 7;
            exceptionIndex = 8;
            throwExceptionPartStart = new LabelNode();
            throwExceptionPartEnd = new LabelNode();
            throwExceptionHandler = new LabelNode();
            labelMethod.localVariables.add(new LocalVariableNode(_TRANSFORMATION_PREFIX + "field",
                    cdescForClass(Field.class), null, start, end, fieldIndex));
            labelMethod.localVariables.add(new LocalVariableNode(_TRANSFORMATION_PREFIX + "fieldVal",
                    objectDesc, null, start, end, fieldValIndex));
            labelMethod.tryCatchBlocks.add(new TryCatchBlockNode(throwExceptionPartStart, throwExceptionPartEnd,
                    throwExceptionHandler, cpForClass(ReflectiveOperationException.class)));
        }
        LabelNode beforeReturn = new LabelNode();
        labelMethod.localVariables.add(new LocalVariableNode(thisDesc, "L" + result.name + ";", null, start, end, 0));
        labelMethod.localVariables.add(new LocalVariableNode(_TRANSFORMATION_PREFIX + "originalObject",
                "L" + originalCn.name + ";", null, start, end, originalObjectIndex));
        labelMethod.localVariables.add(new LocalVariableNode(_TRANSFORMATION_PREFIX + "valueTransformer",
                mulibValueTransformerDesc, null, start, end, valueTransformerIndex));

        InsnList insns = labelMethod.instructions;
        insns.add(start);

        // Cast object to actual type
        insns.add(loadObjVar(nonCastedOriginalObjectIndex));
        insns.add(new TypeInsnNode(CHECKCAST, originalCn.name));
        insns.add(new VarInsnNode(ASTORE, originalObjectIndex));

        if (reflectionRequired) {
            insns.add(throwExceptionPartStart);
            insns.add(loadObjVar(originalObjectIndex));
            insns.add(new MethodInsnNode(INVOKEVIRTUAL, objectCp, "getClass", toMethodDesc("", classDesc)));
            insns.add(new VarInsnNode(ASTORE, classIndex));
        }

        for (FieldNode fn : result.fields)  {
            if (Modifier.isStatic(fn.access)) {
                // We do not set the static field each time.
                continue;
            }
            boolean reflectionRequiredForField = calculateReflectionRequiredForFieldInNonStaticMethod(fn); // Original field is private as well then
            if (reflectionRequiredForField) {
                insns.add(loadObjVar(originalObjectIndex));
                // Get field and set accessible
                insns.add(loadObjVar(classIndex));
                insns.add(new LdcInsnNode(fn.name));
                insns.add(new MethodInsnNode(INVOKEVIRTUAL, classCp, "getDeclaredField", toMethodDesc(stringDesc, cdescForClass(Field.class))));
                insns.add(new InsnNode(DUP));
                insns.add(new VarInsnNode(ASTORE, fieldIndex));
                insns.add(new InsnNode(ICONST_1));
                insns.add(new MethodInsnNode(INVOKEVIRTUAL, cpForClass(Field.class), "setAccessible", toMethodDesc("Z", "V")));
                // Load field and original object so that we can use the GETFIELD as the value in Field.set(obj, value)
                insns.add(loadObjVar(fieldIndex));
            }


            if (fn.desc.startsWith("[")) {
                insns.add(loadObjVar(originalObjectIndex));
                insns.add(new InsnNode(ACONST_NULL));
                insns.add(new FieldInsnNode(PUTFIELD, originalCn.name, fn.name, fn.desc));
                // TODO Free arrays
            } else if (!getMulibPrimitiveWrapperDescs.contains(fn.desc)) { // Is object
                insns.add(loadObjVar(originalObjectIndex));

                insns.add(loadObjVar(valueTransformerIndex));
                insns.add(loadThis());
                insns.add(new FieldInsnNode(GETFIELD, result.name, fn.name, fn.desc));
                insns.add(loadObjVar(solverManagerIndex));
                insns.add(new MethodInsnNode(
                        INVOKEVIRTUAL,
                        mulibValueTransformerCp,
                        "labelValue",
                        toMethodDesc(objectDesc + solverManagerDesc, objectDesc)
                ));

                insns.add(new TypeInsnNode(CHECKCAST, fn.desc.substring(1, fn.desc.length() - 1).replace(_TRANSFORMATION_PREFIX, "")));
                // Set field
                if (reflectionRequiredForField) {
                    // Use the loaded field and the originalObject to set the respective value
                    insns.add(new MethodInsnNode(INVOKEVIRTUAL, cpForClass(Field.class), "set", toMethodDesc(objectDesc + objectDesc, "V")));
                } else {
                    // If no reflection is required, simply set the field to its value
                    insns.add(new FieldInsnNode(PUTFIELD, originalCn.name, fn.name, fn.desc.replace(_TRANSFORMATION_PREFIX, "")));
                }
            } else { // is primitive
                String originalFieldDesc;
                // Attributes used if reflection is used
                String concSCp;
                String concSmethod;
                String setMethodName;
                String castToCp;
                String toValueMethod;
                LabelNode isSym = new LabelNode();
                LabelNode putfield = new LabelNode();
                insns.add(loadObjVar(originalObjectIndex));
                insns.add(loadThis());
                insns.add(new FieldInsnNode(GETFIELD, result.name, fn.name, fn.desc));
                insns.add(new TypeInsnNode(INSTANCEOF, symSprimitiveCp));
                insns.add(new JumpInsnNode(IFNE, isSym));
                insns.add(loadThis());
                insns.add(new FieldInsnNode(GETFIELD, result.name, fn.name, fn.desc));
                if (sintDesc.equals(fn.desc)) {
                    concSCp = concSintCp;
                    concSmethod = "intVal";
                    originalFieldDesc = "I";
                    setMethodName = "setInt";
                    castToCp = integerCp;
                    toValueMethod = "intValue";
                } else if (slongDesc.equals(fn.desc)) {
                    concSCp = concSlongCp;
                    concSmethod = "longVal";
                    originalFieldDesc = "J";
                    setMethodName = "setLong";
                    castToCp = longCp;
                    toValueMethod = "longValue";
                } else if (sdoubleDesc.equals(fn.desc)) {
                    concSCp = concSdoubleCp;
                    concSmethod = "doubleVal";
                    originalFieldDesc = "D";
                    setMethodName = "setDouble";
                    castToCp = doubleCp;
                    toValueMethod = "doubleValue";
                } else if (sfloatDesc.equals(fn.desc)) {
                    concSCp = concSFloatCp;
                    concSmethod = "floatVal";
                    originalFieldDesc = "F";
                    setMethodName = "setFloat";
                    castToCp = floatCp;
                    toValueMethod = "floatValue";
                } else if (sshortDesc.equals(fn.desc)) {
                    concSCp = concSshortCp;
                    concSmethod = "shortVal";
                    originalFieldDesc = "S";
                    setMethodName = "setShort";
                    castToCp = shortCp;
                    toValueMethod = "shortValue";
                } else if (sbyteDesc.equals(fn.desc)) {
                    concSCp = concSbyteCp;
                    concSmethod = "byteVal";
                    originalFieldDesc = "B";
                    setMethodName = "setByte";
                    castToCp = byteCp;
                    toValueMethod = "byteValue";
                } else if (sboolDesc.equals(fn.desc)) {
                    concSCp = concSboolCp;
                    concSmethod = "isTrue";
                    originalFieldDesc = "Z";
                    setMethodName = "setBoolean";
                    castToCp = booleanCp;
                    toValueMethod = "booleanValue";
                } else {
                    throw new NotYetImplementedException();
                }
                // Case non-symbolic
                insns.add(new TypeInsnNode(CHECKCAST, concSCp));
                insns.add(new MethodInsnNode(INVOKEVIRTUAL, concSCp, concSmethod, toMethodDesc("", originalFieldDesc)));
                insns.add(new JumpInsnNode(GOTO, putfield));
                // Case symbolic
                insns.add(isSym);
                insns.add(loadObjVar(valueTransformerIndex));
                insns.add(loadThis());
                insns.add(new FieldInsnNode(GETFIELD, result.name, fn.name, fn.desc));
                insns.add(loadObjVar(solverManagerIndex));
                insns.add(new MethodInsnNode(INVOKEVIRTUAL, mulibValueTransformerCp, "labelPrimitiveValue",
                        toMethodDesc(sprimitiveDesc + solverManagerDesc, objectDesc)));
                insns.add(new TypeInsnNode(CHECKCAST, castToCp));
                insns.add(new MethodInsnNode(INVOKEVIRTUAL, castToCp, toValueMethod, toMethodDesc("", originalFieldDesc)));
                insns.add(putfield);
                if (reflectionRequiredForField) {
                    insns.add(new MethodInsnNode(INVOKEVIRTUAL, cpForClass(Field.class), setMethodName, toMethodDesc(objectDesc + originalFieldDesc, "V")));
                } else {
                    // If no reflection is required, simply set the field to its value
                    insns.add(new FieldInsnNode(PUTFIELD, originalCn.name, fn.name, originalFieldDesc));
                }
            }
        }
        if (reflectionRequired) {
            insns.add(throwExceptionPartEnd);
            insns.add(new JumpInsnNode(GOTO, beforeReturn));
            insns.add(throwExceptionHandler);
            insns.add(new VarInsnNode(ASTORE, exceptionIndex));
            insns.add(new TypeInsnNode(NEW, cpForClass(MulibRuntimeException.class)));
            insns.add(new InsnNode(DUP));
            insns.add(loadObjVar(exceptionIndex));
            insns.add(new MethodInsnNode(INVOKESPECIAL, cpForClass(MulibRuntimeException.class), init, toMethodDesc(cdescForClass(Exception.class), "V")));
            insns.add(new InsnNode(ATHROW));
        }
        insns.add(beforeReturn);
        insns.add(loadObjVar(originalObjectIndex));
        insns.add(new InsnNode(ARETURN));
        insns.add(end);
        return labelMethod;
    }

    private static String[] getOwnerAndNameAndDescAndOriginalDescOfFieldForConcreteSprimitive(FieldNode fn) {
        String[] nameAndDesc;
        String owner;
        String originalDesc;
        if (sintDesc.equals(fn.desc)) {
            nameAndDesc = getNameAndDescriptorForConcMethodOfSPrimitiveSubclass(WR_INT, false);
            originalDesc = "I";
            owner = sintCp;
        } else if (sdoubleDesc.equals(fn.desc)) {
            nameAndDesc = getNameAndDescriptorForConcMethodOfSPrimitiveSubclass(WR_DOUBLE, false);
            originalDesc = "D";
            owner = sdoubleCp;
        } else if (sfloatDesc.equals(fn.desc)) {
            nameAndDesc = getNameAndDescriptorForConcMethodOfSPrimitiveSubclass(WR_FLOAT, false);
            originalDesc = "F";
            owner = sfloatCp;
        } else if (sboolDesc.equals(fn.desc)) {
            nameAndDesc = getNameAndDescriptorForConcMethodOfSPrimitiveSubclass(WR_BOOL, false);
            originalDesc = "Z";
            owner = sboolCp;
        } else if (slongDesc.equals(fn.desc)) {
            nameAndDesc = getNameAndDescriptorForConcMethodOfSPrimitiveSubclass(WR_LONG, false);
            originalDesc = "J";
            owner = slongCp;
        } else if (sshortDesc.equals(fn.desc)) {
            nameAndDesc = getNameAndDescriptorForConcMethodOfSPrimitiveSubclass(WR_SHORT, false);
            originalDesc = "S";
            owner = sshortCp;
        } else if (sbyteDesc.equals(fn.desc)) {
            nameAndDesc = getNameAndDescriptorForConcMethodOfSPrimitiveSubclass(WR_BYTE, false);
            originalDesc = "B";
            owner = sbyteCp;
        } else {
            nameAndDesc = new String[] {
                    init,
                    fn.desc + mulibValueTransformerDesc
            };
            originalDesc = fn.desc.replace(_TRANSFORMATION_PREFIX, "");
            owner = fn.desc.substring(1, fn.desc.length() - 1);
        }
        assert nameAndDesc != null;
        return new String[] { owner, nameAndDesc[0], nameAndDesc[1], originalDesc };
    }

    /* TRANSFORMING METHOD TO POTENTIALLY SYMBOLIC REPRESENTATION */
    // Transform the ASM representation of the original class' method to the partner class' representation thereof.
    private MethodNode transformMethodNode(MethodNode mn, String newOwner, String oldOwner) {
        MethodNode result = new MethodNode(
                _API_VERSION,
                mn.access,
                mn.name, // Constructor as special case
                transformMethodDesc(mn.desc),
                /* signature */null,
                mn.exceptions.toArray(new String[0])
        );
        checkMethodNode(mn);
        TaintAnalyzer ta = new TaintAnalyzer(this, mn, oldOwner);
        TaintAnalysis analysis = ta.analyze();

        /*
        ADD TRY-CATCH INFORMATION TO NEW METHOD
        */
        for (TryCatchBlockNode tc : mn.tryCatchBlocks) {
            if (shouldBeTransformed(tc.type)) {
                decideOnAddToClassesToTransform(tc.type);
            }
            String newType = decideOnReplaceDesc("L" + tc.type + ";");
            tc.type = newType.substring(1, newType.length() - 1);
        }
        result.tryCatchBlocks = mn.tryCatchBlocks;
        /*
        GATHER INFORMATION ON TAINTED LOCAL VARIABLES AND TAINTED INSTRUCTIONS.
        WE ASSUME THAT EACH INPUT-ARGUMENT CAN BE SYMBOLIC, THUS MULIB'S OWN TYPES MUST BE USED.
        */
        // We evaluate which method-local variables must be replaced.
        Set<LocalVariableNode> localsToReplace = analysis.taintedLocalVariables;
        Map<LocalVariableNode, Integer> newLocalVariablesIndices = analysis.newLvnIndices;
        // Replace tainted locals
        result.localVariables = new ArrayList<>();
        if (mn.localVariables == null) {
            mn.localVariables = new ArrayList<>();
        }
        boolean seIsTaken = false;
        for (LocalVariableNode lvn : mn.localVariables) {
            if (lvn.name.equals(seName)) {
                seIsTaken = true;
            }
            if (!primitiveTypes.contains(lvn.desc) || (lvn.desc.length() > 2 && lvn.desc.charAt(0) == '[' && isPrimitive(String.valueOf(lvn.desc.charAt(1))))) { // TODO Proper check for multi-arrays...why would this even be needed?
                String typeOrPath = determineClassSubstringFromDesc(lvn.desc);
                if (shouldBeTransformed(typeOrPath)
                        && !isAlreadyTransformedOrToBeTransformedPath(typeOrPath)) {
                    decideOnAddToClassesToTransform(typeOrPath);
                }
            }

            if (localsToReplace.contains(lvn)) {
                result.localVariables.add(
                        new LocalVariableNode(
                                lvn.name,
                                decideOnReplaceDesc(lvn.desc),
                                calculateSignatureForSarrayIfNecessary(lvn.desc),
                                lvn.start,
                                lvn.end,
                                newLocalVariablesIndices.get(lvn)
                        )
                );
            } else {
                if (lvn.desc.contains(oldOwner)) {
                    lvn.desc = lvn.desc.replace(oldOwner, newOwner);
                }
                lvn.index = newLocalVariablesIndices.get(lvn);
                result.localVariables.add(lvn);
            }
        }
        // Ensure these are not used after SymbolicExecution argument possibly is added. They relied on the current local
        // variable indexes.
        // Add SymbolicExecution argument
        // If there are no arguments for the given method, we must create an appropriate label node
        LabelNode inputStartLabelNode = null;
        LabelNode inputEndLabelNode = null;
        for (AbstractInsnNode ain : mn.instructions) {
            if (inputStartLabelNode == null && ain instanceof LabelNode) {
                inputStartLabelNode = (LabelNode) ain;
            }
            if (ain instanceof LabelNode) {
                inputEndLabelNode = (LabelNode) ain;
            }
        }
        // We conservatively set the index of SymbolicExecution to be the last index of the original method +2 .
        // +2 is necessary since the variable with the last index might be a long or double, in which case two slots would be used
        boolean anyIsTaintedOrWrapped = symbolicExecutionVarIsNeeded(analysis);
        int seIndex = -1;
        if (anyIsTaintedOrWrapped) {
            seIndex = analysis.maxVarIndexInsn + 2;
            assert (inputStartLabelNode != null || mn.instructions.size() == 0);
            if (inputStartLabelNode != null) {
                LabelNode newStartNode = new LabelNode();
                // Add new start node etc.
                result.instructions.add(newStartNode);
                result.instructions.add(new LineNumberNode(((LineNumberNode) inputStartLabelNode.getNext()).line, newStartNode));
                result.instructions.add(getSymbolicExecution());
                result.instructions.add(storeSe(seIndex));
            }
            LocalVariableNode seArgument = new LocalVariableNode(
                    seIsTaken ? _TRANSFORMATION_PREFIX + seName : seName,
                    seDesc,
                    null,
                    inputStartLabelNode,
                    inputEndLabelNode,
                    seIndex
            );
            result.localVariables.add(seArgument);
        }

        // Possibly transformed instructions are added to result
        transformAndAddInstructions(analysis, seIndex, mn, result);

        return result;
    }

    // Check whether we need to introduce the local SymbolicExecution-variable since it is needed for transformed
    // instructions.
    private static boolean symbolicExecutionVarIsNeeded(TaintAnalysis ta) {
        if (ta.returnsBoolean || ta.returnsByte || ta.returnsShort) {
            return true;
        }
        Function<Integer, Boolean> opCheck = (op) -> (op < IRETURN || op > RETURN) && op != ALOAD && op != ASTORE &&
                op != DUP && op != POP && op != GETFIELD && op != PUTFIELD;
        for (AbstractInsnNode ain : ta.instructionsToWrap) {
            int op = ain.getOpcode();
            if (opCheck.apply(op)) {
                return true;
            }
        }
        for (AbstractInsnNode ain : ta.taintedInstructions) {
            int op = ain.getOpcode();
            if (opCheck.apply(op)) {
                return true;
            }
        }
        return false;
    }

    private static String transformToSarrayDesc(String originalArDesc) {
        assert originalArDesc.startsWith("[");
        if (originalArDesc.startsWith("[[")) {
            return sarraySarrayCp;
        }
        if (originalArDesc.startsWith("[L")) {
            return partnerClassSarrayCp;
        }
        if (originalArDesc.startsWith("[I")) {
            return sintSarrayCp;
        }
        if (originalArDesc.startsWith("[J")) {
            return slongSarrayCp;
        }
        if (originalArDesc.startsWith("[D")) {
            return sdoubleSarrayCp;
        }
        if (originalArDesc.startsWith("[F")) {
            return sfloatSarrayCp;
        }
        if (originalArDesc.startsWith("[S")) {
            return sshortSarrayCp;
        }
        if (originalArDesc.startsWith("[B")) {
            return sbyteSarrayCp;
        }
        if (originalArDesc.startsWith("[Z")) {
            return sshortSarrayCp;
        }
        if (originalArDesc.startsWith("[C")) {
            return partnerClassSarrayCp;
        }
        throw new MulibRuntimeException("Array type is not treated: " + originalArDesc);
    }

    /* MAIN TRANSFORMATION LOOP */
    // Replace tainted instructions and wrap to-be-wrapped instructions.
    private void transformAndAddInstructions(
            TaintAnalysis analysis,
            int seIndex,
            MethodNode original,
            MethodNode result) {
        AbstractInsnNode[]  instructions = original.instructions.toArray();
        // We furthermore check which instructions are using these tainted locals and which
        // instructions produce those tainted locals. All of these instructions are tainted as well.
        Set<AbstractInsnNode> taintedInsns = analysis.taintedInstructions;
        // We will later on treat those instructions which serve as input for a tainted instruction.
        Set<AbstractInsnNode> insnsToWrap = analysis.instructionsToWrap;
        Map<AbstractInsnNode, Integer> newIndexInsnIndices = analysis.newIndexInsnIndices;
        // Replace tainted instructions
        // Also extend those instructions which were not produced by tainted values
        for (int i = 0; i < instructions.length; i++) {
            AbstractInsnNode insn = instructions[i];

            /* SPECIAL CASES */
            if (insn instanceof TypeInsnNode) {
                // CHECKCAST is handled as a special case. For generalized methods the outcome might be a transformed type
                // This is the case if, for instance, a java.util.Iterator of an ArrayList is spawned to return elements
                // TODO Handle this via the copy-/lazy-load constructor instead
                TypeInsnNode tin = (TypeInsnNode) insn;
                if (getClassForPath(tin.desc).isArray()) {
                    tin.desc = transformToSarrayDesc(tin.desc);
                } else if (shouldBeTransformed(tin.desc)) { //
                    tin.desc = decideOnReplaceName(tin.desc);
                }
            } else if (insn instanceof VarInsnNode) {
                // Assign new local variable index. Stays the same, if no double/long variables were transformed
                // that have a prior slot place.
                VarInsnNode vin = (VarInsnNode) insn;
                vin.var = newIndexInsnIndices.get(vin);
            } else if (insn instanceof IincInsnNode) {
                IincInsnNode iin = (IincInsnNode) insn;
                iin.var = newIndexInsnIndices.get(iin);
            }
            /* CHECK IF TAINTED */
            if (taintedInsns.contains(insn)) {
                replaceAndAddInstruction(analysis, original.instructions, i, original, result.instructions, insn, seIndex);
            } else {
                /* CHECK IF TO BE WRAPPED */
                if (insnsToWrap.contains(insn)) {
                    wrapInsn(insn, analysis, result.instructions, seIndex);
                } else { // If not to be substituted or to be wrapped, add
                    result.instructions.add(insn);
                }
            }

            /* CHECK IF INSTRUCTION SHOULD BE CONCRETIZED */
            if (analysis.concretizeForMethodCall.containsKey(insn)) {
                // Create method call for SymbolicExecution.concretize(..., se)
                InsnList insns = newStaticSeCall(concretize, concretizeDesc, seIndex);

                String desc = analysis.concretizeForMethodCall.get(insn);
                TypeInsnNode checkCastNode;
                AbstractInsnNode getValueInsn = null;
                switch (desc) {
                    case "I":
                        checkCastNode = newCastNode(Integer.class);
                        getValueInsn = newVirtualCall("intValue", "()I", integerCp);
                        break;
                    case "D":
                        checkCastNode = newCastNode(Double.class);
                        getValueInsn = newVirtualCall("doubleValue", "()D", doubleCp);
                        break;
                    case "F":
                        checkCastNode = newCastNode(Float.class);
                        getValueInsn = newVirtualCall("floatValue", "()F", floatCp);
                        break;
                    case "Z":
                        checkCastNode = newCastNode(Boolean.class);
                        getValueInsn = newVirtualCall("booleanValue", "()Z", booleanCp);
                        break;
                    case "Ljava/lang/String;":
                        checkCastNode = new TypeInsnNode(CHECKCAST, stringCp);
                        break;
                    default:
                        throw new NotYetImplementedException();
                }

                if (getValueInsn != null) {
                    result.instructions.add(insns);
                    result.instructions.add(checkCastNode);
                    result.instructions.add(getValueInsn);
                } else {
                    result.instructions.add(checkCastNode);
                }
            }

            /* CHECK IF INSTRUCTION SHOULD BE GENERALIZED */
            if (analysis.tryToGeneralize.contains(insn)) {
                assert insn instanceof MethodInsnNode;
                MethodInsnNode min = (MethodInsnNode) insn;
                String methodName = min.name;
                Class<?> ownerClass = getClassForPath(min.owner);
                Method[] methods = ownerClass.getDeclaredMethods();
                Method generalization = null;
                for (Method m : methods) {
                    if (m.getName().equals(methodName)) {
                        boolean applicableGeneralization = true;
                        for (Class<?> t : m.getParameterTypes()) {
                            if (t != Object.class) {
                                applicableGeneralization = false;
                                break;
                            }
                        }
                        if (applicableGeneralization) {
                            generalization = m;
                            break;
                        }
                    }
                }
                if (generalization == null) {
                    throw new MulibRuntimeException(
                            "Generalization of method: " + min.owner + "." + min.name + " with desc " + min.desc + " not possible"
                    );
                }
                StringBuilder newDesc = new StringBuilder("(");
                int numInputs = TransformationUtility.getNumInputs(min.desc, true);
                newDesc.append(objectDesc.repeat(numInputs));
                newDesc.append(")");
                newDesc.append(org.objectweb.asm.Type.getReturnType(generalization).getInternalName());
                min.desc = newDesc.toString();
            }
        }
    }

    // Wrap an instruction.
    private void wrapInsn(AbstractInsnNode insn, TaintAnalysis ta, InsnList resultInstrs, int seIndex) {
        // No changes
        if (List.of(ALOAD, ASTORE, AALOAD, AASTORE, ACONST_NULL, DUP).contains(insn.getOpcode())) {
            resultInstrs.add(insn);
            return;
        }

        if (insn instanceof InsnNode) {
            int op = insn.getOpcode();
            if (op <= DCONST_1 && op >= ICONST_M1) {
                wrapInsnNodeCONST(insn, ta, resultInstrs, seIndex);
            } else if (op >= IADD && op <= DNEG) { // Arithmetic
                resultInstrs.add(newConstantAndWrapper(insn, getWrappingTypeForArithInsn(insn), seIndex));
            } else if (op >= I2L && op <= I2S) {
                byte typeToCastTo = getWrappingTypeForCastConversionInsn(insn);
                String[] nameAndDesc = getNameAndDescriptorForConcMethodOfSPrimitiveSubclass(typeToCastTo);
                if (nameAndDesc == null) {
                    throw new MulibRuntimeException("Should not occur");
                }
                resultInstrs.add(insn);
                // Wrap
                resultInstrs.add(newStaticSeCall(nameAndDesc[0], nameAndDesc[1], seIndex));
            } else {
                throw new NotYetImplementedException();
            }
        } else if (insn instanceof IntInsnNode) {
            if (insn.getOpcode() == NEWARRAY) {
                throw new NotYetImplementedException();
//                IntInsnNode iin = (IntInsnNode) insn;
//                AbstractInsnNode last = resultInstrs.getLast();
//                if (last.getOpcode() == ALOAD || last.getOpcode() == GETFIELD) { // TODO Temporary fix until free arrays are implemented; this should not be necessary if we use Sarray
//                    // For now we concretize the Sint which is used for loading.
//                    resultInstrs.add(newStaticSeCall(concretize, concretizeDesc, seIndex));
//                    // We cast the concretized result to an int
//                    resultInstrs.add(newCastNode(Integer.class));
//                    // We retrieve the int value
//                    resultInstrs.add(newVirtualCall("intValue", "()I", integerCp));
//                }
//                switch (iin.operand) {
//                    case T_INT:
//                        resultInstrs.add(new TypeInsnNode(ANEWARRAY, sintCp));
//                        break;
//                    case T_BOOLEAN:
//                        resultInstrs.add(new TypeInsnNode(ANEWARRAY, sboolCp));
//                        break;
//                    default:
//                        throw new NotYetImplementedException();
//                }
            } else {
                wrapInsnNodeCONST(insn, ta, resultInstrs, seIndex);
            }
        } else if (insn instanceof VarInsnNode) {
            if (List.of(ISTORE, LSTORE, DSTORE, FSTORE).contains(insn.getOpcode())) {
                wrapVarInsnNodeSTORE((VarInsnNode) insn, ta, resultInstrs, seIndex);
            } else if (List.of(ILOAD, DLOAD, FLOAD, LLOAD).contains(insn.getOpcode())) {
                wrapVarInsnNodeLOAD((VarInsnNode) insn, ta, resultInstrs, seIndex);
            } else {
                throw new NotYetImplementedException(String.valueOf(insn.getOpcode()));
            }
        } else if (insn instanceof LdcInsnNode) {
            LdcInsnNode ldc = (LdcInsnNode) insn;
            byte typeOfLdcInsn = getWrappingTypeForLdcInsn(ldc);
            InsnList wrapper;
            if (typeOfLdcInsn != WR_TYPE) {
                wrapper = newConstantAndWrapper(insn, typeOfLdcInsn, seIndex);
            } else {
                Type type = (Type) ldc.cst;
                String newTypeDesc = decideOnReplaceDesc(type.getDescriptor());
                Type newType = Type.getObjectType(newTypeDesc.substring(1, newTypeDesc.length() - 1));
                wrapper = new InsnList();
                wrapper.add(new LdcInsnNode(newType));
            }
            resultInstrs.add(wrapper);
        } else if (insn.getOpcode() == NEW) {
            TypeInsnNode typeInsnNode = (TypeInsnNode) insn;
            String descForType = "L" + typeInsnNode.desc + ";";
            String replacementDesc = decideOnReplaceDesc(descForType);
            typeInsnNode.desc = replacementDesc.substring(1, replacementDesc.length() - 1);
            resultInstrs.add(insn);
        } else if (insn.getOpcode() == ANEWARRAY) {
            throw new NotYetImplementedException();
//            TypeInsnNode typeInsnNode = (TypeInsnNode) insn;
//            String descForType = "[L" + typeInsnNode.desc + ";";
//            String replacementDesc = decideOnReplaceDesc(descForType);
//            typeInsnNode.desc = replacementDesc.substring(2, replacementDesc.length() - 1);
//            resultInstrs.add(insn);
        } else if (insn instanceof MethodInsnNode) {
            MethodInsnNode min = (MethodInsnNode) insn;
            if (shouldBeTransformed(min.owner)) {
                min.owner = decideOnReplaceDesc(min.owner);
                min.desc = transformMethodDesc(min.desc);
                resultInstrs.add(min);
            } else {
                resultInstrs.add(newConstantAndWrapper(min, getWrappingTypeForMethodInsn((MethodInsnNode) insn), seIndex));
            }
        } else if (insn.getOpcode() == INVOKEDYNAMIC) {
            InvokeDynamicInsnNode idin = (InvokeDynamicInsnNode) insn;
            if (!idin.bsm.getOwner().equals("java/lang/invoke/StringConcatFactory")) { // TODO Should be relatively easy; if owner is to be replaced: assume that method was replaced and add this method
                throw new NotYetImplementedException("Support for lambda replacements have not yet been implemented.");
            }
            resultInstrs.add(insn);
        } else if (insn instanceof  FieldInsnNode) {
            FieldInsnNode fin = (FieldInsnNode) insn;
            if (shouldBeTransformed(fin.owner)) {
                fin.owner = decideOnReplaceName(fin.owner);
                fin.desc = decideOnReplaceDesc(fin.desc);
            }
            resultInstrs.add(fin);
        } else if (insn instanceof TypeInsnNode) {
            TypeInsnNode tin = (TypeInsnNode) insn;
            assert tin.desc.contains(_TRANSFORMATION_PREFIX);
            if (tin.getOpcode() == INSTANCEOF) {
                resultInstrs.add(new LdcInsnNode(Type.getObjectType(tin.desc.substring(1, tin.desc.length() - 1))));
                resultInstrs.add(loadObjVar(seIndex));
                resultInstrs.add(new MethodInsnNode(
                        INVOKESTATIC, seCp, "evalInstanceof", toMethodDesc(partnerClassDesc + classDesc, sboolDesc)
                ));
            } else {
                resultInstrs.add(tin);
            }
        } else {
            throw new NotYetImplementedException();
        }
    }

    // Wrap load instructions by creating a respective ConcXYZ object
    private void wrapVarInsnNodeLOAD(VarInsnNode insn, TaintAnalysis ta, InsnList resultInstrs, int seIndex) {
        byte wrappingType = getWrappingTypeForLoadInsn(insn, ta);
        resultInstrs.add(newConstantAndWrapper(insn, wrappingType, seIndex));
    }

    // Wrap XYZCONST opcode instructions.
    private void wrapInsnNodeCONST(AbstractInsnNode insn, TaintAnalysis ta, InsnList resultInstrs, int seIndex) {
        byte wrappingType = getWrappingTypeForConstInsn(insn, ta);
        resultInstrs.add(newConstantAndWrapper(insn, wrappingType, seIndex));
    }

    // Wrap store instruction. We have to account for, e.g., booleans, since various types are written into using ISTORE.
    private void wrapVarInsnNodeSTORE(VarInsnNode insn, TaintAnalysis ta, InsnList resultInstrs, int seIndex) {
        String[] nameAndDescriptor = getNameAndDescriptorForConcMethodOfSPrimitiveSubclass(getWrappingTypeForStoreInsn(insn, ta));
        resultInstrs.add(newStaticSeCall(nameAndDescriptor[0], nameAndDescriptor[1], seIndex));
        resultInstrs.add(new VarInsnNode(ASTORE, insn.var));
    }

    // Replace tainted instructions by a new (set of) instruction(s).
    private void replaceAndAddInstruction(
            TaintAnalysis ta,
            InsnList oldInstructions,
            int currentIndex,
            MethodNode mn,
            InsnList resultInstrs,
            AbstractInsnNode insn,
            int seIndex) {
        int op = insn.getOpcode();
        if (insn instanceof VarInsnNode) {
            VarInsnNode vin = (VarInsnNode) insn;
            switch (vin.getOpcode()) {
                case RET:
                case ASTORE:
                case ALOAD:
                    resultInstrs.add(vin);
                    return;
                case LLOAD:
                case FLOAD:
                case DLOAD:
                case ILOAD:
                    resultInstrs.add(new VarInsnNode(ALOAD, vin.var));
                    return;
                case LSTORE:
                case FSTORE:
                case DSTORE:
                case ISTORE:
                    resultInstrs.add(new VarInsnNode(ASTORE, vin.var));
                    return;
                default:
                    throw new NotYetImplementedException(String.valueOf(op));
            }
        } else if (insn instanceof MethodInsnNode) {
            MethodInsnNode min = (MethodInsnNode) insn;
            switch (min.getOpcode()) {
                case INVOKESPECIAL:
                case INVOKEVIRTUAL:
                case INVOKEINTERFACE:
                case INVOKESTATIC:
                    if (min.getOpcode() == INVOKESTATIC && min.owner.equals(mulibCp)) {
                        String methodName;
                        String methodDesc;
                        /// TODO Insert support for Mulib.freeArray(int)
                        boolean initializeFreeArray = false;
                        switch (min.name) {
                            case freeInt:
                                methodName = symSint;
                                methodDesc = toMethodDesc(seDesc, sintDesc);
                                break;
                            case freeDouble:
                                methodName = symSdouble;
                                methodDesc = toMethodDesc(seDesc, sdoubleDesc);
                                break;
                            case freeFloat:
                                methodName = symSfloat;
                                methodDesc = toMethodDesc(seDesc, sfloatDesc);
                                break;
                            case freeBoolean:
                                methodName = symSbool;
                                methodDesc = toMethodDesc(seDesc, sboolDesc);
                                break;
                            case namedFreeInt:
                                methodName = namedSymSint;
                                methodDesc = toMethodDesc(stringDesc+seDesc, sintDesc);
                                break;
                            case namedFreeDouble:
                                methodName = namedSymSdouble;
                                methodDesc = toMethodDesc(stringDesc+seDesc, sdoubleDesc);
                                break;
                            case namedFreeFloat:
                                methodName = namedSymSfloat;
                                methodDesc = toMethodDesc(stringDesc+seDesc, sfloatDesc);
                                break;
                            case namedFreeBoolean:
                                methodName = namedSymSbool;
                                methodDesc = toMethodDesc(stringDesc+seDesc, sboolDesc);
                                break;
                            case freeLong:
                                methodName = symSlong;
                                methodDesc = toMethodDesc(seDesc, slongDesc);
                                break;
                            case freeShort:
                                methodName = symSshort;
                                methodDesc = toMethodDesc(seDesc, sshortDesc);
                                break;
                            case freeByte:
                                methodName = symSbyte;
                                methodDesc = toMethodDesc(seDesc, sbyteDesc);
                                break;
                            case namedFreeLong:
                                methodName = namedSymSlong;
                                methodDesc = toMethodDesc(stringDesc+seDesc, slongDesc);
                                break;
                            case namedFreeShort:
                                methodName = namedSymSshort;
                                methodDesc = toMethodDesc(stringDesc+seDesc, sshortDesc);
                                break;
                            case namedFreeByte:
                                methodName = namedSymSbyte;
                                methodDesc = toMethodDesc(stringDesc+seDesc, sbyteDesc);
                                break;
                            case freeArray:
                                initializeFreeArray = true;
                                if (ta.taintedNewObjectArrayInsns.contains(min)) {
                                    methodName = partnerClassSarray;
                                    methodDesc = newPartnerClassSarrayDesc;
                                } else {
                                    methodName = sarraySarray;
                                    methodDesc = newSarraySarrayDesc;
                                }
                                break;
                            case freeIntArray:
                                initializeFreeArray = true;
                                methodName = sintSarray;
                                methodDesc = newSintSarrayDesc;
                                break;
                            case freeLongArray:
                                initializeFreeArray = true;
                                methodName = slongSarray;
                                methodDesc = newSlongSarrayDesc;
                                break;
                            case freeDoubleArray:
                                initializeFreeArray = true;
                                methodName = sdoubleSarray;
                                methodDesc = newSdoubleSarrayDesc;
                                break;
                            case freeFloatArray:
                                initializeFreeArray = true;
                                methodName = sfloatSarray;
                                methodDesc = newSfloatSarrayDesc;
                                break;
                            case freeShortArray:
                                initializeFreeArray = true;
                                methodName = sshortSarray;
                                methodDesc = newSshortSarrayDesc;
                                break;
                            case freeByteArray:
                                initializeFreeArray = true;
                                methodName = sbyteSarray;
                                methodDesc = newSbyteSarrayDesc;
                                break;
                            case freeBooleanArray:
                                initializeFreeArray = true;
                                methodName = sboolSarray;
                                methodDesc = newSboolSarrayDesc;
                                break;
                            case namedFreeIntArray:
                                initializeFreeArray = true;
                                methodName = namedSintSarray;
                                methodDesc = newNamedSintSarrayDesc;
                                break;
                            case namedFreeLongArray:
                                initializeFreeArray = true;
                                methodName = namedSlongSarray;
                                methodDesc = newNamedSlongSarrayDesc;
                                break;
                            case namedFreeDoubleArray:
                                initializeFreeArray = true;
                                methodName = namedSdoubleSarray;
                                methodDesc = newNamedSdoubleSarrayDesc;
                                break;
                            case namedFreeFloatArray:
                                initializeFreeArray = true;
                                methodName = namedSfloatSarray;
                                methodDesc = newNamedSfloatSarrayDesc;
                                break;
                            case namedFreeShortArray:
                                initializeFreeArray = true;
                                methodName = namedSshortSarray;
                                methodDesc = newNamedSshortSarrayDesc;
                                break;
                            case namedFreeByteArray:
                                initializeFreeArray = true;
                                methodName = namedSbyteSarray;
                                methodDesc = newNamedSbyteSarrayDesc;
                                break;
                            case namedFreeBooleanArray:
                                initializeFreeArray = true;
                                methodName = namedSboolSarray;
                                methodDesc = newNamedSboolSarrayDesc;
                                break;
                            case namedFreeArray:
                                initializeFreeArray = true;
                                if (ta.taintedNewObjectArrayInsns.contains(min)) {
                                    methodName = namedPartnerClassSarray;
                                    methodDesc = newNamedPartnerClassSarrayDesc;
                                } else {
                                    methodName = namedSarraySarray;
                                    methodDesc = newNamedSarraySarrayDesc;
                                }
                                break;
                            default:
                                throw new NotYetImplementedException(min.name);
                        }

                        if (initializeFreeArray) {
                            /// TODO Regard special case where length is set
                            resultInstrs.add(newStaticSeCall(symSint, toMethodDesc(seDesc, sintDesc), seIndex));
                            resultInstrs.add(new InsnNode(ICONST_1)); // true /// TODO Default is symbolic if not set differently
                        }


                        resultInstrs.add(newStaticSeCall(methodName, methodDesc, seIndex));
                        return;
                    }
                    String newMethodDesc = transformMethodDesc(min.desc);
                    MethodInsnNode newmin = new MethodInsnNode(
                            min.getOpcode(),
                            decideOnReplaceName(min.owner),
                            min.name,
                            newMethodDesc,
                            min.itf
                    );
                    resultInstrs.add(newmin);
                    return;
                default:
                    throw new NotYetImplementedException(String.valueOf(min.getOpcode()));
            }
        } else if (insn instanceof LdcInsnNode) {
            resultInstrs.add(newConstantAndWrapper(insn, getWrappingTypeForLdcInsn((LdcInsnNode) insn), seIndex));
        } else if (insn instanceof FieldInsnNode) {
            FieldInsnNode fin = (FieldInsnNode) insn;
            switch (fin.getOpcode()) {
                case GETFIELD:
                case PUTFIELD:
                case GETSTATIC:
                case PUTSTATIC:
                    FieldInsnNode newNode = new FieldInsnNode(
                            fin.getOpcode(),
                            decideOnReplaceName(fin.owner),
                            fin.name,
                            decideOnReplaceDesc(fin.desc)
                    );
                    resultInstrs.add(newNode);
                    return;
                default:
                    throw new NotYetImplementedException();
            }
        } else if (insn instanceof JumpInsnNode) {
            JumpInsnNode jin = (JumpInsnNode) insn;
            replaceJumpInsn(jin, ta, resultInstrs, seIndex);
        } else if (insn instanceof TypeInsnNode) {
            TypeInsnNode tin = (TypeInsnNode) insn;
            assert tin.desc.contains(_TRANSFORMATION_PREFIX) || freeArrayDescs.contains("L" + tin.desc + ";");
            if (tin.getOpcode() == INSTANCEOF) {
                resultInstrs.add(new TypeInsnNode(CHECKCAST, partnerClassCp));
                resultInstrs.add(new LdcInsnNode(Type.getObjectType(tin.desc)));
                resultInstrs.add(loadObjVar(seIndex));
                resultInstrs.add(new MethodInsnNode(
                        INVOKESTATIC, seCp, "evalInstanceof", toMethodDesc(partnerClassDesc + classDesc + seDesc, sboolDesc)
                ));
            } else {
                resultInstrs.add(tin);
            }
        } else if (insn instanceof IntInsnNode) {
            IntInsnNode iin = (IntInsnNode) insn;
            if (op == NEWARRAY) {
                throw new NotYetImplementedException();
//                AbstractInsnNode last = resultInstrs.getLast();
//                if (last.getOpcode() == ALOAD || last.getOpcode() == GETFIELD) { // TODO Temporary fix until free arrays are implemented; this should not be necessary if we use Sarray
//                    // For now we concretize the Sint which is used for loading.
//                    resultInstrs.add(newStaticSeCall(concretize, concretizeDesc, seIndex));
//                    // We cast the concretized result to an int
//                    resultInstrs.add(newCastNode(Integer.class));
//                    // We retrieve the int value
//                    resultInstrs.add(newVirtualCall("intValue", "()I", integerCp));
//                }
//                switch (iin.operand) {
//                    case T_INT:
//                        resultInstrs.add(new TypeInsnNode(ANEWARRAY, sintCp));
//                        break;
//                    case T_BOOLEAN:
//                        resultInstrs.add(new TypeInsnNode(ANEWARRAY, sboolCp));
//                        break;
//                    default:
//                        throw new NotYetImplementedException();
//                }
            } else {
                throw new NotYetImplementedException();
            }
        } else if (insn instanceof InsnNode) {
            InsnNode in = (InsnNode) insn;
            op = insn.getOpcode();
            if (op >= IADD && op <= DNEG) {
                resultInstrs.add(loadObjVar(seIndex));
                if (List.of(IADD, ISUB, IMUL, IDIV, IREM, INEG).contains(op)) {
                    resultInstrs.add(newSeArithOp(op, IADD, ISUB, IMUL, IDIV, IREM, INEG, 
                            singlesintDesc, sintArithDesc, sintCp));
                } else if (List.of(DADD, DSUB, DMUL, DDIV, DREM, DNEG).contains(op)) {
                    resultInstrs.add(newSeArithOp(op, DADD, DSUB, DMUL, DDIV, DREM, DNEG, 
                            singlesdoubleDesc, sdoubleArithDesc, sdoubleCp));
                } else if (List.of(FADD, FSUB, FMUL, FDIV, FREM, FNEG).contains(op)) {
                    resultInstrs.add(newSeArithOp(op, FADD, FSUB, FMUL, FDIV, FREM, FNEG, 
                            singlesfloatDesc, sfloatArithDesc, sfloatCp));
                } else { // Is List.of(LADD, LSUB, LMUL, LDIV, LREM, LNEG).contains(op)
                    resultInstrs.add(newSeArithOp(op, LADD, LSUB, LMUL, LDIV, LREM, LNEG, 
                            singleslongDesc, slongArithDesc, slongCp));
                }
                return;
            }
            if (op >= I2L && op <= I2S) {
                Class<?> clazz = getClassOfWrappingType(getWrappingTypeForCastConversionInsn(insn));
                resultInstrs.add(newSeCastCall(op, seIndex));
                return;
            }
            if (op >= ICONST_M1 && op <= DCONST_1) {
                wrapInsnNodeCONST(in, ta, resultInstrs, seIndex);
                return;
            }
            if (op >= IASTORE && op <= SASTORE) {
                resultInstrs.add(new InsnNode(AASTORE));
                return;
            }
            if (op >= LCMP && op <= DCMPG) {
                String cmpOwner;
                switch (op) { // TODO XCMPL and XCMPG comparison, we do not differentiate between them
                    case LCMP:
                        cmpOwner = slongCp;
                        break;
                    case DCMPG:
                    case DCMPL:
                        cmpOwner = sdoubleCp;
                        break;
                    case FCMPG:
                    case FCMPL:
                        cmpOwner = sfloatCp;
                        break;
                    default:
                        throw new NotYetImplementedException(String.valueOf(op));
                }
                resultInstrs.add(loadObjVar(seIndex));
                resultInstrs.add(newVirtualCall(
                        cmp,
                        toMethodDesc("L" + cmpOwner + ";" + seDesc, sintDesc),
                        cmpOwner)
                );
                return;
            }
            if (op >= IRETURN && op <= ARETURN) {
                if (op == IRETURN) {
                    // Boolean is already treated in that explicitly Sbool is regarded.
                    String returnType = splitMethodDesc(mn.desc)[1];
                    if (returnType.equals("S")) {
                        resultInstrs.add(newSeCastCall(I2S, seIndex));
                    } else if (returnType.equals("B")) {
                        resultInstrs.add(newSeCastCall(I2B, seIndex));
                    }
                }
                resultInstrs.add(new InsnNode(ARETURN));
                return;
            }
            if (op >= IALOAD && op <= SALOAD) {
                AbstractInsnNode loadIndex = resultInstrs.getLast();
                if (loadIndex.getOpcode() == ALOAD) { // TODO Temporary fix until free arrays are implemented; this should not be necessary if we use Sarray
                    // For now we concretize the Sint which is used for loading.
                    resultInstrs.add(newStaticSeCall(concretize, concretizeDesc, seIndex));
                    // We cast the concretized result to an int
                    resultInstrs.add(newCastNode(Integer.class));
                    // We retrieve the int value
                    resultInstrs.add(newVirtualCall("intValue", "()I", integerCp));
                } else if (loadIndex.getOpcode() == ALOAD || loadIndex.getOpcode() == GETFIELD) { // TODO Temporary fix until free arrays are implemented; this should not be necessary if we use Sarray
                    // For now we concretize the Sint which is used for loading.
                    resultInstrs.add(loadObjVar(seIndex));
                    resultInstrs.add(newStaticCall(concretize, concretizeDesc, seCp));
                    // We cast the concretized result to an int
                    resultInstrs.add(newCastNode(Integer.class));
                    // We retrieve the int value
                    resultInstrs.add(newVirtualCall("intValue", "()I", integerCp));
                }
                resultInstrs.add(new InsnNode(AALOAD));
                return;
            }
            
            switch (insn.getOpcode()) {
                case DUP:
                case RETURN:
                case ACONST_NULL:
                case AASTORE:
                case ATHROW:
                case MONITORENTER:
                case MONITOREXIT:
                    resultInstrs.add(insn);
                    return;
                case POP2:
                    resultInstrs.add(new InsnNode(POP));
                    return;
                case ARRAYLENGTH:
                    resultInstrs.add(newConstantAndWrapper(insn, WR_INT, seIndex));
                    return;
                default:
                    throw new NotYetImplementedException(String.valueOf(insn.getOpcode()));
            }
        } else if (insn instanceof TableSwitchInsnNode) {
            TableSwitchInsnNode tsin = (TableSwitchInsnNode) insn;
            if (insn.getOpcode() != TABLESWITCH) {
                throw new NotYetImplementedException();
            }
            throw new NotYetImplementedException();
        } else if (insn instanceof IincInsnNode) {
            IincInsnNode iin = (IincInsnNode) insn;
            resultInstrs.add(newConstantAndWrapper(new IntInsnNode(BIPUSH, iin.incr), WR_INT, seIndex));
            resultInstrs.add(new VarInsnNode(ALOAD, iin.var)); // Adaptation of iin.var happened beforehand

            resultInstrs.add(loadObjVar(seIndex));
            resultInstrs.add(
                    newVirtualCall(
                            add,
                            sintArithDesc,
                            sintCp
                    )
            );
            resultInstrs.add(new VarInsnNode(ASTORE, iin.var)); // Adaptation of iin.var happened beforehand
        } else if (insn instanceof MultiANewArrayInsnNode) {
            throw new NotYetImplementedException();
//            MultiANewArrayInsnNode man = (MultiANewArrayInsnNode) insn;
//            if (insn.getOpcode() != MULTIANEWARRAY) {
//                throw new NotYetImplementedException();
//            }
//            if (man.desc.equals("[[I")) { // TODO Temporary fix until free arrays are allowed
//
//                resultInstrs.add(new MultiANewArrayInsnNode("[[" + sintDesc, 2));
//                return;
//            }
//            throw new NotYetImplementedException();
        } else if (insn instanceof InvokeDynamicInsnNode) {
            InvokeDynamicInsnNode idin = (InvokeDynamicInsnNode) insn;
            if (!idin.bsm.getOwner().equals("java/lang/invoke/StringConcatFactory")) { // TODO Should be relatively easy; if owner is to be replaced: assume that method was replaced and add this method
                throw new NotYetImplementedException("Support for lambda replacements have not yet been implemented.");
            }
            resultInstrs.add(insn);
        } else if (insn instanceof FrameNode) {
            FrameNode fn = (FrameNode) insn;
            throw new NotYetImplementedException();
        } else if (insn instanceof LookupSwitchInsnNode) {
            LookupSwitchInsnNode lsin = (LookupSwitchInsnNode) insn;
            if (insn.getOpcode() != LOOKUPSWITCH) {
                throw new NotYetImplementedException();
            }
//            resultInstrs.add(insn); /// TODO
            throw new NotYetImplementedException();
        } else {
            throw new NotYetImplementedException();
        }
    }

    // Wrap JMP instructions. These instructions could be, for instance, loop-conditions, or if-branch-conditions.
    private void replaceJumpInsn(JumpInsnNode jin, TaintAnalysis analysis, InsnList insnList, int seIndex) {
        String choiceMethodName;
        boolean booleanJmp = !config.TREAT_BOOLEANS_AS_INTS && analysis.taintedBooleanInsns.contains(jin);
        String choiceMethodDescriptor = jin.getOpcode() < IF_ICMPEQ ? singleVarChoiceDesc : itwoNumbersChoiceDesc;
        String owner = sintCp;
        switch (jin.getOpcode()) {
            case IFNE:
            case IF_ICMPNE:
                if (booleanJmp) {
                    if (jin.getOpcode() == IF_ICMPNE) {
                        choiceMethodDescriptor = toMethodDesc(sboolDesc + seDesc, "Z");
                    }
                    choiceMethodName = negatedBoolChoice;
                } else {
                    choiceMethodName = eqChoice;
                }
                break;
            case IFEQ:
            case IF_ICMPEQ:
                if (booleanJmp) {
                    choiceMethodName = boolChoice;
                    if (jin.getOpcode() == IF_ICMPEQ) {
                        choiceMethodDescriptor = toMethodDesc(sboolDesc + seDesc, "Z");
                    }
                } else {
                    choiceMethodName = notEqChoice;
                }
                break;
            case IFLT:
            case IF_ICMPLT:
                choiceMethodName = gteChoice;
                break;
            case IFGE:
            case IF_ICMPGE:
                choiceMethodName = ltChoice;
                break;
            case IFGT:
            case IF_ICMPGT:
                choiceMethodName = lteChoice;
                break;
            case IFLE:
            case IF_ICMPLE:
                choiceMethodName = gtChoice;
                break;
            case IF_ACMPEQ:
            case IF_ACMPNE:
            case IFNULL:
            case IFNONNULL:
                insnList.add(jin);
                return;
            default:
                throw new NotYetImplementedException();
        }
        insnList.add(loadObjVar(seIndex));
        AbstractInsnNode methodCall;
        if (booleanJmp) {
            methodCall = newVirtualCall(
                    choiceMethodName,
                    choiceMethodDescriptor,
                    sboolCp
            );
        } else {
            methodCall =
                    newVirtualCall(
                            choiceMethodName,
                            choiceMethodDescriptor,
                            owner
                    );
        }
        insnList.add(methodCall);
        insnList.add(new JumpInsnNode(IFEQ, jin.label));
    }

    /* PRIVATE METHODS FOR TRANSFORMING PARAMETERS AND TYPES */
    // Decide on whether to replace the given class, as specified by its path, should be replaced
    // with the partner class' name.
    private String decideOnReplaceName(String s) {
        if (shouldBeTransformed(s)) {
            if (!isAlreadyTransformedOrToBeTransformedPath(s)) {
                decideOnAddToClassesToTransform(s);
            }
            return addPrefix(s);
        } else {
            return s;
        }
    }

    private void decideOnAddToClassesToTransform(String path) {
        if (path.startsWith("[")) {
            path = path.substring(path.lastIndexOf('[') + 1);
        }
        if (path.startsWith("L")) {
            assert path.endsWith(";");
            path = path.substring(1, path.length() - 1);
        }
        classesToTransform.add(getClassForPath(path));
    }

    // Decide on the value by means of which we replace the given descriptor.
    private String decideOnReplaceDesc(String currentDesc) {
        char startChar = currentDesc.charAt(0);
        switch (startChar) {
            case 'I': // int
                return sintDesc;
            case 'D': // double
                return sdoubleDesc;
            case 'Z': // boolean
                return sboolDesc;
            case 'J': // long
                return slongDesc;
            case 'F': // float
                return sfloatDesc;
            case 'S': // short
                return sshortDesc;
            case 'B': // byte
                return sbyteDesc;
            case 'C': // char
                throw new NotYetImplementedException();
            case 'V':
                return currentDesc;
            case '[': // array
                char nextChar = currentDesc.charAt(1);
                switch (nextChar) {
                    case 'I': // int
                        return sintSarrayDesc;
                    case 'J': // long
                        return slongSarrayDesc;
                    case 'D': // double
                        return sdoubleSarrayDesc;
                    case 'F': // float
                        return sfloatSarrayDesc;
                    case 'S': // short
                        return sshortSarrayDesc;
                    case 'B': // byte
                        return sbyteSarrayDesc;
                    case 'Z': // boolean
                        return sboolSarrayDesc;
                    case 'L':
                        return partnerClassSarrayDesc;
                    case '[':
                        return sarraySarrayDesc;
                    default:
                        throw new NotYetImplementedException(String.valueOf(nextChar));
                }
            case 'L': // object
                String typeName = currentDesc.substring(1, currentDesc.length() - 1);
                if (shouldBeTransformed(typeName)) {
                    currentDesc = addPrefix(currentDesc);
                }
                return currentDesc;
            default:
                throw new IllegalStateException();
        }
    }

    // Transform a method descriptor.
    private String transformMethodDesc(String mdesc) {
        String[] splitMethodDesc = splitMethodDesc(mdesc);

        return addPrefixToParameterPart(splitMethodDesc[0]) + decideOnReplaceDesc(splitMethodDesc[1]);
    }

    // Adds the prefix to those parts of the parameter where needed.
    private String addPrefixToParameterPart(String parameterPart) {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i < parameterPart.length() - 1; i++) { // Without parentheses
            char firstPosition = parameterPart.charAt(i);
            StringBuilder currentDesc = new StringBuilder(String.valueOf(firstPosition));
            while (firstPosition == '[') {
                i++;
                // Get type of array
                currentDesc.append(parameterPart.charAt(i));
                firstPosition = parameterPart.charAt(i);
            }
            if (currentDesc.toString().contains("L")) {
                int endOfObjectDesc = parameterPart.indexOf(';', i);
                i++;
                currentDesc.append(parameterPart, i, endOfObjectDesc + 1);
                i = endOfObjectDesc;
            }
            sb.append(decideOnReplaceDesc(currentDesc.toString()));
        }

        return "(" + sb + ")";
    }

    /* OPTIONALLY EXECUTED METHODS */
    // Evaluates the validity of the class by initializing an instance and thus using the JVM's on-board measures
    // for validation.
    private void maybeCheckAreValidInitializedClasses(Collection<Class<?>> generatedClasses) {
        if (validate) {
            for (Class<?> generatedClass : generatedClasses) {
                try {
                    if (Modifier.isAbstract(generatedClass.getModifiers())) {
                        continue;
                    }
                    if (determineNestHostFieldName(generatedClass.getName().replace(_TRANSFORMATION_PREFIX, "")) != null) {
                        Class<?> hostFieldClass = generatedClass.getEnclosingClass();
                        generatedClass.getDeclaredConstructor(new Class[]{hostFieldClass, SymbolicExecution.class}).newInstance(null, null);
                    } else {
                        generatedClass.getDeclaredConstructor(new Class[]{SymbolicExecution.class}).newInstance(new Object[]{null});
                    }
                } catch (NoSuchMethodException e) {
                    throw new MulibRuntimeException("Validation of generated class failed due to missing constructor.", e);
                } catch (IllegalAccessException e) {
                    throw new MulibRuntimeException("Validation of generated class failed due "
                            + "to illegal access exception.", e);
                } catch (InstantiationException e) {
                    throw new MulibRuntimeException("Validation of generated class failed due "
                            + "to it being an abstract class.", e);
                } catch (InvocationTargetException e) {
                    throw new MulibRuntimeException("Validation of generated class failed due "
                            + "to the constructor throwing an exception.", e);
                }
            }
        }
    }

    // Evaluates the validity of the class by using the ASM CheckClassAdapter. This does not find "everything"
    // but gives better information on what might have gone wrong.
    private void maybeCheckIsValidAsmWrittenClass(ClassNode classNode) {
        if (validate) {
            // Following the documentation of CheckClassAdapter from here on
            ClassWriter classWriter = new MulibClassWriter(ClassWriter.COMPUTE_FRAMES);
            classNode.accept(classWriter);
            ClassReader classReader = new ClassReader(classWriter.toByteArray());
            ClassVisitor classVisitor = new CheckClassAdapter(classWriter, true);
            Mulib.log.log(Level.INFO, "Validating ClassNode for " + classNode.name);
            classReader.accept(classVisitor, 0);
            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);
            CheckClassAdapter.verify(new ClassReader(classWriter.toByteArray()), true, printWriter);
        }
    }

    // Writes the class to a file to be evaluated using a decompiler.
    private void maybeWriteToFile(ClassNode classNode) {
        if (writeToFile) {
            synchronized (syncObject) {
                String className;
                OutputStream os;
                ClassWriter cw;
                try {
                    className = classNode.name;
                    if (!includePackageName) {
                        className = className.substring(classNode.name.lastIndexOf('/') + 1);
                    }
                    cw = new MulibClassWriter(ClassWriter.COMPUTE_FRAMES);
                    classNode.accept(cw);
                    os = new FileOutputStream(String.format(generatedClassesPathPattern, className));
                    os.write(cw.toByteArray());
                    os.flush();
                    os.close();
                    cw.visitSource(String.format(generatedClassesPathPattern, className), null); // TODO connect to debugger
                } catch (Exception e) {
                    throw new MulibRuntimeException("Class file could not be written to file. Bytecode:\r\n" +
                            getBytecodeForClassNodeMethods(classNode), e);
                }
            }
        }
    }
}