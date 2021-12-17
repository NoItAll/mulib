package de.wwu.mulib.transformer;

import de.wwu.mulib.Mulib;
import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.exceptions.MulibRuntimeException;
import de.wwu.mulib.exceptions.NotYetImplementedException;
import de.wwu.mulib.search.executors.SymbolicExecution;
import de.wwu.mulib.substitutions.primitives.*;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.util.CheckClassAdapter;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.*;
import java.util.logging.Level;

import static de.wwu.mulib.transformer.StringConstants.*;
import static org.objectweb.asm.Opcodes.*;


public class MulibTransformer {
    private static final int _API_VERSION = Opcodes.ASM9;
    public static final String _TRANSFORMATION_PREFIX = "__mulib__";
    @SuppressWarnings("all")
    private final String generatedClassesPath;
    private final String generatedClassesPathPattern;
    private final boolean writeToFile;
    private final boolean validate;
    private final List<String> ignoreFromPackages;
    private final List<Class<?>> ignoreClasses;
    private final List<Class<?>> ignoreSubclassesOf;
    private final List<Class<?>> regardSpecialCase;
    // original class -> class transformed for symbolic execution
    private final Map<String, Class<?>> transformedClasses = new HashMap<>();
    private final Queue<Class<?>> classesToTransform = new ArrayDeque<>();
    private final Set<Class<?>> explicitlyAddedClasses = new HashSet<>();
    private final List<Class<?>> concretizeFor;
    private final List<Class<?>> generalizeMethodCallsFor;

    private static boolean isPrimitive(String desc) {
        return primitiveTypes.contains(desc);
    }

    private MulibClassLoader instance;

    private MulibClassLoader getClassLoaderInstance(ClassLoader parent) {
        if (instance == null) {
            instance = new MulibClassLoader(parent);
        }
        if (instance.getParent() != parent) {
            throw new MulibRuntimeException("The parent class loaders should never be different.");
        }
        return instance;
    }

    private class MulibClassLoader extends ClassLoader {

        private MulibClassLoader(ClassLoader parent) {
            super(parent);
            if (parent == null) {
                throw new MulibRuntimeException("Parent should be defined for a MulibClassLoader.");
            }
        }

        private Class<?> defineClass(String originalName, String name, byte[] classFileBytes) {
            Class<?> result = defineClass(name, classFileBytes, 0, classFileBytes.length);
            transformedClasses.put(originalName, result);
            return result;
        }

        @Override
        public Class<?> loadClass(String name) {
            String classNameWithoutPackage = name.substring(name.lastIndexOf('.') + 1);
            if (!classNameWithoutPackage.startsWith(_TRANSFORMATION_PREFIX)) {
                try {
                    return super.loadClass(name, true);
                } catch (ClassNotFoundException e) {
                    throw new MulibRuntimeException(e);
                }
            }
            String withoutPrefix = name.replace(_TRANSFORMATION_PREFIX, "");
            Class<?> result = transformedClasses.get(withoutPrefix);
            if (result != null) {
                return result;
            }
            ClassNode classNode = transformedClassNodes.get(withoutPrefix);
            ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
            classNode.accept(classWriter);
            result = defineClass(withoutPrefix, classNode.name.replaceAll("\\/", "."), classWriter.toByteArray());
            return result;
        }

        public Class<?> generateClass(ClassNode classNode) {
            return loadClass(classNode.name.replaceAll("/", "\\."));
        }

    }

    public MulibTransformer(MulibConfig config) {
        this.writeToFile = config.TRANSF_WRITE_TO_FILE;
        this.validate = config.TRANSF_VALIDATE_TRANSFORMATION;
        this.generatedClassesPath = config.TRANSF_GENERATED_CLASSES_PATH;
        this.generatedClassesPathPattern = generatedClassesPath + "%s.class";
        this.ignoreFromPackages = config.TRANSF_IGNORE_FROM_PACKAGES;
        this.ignoreClasses = config.TRANSF_IGNORE_CLASSES;
        this.ignoreSubclassesOf = config.TRANSF_IGNORE_SUBCLASSES_OF;
        this.regardSpecialCase = config.TRANSF_REGARD_SPECIAL_CASE;
        this.concretizeFor = config.TRANSF_CONCRETIZE_FOR;
        this.generalizeMethodCallsFor = config.TRANSF_TRY_USE_MORE_GENERAL_METHOD_FOR;
    }

    public final Class<?> getTransformedClass(Class<?> beforeTransformation) {
        Class<?> result = transformedClasses.get(beforeTransformation.getName());
        if (result == null) {
            throw new MulibRuntimeException("Class has not been transformed: " + beforeTransformation);
        }
        return result;
    }

    public final Class<?> getPossiblyTransformedClass(Class<?> beforeTransformation) {
        Class<?> result = transformedClasses.get(beforeTransformation.getName());
        if (result == null) {
            return beforeTransformation;
        }
        return result;
    }

    public void transformAndLoadClasses(Class<?>... toTransform) {
        List<Class<?>> definitelyTransform = Arrays.asList(toTransform);
        explicitlyAddedClasses.addAll(definitelyTransform);
        classesToTransform.addAll(definitelyTransform);

        while (!classesToTransform.isEmpty()) {
            transformClass(classesToTransform.poll());
        }
        defineClasses();
        maybeCheckAreValidInitializedClasses(transformedClasses.values());
    }

    private void defineClasses() {
        for (Map.Entry<String, ClassNode> entry : transformedClassNodes.entrySet()) {
            if (transformedClasses.get(entry.getKey()) != null) {
                continue;
            }
            Class<?> result = getClassLoaderInstance(getClass().getClassLoader()).generateClass(entry.getValue());
            transformedClasses.put(entry.getKey(), result);
        }
    }

    private static final Map<String, ClassNode> transformedClassNodes = new HashMap<>();
    private void transformClass(Class<?> toTransform) {
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

    private static void throwExceptionIfNotEmpty(Object o) {
        if (o instanceof List) {
            if (((List<?>) o).size() != 0) {
                throw new NotYetImplementedException();
            }
        } else if (o != null) {
            throw new NotYetImplementedException();
        }
    }

    private ClassNode transform(ClassNode cn) {
        ClassNode result = new ClassNode(MulibTransformer._API_VERSION);
        result.name = MulibTransformer.addPrefix(cn.name);
        result.access = cn.access;
        result.version = cn.version;
        throwExceptionIfNotEmpty(cn.recordComponents);
        throwExceptionIfNotEmpty(cn.attrs);
        result.innerClasses = new ArrayList<>();
        for (InnerClassNode icn : cn.innerClasses) {
            if (shouldBeTransformed(icn.outerName)) {
                icn.outerName = decideOnReplaceName(icn.outerName);
                icn.innerName = addPrefix(icn.innerName);
                icn.name = decideOnReplaceName(icn.name); // To add to queue if needed
                result.innerClasses.add(icn);
            }
        }
        throwExceptionIfNotEmpty(cn.invisibleAnnotations);
        throwExceptionIfNotEmpty(cn.invisibleTypeAnnotations);
        if (cn.nestHostClass != null) {
            result.nestHostClass = decideOnReplaceName(cn.nestHostClass);
        }
        if (cn.nestMembers != null) {
            result.nestMembers = new ArrayList<>();
            for (String ic : cn.nestMembers) {
                result.nestMembers.add(addPrefix(ic));
            }
        }
        throwExceptionIfNotEmpty(cn.visibleAnnotations);
        throwExceptionIfNotEmpty(cn.visibleTypeAnnotations);
        throwExceptionIfNotEmpty(cn.outerMethod);
        throwExceptionIfNotEmpty(cn.outerClass);
        throwExceptionIfNotEmpty(cn.outerMethodDesc);
        throwExceptionIfNotEmpty(cn.permittedSubclasses);

        // Adjust type name of super class and add to queue where needed.
        result.superName = decideOnReplaceName(cn.superName);
        result.interfaces = new ArrayList<>();
        for (String i : cn.interfaces) {
            result.interfaces.add(decideOnReplaceName(i));
        }
        result.recordComponents = cn.recordComponents;
        result.attrs = cn.attrs;
        result.innerClasses = cn.innerClasses;
        result.invisibleAnnotations = cn.invisibleAnnotations;
        result.invisibleTypeAnnotations = cn.invisibleTypeAnnotations;
        result.visibleAnnotations = cn.visibleAnnotations;
        result.visibleTypeAnnotations = cn.visibleTypeAnnotations;
        result.outerMethod = cn.outerMethod;
        result.outerClass = cn.outerClass;
        result.outerMethodDesc = cn.outerMethodDesc;
        result.permittedSubclasses = cn.permittedSubclasses;

        result.fields = new ArrayList<>();
        for (FieldNode fn : cn.fields) {
            result.fields.add(transformFieldNode(fn));
        }

        List<MethodNode> resultMethods = new ArrayList<>();
        int lastLineNumber = 0;
        for (MethodNode mn : cn.methods) {
            MethodNode intermediateNode = transformMethodNode(mn, result.name, cn.name);
            // Additionally, if we have to, e.g., apply concretizations, we now postprocess this intermediate method node.
            resultMethods.add(intermediateNode);
            for (AbstractInsnNode insn : mn.instructions) {
                if (insn instanceof LineNumberNode) {
                    LineNumberNode lnn = (LineNumberNode) insn;
                    if (lnn.line > lastLineNumber) {
                        lastLineNumber = lnn.line;
                    }
                }
            }
        }
        if (!Modifier.isInterface(cn.access)) {
            // Generate <init>(SymbolicExecution)V. Currently, this is just a dummy. Later on, SymbolicExecution might be added. /// TODO Used later for free objects
            MethodNode mnInit = new MethodNode(ACC_PUBLIC, "<init>", toMethodDesc(seDesc, "V"), null, null);
            LabelNode initStart = new LabelNode();
            mnInit.instructions.add(initStart);
            mnInit.instructions.add(new LineNumberNode(lastLineNumber + 1, initStart));
            mnInit.instructions.add(new VarInsnNode(ALOAD, 0));
            // Either the super class is Object, or a transformed class.
            String constructorDesc;
            if (isIgnored(getClassForPath(cn.superName))) {
                // TODO We assume an empty constructor for ignored superclasses (e.g. Exception.class and Object.class)
                constructorDesc = toMethodDesc("", "V");
            } else {
                constructorDesc = toMethodDesc(seDesc, "V");
                mnInit.instructions.add(new VarInsnNode(ALOAD, 1));
            }
            mnInit.instructions.add(new MethodInsnNode(INVOKESPECIAL, decideOnReplaceName(cn.superName), init, constructorDesc, false));
            LabelNode beforeReturn = new LabelNode();
            mnInit.instructions.add(new VarInsnNode(ALOAD, 1));
            mnInit.instructions.add(new JumpInsnNode(IFNULL, beforeReturn));
            // Set all fields to symbolic variables, if this constructor is used
            for (FieldNode fn : result.fields) {
                VarInsnNode thisNode = new VarInsnNode(ALOAD, 0);
                mnInit.instructions.add(thisNode);
                VarInsnNode seNode = new VarInsnNode(ALOAD, 1);
                mnInit.instructions.add(seNode);
                if (sintDesc.equals(fn.desc)) {
                    mnInit.instructions.add(newVirtualCall(symSint, toMethodDesc("", symSintDesc), seCp));
                } else if (sdoubleDesc.equals(fn.desc)) {
                    mnInit.instructions.add(newVirtualCall(symSdouble, toMethodDesc("", symSdoubleDesc), seCp));
                } else if (sfloatDesc.equals(fn.desc)) {
                    mnInit.instructions.add(newVirtualCall(symSfloat, toMethodDesc("", symSfloatDesc), seCp));
                } else if (sboolDesc.equals(fn.desc)) {
                    mnInit.instructions.add(newVirtualCall(symSbool, toMethodDesc("", symSboolDesc), seCp));
                } else if (fn.desc.charAt(0) == '[') {
                    // TODO Add support for symbolic arrays and other types; SymbolicExecution would then be needed.
                    mnInit.instructions.remove(seNode);
                    mnInit.instructions.add(new InsnNode(ACONST_NULL));
//                mnInit.instructions.add(new InsnNode(ICONST_0));
//                mnInit.instructions.add(new TypeInsnNode(ANEWARRAY, sintCp));
//                throw new NotYetImplementedException();
                } else if (!primitiveTypes.contains(fn.desc)) {
                    mnInit.instructions.remove(seNode);
                    mnInit.instructions.add(new InsnNode(ACONST_NULL));
                } else {
                    throw new NotYetImplementedException();
                }
                mnInit.instructions.add(new FieldInsnNode(PUTFIELD, result.name, fn.name, fn.desc));
            }
            mnInit.instructions.add(beforeReturn);
            mnInit.instructions.add(new InsnNode(RETURN));
            LabelNode initEnd = new LabelNode();
            mnInit.instructions.add(initEnd);
            mnInit.localVariables.add(new LocalVariableNode(thisDesc, "L" + result.name + ";", null, initStart, initEnd, 0));
            // TODO Mechanism for wrapping old object...
            resultMethods.add(mnInit);
        }
        result.methods.addAll(resultMethods);

        // Optionally, conduct some checks and write class node to class file
        maybeWriteToFile(result);
        maybeCheckIsValidAsmWrittenClass(result);
        return result;
    }

    private String decideOnReplaceName(String s) {
        if (shouldBeTransformed(s)) {
            if (!isAlreadyTransformedOrToBeTransformedPath(s)) {
                classesToTransform.add(getClassForPath(s));
            }
            return addPrefix(s);
        } else {
            return s;
        }
    }

    boolean shouldBeConcretizedFor(String owner) {
        Class<?> c = getClassForPath(owner);
        return concretizeFor.contains(c);
    }

    boolean shouldTryToUseGeneralizedMethodCall(String owner) {
        Class<?> c = getClassForPath(owner);
        return generalizeMethodCallsFor.contains(c);
    }

    boolean shouldBeConcretizedFor(Class<?> owner) {
        return concretizeFor.contains(owner);
    }

    boolean shouldTryToUseGeneralizedMethodCall(Class<?> owner) {
        return generalizeMethodCallsFor.contains(owner);
    }

    private void maybeCheckAreValidInitializedClasses(Collection<Class<?>> generatedClasses) {
        if (validate) {
            for (Class<?> generatedClass : generatedClasses) {
                try {
                    if (Modifier.isAbstract(generatedClass.getModifiers())) {
                        continue;
                    }
                    generatedClass.getDeclaredConstructor(new Class[] { SymbolicExecution.class }).newInstance(new Object[] { null });
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

    private void maybeCheckIsValidAsmWrittenClass(ClassNode classNode) {
        if (validate) {
            // Following the documentation of CheckClassAdapter from here on
            ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
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

    private void maybeWriteToFile(ClassNode classNode) {
        if (writeToFile) {
            try {
                String className = classNode.name.substring(classNode.name.lastIndexOf('/') + 1);
                OutputStream os = new FileOutputStream(String.format(generatedClassesPathPattern, className));
                ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
                classNode.accept(cw);
                os.write(cw.toByteArray());
                os.flush();
                os.close();
                cw.visitSource(String.format(generatedClassesPathPattern, className), null); // TODO connect to debugger
            } catch (IOException e) {
                throw new MulibRuntimeException("Class file could not be written to file.", e);
            }
        }
    }

    boolean shouldBeTransformed(String path) {
        Class<?> c = getClassForPath(path);
        return !isIgnored(c);
    }

    private boolean isAlreadyTransformedOrToBeTransformedPath(String path) {
        Class<?> c = getClassForPath(path);
        return classesToTransform.contains(c) // Should not be already enqueued for transformation
                || transformedClasses.containsKey(c.getName()); // Should not already be transformed
    }

    static Class<?> getClassForPath(String path) {
        return getClassForName(classPathToType(path));
    }

    private static Class<?> getClassForName(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException e) {
            throw new MulibRuntimeException("Cannot locate class for String " + name, e);
        }
    }

    private static String classPathToType(String s) {
        return s.replaceAll("/", "\\.");
    }

    private boolean isIgnored(Class<?> toTransform) {
        return shouldBeConcretizedFor(toTransform)
                || shouldTryToUseGeneralizedMethodCall(toTransform)
                || (
                !explicitlyAddedClasses.contains(toTransform)
                        && regardSpecialCase.stream().noneMatch(x -> x.equals(toTransform))
                        && (ignoreFromPackages.stream().anyMatch(x -> toTransform.getPackageName().startsWith(x))
                        || ignoreClasses.stream().anyMatch(x -> x.equals(toTransform))
                        || ignoreSubclassesOf.stream().anyMatch(x -> x.isAssignableFrom(toTransform)))
        );
    }

    private static String addPrefix(String addTo) {
        if (addTo == null) {
            return null;
        }
        int actualNameIndex = addTo.lastIndexOf('/') + 1;
        String packageName = addTo.substring(0, actualNameIndex);
        String actualName = addTo.substring(actualNameIndex);
        String[] innerClassSplit = actualName.split("\\$");
        StringBuilder resultBuilder = new StringBuilder(packageName);
        for (int i = 0; i < innerClassSplit.length; i++) {
            String s = innerClassSplit[i];
            resultBuilder.append(_TRANSFORMATION_PREFIX)
                    .append(s);
            if (i < innerClassSplit.length - 1) {
                resultBuilder.append('$');
            }
        }
        return resultBuilder.toString();
    }

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
                return "[" + decideOnReplaceDesc(currentDesc.substring(1));
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

    public Object replaceValue(Object currentValue) {
        if (currentValue instanceof Integer) {
            return Sint.newConcSint((Integer) currentValue);
        } else if (currentValue instanceof Double) {
            return Sdouble.newConcSdouble((Double) currentValue);
        } else if (currentValue instanceof Boolean) {
            return Sbool.newConcSbool((Boolean) currentValue);
        } else if (currentValue instanceof Long) {
            throw new NotYetImplementedException();
        } else if (currentValue instanceof Float) {
            return Sfloat.newConcSfloat((Float) currentValue);
        } else if (currentValue instanceof Short) {
            throw new NotYetImplementedException();
        } else if (currentValue instanceof Byte) {
            throw new NotYetImplementedException();
        } else if (currentValue instanceof Character) {
            throw new NotYetImplementedException();
        } else if (transformedClasses.containsKey(currentValue.getClass().getName())) {
            try {
                return transformedClasses.get(currentValue.getClass().getName()).getDeclaredConstructor(SymbolicExecution.class).newInstance(new Object[] {null});
            } catch (NoSuchMethodException
                    | IllegalAccessException
                    | InstantiationException
                    | InvocationTargetException e) {
                // Transformed classes should have an empty constructor
                throw new MulibRuntimeException(e);
            }
        } else {
            return currentValue;
        }
    }

    private FieldNode transformFieldNode(FieldNode fn) {
        String resultName = fn.name;
        throwExceptionIfNotEmpty(fn.attrs);
        throwExceptionIfNotEmpty(fn.invisibleAnnotations);
        throwExceptionIfNotEmpty(fn.invisibleTypeAnnotations);
        int access = fn.access;
        return new FieldNode(
                MulibTransformer._API_VERSION,
                access,
                resultName,
                decideOnReplaceDesc(fn.desc),
                /* signature */ null,
                /* value */ null
        );
    }

    private String transformMethodDesc(String mdesc) {
        String[] splitMethodDesc = splitMethodDesc(mdesc);

        return addPrefixToParameterPart(splitMethodDesc[0]) + decideOnReplaceDesc(splitMethodDesc[1]);
    }

    private void _checkMethodNode(MethodNode mn) {
        throwExceptionIfNotEmpty(mn.attrs);
        throwExceptionIfNotEmpty(mn.annotationDefault);
        throwExceptionIfNotEmpty(mn.invisibleAnnotations);
        throwExceptionIfNotEmpty(mn.invisibleTypeAnnotations);
        throwExceptionIfNotEmpty(mn.invisibleLocalVariableAnnotations);
        throwExceptionIfNotEmpty(mn.visibleAnnotations);
        throwExceptionIfNotEmpty(mn.visibleTypeAnnotations);
        throwExceptionIfNotEmpty(mn.visibleLocalVariableAnnotations);
        throwExceptionIfNotEmpty(mn.invisibleParameterAnnotations);
        throwExceptionIfNotEmpty(mn.visibleParameterAnnotations);
        throwExceptionIfNotEmpty(mn.parameters);
    }

    static String determineClassSubstringFromDesc(String localVarDesc) {
        if (localVarDesc.length() <= 1 || (localVarDesc.charAt(0) == '[' && localVarDesc.charAt(1) != 'L')) {
            return localVarDesc;
        }
        int start = localVarDesc.charAt(0) == '[' ? 2 : 1;
        return localVarDesc.substring(start, localVarDesc.length() - 1);
    }

    private MethodNode transformMethodNode(MethodNode mn, String newOwner, String oldOwner) {
        MethodNode result = new MethodNode(
                _API_VERSION,
                mn.access,
                mn.name, // Constructor as special case
                transformMethodDesc(mn.desc),
                /* signature */null,
                mn.exceptions.toArray(new String[0])
        ); _checkMethodNode(mn);
        TaintAnalyzer ta = new TaintAnalyzer(this, mn, oldOwner);
        TaintAnalysis analysis = ta.analyze();

        /* ----------------------------------------------------------------------
        ADD TRY-CATCH INFORMATION TO NEW METHOD
        */
        for (TryCatchBlockNode tc : mn.tryCatchBlocks) {
            if (shouldBeTransformed(tc.type)) {
                this.classesToTransform.add(getClassForPath(tc.type));
            }
            String newType = decideOnReplaceDesc("L" + tc.type + ";");
            tc.type = newType.substring(1, newType.length() - 1);
        }
        result.tryCatchBlocks = mn.tryCatchBlocks;
        /* ----------------------------------------------------------------------
        GATHER INFORMATION ON TAINTED LOCAL VARIABLES AND TAINTED INSTRUCTIONS.
        WE ASSUME THAT EACH INPUT-ARGUMENT CAN BE SYMBOLIC, THUS MULIB'S OWN TYPES MUST BE USED.
        */
        // We evaluate which method-local variables must be replaced.
        Set<LocalVariableNode> localsToReplace = analysis.taintedLocalVariables;
        // We furthermore check which instructions are using these tainted locals and which
        // instructions produce those tainted locals. All of these instructions are tainted as well.
        Set<AbstractInsnNode> taintedInsns = analysis.taintedInstructions;
        // We will later on treat those instructions which serve as input for a
        Set<AbstractInsnNode> insnsToWrap = analysis.instructionsToWrap;
        int[] newLocalVariablesIndices = analysis.newLocalVariablesIndices;
        // Replace tainted locals
        result.localVariables = new ArrayList<>();
        if (mn.localVariables == null) {
            mn.localVariables = new ArrayList<>();
        }
        for (LocalVariableNode lvn : mn.localVariables) {
            if (!primitiveTypes.contains(lvn.desc) || (lvn.desc.length() > 2 && lvn.desc.charAt(0) == '[' && isPrimitive(String.valueOf(lvn.desc.charAt(1))))) { // TODO Proper check for multi-arrays
                String typeOrPath = determineClassSubstringFromDesc(lvn.desc);
                if (shouldBeTransformed(typeOrPath)
                        && !isAlreadyTransformedOrToBeTransformedPath(typeOrPath)) {
                    this.classesToTransform.add(getClassForPath(typeOrPath));
                }
            }

            if (localsToReplace.contains(lvn)) {
                result.localVariables.add(
                        // To avoid constant casting, all new numeric local variables are treated as Snumbers:
                        new LocalVariableNode(
                                lvn.name,
                                decideOnReplaceDesc(lvn.desc),
                                null,
                                lvn.start,
                                lvn.end,
                                newLocalVariablesIndices[lvn.index]
                        )
                );
            } else {
                if (lvn.desc.contains(oldOwner)) {
                    lvn.desc = lvn.desc.replaceAll(oldOwner, newOwner);
                }
                lvn.index = newLocalVariablesIndices[lvn.index];
                result.localVariables.add(lvn);
            }
        }
        // Ensure these are not used after SymbolicExecution argument possibly is added. They relied on the current local
        // variable indexes.
        // Add SymbolicExecution argument
        // If there are no arguments for the givenmethod, we must create an appropriate label node
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
        int seIndex = 0;
        boolean lastWasDoubleSlot = false;
        for (LocalVariableNode lvn : mn.localVariables) {
            if (lvn.index > seIndex) {
                seIndex = lvn.index;
                lastWasDoubleSlot = !analysis.taintedLocalVariables.contains(lvn)
                        && (lvn.desc.equals("D") || lvn.desc.equals("J"));

            }
        }
        seIndex = lastWasDoubleSlot ? seIndex + 2 : seIndex + 1;

        boolean mustStillBeAddedAfterConstructor = false;
        if (mn.instructions.size() > 3
                && mn.instructions.get(0) instanceof LabelNode
                && mn.instructions.get(1) instanceof LineNumberNode
                && mn.instructions.get(2).getOpcode() == ALOAD
                && ((VarInsnNode) mn.instructions.get(2)).var == 0 // For constructor, index 0 must be ALOAD-ed
                && mn.instructions.get(3).getOpcode() == INVOKESPECIAL
                && ((MethodInsnNode) mn.instructions.get(3)).name.equals("<init>")) {
            // Add after constructor-call. This must happen in the main transformation-loop,
            // since we first must add other methods.
            mustStillBeAddedAfterConstructor = true;
        } else if (inputStartLabelNode != null) {
            LabelNode newStartNode = new LabelNode();
            // Add new start node etc.
            result.instructions.add(newStartNode);
            result.instructions.add(new LineNumberNode(((LineNumberNode) inputStartLabelNode.getNext()).line, newStartNode));
            result.instructions.add(getSymbolicExecution());
            result.instructions.add(new VarInsnNode(ASTORE, seIndex));
        }
        LocalVariableNode seArgument = new LocalVariableNode(
                seName,
                seDesc,
                null,
                inputStartLabelNode,
                inputEndLabelNode,
                seIndex
        );
        result.localVariables.add(seArgument);

        AbstractInsnNode[] instructions = mn.instructions.toArray();


        /* MAIN TRANSFORMATION LOOP */

        // Replace tainted instructions
        // Also extend those instructions which were not produced by tainted values
        for (int i = 0; i < instructions.length; i++) {
            AbstractInsnNode insn = instructions[i];

            if (insn.getOpcode() == CHECKCAST) {
                TypeInsnNode tin = (TypeInsnNode) insn;
                if (shouldBeTransformed(tin.desc)) {
                    tin.desc = decideOnReplaceName(tin.desc);
                }
            } else if (insn instanceof VarInsnNode) {
                VarInsnNode vin = (VarInsnNode) insn;
                int newVar = newLocalVariablesIndices[vin.var];
                if (newVar != -1) { // TODO: How generalizable is this? for (Object o : List<Object>) does generate a local var ad-hoc which is not listed in the local vars!
                    vin.var = getLocalVarIndexAccountingForAddedSe(newVar, seIndex);
                }
            } else if (insn instanceof IincInsnNode) {
                IincInsnNode iin = (IincInsnNode) insn;
                int newVar = newLocalVariablesIndices[iin.var];
                if (newVar != -1) {
                    iin.var = getLocalVarIndexAccountingForAddedSe(newVar, seIndex);
                }
            }
            if (taintedInsns.contains(insn)) {
                replaceAndAddInstruction(analysis, mn.instructions, i, mn, result.instructions, insn, seIndex);
            } else {
                if (insnsToWrap.contains(insn)) {
                    wrapInsn(insn, analysis, result.instructions, seIndex);
                } else if (insn.getOpcode() == NEW) {
                    TypeInsnNode typeInsnNode = (TypeInsnNode) insn;
                    String descForType = "L" + typeInsnNode.desc + ";";
                    String replacementDesc = decideOnReplaceDesc(descForType);
                    typeInsnNode.desc = replacementDesc.substring(1, replacementDesc.length() - 1);
                    result.instructions.add(insn);
                } else if (insn.getOpcode() == ANEWARRAY) {
                    TypeInsnNode typeInsnNode = (TypeInsnNode) insn;
                    String descForType = "[L" + typeInsnNode.desc + ";";
                    String replacementDesc = decideOnReplaceDesc(descForType);
                    typeInsnNode.desc = replacementDesc.substring(2, replacementDesc.length() - 1);
                    result.instructions.add(insn);
                } else if (List.of(INVOKEVIRTUAL, INVOKESPECIAL, INVOKESTATIC, INVOKEINTERFACE, INVOKEDYNAMIC).contains(insn.getOpcode())) {
                    MethodInsnNode min = (MethodInsnNode) insn;
                    if (shouldBeTransformed(min.owner)) {
                        min.owner = decideOnReplaceName(min.owner);
                        min.desc = replaceMethodDesc(min.desc);
                        // Name stays the same
                    }
                    result.instructions.add(min);
                } else {
                    result.instructions.add(insn);
                }
            }
            if (mustStillBeAddedAfterConstructor && insn instanceof MethodInsnNode) {
                mustStillBeAddedAfterConstructor = false;
                result.instructions.add(getSymbolicExecution());
                result.instructions.add(new VarInsnNode(ASTORE, seIndex));
            }
            if (analysis.concretizeForMethodCall.containsKey(insn)) {
                // Load SymbolicExecution variable
                VarInsnNode seAload = new VarInsnNode(ALOAD, seIndex);
                // Create method call for SymbolicExecution.concretize(..., se)
                MethodInsnNode min = newStaticCall(concretize, concretizeDesc, seCp);

                String desc = analysis.concretizeForMethodCall.get(insn);
                TypeInsnNode checkCastNode;
                AbstractInsnNode getValueInsn = null;
                switch (desc) {
                    case "I":
                        checkCastNode = new TypeInsnNode(Opcodes.CHECKCAST, org.objectweb.asm.Type.getInternalName(Integer.class));
                        getValueInsn = newVirtualCall("intValue", "()I", integerCp);
                        break;
                    case "D":
                        checkCastNode = new TypeInsnNode(Opcodes.CHECKCAST, org.objectweb.asm.Type.getInternalName(Double.class));
                        getValueInsn = newVirtualCall("doubleValue", "()D", doubleCp);
                        break;
                    case "F":
                        checkCastNode = new TypeInsnNode(Opcodes.CHECKCAST, org.objectweb.asm.Type.getInternalName(Float.class));
                        getValueInsn = newVirtualCall("floatValue", "()F", floatCp);
                        break;
                    case "Z":
                        checkCastNode = new TypeInsnNode(Opcodes.CHECKCAST, org.objectweb.asm.Type.getInternalName(Boolean.class));
                        getValueInsn = newVirtualCall("booleanValue", "()Z", booleanCp);
                        break;
                    case "Ljava/lang/String;":
                        checkCastNode = new TypeInsnNode(Opcodes.CHECKCAST, stringCp);
                        break;
                    default:
                        throw new NotYetImplementedException();
                }


                if (getValueInsn != null) {
                    result.instructions.add(seAload);
                    result.instructions.add(min);
                    result.instructions.add(checkCastNode);
                    result.instructions.add(getValueInsn);
                } else {
                    result.instructions.add(checkCastNode);
                }
            }

            if (analysis.tryToGeneralize.contains(insn)) {
                MethodInsnNode min = (MethodInsnNode) insn;
                String methodName = min.name;
                Class<?> ownerClass = MulibTransformer.getClassForPath(min.owner);
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

        return result;
    }

    private String replaceMethodDesc(String methodDesc) {
        String[] split = splitMethodDesc(methodDesc);
        String params = split[0];
        params = params.substring(1, params.length() - 1);
        String result = split[1];
        String[] paramSplit = params.split(";");
        StringBuilder b = new StringBuilder("(");
        if (!params.equals("")) {
            for (String s : paramSplit) {
                b.append(decideOnReplaceDesc(s + ";"));
            }
        }
        b.append(")");
        params = b.toString();

        result = decideOnReplaceDesc(result);

        return params + result;
    }

    private void wrapInsn(AbstractInsnNode insn, TaintAnalysis ta, InsnList resultInstrs, int seIndex) {
        if (insn instanceof InsnNode) {
            int op = insn.getOpcode();
            if (op <= DCONST_1 && op >= ACONST_NULL) {
                wrapInsnNodeCONST((InsnNode) insn, ta, resultInstrs, seIndex);
            } else if (op >= IADD && op <= DNEG) { // Arithmetic
                wrapVarInsnNodeMulibArithOperation(insn, resultInstrs, seIndex);
            } else if (op == AALOAD) {
                resultInstrs.add(insn);
            } else if (op == DUP) {
                resultInstrs.add(insn);
            } else if (op == I2S) {
                // First wrap value
                resultInstrs.add(new VarInsnNode(ALOAD, seIndex));
                resultInstrs.add(newStaticCall(concSshort, toMethodDesc('S' + seDesc, sshortDesc), seCp));
                // Then perform castTo
                resultInstrs.add(new LdcInsnNode(org.objectweb.asm.Type.getType(Sshort.class)));
                resultInstrs.add(new VarInsnNode(ALOAD, seIndex));
                resultInstrs.add(
                        newInterfaceCall(
                                castTo,
                                toMethodDesc(classDesc + seDesc, sprimitiveDesc),
                                sprimitiveCp
                        )
                );
                resultInstrs.add(new TypeInsnNode(Opcodes.CHECKCAST, org.objectweb.asm.Type.getInternalName(Sshort.class)));
            } else if (op == I2B) {
                // First wrap value
                resultInstrs.add(new VarInsnNode(ALOAD, seIndex));
                resultInstrs.add(newStaticCall(concSbyte, toMethodDesc('B' + seDesc, sbyteDesc), seCp));
                // Then perform castTo
                resultInstrs.add(new LdcInsnNode(org.objectweb.asm.Type.getType(Sbyte.class)));
                resultInstrs.add(new VarInsnNode(ALOAD, seIndex));
                resultInstrs.add(
                        newInterfaceCall(
                                castTo,
                                toMethodDesc(classDesc + seDesc, sprimitiveDesc),
                                sprimitiveCp
                        )
                );
                resultInstrs.add(new TypeInsnNode(Opcodes.CHECKCAST, org.objectweb.asm.Type.getInternalName(Sbyte.class)));
            } else {
                throw new NotYetImplementedException();
            }
        } else if (insn instanceof IntInsnNode) {
            if (insn.getOpcode() == NEWARRAY) {
                IntInsnNode iin = (IntInsnNode) insn;
                AbstractInsnNode last = resultInstrs.getLast();
                if (last.getOpcode() == ALOAD || last.getOpcode() == GETFIELD) { /// TODO Temporary fix until free arrays are implemented; this should not be necessary if we use Sarray
                    // For now we concretize the Sint which is used for loading.
                    resultInstrs.add(new VarInsnNode(ALOAD, seIndex));
                    resultInstrs.add(newStaticCall(concretize, concretizeDesc, seCp));
                    // We cast the concretized result to an int
                    resultInstrs.add(new TypeInsnNode(Opcodes.CHECKCAST, org.objectweb.asm.Type.getInternalName(Integer.class)));
                    // We retrieve the int value
                    resultInstrs.add(newVirtualCall("intValue", "()I", integerCp));
                }
                switch (iin.operand) {
                    case T_INT:
                        resultInstrs.add(new TypeInsnNode(ANEWARRAY, sintCp));
                        break;
                    case T_BOOLEAN:
                        resultInstrs.add(new TypeInsnNode(ANEWARRAY, sboolCp));
                        break;
                    default:
                        throw new NotYetImplementedException();
                }
            } else {
                wrapIntInsnNodeBIPUSH((IntInsnNode) insn, resultInstrs, ta, seIndex);
            }
        } else if (insn instanceof VarInsnNode) {
            if (insn.getOpcode() == ISTORE || insn.getOpcode() == LSTORE) {
                wrapVarInsnNodeSTORE((VarInsnNode) insn, ta, resultInstrs, seIndex);
            } else if (List.of(ILOAD, DLOAD, FLOAD, LLOAD).contains(insn.getOpcode())) {
                wrapVarInsnNodeLOAD((VarInsnNode) insn, ta, resultInstrs, seIndex);
            } else if (insn.getOpcode() == ALOAD) {
                resultInstrs.add(insn);
            } else if (insn.getOpcode() == ASTORE) {
                resultInstrs.add(insn);
            } else {
                throw new NotYetImplementedException();
            }
        } else if (insn instanceof LdcInsnNode) {
            resultInstrs.add(insn);
            resultInstrs.add(new VarInsnNode(ALOAD, seIndex));
            if (((LdcInsnNode) insn).cst instanceof Integer) {
                resultInstrs.add(
                        newStaticCall(
                                concSint,
                                toMethodDesc("I" + seDesc, concSintDesc),
                                seCp
                        )
                );
            } else if (((LdcInsnNode) insn).cst instanceof Double) {
                resultInstrs.add(
                        newStaticCall(
                                concSdouble,
                                toMethodDesc("D" + seDesc, concSdoubleDesc),
                                seCp
                        )
                );
            } else if (((LdcInsnNode) insn).cst instanceof Float) {
                resultInstrs.add(
                        newStaticCall(
                                concSfloat,
                                toMethodDesc("F" + seDesc, concSfloatDesc),
                                seCp
                        )
                );
            } else if (((LdcInsnNode) insn).cst instanceof String) {
                resultInstrs.add(insn);
            } else if (((LdcInsnNode) insn).cst instanceof Long) {
                resultInstrs.add(
                        newStaticCall(
                                concSlong,
                                toMethodDesc("J" + seDesc, concSlongDesc),
                                seCp
                        )
                );
            } else {
                throw new NotYetImplementedException(insn.toString());
            }
        } else if (insn.getOpcode() == NEW) {
            TypeInsnNode typeInsnNode = (TypeInsnNode) insn;
            String descForType = "L" + typeInsnNode.desc + ";";
            String replacementDesc = decideOnReplaceDesc(descForType);
            typeInsnNode.desc = replacementDesc.substring(1, replacementDesc.length() - 1);
            resultInstrs.add(insn);
        } else if (insn.getOpcode() == ANEWARRAY) {
            TypeInsnNode typeInsnNode = (TypeInsnNode) insn;
            String descForType = "[L" + typeInsnNode.desc + ";";
            String replacementDesc = decideOnReplaceDesc(descForType);
            typeInsnNode.desc = replacementDesc.substring(2, replacementDesc.length() - 1);
            resultInstrs.add(insn);
        } else if (insn instanceof MethodInsnNode) { // TODO Unify
            MethodInsnNode min = (MethodInsnNode) insn;
            if (shouldBeTransformed(min.owner)) {
                min.owner = decideOnReplaceName(min.owner);
                min.desc = replaceMethodDesc(min.desc);
                resultInstrs.add(min);
            } else {
                resultInstrs.add(min);
                resultInstrs.add(new VarInsnNode(ALOAD, seIndex));
                String name;
                String descriptor;
                switch (splitMethodDesc(min.desc)[1].charAt(0)) {
                    case 'V':
                        throw new MulibRuntimeException("Should not happen.");
                    case 'I':
                        name = concSint;
                        descriptor = toMethodDesc("I" + seDesc, concSintDesc);
                        break;
                    case 'J':
                    case 'B':
                    case 'S':
                        throw new NotYetImplementedException();
                    case 'D':
                        name = concSdouble;
                        descriptor = toMethodDesc("D" + seDesc, concSdoubleDesc);
                        break;
                    case 'F':
                        name = concSfloat;
                        descriptor = toMethodDesc("F" + seDesc, concSfloatDesc);
                        break;
                    case 'Z':
                    case 'C':
                    case 'L':
                    case '[':
                    default:
                        throw new NotYetImplementedException();
                }
                resultInstrs.add(newStaticCall(name, descriptor, seCp));
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
        } else {
            throw new NotYetImplementedException();
        }
    }

    private void wrapIntInsnNodeBIPUSH(IntInsnNode insn, InsnList resultInstrs, TaintAnalysis ta, int seIndex) {
        resultInstrs.add(insn);
        String[] nameAndDescriptor =
                getNameAndDescriptorForConcMethodOfSPrimitiveSubclass(
                        insn.getOpcode(),
                        ta.instructionsToWrapSinceUsedByBoolInsns.contains(insn),
                        ta.instructionsToWrapSinceUsedByByteInsns.contains(insn),
                        ta.instructionsToWrapSinceUsedByShortInsns.contains(insn)
                );
        newVariableFromSe(
                nameAndDescriptor[0],
                nameAndDescriptor[1],
                resultInstrs,
                seIndex
        );
    }

    private void wrapVarInsnNodeMulibArithOperation(AbstractInsnNode insn, InsnList resultInstrs, int seIndex) {
        resultInstrs.add(insn);
        String name;
        String descriptor;
        switch (insn.getOpcode()) {
            case IADD:
            case ISUB:
            case IMUL:
            case IDIV:
            case IREM:
            case INEG:
                name = concSint;
                descriptor = toMethodDesc("I" + seDesc, concSintDesc);
                break;
            case FADD:
            case FSUB:
            case FMUL:
            case FDIV:
            case FREM:
            case FNEG:
                name = concSfloat;
                descriptor = toMethodDesc("F" + seDesc, concSfloatDesc);
                break;
            case DADD:
            case DSUB:
            case DMUL:
            case DDIV:
            case DREM:
            case DNEG:
                name = concSfloat;
                descriptor = toMethodDesc("D" + seDesc, concSdoubleDesc);
                break;
            case LADD:
            case LSUB:
            case LMUL:
            case LDIV:
            case LREM:
            case LNEG:
                name = concSlong;
                descriptor = toMethodDesc("J" + seDesc, concSlongDesc);
                break;
            default:
                throw new NotYetImplementedException();
        }

        newVariableFromSe(name, descriptor, resultInstrs, seIndex);
    }

    private void wrapVarInsnNodeLOAD(VarInsnNode insn, TaintAnalysis ta, InsnList resultInstrs, int seIndex) {
        resultInstrs.add(insn);
        String[] nameAndDescriptor = getNameAndDescriptorForConcMethodOfSPrimitiveSubclass(
                insn.getOpcode(),
                ta.instructionsToWrapSinceUsedByBoolInsns.contains(insn),
                ta.instructionsToWrapSinceUsedByByteInsns.contains(insn),
                ta.instructionsToWrapSinceUsedByShortInsns.contains(insn)
        );
        newVariableFromSe(
                nameAndDescriptor[0],
                nameAndDescriptor[1],
                resultInstrs,
                seIndex
        );
    }

    private void wrapVarInsnNodeSTORE(VarInsnNode insn, TaintAnalysis ta, InsnList resultInstrs, int seIndex) {
        String[] nameAndDescriptor = getNameAndDescriptorForConcMethodOfSPrimitiveSubclass(
                insn.getOpcode(),
                ta.instructionsToWrapSinceUsedByBoolInsns.contains(insn),
                ta.instructionsToWrapSinceUsedByByteInsns.contains(insn),
                ta.instructionsToWrapSinceUsedByShortInsns.contains(insn)
        );
        newVariableFromSe(
                nameAndDescriptor[0],
                nameAndDescriptor[1],
                resultInstrs,
                seIndex
        );
        resultInstrs.add(new VarInsnNode(Opcodes.ASTORE, insn.var));
    }

    private void wrapInsnNodeCONST(InsnNode insn, TaintAnalysis ta, InsnList resultInstrs, int seIndex) {
        resultInstrs.add(insn);
        if (insn.getOpcode() == ACONST_NULL) {
            resultInstrs.add(insn);
            return;
        }
        String[] nameAndDescriptor = getNameAndDescriptorForConcMethodOfSPrimitiveSubclass(
                insn.getOpcode(),
                ta.instructionsToWrapSinceUsedByBoolInsns.contains(insn),
                ta.instructionsToWrapSinceUsedByByteInsns.contains(insn),
                ta.instructionsToWrapSinceUsedByShortInsns.contains(insn)
        );
        newVariableFromSe(
                nameAndDescriptor[0],
                nameAndDescriptor[1],
                resultInstrs,
                seIndex
        );
    }

    private static String[] getNameAndDescriptorForConcMethodOfSPrimitiveSubclass(int op, boolean isBool, boolean isByte, boolean isShort) {
        String[] result = new String[2];
        if (isBool) {
            result[0] = concSbool;
            result[1] = toMethodDesc("Z" + seDesc, concSboolDesc);
        } else if (isByte) {
            result[0] = concSbyte;
            result[1] = toMethodDesc("B" + seDesc, concSbyteDesc);
        } else if (isShort) {
            result[0] = concSshort;
            result[1] = toMethodDesc("S" + seDesc, concSshortDesc);
        } else if (List.of(ICONST_0,ICONST_1,ICONST_2,ICONST_3,ICONST_4,ICONST_5,BIPUSH, SIPUSH,ILOAD,ISTORE).contains(op)) {
            result[0] = concSint;
            result[1] = toMethodDesc("I" + seDesc, concSintDesc);
        } else if (List.of(DCONST_0,DCONST_1,DLOAD,DSTORE).contains(op)) {
            result[0] = concSdouble;
            result[1] = toMethodDesc("D" + seDesc, concSdoubleDesc);
        } else if (List.of(FCONST_0,FCONST_1,FCONST_2,FLOAD,FSTORE).contains(op)) {
            result[0] = concSfloat;
            result[1] = toMethodDesc("F" + seDesc, concSfloatDesc);
        } else if (List.of(LCONST_0, LCONST_1, LLOAD, LSTORE).contains(op)) {
            result[0] = concSlong;
            result[1] = toMethodDesc("J" + seDesc, concSlongDesc);
        } else {
            throw new NotYetImplementedException();
        }
        return result;
    }

    private int getLocalVarIndexAccountingForAddedSe(int currentIndex, int seIndex) {
        if (seIndex == -1) {
            return currentIndex;
        }
        return currentIndex < seIndex ? currentIndex : (currentIndex + 1);
    }

    private void replaceAndAddInstruction(
            TaintAnalysis ta,
            InsnList oldInstructions,
            int currentIndex,
            MethodNode mn,
            InsnList resultInstrs,
            AbstractInsnNode insn,
            int seIndex) {
        int op = insn.getOpcode();
        if (insn instanceof LabelNode || insn instanceof LineNumberNode){
            throw new IllegalStateException("Should not have to be replaced.");
        } else if (insn instanceof VarInsnNode) {
            VarInsnNode vin = (VarInsnNode) insn;
            switch (vin.getOpcode()) {
                case Opcodes.RET:
                case Opcodes.ASTORE:
                case Opcodes.ALOAD:
                    resultInstrs.add(vin);
                    return;
                case Opcodes.LLOAD:
                case Opcodes.FLOAD:
                case Opcodes.DLOAD:
                case Opcodes.ILOAD:
                    resultInstrs.add(new VarInsnNode(Opcodes.ALOAD, vin.var));
                    return;
                case Opcodes.LSTORE:
                case Opcodes.FSTORE:
                case Opcodes.DSTORE:
                case Opcodes.ISTORE:
                    resultInstrs.add(new VarInsnNode(Opcodes.ASTORE, vin.var));
                    return;
                default:
                    throw new NotYetImplementedException();
            }
        } else if (insn instanceof MethodInsnNode) {
            MethodInsnNode min = (MethodInsnNode) insn;
            switch (min.getOpcode()) {
                case Opcodes.INVOKESPECIAL:
                case Opcodes.INVOKEVIRTUAL:
                case Opcodes.INVOKEINTERFACE:
                case Opcodes.INVOKESTATIC:
                    if (min.getOpcode() == INVOKESTATIC && min.owner.equals(mulibCp)) {
                        String methodName;
                        String methodDesc;
                        switch (min.name) {
                            case freeInt:
                                methodName = symSint;
                                methodDesc = toMethodDesc(seDesc, symSintDesc);
                                break;
                            case freeDouble:
                                methodName = symSdouble;
                                methodDesc = toMethodDesc(seDesc, symSdoubleDesc);
                                break;
                            case freeFloat:
                                methodName = symSfloat;
                                methodDesc = toMethodDesc(seDesc, symSfloatDesc);
                                break;
                            case freeBoolean:
                                methodName = symSbool;
                                methodDesc = toMethodDesc(seDesc, symSboolDesc);
                                break;
                            case trackedFreeInt:
                                methodName = trackedSymSint;
                                methodDesc = toMethodDesc(stringDesc+seDesc, symSintDesc);
                                break;
                            case trackedFreeDouble:
                                methodName = trackedSymSdouble;
                                methodDesc = toMethodDesc(stringDesc+seDesc, symSdoubleDesc);
                                break;
                            case trackedFreeFloat:
                                methodName = trackedSymSfloat;
                                methodDesc = toMethodDesc(stringDesc+seDesc, symSfloatDesc);
                                break;
                            case trackedFreeBoolean:
                                methodName = trackedSymSbool;
                                methodDesc = toMethodDesc(stringDesc+seDesc, symSboolDesc);
                                break;
                            case freeLong:
                                methodName = symSlong;
                                methodDesc = toMethodDesc(seDesc, symSlongDesc);
                                break;
                            case freeShort:
                                methodName = symSshort;
                                methodDesc = toMethodDesc(seDesc, symSshortDesc);
                                break;
                            case freeByte:
                                methodName = symSbyte;
                                methodDesc = toMethodDesc(seDesc, symSbyteDesc);
                                break;
                            case trackedFreeLong:
                                methodName = trackedSymSlong;
                                methodDesc = toMethodDesc(stringDesc+seDesc, symSlongDesc);
                                break;
                            case trackedFreeShort:
                                methodName = trackedSymSshort;
                                methodDesc = toMethodDesc(stringDesc+seDesc, symSshortDesc);
                                break;
                            case trackedFreeByte:
                                methodName = trackedSymSbyte;
                                methodDesc = toMethodDesc(stringDesc+seDesc, symSbyteDesc);
                                break;
                            case trackedFreeChar:
                            case freeChar:
                            default:
                                throw new NotYetImplementedException();
                        }
                        resultInstrs.add(new VarInsnNode(Opcodes.ALOAD, seIndex));
                        MethodInsnNode newmin = new MethodInsnNode(
                                INVOKESTATIC,
                                seCp,
                                methodName,
                                methodDesc,
                                min.itf
                        );
                        resultInstrs.add(newmin);
                        return;
                    }
                    boolean isIgnored = isIgnored(getClassForPath(min.owner));
                    if (isIgnored) {
                        resultInstrs.add(insn);
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
                    throw new NotYetImplementedException();
            }
        } else if (insn instanceof LdcInsnNode) {
            LdcInsnNode lin = (LdcInsnNode) insn;
            if (lin.getOpcode() != Opcodes.LDC) {
                throw new NotYetImplementedException();
            }
            String name;
            String descriptor;
            if (lin.cst instanceof Double) {
                name = concSdouble;
                descriptor = toMethodDesc("D", concSdoubleDesc);
            } else if (lin.cst instanceof Integer) {
                name = concSint;
                descriptor = toMethodDesc("I", concSintDesc);
            } else if (lin.cst instanceof Float) {
                name = concSfloat;
                descriptor =  toMethodDesc("F", sfloatDesc);
            } else if (lin.cst instanceof Boolean) {
                name = concSbool;
                descriptor = toMethodDesc("Z", concSboolDesc);
            } else if (lin.cst instanceof Long) {
                throw new NotYetImplementedException();
            } else if (lin.cst instanceof String) {
                throw new NotYetImplementedException();
            } else if (lin.cst instanceof Type) {
                throw new NotYetImplementedException();
            } else {
                throw new NotYetImplementedException();
            }
            resultInstrs.add(insn); // Push constant to stack
            newVariableFromSe(name, descriptor, resultInstrs, seIndex);
        } else if (insn instanceof FieldInsnNode) {
            FieldInsnNode fin = (FieldInsnNode) insn;
            switch (fin.getOpcode()) {
                case Opcodes.GETFIELD:
                case Opcodes.PUTFIELD:
                case Opcodes.GETSTATIC:
                case Opcodes.PUTSTATIC:
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
            wrapJumpInsn(jin, ta, resultInstrs, seIndex);
        } else if (insn instanceof TypeInsnNode) {
            TypeInsnNode typeInsnNode = (TypeInsnNode) insn;
            String descForType = "L" + typeInsnNode.desc + ";";
            String replacementDesc = decideOnReplaceDesc(descForType);
            typeInsnNode.desc = replacementDesc.substring(1, replacementDesc.length() - 1);
            resultInstrs.add(insn);
        } else if (insn instanceof IntInsnNode) {
            IntInsnNode iin = (IntInsnNode) insn;
            if (op == Opcodes.BIPUSH) {
                resultInstrs.add(iin);
                String[] nameAndDescriptor = getNameAndDescriptorForConcMethodOfSPrimitiveSubclass(
                        insn.getOpcode(),
                        ta.instructionsToWrapSinceUsedByBoolInsns.contains(insn),
                        ta.instructionsToWrapSinceUsedByByteInsns.contains(insn),
                        ta.instructionsToWrapSinceUsedByShortInsns.contains(insn)
                );
                newVariableFromSe(
                        nameAndDescriptor[0],
                        nameAndDescriptor[1],
                        resultInstrs,
                        seIndex
                );
            } else if (op == Opcodes.NEWARRAY) {
                AbstractInsnNode last = resultInstrs.getLast();
                if (last.getOpcode() == ALOAD || last.getOpcode() == GETFIELD) { /// TODO Temporary fix until free arrays are implemented; this should not be necessary if we use Sarray
                    // For now we concretize the Sint which is used for loading.
                    resultInstrs.add(new VarInsnNode(ALOAD, seIndex));
                    resultInstrs.add(newStaticCall(concretize, concretizeDesc, seCp));
                    // We cast the concretized result to an int
                    resultInstrs.add(new TypeInsnNode(Opcodes.CHECKCAST, org.objectweb.asm.Type.getInternalName(Integer.class)));
                    // We retrieve the int value
                    resultInstrs.add(newVirtualCall("intValue", "()I", integerCp));
                }
                switch (iin.operand) {
                    case T_INT:
                        resultInstrs.add(new TypeInsnNode(ANEWARRAY, sintCp));
                        break;
                    case T_BOOLEAN:
                        resultInstrs.add(new TypeInsnNode(ANEWARRAY, sboolCp));
                        break;
                    default:
                        throw new NotYetImplementedException();
                }
//                throw new NotYetImplementedException();
            } else {
                throw new NotYetImplementedException();
            }
        } else if (insn instanceof InsnNode) {
            InsnNode in = (InsnNode) insn;
            switch (insn.getOpcode()) {
                case Opcodes.DUP:
                case Opcodes.RETURN:
                    resultInstrs.add(insn);
                    return;
                case Opcodes.ICONST_0:
                case Opcodes.ICONST_1:
                case Opcodes.ICONST_2:
                case Opcodes.ICONST_3:
                case Opcodes.ICONST_4:
                case Opcodes.ICONST_5:
                case Opcodes.LCONST_0:
                case Opcodes.LCONST_1:
                case Opcodes.FCONST_0:
                case Opcodes.FCONST_1:
                case Opcodes.FCONST_2:
                case Opcodes.DCONST_0:
                case Opcodes.DCONST_1:
                    wrapInsnNodeCONST(in, ta, resultInstrs, seIndex);
                    return;
                case Opcodes.NOP:
                    throw new IllegalStateException("Should not have to be replaced");
                case Opcodes.ACONST_NULL:
                    resultInstrs.add(insn);
                    return;
                case Opcodes.AALOAD:
                    AbstractInsnNode loadIndex = resultInstrs.getLast();
                    if (loadIndex.getOpcode() == ALOAD) { /// TODO Temporary fix until free arrays are implemented; this should not be necessary if we use Sarray
                        // For now we concretize the Sint which is used for loading.
                        resultInstrs.add(new VarInsnNode(ALOAD, seIndex));
                        resultInstrs.add(newStaticCall(concretize, concretizeDesc, seCp));
                        // We cast the concretized result to an int
                        resultInstrs.add(new TypeInsnNode(Opcodes.CHECKCAST, org.objectweb.asm.Type.getInternalName(Integer.class)));
                        // We retrieve the int value
                        resultInstrs.add(newVirtualCall("intValue", "()I", integerCp));
                    }
                    resultInstrs.add(insn);
                    return;
                case Opcodes.AASTORE:
                    resultInstrs.add(insn);
                    return;
                case IASTORE:
                    resultInstrs.add(new InsnNode(AASTORE));
                    return;
                case Opcodes.IALOAD:
                    AbstractInsnNode iloadIndex = resultInstrs.getLast();
                    if (iloadIndex.getOpcode() == ALOAD || iloadIndex.getOpcode() == GETFIELD) { /// TODO Temporary fix until free arrays are implemented; this should not be necessary if we use Sarray
                        // For now we concretize the Sint which is used for loading.
                        resultInstrs.add(new VarInsnNode(ALOAD, seIndex));
                        resultInstrs.add(newStaticCall(concretize, concretizeDesc, seCp));
                        // We cast the concretized result to an int
                        resultInstrs.add(new TypeInsnNode(Opcodes.CHECKCAST, org.objectweb.asm.Type.getInternalName(Integer.class)));
                        // We retrieve the int value
                        resultInstrs.add(newVirtualCall("intValue", "()I", integerCp));
                    }
                    resultInstrs.add(new InsnNode(AALOAD));
                    return;
                case Opcodes.BALOAD:
                    resultInstrs.add(new InsnNode(AALOAD));
                    return;
                case BASTORE:
                    resultInstrs.add(new InsnNode(AASTORE));
                    return;
                case Opcodes.ICONST_M1:
                case Opcodes.LALOAD:
                case Opcodes.FALOAD:
                case Opcodes.DALOAD:
                case Opcodes.CALOAD:
                case Opcodes.SALOAD:
                case Opcodes.LASTORE:
                case Opcodes.FASTORE:
                case Opcodes.DASTORE:
                case Opcodes.CASTORE:
                case Opcodes.SASTORE:
                case Opcodes.POP:
                case Opcodes.DUP_X1:
                case Opcodes.DUP_X2:
                case Opcodes.DUP2:
                case Opcodes.DUP2_X1:
                case Opcodes.DUP2_X2:
                case Opcodes.SWAP:
                    throw new NotYetImplementedException(String.valueOf(insn.getOpcode()));
                case Opcodes.POP2:
                    resultInstrs.add(new InsnNode(POP));
                    return;
                case Opcodes.IADD:
                    resultInstrs.add(new VarInsnNode(Opcodes.ALOAD, seIndex));
                    resultInstrs.add(newVirtualCall(
                            add,
                            sintArithDesc,
                            sintegerCp
                    ));
                    return;
                case Opcodes.LADD:
                    resultInstrs.add(new VarInsnNode(Opcodes.ALOAD, seIndex));
                    resultInstrs.add(newVirtualCall(
                            add,
                            slongArithDesc,
                            slongCp
                    ));
                    return;
                case Opcodes.FADD:
                    resultInstrs.add(new VarInsnNode(Opcodes.ALOAD, seIndex));
                    resultInstrs.add(newVirtualCall(
                            add,
                            sfloatArithDesc,
                            sfloatCp
                    ));
                    return;
                case Opcodes.DADD:
                    resultInstrs.add(new VarInsnNode(Opcodes.ALOAD, seIndex));
                    resultInstrs.add(newVirtualCall(
                            add,
                            sdoubleArithDesc,
                            sdoubleCp
                    ));
                    return;
                case Opcodes.ISUB:
                    resultInstrs.add(new VarInsnNode(Opcodes.ALOAD, seIndex));
                    resultInstrs.add(newVirtualCall(
                            sub,
                            sintArithDesc,
                            sintegerCp
                    ));
                    return;
                case Opcodes.LSUB:
                    resultInstrs.add(new VarInsnNode(Opcodes.ALOAD, seIndex));
                    resultInstrs.add(newVirtualCall(
                            sub,
                            slongArithDesc,
                            slongCp
                    ));
                    return;
                case Opcodes.FSUB:
                    resultInstrs.add(new VarInsnNode(Opcodes.ALOAD, seIndex));
                    resultInstrs.add(newVirtualCall(
                            sub,
                            sfloatArithDesc,
                            sfloatCp
                    ));
                    return;
                case Opcodes.DSUB:
                    resultInstrs.add(new VarInsnNode(Opcodes.ALOAD, seIndex));
                    resultInstrs.add(newVirtualCall(
                            sub,
                            sdoubleArithDesc,
                            sdoubleCp
                    ));
                    return;
                case Opcodes.LMUL:
                    resultInstrs.add(new VarInsnNode(Opcodes.ALOAD, seIndex));
                    resultInstrs.add(newVirtualCall(
                            mul,
                            slongArithDesc,
                            slongCp
                    ));
                    return;
                case Opcodes.FMUL:
                    resultInstrs.add(new VarInsnNode(Opcodes.ALOAD, seIndex));
                    resultInstrs.add(newVirtualCall(
                            mul,
                            sfloatArithDesc,
                            sfloatCp
                    ));
                    return;
                case Opcodes.DMUL:
                    resultInstrs.add(new VarInsnNode(Opcodes.ALOAD, seIndex));
                    resultInstrs.add(newVirtualCall(
                            mul,
                            sdoubleArithDesc,
                            sdoubleCp
                    ));
                    return;
                case Opcodes.IMUL:
                    resultInstrs.add(new VarInsnNode(Opcodes.ALOAD, seIndex));
                    resultInstrs.add(newVirtualCall(
                            mul,
                            sintArithDesc,
                            sintegerCp
                    ));
                    return;
                case Opcodes.IDIV:
                    resultInstrs.add(new VarInsnNode(Opcodes.ALOAD, seIndex));
                    resultInstrs.add(newVirtualCall(
                            div,
                            sintArithDesc,
                            sintegerCp
                    ));
                    return;
                case Opcodes.LDIV:
                    resultInstrs.add(new VarInsnNode(Opcodes.ALOAD, seIndex));
                    resultInstrs.add(newVirtualCall(
                            div,
                            slongArithDesc,
                            slongCp
                    ));
                    return;
                case Opcodes.FDIV:
                    resultInstrs.add(new VarInsnNode(Opcodes.ALOAD, seIndex));
                    resultInstrs.add(newVirtualCall(
                            div,
                            sfloatArithDesc,
                            sfloatCp
                    ));
                    return;
                case Opcodes.DDIV:
                    resultInstrs.add(new VarInsnNode(Opcodes.ALOAD, seIndex));
                    resultInstrs.add(newVirtualCall(
                            div,
                            sdoubleArithDesc,
                            sdoubleCp
                    ));
                    return;
                case Opcodes.IREM:
                    resultInstrs.add(new VarInsnNode(Opcodes.ALOAD, seIndex));
                    resultInstrs.add(newVirtualCall(
                            rem,
                            sintArithDesc,
                            sintegerCp
                    ));
                    return;
                case Opcodes.LREM:
                    resultInstrs.add(new VarInsnNode(Opcodes.ALOAD, seIndex));
                    resultInstrs.add(newVirtualCall(
                            rem,
                            slongArithDesc,
                            slongCp
                    ));
                    return;
                case Opcodes.FREM:
                    resultInstrs.add(new VarInsnNode(Opcodes.ALOAD, seIndex));
                    resultInstrs.add(newVirtualCall(
                            rem,
                            sfloatArithDesc,
                            sfloatCp
                    ));
                    return;
                case Opcodes.DREM:
                    resultInstrs.add(new VarInsnNode(Opcodes.ALOAD, seIndex));
                    resultInstrs.add(newVirtualCall(
                            rem,
                            sdoubleArithDesc,
                            sdoubleCp
                    ));
                    return;
                case Opcodes.INEG:
                    resultInstrs.add(new VarInsnNode(Opcodes.ALOAD, seIndex));
                    resultInstrs.add(newVirtualCall(
                            neg,
                            singlesintDesc,
                            sintegerCp
                    ));
                    return;
                case Opcodes.LNEG:
                    resultInstrs.add(new VarInsnNode(Opcodes.ALOAD, seIndex));
                    resultInstrs.add(newVirtualCall(
                            neg,
                            slongArithDesc,
                            slongCp
                    ));
                    return;
                case Opcodes.FNEG:
                    resultInstrs.add(new VarInsnNode(Opcodes.ALOAD, seIndex));
                    resultInstrs.add(newVirtualCall(
                            neg,
                            singlesfloatDesc,
                            sfloatCp
                    ));
                    return;
                case Opcodes.DNEG:
                    resultInstrs.add(new VarInsnNode(Opcodes.ALOAD, seIndex));
                    resultInstrs.add(newVirtualCall(
                            neg,
                            singlesdoubleDesc,
                            sdoubleCp
                    ));
                    return;
                case Opcodes.ISHL:
                case Opcodes.LSHL:
                case Opcodes.ISHR:
                case Opcodes.LSHR:
                case Opcodes.IUSHR:
                case Opcodes.LUSHR:
                case Opcodes.IAND:
                case Opcodes.LAND:
                case Opcodes.IOR:
                case Opcodes.LOR:
                case Opcodes.IXOR:
                case Opcodes.LXOR:
                    throw new NotYetImplementedException();
                case Opcodes.L2F:
                case Opcodes.I2F:
                case Opcodes.D2F:
                case Opcodes.F2D:
                case Opcodes.L2D:
                case Opcodes.I2D:
                case Opcodes.L2I:
                case Opcodes.F2I:
                case Opcodes.D2I:
                case Opcodes.I2L:
                case Opcodes.F2L:
                case Opcodes.D2L:
                case Opcodes.I2B:
                case Opcodes.I2C:
                case Opcodes.I2S:
                    LdcInsnNode castToClassNode;
                    TypeInsnNode checkCastNode;
                    if (List.of(L2F, I2F, D2F).contains(op)) {
                        castToClassNode = new LdcInsnNode(org.objectweb.asm.Type.getType(Sfloat.class));
                        checkCastNode = new TypeInsnNode(Opcodes.CHECKCAST, org.objectweb.asm.Type.getInternalName(Sfloat.class));
                    } else if (List.of(F2D, L2D, I2D).contains(op)) {
                        castToClassNode = new LdcInsnNode(org.objectweb.asm.Type.getType(Sdouble.class));
                        checkCastNode = new TypeInsnNode(Opcodes.CHECKCAST, org.objectweb.asm.Type.getInternalName(Sdouble.class));
                    } else if (List.of(L2I, F2I, D2I).contains(op)) {
                        castToClassNode = new LdcInsnNode(org.objectweb.asm.Type.getType(Sint.class));
                        checkCastNode = new TypeInsnNode(Opcodes.CHECKCAST, org.objectweb.asm.Type.getInternalName(Sint.class));
                    } else if (List.of(I2L, F2L, D2L).contains(op)) {
                        throw new NotYetImplementedException();
                    } else if (I2B == op) {
                        castToClassNode = new LdcInsnNode(org.objectweb.asm.Type.getType(Sbyte.class));
                        checkCastNode = new TypeInsnNode(Opcodes.CHECKCAST, org.objectweb.asm.Type.getInternalName(Sbyte.class));
                    } else if (op == I2C) {
                        throw new NotYetImplementedException();
                    } else if (op == I2S) {
                        castToClassNode = new LdcInsnNode(org.objectweb.asm.Type.getType(Sshort.class));
                        checkCastNode = new TypeInsnNode(Opcodes.CHECKCAST, org.objectweb.asm.Type.getInternalName(Sshort.class));
                    } else {
                        throw new NotYetImplementedException();
                    }

                    MethodInsnNode castCall =
                            newInterfaceCall(
                                    castTo,
                                    toMethodDesc(classDesc + seDesc, sprimitiveDesc),
                                    sprimitiveCp
                            );

                    resultInstrs.add(castToClassNode);
                    resultInstrs.add(new VarInsnNode(Opcodes.ALOAD, seIndex));
                    resultInstrs.add(castCall);
                    resultInstrs.add(checkCastNode);
                    break;
                case Opcodes.LCMP:
                case Opcodes.FCMPL:
                case Opcodes.DCMPL:
                case Opcodes.FCMPG:
                case Opcodes.DCMPG:
                    resultInstrs.add(new VarInsnNode(ALOAD, seIndex));
                    resultInstrs.add(newInterfaceCall(
                            cmp,
                            toMethodDesc(snumberDesc + seDesc, sintDesc),
                            snumberCp)
                    );
                    break;
                case Opcodes.IRETURN:
                    String returnType = splitMethodDesc(mn.desc)[1];
                    if (returnType.equals("S")) {
                        resultInstrs.add(new LdcInsnNode(org.objectweb.asm.Type.getType(Sshort.class)));
                        resultInstrs.add(new VarInsnNode(ALOAD, seIndex));
                        resultInstrs.add(newInterfaceCall(
                                castTo,
                                toMethodDesc(classDesc + seDesc, sprimitiveDesc),
                                sprimitiveCp
                        ));
                        resultInstrs.add(new TypeInsnNode(Opcodes.CHECKCAST, org.objectweb.asm.Type.getInternalName(Sshort.class)));

                    } else if (returnType.equals("B")) {
                        resultInstrs.add(new LdcInsnNode(org.objectweb.asm.Type.getType(Sbyte.class)));
                        resultInstrs.add(new VarInsnNode(ALOAD, seIndex));
                        resultInstrs.add(newInterfaceCall(
                                castTo,
                                toMethodDesc(classDesc + seDesc, sprimitiveDesc),
                                sprimitiveCp
                        ));
                        resultInstrs.add(new TypeInsnNode(Opcodes.CHECKCAST, org.objectweb.asm.Type.getInternalName(Sbyte.class)));
                    }
                    // Intentionally no break!
                case Opcodes.LRETURN:
                case Opcodes.FRETURN:
                case Opcodes.DRETURN:
                case Opcodes.ARETURN:
                    resultInstrs.add(new InsnNode(Opcodes.ARETURN));
                    return;
                case Opcodes.ARRAYLENGTH:
                    resultInstrs.add(insn);
                    resultInstrs.add(new VarInsnNode(ALOAD, seIndex)); // TODO Symbolic length and Sarray currently not supported!
                    resultInstrs.add(
                            newStaticCall(
                                    concSint,
                                    toMethodDesc("I" + seDesc, concSintDesc),
                                    seCp
                            )
                    );
                    return;
                case Opcodes.ATHROW:
                case Opcodes.MONITORENTER:
                case Opcodes.MONITOREXIT:
                    resultInstrs.add(insn);
                    return;
                default:
                    throw new NotYetImplementedException();
            }
        } else if (insn instanceof TableSwitchInsnNode) {
            TableSwitchInsnNode tsin = (TableSwitchInsnNode) insn;
            if (insn.getOpcode() != Opcodes.TABLESWITCH) {
                throw new NotYetImplementedException();
            }
            throw new NotYetImplementedException();
        } else if (insn instanceof IincInsnNode) {
            IincInsnNode iin = (IincInsnNode) insn;
            if (insn.getOpcode() != Opcodes.IINC) {
                throw new NotYetImplementedException();
            }

            resultInstrs.add(new IntInsnNode(BIPUSH, iin.incr));
            resultInstrs.add(new VarInsnNode(ALOAD, seIndex));
            resultInstrs.add(
                    newStaticCall(
                            concSint,
                            toMethodDesc("I" + seDesc, concSintDesc),
                            seCp
                    )
            );
            resultInstrs.add(new VarInsnNode(ALOAD, iin.var)); // Adaptation of iin.var happened beforehand

            resultInstrs.add(new VarInsnNode(ALOAD, seIndex));
            resultInstrs.add(
                    newInterfaceCall(
                            add,
                            sintArithDesc,
                            sintegerCp
                    )
            );
            resultInstrs.add(new VarInsnNode(ASTORE, iin.var)); // Adaptation of iin.var happened beforehand
        } else if (insn instanceof MultiANewArrayInsnNode) {
            MultiANewArrayInsnNode man = (MultiANewArrayInsnNode) insn;
            if (insn.getOpcode() != Opcodes.MULTIANEWARRAY) {
                throw new NotYetImplementedException();
            }
            if (man.desc.equals("[[I")) { /// TODO Temporary fix until free arrays are allowed

                resultInstrs.add(new MultiANewArrayInsnNode("[[" + sintDesc, 2));
                return;
            }
            throw new NotYetImplementedException();
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
            if (insn.getOpcode() != Opcodes.LOOKUPSWITCH) {
                throw new NotYetImplementedException();
            }
            throw new NotYetImplementedException();
        } else {
            throw new NotYetImplementedException();
        }
    }

    private static AbstractInsnNode getSymbolicExecution() {
        return new MethodInsnNode(INVOKESTATIC, seCp, "get", toMethodDesc("", seDesc));
    }


    private void wrapJumpInsn(JumpInsnNode jin, TaintAnalysis analysis, InsnList insnList, int seIndex) {
        String choiceMethodName;
        boolean booleanJmp = analysis.taintedBooleanInsns.contains(jin);
        String choiceMethodDescriptor = jin.getOpcode() < IF_ICMPEQ ? singleVarChoiceDesc : twoNumbersChoiceDesc;

        switch (jin.getOpcode()) {
            case IFNE:
            case IF_ICMPNE:
                choiceMethodName = booleanJmp ?  negatedBoolChoice : eqChoice;
                break;
            case IFEQ:
            case IF_ICMPEQ:
                choiceMethodName = booleanJmp ?  boolChoice : notEqChoice;
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
        insnList.add(new VarInsnNode(ALOAD, seIndex));
        AbstractInsnNode methodCall;
        if (booleanJmp) {
            methodCall = newVirtualCall(
                    choiceMethodName,
                    choiceMethodDescriptor,
                    sboolCp
            );
        } else {
            methodCall =
                    newInterfaceCall(
                            choiceMethodName,
                            choiceMethodDescriptor,
                            snumberCp
                    );
        }
        insnList.add(methodCall);
        insnList.add(new JumpInsnNode(IFEQ, jin.label));
    }

    private void newVariableFromSe(
            String name,
            String descriptor,
            InsnList insnList,
            int seIndex) {
        if (seIndex == -1) {
            throw new IllegalStateException("Should not occur");
        }
        insnList.add(new VarInsnNode(Opcodes.ALOAD, seIndex));
        insnList.add(newStaticCall(name, descriptor, seCp));
    }

    private static MethodInsnNode newVirtualCall(String methodName, String descriptor, String owningClass) {
        return new MethodInsnNode(
                Opcodes.INVOKEVIRTUAL,
                owningClass,
                methodName,
                descriptor
        );
    }

    private static MethodInsnNode newInterfaceCall(String methodName, String descriptor, String owningClass) {
        return new MethodInsnNode(
                Opcodes.INVOKEINTERFACE,
                owningClass,
                methodName,
                descriptor
        );
    }

    private static MethodInsnNode newStaticCall(String methodName, String descriptor, String owningClass) {
        return new MethodInsnNode(
                INVOKESTATIC,
                owningClass,
                methodName,
                descriptor
        );
    }

    public static String[] getSingleDescsFromMethodParams(String methodParams) {
        if (methodParams.charAt(0) != '(' || methodParams.charAt(methodParams.length() - 1) != ')') {
            throw new MulibRuntimeException("Should be methodParams");
        }
        methodParams = methodParams.substring(1, methodParams.length() - 1);
        List<String> singleDescs = new ArrayList<>();
        for (int i = 0; i < methodParams.length();) {
            if (isPrimitive(String.valueOf(methodParams.charAt(i)))) {
                singleDescs.add(String.valueOf(methodParams.charAt(i)));
                i++;
            } else if (methodParams.charAt(i) == '[') {
                i++;
                StringBuilder sb = new StringBuilder();
                sb.append('[');
                while (methodParams.charAt(i) == '[') {
                    i++;
                    sb.append('[');
                }
                // Again check for primitive vs object
                if (isPrimitive(String.valueOf(methodParams.charAt(i)))) {
                    sb.append(methodParams.charAt(i));
                } else {
                    int endIndex = methodParams.indexOf(';', i) + 1;
                    sb.append(methodParams, i, endIndex);
                }
                singleDescs.add(sb.toString());
            } else {
                int endIndex = methodParams.indexOf(';', i) + 1;
                String desc = methodParams.substring(i, endIndex);
                singleDescs.add(desc);
                i = endIndex;
            }
        }
        return singleDescs.toArray(new String[0]);
    }

    public static String[] splitMethodDesc(String methodDesc) {
        int lastIndexOfParameterPart = methodDesc.lastIndexOf(')') + 1;
        String parameterPart = methodDesc.substring(0, lastIndexOfParameterPart);
        String returnPart = methodDesc.substring(lastIndexOfParameterPart);

        return new String[] {parameterPart, returnPart};
    }

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
}