package de.wwu.mulib.transformations.soot_transformations;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.exceptions.MulibIllegalStateException;
import de.wwu.mulib.exceptions.MulibRuntimeException;
import de.wwu.mulib.exceptions.NotYetImplementedException;
import de.wwu.mulib.substitutions.Sarray;
import de.wwu.mulib.substitutions.primitives.*;
import de.wwu.mulib.transformations.AbstractMulibTransformer;
import de.wwu.mulib.transformations.MulibClassFileWriter;
import de.wwu.mulib.transformations.MulibClassLoader;
import soot.*;
import soot.jimple.*;
import soot.jimple.internal.JInvokeStmt;
import soot.options.Options;
import soot.tagkit.InnerClassTag;
import soot.tagkit.Tag;
import soot.util.Chain;

import java.io.File;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static de.wwu.mulib.transformations.StringConstants.*;
import static de.wwu.mulib.transformations.TransformationUtility.determineNestHostFieldName;
import static de.wwu.mulib.transformations.TransformationUtility.getClassForName;
import static de.wwu.mulib.transformations.soot_transformations.SootMulibClassesAndMethods.methodNameImpliesRememberedInitialization;

/**
 * Current reference implementation for a {@link de.wwu.mulib.transformations.MulibTransformer} using {@link AbstractMulibTransformer}
 * using Soot.
 * Contains some obsolete parts.
 */
public class SootMulibTransformer extends AbstractMulibTransformer<SootClass> {

    static class TcArgs {
        final MethodInfoContainer methodInfoContainer;
        final boolean _isTainted;
        final boolean _isToWrap;

        private TcArgs(
                MethodInfoContainer methodInfoContainer,
                Unit u) {
            this.methodInfoContainer = methodInfoContainer;
            this._isTainted = methodInfoContainer.taintAnalysis.tainted.contains(u);
            this._isToWrap = methodInfoContainer.taintAnalysis.toWrap.contains(u);
        }

        Local spawnStackLocal(Type t) {
            return methodInfoContainer.spawnNewStackLocal(t);
        }

        Local seLocal() {
            return methodInfoContainer.seLocal;
        }

        TaintAnalysis taintAnalysis() {
            return methodInfoContainer.taintAnalysis;
        }

        boolean isTainted() {
            return _isTainted;
        }

        boolean isToWrap() {
            return _isToWrap;
        }

        boolean isTainted(Value value) {
            return methodInfoContainer.taintAnalysis.taintedValues.contains(value);
        }

        SootMethod newMethod() {
            return methodInfoContainer.newMethod;
        }

        private void addUnit(Unit u) {
            methodInfoContainer.toAddTo.add(u);
        }
    }

    private enum ChosenConstructor {
        SE_CONSTR, COPY_CONSTR, TRANSFORMATION_CONSTR
    }

    private class MulibTransformationInformation {
        private final Type transformedOuterClassType;
        private final List<Type> parameterTypesOfSeConstructor;
        private final List<Type> parameterTypesOfTransfConstructor;

        private final List<Type> parameterTypesOfCopyConstructor;
        private final boolean isInnerNonStatic;

        private final boolean shouldBeTransformed;

        private final SootClass original;

        private final SootClass transformed;

        private MulibTransformationInformation(SootClass original, SootClass transformed) {
            this.original = original;
            this.transformed = transformed;
            isInnerNonStatic = transformed.isInnerClass() && getOuterClassField(original) != null;
            shouldBeTransformed = SootMulibTransformer.this.shouldBeTransformed(original.getName());
            if (isInnerNonStatic()) {
                transformedOuterClassType =
                        shouldBeTransformed ?
                                transformed.getOuterClass().getType()
                                :
                                original.getOuterClass().getType();
                parameterTypesOfSeConstructor =
                        shouldBeTransformed ?
                                List.of(transformedOuterClassType, v.TYPE_SE)
                                :
                                List.of(original.getOuterClass().getType());
            } else {
                transformedOuterClassType = null;
                parameterTypesOfSeConstructor = shouldBeTransformed ? List.of(v.TYPE_SE) : List.of();
            }
            // We treat the outer class field as a regular field. Thus, we do not need to add it as a parameter.
            parameterTypesOfTransfConstructor =
                    shouldBeTransformed ?
                            List.of(v.TYPE_OBJECT, v.TYPE_MULIB_VALUE_TRANSFORMER)
                            :
                            List.of();
            parameterTypesOfCopyConstructor =
                    shouldBeTransformed ?
                            List.of(transformed.getType(), v.TYPE_MULIB_VALUE_COPIER)
                            :
                            List.of();
        }

        private Local getAdditionalLocal(ChosenConstructor cc, LocalSpawner localSpawner) {
            if (cc == ChosenConstructor.SE_CONSTR) {
                return null;
            } else if (cc == ChosenConstructor.COPY_CONSTR) {
                return localSpawner.spawnNewLocal(transformed.getType());
            } else if (cc == ChosenConstructor.TRANSFORMATION_CONSTR) {
                return localSpawner.spawnNewLocal(v.TYPE_OBJECT);
            } else {
                throw new NotYetImplementedException();
            }
        }

        Local getAdditionalTransformedLocal(LocalSpawner localSpawner) {
            return localSpawner.spawnNewLocal(original.getType());
        }

        private boolean isInnerNonStatic() {
            return isInnerNonStatic;
        }

        private Type getTransformedOuterClassType() {
            return transformedOuterClassType;
        }

        private List<Type> getParameterTypesOfConstr(ChosenConstructor cc) {
            switch (cc) {
                case SE_CONSTR:
                    return getParameterTypesOfSeConstructor();
                case COPY_CONSTR:
                    return getParameterTypesOfCopyConstructor();
                case TRANSFORMATION_CONSTR:
                    return getParameterTypesOfTransfConstructor();
                default:
                    throw new NotYetImplementedException();
            }
        }

        private List<Value> getParameterValuesOfConstrExceptThis(
                ChosenConstructor cc,
                Value additionalLocal,
                Value thisOuterLocal,
                Value seOrMvtLocal) {
            if (!shouldBeTransformed) {
                // Should not be transformed, empty constructor assumed
                return List.of();
            }
            switch (cc) {
                case SE_CONSTR:
                    assert additionalLocal == null;
                    assert seOrMvtLocal != null;
                    if (thisOuterLocal != null) {
                        // Is inner class, must be set like this:
                        return List.of(thisOuterLocal, seOrMvtLocal);
                    } else {
                        // Is "normal" class, seLocal
                        return List.of(seOrMvtLocal);
                    }
                case COPY_CONSTR:
                case TRANSFORMATION_CONSTR:
                    assert additionalLocal != null;
                    assert seOrMvtLocal != null;
                    return List.of(additionalLocal, seOrMvtLocal);
                default:
                    throw new NotYetImplementedException();
            }
        }

        private List<Type> getParameterTypesOfSeConstructor() {
            return parameterTypesOfSeConstructor;
        }

        private List<Type> getParameterTypesOfTransfConstructor() {
            return parameterTypesOfTransfConstructor;
        }

        private List<Type> getParameterTypesOfCopyConstructor() {
            return parameterTypesOfCopyConstructor;
        }
    }

    private static class MethodInfoContainer {
        private final SootMethod newMethod;
        private final UnitPatchingChain toAddTo;
        private final LocalSpawner localSpawner;
        private final Local seLocal;
        private final TaintAnalysis taintAnalysis;

        private MethodInfoContainer(
                final SootMethod newMethod,
                final UnitPatchingChain toAddTo,
                final LocalSpawner localSpawner,
                final Local seLocal,
                final TaintAnalysis taintAnalysis) {
            this.newMethod = newMethod;
            this.toAddTo = toAddTo;
            this.seLocal = seLocal;
            this.localSpawner = localSpawner;
            this.taintAnalysis = taintAnalysis;
        }

        private Local spawnNewStackLocal(Type t) {
            return localSpawner.spawnNewStackLocal(t);
        }
    }

    private static class LocalSpawner {

        private final JimpleBody body;
        private int nIntLocals;
        private int nDoubleLocals;
        private int nLongLocals;
        private int nFloatLocals;
        private int nShortLocals;
        private int nByteLocals;
        private int nBoolLocals;
        private int nRefLocals;
        private int nCharLocals;

        private static boolean startsWithOneOf(String toCheck, String firstOption, String secondOption) {
            return toCheck.startsWith(firstOption) || toCheck.startsWith(secondOption);
        }

        private LocalSpawner(JimpleBody body) {
            this.body = body;
            Chain<Local> existingLocals = body.getLocals();
            for (Local l : existingLocals) {
                String localName = l.getName();
                if (startsWithOneOf(localName, "$r", "r")) {
                    nRefLocals++;
                } else if (startsWithOneOf(localName, "$i", "i")) {
                    nIntLocals++;
                } else if (startsWithOneOf(localName, "$j", "j")) {
                    nLongLocals++;
                } else if (startsWithOneOf(localName, "$d", "d")) {
                    nDoubleLocals++;
                } else if (startsWithOneOf(localName, "$f", "f")) {
                    nFloatLocals++;
                } else if (startsWithOneOf(localName, "$s", "s")) {
                    nShortLocals++;
                } else if (startsWithOneOf(localName, "$b", "b")) {
                    nByteLocals++;
                } else if (startsWithOneOf(localName, "$z", "z")) {
                    nBoolLocals++;
                } else if (startsWithOneOf(localName, "$c", "c")) {
                    nCharLocals++;
                } else if (l.getType() instanceof VoidType) {
                    throw new MulibRuntimeException("Void type as local variable");
                }
            }
        }

        private Local spawnNewStackLocal(Type t) {
            return _spawnNewLocal("$", t);
        }

        private Local _spawnNewLocal(String prefix, Type t) {
            Local result;
            if (t instanceof RefLikeType) {
                nRefLocals++;
                result = Jimple.v().newLocal(prefix + "r" + nRefLocals, t);
            } else if (t instanceof IntType) {
                nIntLocals++;
                result = Jimple.v().newLocal(prefix + "i" + nIntLocals, t);
            } else if (t instanceof LongType) {
                nLongLocals++;
                result = Jimple.v().newLocal(prefix + "j" + nLongLocals, t);
            } else if (t instanceof DoubleType) {
                nDoubleLocals++;
                result = Jimple.v().newLocal(prefix + "d" + nDoubleLocals, t);
            } else if (t instanceof FloatType) {
                nFloatLocals++;
                result = Jimple.v().newLocal(prefix + "f" + nFloatLocals, t);
            } else if (t instanceof ShortType) {
                nShortLocals++;
                result = Jimple.v().newLocal(prefix + "s" + nShortLocals, t);
            } else if (t instanceof ByteType) {
                nByteLocals++;
                result = Jimple.v().newLocal(prefix + "b" + nByteLocals, t);
            } else if (t instanceof BooleanType) {
                nBoolLocals++;
                result = Jimple.v().newLocal(prefix + "z" + nBoolLocals, t);
            } else if (t instanceof CharType) {
                nCharLocals++;
                result = Jimple.v().newLocal(prefix + "c" + nCharLocals, t);
            } else if (t instanceof VoidType) {
                throw new MulibRuntimeException("Void type as local variable");
            } else {
                throw new NotYetImplementedException();
            }
            body.getLocals().add(result);
            return result;
        }

        private Local spawnNewLocal(Type t) {
            return _spawnNewLocal("", t);
        }
    }

    private long nextIdOfChoicePoint = 0L;
    private static final String JAVA_CLASS_PATH;
    private static final String DEFAULT_SOOT_JCP;
    private final Map<String, SootClass> resolvedClasses = new HashMap<>();
    static {
        JAVA_CLASS_PATH = System.getProperty("java.class.path").replace("build/resources/test", "build").replace("build/resources/main", "build");
        DEFAULT_SOOT_JCP = Scene.defaultJavaClassPath();
        Options.v().set_soot_classpath(JAVA_CLASS_PATH + File.pathSeparator + DEFAULT_SOOT_JCP);
        Options.v().set_drop_bodies_after_load(false);
        v = new SootMulibClassesAndMethods();
    }
    private static final SootMulibClassesAndMethods v;

    private final Map<SootMethod, SootMethod> sootReplaceMethodWithOtherMethod = new HashMap<>();

    @Override
    public void transformAndLoadClasses(Class<?>... toTransform) {
        try {
            super.transformAndLoadClasses(toTransform);
        } catch (Exception e) {
            throw new MulibRuntimeException("Exception during transformation", e);
        }
    }

    @Override
    public synchronized Class<?> getTransformedClass(Class<?> beforeTransformation) {
        return super.getTransformedClass(beforeTransformation);
    }

    @Override
    public synchronized Class<?> getPossiblyTransformedClass(Class<?> beforeTransformation) {
        return super.getPossiblyTransformedClass(beforeTransformation);
    }

    @Override
    public synchronized void setPartnerClass(Class<?> original, Class<?> partnerClass) {
        super.setPartnerClass(original, partnerClass);
    }

    @Override
    public synchronized Map<Class<?>, Class<?>> getArrayTypesToSpecializedSarrayClass() {
        Map<Class<?>, Class<?>> result = new HashMap<>();
        for (Map.Entry<String, SootClass> entry : this.arrayTypeToSpecialPartnerClassSarray.entrySet()) {
            try {
                Class<?> keyTypeTransformedToArrayType = transformType(Class.forName(entry.getKey()), true);
                Class<?> actualClass = classLoader.loadClass(entry.getValue().getName());
                result.put(keyTypeTransformedToArrayType, actualClass);
            } catch (ClassNotFoundException e) {
                throw new MulibRuntimeException(e);
            }
        }
        return result;
    }

    @Override
    public long getNumberNumberedChoicePoints() {
        if (!config.TRANSF_CFG_GENERATE_CHOICE_POINTS_WITH_ID) {
            throw new MulibRuntimeException("Must not call this method if choice points are not to be generated with an ID");
        }
        return nextIdOfChoicePoint;
    }


    /**
     * Constructs an instance of MulibTransformer according to the configuration.
     *
     * @param config The configuration.
     */
    public SootMulibTransformer(MulibConfig config) {
        super(config);
        for (Map.Entry<Method, Method> e : this.replaceMethodCallWithOtherMethodCall.entrySet()) {
            sootReplaceMethodWithOtherMethod.put(
                    getSootMethodForMethod(e.getKey()),
                    getSootMethodForMethod(e.getValue())
            );
        }
    }

    @Override
    public MulibClassFileWriter<SootClass> generateMulibClassFileWriter() {
        return new SootClassFileWriter();
    }

    @Override
    protected Class<?> getTransformedSpecializedPartnerClassSarrayClass(Class<?> c) {
        if (!c.isArray()) {
            throw new MulibRuntimeException("This method can only be used to get the specialized array type for an array type. A non-array type was passed.");
        }
        SootClass specializedPartnerClassSarray = arrayTypeToSpecialPartnerClassSarray.get(c.getName());
        if (specializedPartnerClassSarray == null) {
            throw new MulibRuntimeException("Specialized sarray type for '" + c.getName() + "' not found.");
        }
        try {
            // Load the class with the classLoader
            return classLoader.loadClass(specializedPartnerClassSarray.getName());
        } catch (ClassNotFoundException e) {
            throw new MulibRuntimeException(e);
        }
    }

    @Override
    protected String getNameToLoadOfClassNode(SootClass classNode) {
        return classNode.getName().replace("/", ".");
    }

    @Override
    protected Map<String, SootClass> getArrayTypeNameToGeneratedSpecializedPartnerClassSarrayClass() {
        return arrayTypeToSpecialPartnerClassSarray;
    }

    @Override
    protected Map<String, String> getSpecializedArrayTypeNameToOriginalTypeName() {
        return specializedArrayTypeNameToOriginalTypeName;
    }

    @Override
    protected boolean isInterface(SootClass classNode) {
        return classNode.isInterface();
    }

    @Override
    protected SootClass getClassNodeForName(String name) {
        SootClass c;
        // For some reason Scene.v().loadClass(String,int) cannot resolve properly,
        // this is a workaround.
        if ((c = resolvedClasses.get(name)) != null) {
            return c;
        }
        c = Scene.v().forceResolve(name, SootClass.BODIES);
        resolvedClasses.put(name, c);
        return c;
    }

    private static boolean classRepresentedByType(Class<?> c, Type t) {
        if (c.isPrimitive()) {
            if (c == int.class) {
                return t instanceof IntType;
            } else if (c == long.class) {
                return t instanceof LongType;
            } else if (c == double.class) {
                return t instanceof DoubleType;
            } else if (c == float.class) {
                return t instanceof FloatType;
            } else if (c == short.class) {
                return t instanceof ShortType;
            } else if (c == byte.class) {
                return t instanceof ByteType;
            } else if (c == boolean.class) {
                return t instanceof BooleanType;
            } else {
                assert c == char.class;
                return t instanceof CharType;
            }
        } else if (c.isArray()) {
            if (!(t instanceof ArrayType)) {
                return false;
            }
            return classRepresentedByType(c.getComponentType(), ((ArrayType) t).getElementType());
        } else {
            if (!(t instanceof RefType)) {
                return false;
            }
            return ((RefType) t).getClassName().equals(c.getName());
        }
    }

    private static boolean classesRepresentedByTypes(Class<?>[] cs, List<Type> ts) {
        for (int i = 0; i < cs.length; i++) {
            Class<?> c = cs[i];
            Type t = ts.get(i);
            if (!classRepresentedByType(c, t)) {
                return false;
            }
        }
        return true;
    }

    private SootMethod getSootMethodForMethod(Method m) {
        Class<?> declaringClass = m.getDeclaringClass();
        SootClass sootClass = getClassNodeForName(declaringClass.getName());
        SootMethod result = null;
        Class<?>[] parameterTypes = m.getParameterTypes();
        for (SootMethod sm : sootClass.getMethods()) {
            List<Type> types = sm.getParameterTypes();
            if (!m.getName().equals(sm.getName()) || parameterTypes.length != types.size()) {
                continue;
            }
            if (classesRepresentedByTypes(parameterTypes, types)) {
                result = sm;
                break;
            }
        }

        assert result != null;
        return result;
    }

    private String getOuterClassField(SootClass c) {
        if (!c.isInnerClass()) {
            throw new MulibRuntimeException("Input must be an inner class");
        }
        return determineNestHostFieldName(c.getName().replace(".", "/"));
    }

    private void generateSpecializedConstructor(
            SootClass old,
            SootClass result,
            ChosenConstructor cc) {
        MulibTransformationInformation resultData = new MulibTransformationInformation(old, result);
        MulibTransformationInformation superData = new MulibTransformationInformation(old.getSuperclass(), result.getSuperclass());

        if (superData.isInnerNonStatic()) {
            throw new NotYetImplementedException("We currently do not support inner super classes");
        }

        // Create constructor
        SootMethod newConstructorOfResult =
                new SootMethod(init, resultData.getParameterTypesOfConstr(cc), v.TYPE_VOID, Modifier.PUBLIC);

        // Create parameter locals for method
        JimpleBody b = Jimple.v().newBody(newConstructorOfResult);
        newConstructorOfResult.setActiveBody(b);
        LocalSpawner localSpawner = new LocalSpawner(b);
        // Create locals for body
        Local thisLocal = localSpawner.spawnNewLocal(result.getType());
        // In the case of TRANSFORMATION_CONSTR additionalLocal is the originalLocal; in case of COPY_CONSTR is the
        // transformed local from which to copy
        Local additionalLocal = resultData.getAdditionalLocal(cc, localSpawner);

        Local thisOuterLocal = resultData.isInnerNonStatic()
                ?
                localSpawner.spawnNewLocal(resultData.getTransformedOuterClassType())
                :
                null;

        Local seOrMvtLocal;
        RefType mulibValueTransformerOrCopierIfAny = null;
        if (cc == ChosenConstructor.SE_CONSTR) {
           seOrMvtLocal = localSpawner.spawnNewLocal(v.TYPE_SE);
        } else {
            assert cc == ChosenConstructor.COPY_CONSTR || cc == ChosenConstructor.TRANSFORMATION_CONSTR;
            if (cc == ChosenConstructor.COPY_CONSTR) {
                mulibValueTransformerOrCopierIfAny = v.TYPE_MULIB_VALUE_COPIER;
            } else {
                mulibValueTransformerOrCopierIfAny = v.TYPE_MULIB_VALUE_TRANSFORMER;
            }
            seOrMvtLocal = localSpawner.spawnNewLocal(mulibValueTransformerOrCopierIfAny);
        }
        // Get unit chain to add instructions to
        UnitPatchingChain upc = b.getUnits();
        // Create identity statement for parameter locals
        upc.add(Jimple.v().newIdentityStmt(thisLocal, Jimple.v().newThisRef(result.getType())));
        int localNumber = 0;
        if (additionalLocal != null) {
            upc.add(Jimple.v().newIdentityStmt(additionalLocal, Jimple.v().newParameterRef(additionalLocal.getType(), localNumber++)));
        }

        SootField resultOuterClassField = null;
        if (thisOuterLocal != null && cc == ChosenConstructor.SE_CONSTR) {
            String outerClassFieldName = getOuterClassField(old);
            resultOuterClassField = result.getField(outerClassFieldName, result.getOuterClass().getType());
            upc.add(Jimple.v().newIdentityStmt(thisOuterLocal, Jimple.v().newParameterRef(resultData.transformedOuterClassType, localNumber++)));
        }

        if (cc == ChosenConstructor.SE_CONSTR) {
            upc.add(Jimple.v().newIdentityStmt(seOrMvtLocal, Jimple.v().newParameterRef(v.TYPE_SE, localNumber++)));
        } else {
            upc.add(Jimple.v().newIdentityStmt(seOrMvtLocal, Jimple.v().newParameterRef(mulibValueTransformerOrCopierIfAny, localNumber++)));
        }
        SootClass superClass = result.getSuperclass();
        InvokeStmt invokeSuperConstructorStmt;
        // Add super-constructor call
        // TODO Clean up
        if (cc == ChosenConstructor.COPY_CONSTR && (superClass.equals(v.SC_PARTNER_CLASS_OBJECT) || shouldBeTransformed(superClass.getName().replace(_TRANSFORMATION_INDICATOR, "")))) {
            // Call super-constructor
            invokeSuperConstructorStmt = Jimple.v().newInvokeStmt(Jimple.v().newSpecialInvokeExpr(
                    thisLocal, Scene.v().makeConstructorRef(superClass, List.of(superClass.getType(), v.TYPE_MULIB_VALUE_COPIER)), additionalLocal, seOrMvtLocal
            ));
        } else {
            SootMethodRef refOfInit =
                    result.getSuperclass().getMethod(init, superData.getParameterTypesOfConstr(cc)).makeRef();
            SpecialInvokeExpr initExpr =
                    Jimple.v().newSpecialInvokeExpr(
                            thisLocal,
                            refOfInit,
                            superData.getParameterValuesOfConstrExceptThis(cc, additionalLocal, thisOuterLocal, seOrMvtLocal)
                    );
            invokeSuperConstructorStmt = Jimple.v().newInvokeStmt(initExpr);
        }
        upc.add(invokeSuperConstructorStmt);

        if (cc == ChosenConstructor.TRANSFORMATION_CONSTR || cc == ChosenConstructor.COPY_CONSTR) {
            SootMethod chosenMethod =
                    cc == ChosenConstructor.TRANSFORMATION_CONSTR ?
                            v.SM_MULIB_VALUE_TRANSFORMER_REGISTER_TRANSFORMED_OBJECT
                            :
                            v.SM_MULIB_VALUE_COPIER_REGISTER_COPY;
            VirtualInvokeExpr registerCopy =
                    Jimple.v().newVirtualInvokeExpr(seOrMvtLocal, chosenMethod.makeRef(), additionalLocal, thisLocal);
            upc.add(Jimple.v().newInvokeStmt(registerCopy));
        }

        // If this is an inner non-static class, we already set the field of its outer class
        if (thisOuterLocal != null && cc == ChosenConstructor.SE_CONSTR) {
            FieldRef resultOuterClassFieldRef = Jimple.v().newInstanceFieldRef(thisLocal, resultOuterClassField.makeRef());
            upc.add(Jimple.v().newAssignStmt(resultOuterClassFieldRef, thisOuterLocal));
        }

        // For transformation-constructor: We might need reflection to initialize all fields
        boolean reflectionRequired = cc == ChosenConstructor.TRANSFORMATION_CONSTR && calculateReflectionRequired(old);
        Local classLocal = null, fieldLocal = null, exceptionLocal = null;
        if (reflectionRequired) {
            classLocal = localSpawner.spawnNewLocal(v.TYPE_CLASS);
            fieldLocal = localSpawner.spawnNewLocal(v.TYPE_FIELD);
            exceptionLocal = localSpawner.spawnNewLocal(v.TYPE_EXCEPTION);
        }

        // Only used for TRANSFORMATION_CONSTR
        Local seLocal;
        // For SymbolicExecution-Constructor: make null check
        // This is why we already create the return statement (but do only add it at the end of this)
        ReturnVoidStmt returnStmt = Jimple.v().newReturnVoidStmt();
        if (cc == ChosenConstructor.SE_CONSTR) {
            IfStmt nullIfStmt = Jimple.v().newIfStmt(Jimple.v().newEqExpr(seOrMvtLocal, NullConstant.v()), returnStmt);
            upc.add(nullIfStmt);
        } else if (cc == ChosenConstructor.TRANSFORMATION_CONSTR) {
            // We need to additionally add an instance of SymbolicExecution here
            seLocal = localSpawner.spawnNewLocal(v.TYPE_SE);
            AssignStmt assignSeLocal = Jimple.v().newAssignStmt(
                    seLocal,
                    Jimple.v().newStaticInvokeExpr(v.SM_SE_GET.makeRef())
            );
            upc.add(assignSeLocal);
        }

        if (!reflectionRequiredSinceClassIsNotVisible(old) && cc == ChosenConstructor.TRANSFORMATION_CONSTR) {
            Local castedAdditionalLocal = resultData.getAdditionalTransformedLocal(localSpawner);
            upc.add(Jimple.v().newAssignStmt(castedAdditionalLocal, Jimple.v().newCastExpr(additionalLocal, castedAdditionalLocal.getType())));
            additionalLocal = castedAdditionalLocal;
        }

        if (cc == ChosenConstructor.SE_CONSTR) {
            // Set representationState to DEFAULT_IS_SYMBOLIC
            upc.add(Jimple.v().newInvokeStmt(
                    Jimple.v().newInterfaceInvokeExpr(thisLocal, v.SM_PARTNER_CLASS_SET_DEFAULT_IS_SYMBOLIC.makeRef())
            ));
        } else {
            // Initialize fields
            for (SootField oldField : old.getFields()) {
                SootField transformedField = getTransformedFieldForOldField(oldField, result);
                if (transformedField.isStatic()) {
                    // We do this in <clinit>
                    continue;
                }

                if (cc == ChosenConstructor.TRANSFORMATION_CONSTR) {
                    initializeFieldViaTransformation(
                            thisLocal, additionalLocal, seOrMvtLocal, classLocal, fieldLocal,
                            localSpawner, oldField, transformedField, upc, cc);
                } else {
                    initializeFieldViaCopy(thisLocal, additionalLocal, seOrMvtLocal, localSpawner, transformedField, upc, cc);
                }
            }

            // For transformation constructor: If reflection is required, we must catch the exceptions
            if (cc == ChosenConstructor.TRANSFORMATION_CONSTR && reflectionRequired) {
                // The following code should only be executed if an exception is thrown
                upc.add(Jimple.v().newGotoStmt(returnStmt));
                // We start the trap first thing after calling super(...)
                try {
                    Unit successorOfSuperInit = upc.getSuccOf(invokeSuperConstructorStmt);

                    IdentityStmt exceptionStmt = Jimple.v().newIdentityStmt(exceptionLocal, Jimple.v().newCaughtExceptionRef());
                    Local mulibExceptionStackLocal = localSpawner.spawnNewStackLocal(v.TYPE_MULIB_RUNTIME_EXCEPTION);
                    AssignStmt newMulibRuntimeException =
                            Jimple.v().newAssignStmt(
                                    mulibExceptionStackLocal,
                                    Jimple.v().newNewExpr(v.TYPE_MULIB_RUNTIME_EXCEPTION)
                            );
                    InvokeStmt invokeMulibRuntimeExceptionConstr =
                            Jimple.v().newInvokeStmt(Jimple.v().newSpecialInvokeExpr(
                                    mulibExceptionStackLocal,
                                    Scene.v().makeConstructorRef(v.SC_MULIB_RUNTIME_EXCEPTION, List.of(v.TYPE_EXCEPTION)),
                                    exceptionLocal));
                    upc.add(exceptionStmt);
                    upc.add(newMulibRuntimeException);
                    upc.add(invokeMulibRuntimeExceptionConstr);
                    upc.add(Jimple.v().newThrowStmt(mulibExceptionStackLocal));
                    Trap t = Jimple.v().newTrap(v.SC_EXCEPTION, successorOfSuperInit, returnStmt, exceptionStmt);
                    b.getTraps().add(t);
                } catch (Exception e) {
                    throw new MulibRuntimeException(e);
                }
            }
        }
        upc.add(returnStmt);

        newConstructorOfResult.setDeclaringClass(result);
        result.addMethod(newConstructorOfResult);
    }

    private boolean calculateReflectionRequired(SootClass toCheck) {
        if (toCheck.getPackageName().startsWith("java")) {
            // We must not create classes within a Java package. Thus, we add _TRANSFORMATION_PREFIX.
            // However, this means, that we cannot use package-access.
            return true;
        }
        for (SootField f : toCheck.getFields()) {
            if (calculateReflectionRequiredForField(f)) {
                return true;
            }
        }
        return false;
    }

    private SootField getTransformedFieldForOldField(SootField oldField, SootClass newClass) {
        Type transformedType = transformType(oldField.getType());
        return newClass.getField(oldField.getName(), transformedType);
    }

    private ClassConstant getWrapperAwareClassConstantForType(Type t) {
        String toLoad = getClassConstantName(t);
        return ClassConstant.v(toLoad);
    }

    private String getClassConstantName(Type t) {
        String toLoad;
        if (t instanceof ArrayType) {
            ArrayType at = (ArrayType) t;
            Type innermostType = at.baseType;
            while (innermostType instanceof ArrayType) {
                innermostType = ((ArrayType) innermostType).baseType;
            }
            String innermostToLoad = getClassConstantName(innermostType);
            toLoad = "[".repeat(at.numDimensions) + innermostToLoad;
        } else if (t instanceof PrimType) {
            if (t instanceof IntType) {
                toLoad = "I";
            } else if (t instanceof LongType) {
                toLoad = "J";
            } else if (t instanceof DoubleType) {
                toLoad = "D";
            } else if (t instanceof FloatType) {
                toLoad = "F";
            } else if (t instanceof ShortType) {
                toLoad = "S";
            } else if (t instanceof ByteType) {
                toLoad = "B";
            } else if (t instanceof BooleanType) {
                toLoad = "Z";
            } else {
                throw new NotYetImplementedException();
            }
        } else {
            toLoad = "L" + ((RefType) t).getClassName().replace(".", "/") + ";";
        }
        return toLoad;
    }

    private Type wrapTypeIfNecessary(Type t) {
        return Scene.v().getType(
                t instanceof ArrayType
                        ?
                        getSceneLoadableArrayTypeName((ArrayType) t)
                        :
                        getClassAwareOfWrapper(t).getName().replace("/", "."));
    }

    private String getSceneLoadableArrayTypeName(ArrayType arrayType) {
        Type bt = arrayType.baseType;
        String nameOfBaseType;
        if (bt instanceof IntType) {
            nameOfBaseType = "int";
        } else if (bt instanceof LongType) {
            nameOfBaseType = "long";
        } else if (bt instanceof DoubleType) {
            nameOfBaseType = "double";
        } else if (bt instanceof FloatType) {
            nameOfBaseType = "float";
        } else if (bt instanceof ShortType) {
            nameOfBaseType = "short";
        } else if (bt instanceof ByteType) {
            nameOfBaseType = "byte";
        } else if (bt instanceof BooleanType) {
            nameOfBaseType = "boolean";
        } else if (bt instanceof RefType) {
            nameOfBaseType = ((RefType) bt).getClassName();
        } else {
            throw new NotYetImplementedException(bt.toString());
        }
        return nameOfBaseType + "[]".repeat(arrayType.numDimensions);
    }

    private Class<?> getClassAwareOfWrapper(Type t) {
        if (t instanceof ArrayType) {
            throw new MulibRuntimeException("Only primitive and RefTypes are allowed here. Given: " + t);
        }
        if (t instanceof PrimType) {
            Class<?> clazz;
            if (t instanceof IntType) {
                clazz = Integer.class;
            } else if (t instanceof LongType) {
                clazz = Long.class;
            } else if (t instanceof DoubleType) {
                clazz = Double.class;
            } else if (t instanceof FloatType) {
                clazz = Float.class;
            } else if (t instanceof ShortType) {
                clazz = Short.class;
            } else if (t instanceof ByteType) {
                clazz = Byte.class;
            } else if (t instanceof BooleanType) {
                clazz = Boolean.class;
            } else if (t instanceof CharType) {
                clazz = Character.class;
            } else {
                throw new NotYetImplementedException();
            }
            return clazz;
        } else {
            try {
                return Class.forName(((RefType) t).getClassName());
            } catch (Exception e) {
                throw new MulibRuntimeException("Class not found", e);
            }
        }
    }

    private void initializeFieldViaCopy(
            Local thisLocal,
            Local toCopyLocal,
            Local mvtLocal,
            LocalSpawner localSpawner,
            SootField f,
            UnitPatchingChain upc,
            ChosenConstructor chosenConstructor) {
        assert mvtLocal.getType() == v.TYPE_MULIB_VALUE_COPIER;
        Type t = f.getType();
        Local toCopyValueLocal = localSpawner.spawnNewStackLocal(t);
        Local copiedValueLocal = localSpawner.spawnNewStackLocal(t);
        // Assign value to field; we already define it here so that we can jump to it. It is added at the
        // end of this method
        AssignStmt assignToField = Jimple.v().newAssignStmt(
                Jimple.v().newInstanceFieldRef(thisLocal, f.makeRef()),
                copiedValueLocal
        );

        /* GET VALUE FROM ORIGINAL FIELD */
        AssignStmt getField = Jimple.v().newAssignStmt(
                toCopyValueLocal,
                Jimple.v().newInstanceFieldRef(toCopyLocal, f.makeRef())
        );
        upc.add(getField);

        if (isPrimitiveOrSprimitive(t)) {
            copySprimitiveValue(toCopyValueLocal, copiedValueLocal, mvtLocal, localSpawner, upc);
        } else if (isSarray(t)) {
            createSarrayFromCopyOrTransformationConstructor(mvtLocal, toCopyValueLocal, copiedValueLocal, localSpawner, true, upc);
        } else {
            // Is partnerclass
            initializeObjectFieldInSpecialConstructor(
                    toCopyValueLocal, copiedValueLocal, t,
                    mvtLocal, assignToField, localSpawner, upc, chosenConstructor
            );
        }
        
        upc.add(assignToField);
    }

    private void copySprimitiveValue(
            Local toCopyValueLocal,
            Local copiedValueLocal,
            Local mvtLocal,
            LocalSpawner localSpawner,
            UnitPatchingChain upc) {
        Local copyCallResultLocal = localSpawner.spawnNewLocal(v.TYPE_OBJECT);
        AssignStmt copyCallAssign = Jimple.v().newAssignStmt(
                copyCallResultLocal,
                Jimple.v().newVirtualInvokeExpr(mvtLocal, v.SM_MULIB_VALUE_COPIER_COPY_SPRIMITIVE.makeRef(), toCopyValueLocal)
        );
        upc.add(copyCallAssign);
        AssignStmt castAssign = Jimple.v().newAssignStmt(
                copiedValueLocal,
                Jimple.v().newCastExpr(copyCallResultLocal, copiedValueLocal.getType())
        );
        upc.add(castAssign);
    }

    private void initializeFieldViaTransformation(
            Local thisLocal,
            Local originalLocal,
            Local mvtLocal,
            Local classLocal,
            Local fieldLocal,
            LocalSpawner localSpawner,
            SootField originalField,
            SootField transformedField,
            UnitPatchingChain upc,
            ChosenConstructor chosenConstructor) {
        assert mvtLocal.getType() == v.TYPE_MULIB_VALUE_TRANSFORMER;
        Type transformedType = transformedField.getType();
        Type originalType = originalField.getType();
        Local originalValue = localSpawner.spawnNewStackLocal(originalField.getType());
        Local transformedValue = localSpawner.spawnNewStackLocal(transformedType);
        // Assign value to field; we already define it here so that we can jump to it. It is added at the
        // end of this method
        AssignStmt assignToField = Jimple.v().newAssignStmt(
                Jimple.v().newInstanceFieldRef(thisLocal, transformedField.makeRef()),
                transformedValue
        );
        /* GET VALUE FROM ORIGINAL FIELD */
        addInstructionsToGetFieldPotentiallyWithReflection(
                thisLocal,
                classLocal, fieldLocal, localSpawner,
                originalLocal, originalValue,
                originalField, transformedField,
                upc
        );

        /* WRAP THE FIELD VALUE OR CREATE A NEW OBJECT FROM IT */
        if (isPrimitiveOrSprimitive(transformedType)) {
            // If the value is primitive, we can just wrap it. We must not use SymbolicExecution since it might be
            // not-initialized
            SootMethodRef wrapper;
            if (originalType instanceof IntType) {
                wrapper = v.SM_SINT_CONCSINT.makeRef();
            } else if (originalType instanceof LongType) {
                wrapper = v.SM_SLONG_CONCSLONG.makeRef();
            } else if (originalType instanceof DoubleType) {
                wrapper = v.SM_SDOUBLE_CONCSDOUBLE.makeRef();
            } else if (originalType instanceof FloatType) {
                wrapper = v.SM_SFLOAT_CONCSFLOAT.makeRef();
            } else if (originalType instanceof ShortType) {
                wrapper = v.SM_SSHORT_CONCSSHORT.makeRef();
            } else if (originalType instanceof ByteType) {
                wrapper = v.SM_SBYTE_CONCSBYTE.makeRef();
            } else if (originalType instanceof BooleanType) {
                wrapper = v.SM_SBOOL_CONCSBOOL.makeRef();
            } else if (originalType instanceof CharType) {
                wrapper = v.SM_SCHAR_CONCSCHAR.makeRef();
            } else {
                throw new NotYetImplementedException(originalType.toString());
            }
            AssignStmt wrapConstant = Jimple.v().newAssignStmt(
                    transformedValue,
                    Jimple.v().newStaticInvokeExpr(wrapper, originalValue)
            );
            upc.add(wrapConstant);
        } else if (isSarray(transformedType)) {
            createSarrayFromCopyOrTransformationConstructor(
                    mvtLocal, originalValue, transformedValue, localSpawner, false, upc);
        } else {
            // Is partner class
            initializeObjectFieldInSpecialConstructor(
                    originalValue, transformedValue, transformedType, 
                    mvtLocal, assignToField, localSpawner, upc, chosenConstructor
            );
        }

        upc.add(assignToField);
    }

    private void createSarrayFromCopyOrTransformationConstructor(
            Local mvtLocal,
            Local toCopyOrToTransform,
            Local assignToLocal,
            LocalSpawner localSpawner,
            boolean useCopyConstructor,
            UnitPatchingChain upc) {
        SootMethod used;
        if (useCopyConstructor) {
            used = v.SM_MULIB_VALUE_COPIER_COPY_NON_SPRIMITIVE;
        } else {
            used = v.SM_MULIB_VALUE_TRANSFORMER_TRANSFORM;
        }
        InvokeExpr invokeExpr = Jimple.v().newVirtualInvokeExpr(mvtLocal, used.makeRef(), toCopyOrToTransform);
        Local stillToCast = localSpawner.spawnNewStackLocal(v.TYPE_OBJECT);
        AssignStmt assignStmt = Jimple.v().newAssignStmt(stillToCast, invokeExpr);
        AssignStmt castToValue = Jimple.v().newAssignStmt(assignToLocal, Jimple.v().newCastExpr(stillToCast, assignToLocal.getType()));
        upc.add(assignStmt);
        upc.add(castToValue);
    }

    private void addInstructionsToGetFieldPotentiallyWithReflection(
            Local thisLocal,
            Local classLocal, Local fieldLocal, LocalSpawner localSpawner,
            Local toGetFromLocal, Local toStoreInLocal,
            SootField originalField, SootField transformedField,
            UnitPatchingChain upc) {
        boolean reflectionRequiredToGetFromField = calculateReflectionRequiredForField(originalField);
        if (reflectionRequiredToGetFromField) {
            Type originalType = originalField.getType();
            // Get field, store in fieldLocal, set accessible
            addInstructionsToInitializeFieldAndSetAccessible(thisLocal, classLocal, fieldLocal, originalField, upc);
            // Then, we execute Field.get(originalObject) to get the value (if there is a wrapper object involved, we still
            // need to unpack)
            Local tempFieldObjectLocal = localSpawner.spawnNewStackLocal(v.TYPE_OBJECT);
            AssignStmt tempObjectFieldVal = Jimple.v().newAssignStmt(
                    tempFieldObjectLocal,
                    Jimple.v().newVirtualInvokeExpr(fieldLocal, v.SM_FIELD_GET.makeRef(), toGetFromLocal)
            );
            upc.add(tempObjectFieldVal);
            Type typeOfFieldGet = wrapTypeIfNecessary(originalType);
            // Cast
            Local tempFieldValLocal = localSpawner.spawnNewStackLocal(typeOfFieldGet);
            AssignStmt castedTempFieldVal = Jimple.v().newAssignStmt(
                    tempFieldValLocal,
                    Jimple.v().newCastExpr(tempFieldObjectLocal, typeOfFieldGet)
            );
            upc.add(castedTempFieldVal);
            AssignStmt initFieldVal;
            if (originalType instanceof PrimType) {
                // We still have to unwrap, since Field.get(...) returns a wrapper object
                SootMethodRef toCall = getValueFromNumberMethodRef(originalType);
                initFieldVal = Jimple.v().newAssignStmt(
                        toStoreInLocal,
                        Jimple.v().newVirtualInvokeExpr(tempFieldValLocal, toCall)
                );
            } else {
                initFieldVal = Jimple.v().newAssignStmt(
                        toStoreInLocal,
                        tempFieldValLocal
                );
            }


            upc.add(initFieldVal);
        } else {
            // If we do not use reflection, we can simply execute a get
            AssignStmt getField = Jimple.v().newAssignStmt(
                    toStoreInLocal,
                    Jimple.v().newInstanceFieldRef(toGetFromLocal, originalField.makeRef())
            );
            upc.add(getField);
        }
    }

    private SootMethodRef getValueFromNumberMethodRef(Type t) {
        if (t instanceof IntType) {
            return v.SM_INTEGER_GETVAL.makeRef();
        } else if (t instanceof LongType) {
            return v.SM_LONG_GETVAL.makeRef();
        } else if (t instanceof DoubleType) {
            return v.SM_DOUBLE_GETVAL.makeRef();
        } else if (t instanceof FloatType) {
            return v.SM_FLOAT_GETVAL.makeRef();
        } else if (t instanceof ShortType) {
            return v.SM_SHORT_GETVAL.makeRef();
        } else if (t instanceof ByteType) {
            return v.SM_BYTE_GETVAL.makeRef();
        } else if (t instanceof BooleanType) {
            return v.SM_BOOLEAN_GETVAL.makeRef();
        } else if (t instanceof CharType) {
            return v.SM_CHARACTER_GETVAL.makeRef();
        } else {
            throw new NotYetImplementedException(String.valueOf(t.toString()));
        }
    }

    private Stmt addInstructionsToInitializeFieldAndSetAccessible(
            Local thisLocal, Local classLocal, Local fieldLocal, SootField field, UnitPatchingChain upc) {
        AssignStmt initClassVar = Jimple.v().newAssignStmt(
                classLocal,
                Jimple.v().newInterfaceInvokeExpr(thisLocal, v.SM_PARTNER_CLASS_GET_ORIGINAL_CLASS.makeRef())
        );
        upc.add(initClassVar);
        AssignStmt initFieldVar = Jimple.v().newAssignStmt(
                fieldLocal,
                Jimple.v().newVirtualInvokeExpr(classLocal, v.SM_CLASS_GET_DECLARED_FIELD.makeRef(), StringConstant.v(field.getName()))
        );
        upc.add(initFieldVar);
        InvokeStmt setAccessible = Jimple.v().newInvokeStmt(Jimple.v().newVirtualInvokeExpr(
                fieldLocal, v.SM_FIELD_SET_ACCESSIBLE.makeRef(), IntConstant.v(1)
        ));
        upc.add(setAccessible);
        return initClassVar;
    }
    
    private void initializeObjectFieldInSpecialConstructor(
            Local originValue, 
            Local resultValue,
            Type resultType,
            Local mvtLocal, 
            Unit fieldSetUnit, // We might need to jump to this instruction
            LocalSpawner localSpawner, 
            UnitPatchingChain upc,
            ChosenConstructor chosenConstructor) {
        assert chosenConstructor == ChosenConstructor.COPY_CONSTR || chosenConstructor == ChosenConstructor.TRANSFORMATION_CONSTR;
        boolean isTransformation = chosenConstructor == ChosenConstructor.TRANSFORMATION_CONSTR;
        // Is partner class
        // Check if originalValue == null
        AssignStmt assignNull = Jimple.v().newAssignStmt(resultValue, NullConstant.v());
        IfStmt nullCheck = Jimple.v().newIfStmt(Jimple.v().newEqExpr(originValue, NullConstant.v()), assignNull);
        // Check if retrieved original value is null (we add assignNull at the end)
        upc.add(nullCheck);

        // If the value is not null, we calculate whether the value has already been created
        VirtualInvokeExpr callAlreadyCreated =
                Jimple.v().newVirtualInvokeExpr(
                        mvtLocal,
                        isTransformation ? v.SM_MULIB_VALUE_TRANSFORMER_ALREADY_TRANSFORMED.makeRef() : v.SM_MULIB_VALUE_COPIER_ALREADY_COPIED.makeRef(),
                        originValue
                );
        Local stackAlreadyCreated = localSpawner.spawnNewStackLocal(v.TYPE_BOOL);
        AssignStmt computeIfAlreadyCreated =
                Jimple.v().newAssignStmt(stackAlreadyCreated, callAlreadyCreated);
        upc.add(computeIfAlreadyCreated);
        ConditionExpr wasAlreadyCreatedExpr = Jimple.v().newEqExpr(stackAlreadyCreated, IntConstant.v(1)); // Is true?
        // If the object was already created, we jump to get the copy from the value transformer
        VirtualInvokeExpr getCopy =
                Jimple.v().newVirtualInvokeExpr(
                        mvtLocal,
                        isTransformation ? v.SM_MULIB_VALUE_TRANSFORMER_GET_TRANSFORMED_OBJECT.makeRef() : v.SM_MULIB_VALUE_COPIER_GET_COPY.makeRef(),
                        originValue
                );
        Local stackLocalOfAlreadyCreatedObject = localSpawner.spawnNewStackLocal(v.TYPE_OBJECT);
        AssignStmt assignCopy = Jimple.v().newAssignStmt(stackLocalOfAlreadyCreatedObject, getCopy);
        CastExpr castedToExpr = Jimple.v().newCastExpr(stackLocalOfAlreadyCreatedObject, resultType);
        AssignStmt assignCastedCopy = Jimple.v().newAssignStmt(resultValue, castedToExpr);
        IfStmt alreadyCreatedCheck =
                Jimple.v().newIfStmt(wasAlreadyCreatedExpr, assignCopy);
        // Again, we need to add all statements for assigning the copy after treating the false-case
        upc.add(alreadyCreatedCheck);

        // If there was no copy, we initialize a new object using the constructor
        Local stackLocalOfNewObject =
                createStmtsForConstructorCall(
                        (RefType) resultType,
                        localSpawner,
                        upc,
                        List.of(isTransformation ? v.TYPE_OBJECT : originValue.getType(), isTransformation ? v.TYPE_MULIB_VALUE_TRANSFORMER : v.TYPE_MULIB_VALUE_COPIER),
                        List.of(originValue, mvtLocal)
                );
        upc.add(Jimple.v().newAssignStmt(resultValue, stackLocalOfNewObject));
        upc.add(Jimple.v().newGotoStmt(fieldSetUnit));

        upc.add(assignCopy);
        upc.add(assignCastedCopy);
        upc.add(Jimple.v().newGotoStmt(fieldSetUnit));

        // If the retrieved original value is null, we assign null
        upc.add(assignNull);
    }

    private void initializeFieldViaSymbolicExecution(
            Local thisLocal,
            Local seLocal,
            LocalSpawner localSpawner,
            SootField originalField,
            SootField transformedField,
            UnitPatchingChain upc) {
        assert seLocal.getType() == v.TYPE_SE;
        Type t = transformedField.getType();
        FieldRef fieldRef = Jimple.v().newInstanceFieldRef(thisLocal, transformedField.makeRef());
        if (isPrimitiveOrSprimitive(t)) {
            SootMethodRef initFieldMethodRef;
            if (isIntOrSint(t)) {
                initFieldMethodRef = v.SM_SE_FREE_SINT.makeRef();
            } else if (isLongOrSlong(t)) {
                initFieldMethodRef = v.SM_SE_FREE_SLONG.makeRef();
            } else if (isDoubleOrSdouble(t)) {
                initFieldMethodRef = v.SM_SE_FREE_SDOUBLE.makeRef();
            } else if (isFloatOrSfloat(t)) {
                initFieldMethodRef = v.SM_SE_FREE_SFLOAT.makeRef();
            } else if (isShortOrSshort(t)) {
                initFieldMethodRef = v.SM_SE_FREE_SSHORT.makeRef();
            } else if (isByteOrSbyte(t)) {
                initFieldMethodRef = v.SM_SE_FREE_SBYTE.makeRef();
            } else if (isBoolOrSbool(t)) {
                initFieldMethodRef = v.SM_SE_FREE_SBOOL.makeRef();
            } else if (isCharOrSchar(t)) {
                initFieldMethodRef = v.SM_SE_FREE_SCHAR.makeRef();
            } else {
                throw new NotYetImplementedException();
            }
            InvokeExpr invokeExpr = Jimple.v().newVirtualInvokeExpr(seLocal, initFieldMethodRef);
            Local stackLocal = localSpawner.spawnNewStackLocal(t);
            AssignStmt assignToStackLocal = Jimple.v().newAssignStmt(stackLocal, invokeExpr);
            upc.add(assignToStackLocal);
            AssignStmt assignToField = Jimple.v().newAssignStmt(fieldRef, stackLocal);
            upc.add(assignToField);
        } else if (t instanceof RefType) {
            RefType refType = (RefType) t;
            if (isSarray(t)) {
                SootMethod methodToUse;
                boolean isPartnerClassSarray = false;
                boolean isSarraySarray = false;
                if (refType.getClassName().equals(v.SC_SINTSARRAY.getName())) {
                    methodToUse = v.SM_SE_SINTSARRAY;
                } else if (refType.getClassName().equals(v.SC_SLONGSARRAY.getName())) {
                    methodToUse = v.SM_SE_SLONGSARRAY;
                } else if (refType.getClassName().equals(v.SC_SDOUBLESARRAY.getName())) {
                    methodToUse = v.SM_SE_SDOUBLESARRAY;
                } else if (refType.getClassName().equals(v.SC_SFLOATSARRAY.getName())) {
                    methodToUse = v.SM_SE_SFLOATSARRAY;
                } else if (refType.getClassName().equals(v.SC_SSHORTSARRAY.getName())) {
                    methodToUse = v.SM_SE_SSHORTSARRAY;
                } else if (refType.getClassName().equals(v.SC_SBYTESARRAY.getName())) {
                    methodToUse = v.SM_SE_SBYTESARRAY;
                } else if (refType.getClassName().equals(v.SC_SBOOLSARRAY.getName())) {
                    methodToUse = v.SM_SE_SBOOLSARRAY;
                } else if (refType.getClassName().equals(v.SC_SCHARSARRAY.getName())) {
                    methodToUse = v.SM_SE_SCHARSARRAY;
                } else if (isSarraySarray(refType)) {
                    methodToUse = v.SM_SE_SARRAYSARRAY;
                    isSarraySarray = true;
                } else if (isPartnerClassSarray(refType)) {
                    methodToUse = v.SM_SE_PARTNER_CLASSSARRAY;
                    isPartnerClassSarray = true;
                } else {
                    throw new NotYetImplementedException(refType.getClassName());
                }
                Local symbolicLengthLocal = localSpawner.spawnNewStackLocal(v.TYPE_SINT);
                AssignStmt assignSymbolicLength =
                        Jimple.v().newAssignStmt(
                                symbolicLengthLocal,
                                Jimple.v().newVirtualInvokeExpr(seLocal, v.SM_SE_FREE_SINT.makeRef()));
                upc.add(assignSymbolicLength);

                VirtualInvokeExpr initializeSymbolicArray;
                if (isPartnerClassSarray || isSarraySarray) {
                    ArrayType arrayType = (ArrayType) transformArrayType((ArrayType) originalField.getType(), false);
                    Type elementType = arrayType.getElementType();
                    ClassConstant classConstantOfSarraySarrayOrPartnerClassSarray = ClassConstant.fromType(elementType);
                    initializeSymbolicArray =
                            Jimple.v().newVirtualInvokeExpr(
                                    seLocal,
                                    methodToUse.makeRef(),
                                    symbolicLengthLocal,
                                    classConstantOfSarraySarrayOrPartnerClassSarray,
                                    IntConstant.v(1)
                            );
                } else {
                    initializeSymbolicArray =
                            Jimple.v().newVirtualInvokeExpr(
                                    seLocal,
                                    methodToUse.makeRef(),
                                    symbolicLengthLocal,
                                    IntConstant.v(1)
                            );
                }
                Local stackLocalForSarray = localSpawner.spawnNewStackLocal(initializeSymbolicArray.getType());
                AssignStmt assignToStackLocal = Jimple.v().newAssignStmt(stackLocalForSarray, initializeSymbolicArray);
                upc.add(assignToStackLocal);
                Local castedLocal = localSpawner.spawnNewStackLocal(fieldRef.getType());
                upc.add(Jimple.v().newAssignStmt(castedLocal, Jimple.v().newCastExpr(stackLocalForSarray, fieldRef.getType())));
                AssignStmt assignToField = Jimple.v().newAssignStmt(fieldRef, castedLocal);
                upc.add(assignToField);
            } else {
                // Is partnerclass
                Local stackLocalForNew =
                        createStmtsForConstructorCall((RefType) t, localSpawner, upc, List.of(v.TYPE_SE), List.of(seLocal));
                AssignStmt assignToField = Jimple.v().newAssignStmt(fieldRef, stackLocalForNew);
                upc.add(assignToField);
            }
        } else {
            throw new NotYetImplementedException(t.toString());
        }
    }

    // Returns stack local of new value
    private Local createStmtsForConstructorCall(
            RefType transformedType,
            LocalSpawner localSpawner,
            UnitPatchingChain upc,
            List<Type> constructorParameterTypes,
            List<Value> constructorArguments) {
        String className = transformedType.getClassName();
        String originalClassName = className.replace(_TRANSFORMATION_INDICATOR, "");
        Local stackLocalForNew = localSpawner.spawnNewStackLocal(transformedType);
        NewExpr newExpr = Jimple.v().newNewExpr(transformedType);
        AssignStmt assignNewToStackLocal = Jimple.v().newAssignStmt(stackLocalForNew, newExpr);
        SootClass sootClassToInitialize = transformEnrichAndValidateIfNotSpecialCase(originalClassName);
        SootMethodRef invokeSpecialRef;
        SpecialInvokeExpr invokeConstructorExpr;
        if (shouldBeTransformed(originalClassName)) {
            invokeSpecialRef = Scene.v().makeConstructorRef(sootClassToInitialize, constructorParameterTypes);
            invokeConstructorExpr = Jimple.v().newSpecialInvokeExpr(stackLocalForNew, invokeSpecialRef, constructorArguments);
        } else {
            invokeSpecialRef = Scene.v().makeConstructorRef(sootClassToInitialize, List.of());
            invokeConstructorExpr = Jimple.v().newSpecialInvokeExpr(stackLocalForNew, invokeSpecialRef);
        }

        upc.add(assignNewToStackLocal);
        upc.add(Jimple.v().newInvokeStmt(invokeConstructorExpr));
        return stackLocalForNew;
    }

    private final Map<SootField, SootMethodRef> fieldAccessorMethods = new HashMap<>();
    private final Map<SootField, SootMethodRef> fieldSetterMethods = new HashMap<>();
    @Override
    protected void generateAccessorAndSetterMethodsForFieldsAndDiscardIsFinal(SootClass old, SootClass result) {
        Collection<SootField> oldFields = old.getFields();
        for (SootField sf : oldFields) {
            SootField newField = getTransformedFieldForOldField(sf, result);
            if (!fieldWasTransformedAndIsNonStatic(newField, sf.getType(), newField.getType())) {
                if (newField.isFinal()) {
                    newField.setModifiers(newField.getModifiers() - Modifier.FINAL);
                }
                continue;
            }
            SootMethodRef accessor = generateAndAddAccessorMethod(sf, result, newField);
            fieldAccessorMethods.put(newField, accessor);
            if (sf.isFinal()) {
                // Remove isFinal in preparation for enabling lazy initialization
                newField.setModifiers(newField.getModifiers() - Modifier.FINAL);
                // Previously final methods do not need setters
                continue;
            }
            SootMethodRef setter = generateAndAddSetMethod(sf, result, newField);
            fieldSetterMethods.put(newField, setter);
        }
    }

    private boolean fieldWasTransformedAndIsNonStatic(SootField newField, Type oldFieldType, Type newFieldType) {
        return !newField.isStatic() && !oldFieldType.equals(newFieldType);
    }

    private SootMethodRef generateAndAddAccessorMethod(SootField old, SootClass addTo, SootField forField) {
        return generateAndAddFieldMethod(old, addTo, forField, true);
    }

    private SootMethodRef generateAndAddSetMethod(SootField old, SootClass addTo, SootField forField) {
        return generateAndAddFieldMethod(old, addTo, forField, false);
    }

    private SootMethodRef generateAndAddFieldMethod(SootField oldField, SootClass addTo, SootField forField, boolean forGetField) {
        // Create method
        SootMethod artificialFieldMethod;
        if (forGetField) {
            artificialFieldMethod = new SootMethod(
                    _ACCESSOR_PREFIX + forField.getName(),
                    List.of(),
                    forField.getType(),
                    forField.getModifiers()
            );
        } else {
            artificialFieldMethod = new SootMethod(
                    _SETTER_PREFIX + forField.getName(),
                    List.of(forField.getType()),
                    v.TYPE_VOID,
                    forField.getModifiers()
            );
        }
        // Create body
        JimpleBody b = Jimple.v().newBody(artificialFieldMethod);
        artificialFieldMethod.setActiveBody(b);
        LocalSpawner localSpawner = new LocalSpawner(b);
        // Create locals for body
        Local thisLocal = localSpawner.spawnNewLocal(addTo.getType());
        // Only used if !forGetField
        Local setParamLocal = null;
        // Get unit chain
        UnitPatchingChain upc = b.getUnits();
        // Create identity statement for SymbolicExecution parameter
        upc.add(Jimple.v().newIdentityStmt(thisLocal, Jimple.v().newThisRef(addTo.getType())));
        if (!forGetField) {
            // If we set the field, we add a respective local
            setParamLocal = localSpawner.spawnNewLocal(forField.getType());
            upc.add(Jimple.v().newIdentityStmt(setParamLocal, Jimple.v().newParameterRef(forField.getType(), 0)));
        }
        // Jump to null check, if we do not have to lazily initialize:
        InvokeStmt nullCheckStmt = Jimple.v().newInvokeStmt(
                Jimple.v().newInterfaceInvokeExpr(thisLocal, v.SM_PARTNER_CLASS_NULL_CHECK.makeRef())
        );
//        if (forGetField) { // TODO For simplicity, we currently also lazily initialize if a field is set.
        {
            // Check if we still must lazily initialize. We only need to do this for gets
            Local mustBeLazilyInitialized = localSpawner.spawnNewStackLocal(v.TYPE_BOOL);
            upc.add(
                    Jimple.v().newAssignStmt(
                            mustBeLazilyInitialized,
                            Jimple.v().newInterfaceInvokeExpr(thisLocal, v.SM_PARTNER_CLASS_IS_TO_BE_LAZILY_INITIALIZED.makeRef())
                    )
            );

            upc.add(Jimple.v().newIfStmt(Jimple.v().newEqExpr(mustBeLazilyInitialized, IntConstant.v(0)), nullCheckStmt));
            Local seLocal = localSpawner.spawnNewStackLocal(v.TYPE_SE);
            upc.add(Jimple.v().newAssignStmt(seLocal, Jimple.v().newStaticInvokeExpr(v.SM_SE_GET.makeRef())));
            upc.add(Jimple.v().newInvokeStmt(Jimple.v().newVirtualInvokeExpr(seLocal, v.SM_SE_INITIALIZE_LAZY_FIELDS.makeRef(), thisLocal)));
        }
//        }
        // Call nullCheck
        upc.add(nullCheckStmt);

        // We do not add yet since we might need to jump
        Stmt getOrSetField;
        Stmt returnStmt;
        if (forGetField) {
            // We can directly access the field of this
            Local returnLocal = localSpawner.spawnNewStackLocal(forField.getType());
            getOrSetField = Jimple.v().newAssignStmt(
                    returnLocal,
                    Jimple.v().newInstanceFieldRef(thisLocal, forField.makeRef())
            );
            // Return field
            returnStmt = Jimple.v().newReturnStmt(returnLocal);
        } else {
            getOrSetField = Jimple.v().newAssignStmt(
                    Jimple.v().newInstanceFieldRef(thisLocal, forField.makeRef()),
                    setParamLocal
            );
            returnStmt = Jimple.v().newReturnVoidStmt();
        }

        Local cacheIsBlockedLocal = localSpawner.spawnNewStackLocal(v.TYPE_BOOL);
        upc.add(Jimple.v().newAssignStmt(
                cacheIsBlockedLocal,
                Jimple.v().newInterfaceInvokeExpr(thisLocal, v.SM_PARTNER_CLASS_CACHE_IS_BLOCKED.makeRef())
        ));
        // Before getting or setting we must check if the "cache", i.e., the fields, are blocked. In this case,
        // we must ask the constraint solver for a new value or change the representation of this object in the
        // constraint solver
        upc.add(Jimple.v().newIfStmt(
                Jimple.v().newEqExpr(cacheIsBlockedLocal, IntConstant.v(0)),
                getOrSetField
        ));
        Local seLocal = localSpawner.spawnNewStackLocal(v.TYPE_SE);
        // Get SymbolicExecution
        upc.add(Jimple.v().newAssignStmt(
                seLocal,
                Jimple.v().newStaticInvokeExpr(v.SM_SE_GET.makeRef())
        ));
        // If the cache is blocked, we contact the solver via SymbolicExecution. We return before returning or setting
        // the field's value
        if (forGetField) {
            Local retrievedValueLocal = localSpawner.spawnNewStackLocal(forField.getType());
            upc.add(Jimple.v().newAssignStmt(
                    retrievedValueLocal,
                    Jimple.v().newVirtualInvokeExpr(
                            seLocal, v.SM_SE_GET_FIELD.makeRef(),
                            thisLocal,
                            StringConstant.v(forField.getDeclaringClass().getName() + "." + forField.getName()),
                            // Preserve information on arrays
                            ClassConstant.fromType(oldField.getType() instanceof ArrayType ?
                                    transformArrayType((ArrayType) oldField.getType(), false)
                                    :
                                    transformType(oldField.getType())
                            )
                    )
            ));
            Local castedRetrievedValueLocal = localSpawner.spawnNewLocal(forField.getType());
            upc.add(Jimple.v().newAssignStmt(
                    castedRetrievedValueLocal,
                    Jimple.v().newCastExpr(retrievedValueLocal, forField.getType())
            ));
            upc.add(Jimple.v().newReturnStmt(castedRetrievedValueLocal));
        } else {
            upc.add(Jimple.v().newInvokeStmt(
                    Jimple.v().newVirtualInvokeExpr(
                            seLocal, v.SM_SE_PUT_FIELD.makeRef(),
                            thisLocal, StringConstant.v(forField.getDeclaringClass().getName() + "." + forField.getName()), setParamLocal)
            ));
            upc.add(Jimple.v().newReturnVoidStmt());
        }

        // Set or get field
        upc.add(getOrSetField);
        // Return result or void
        upc.add(returnStmt);
        artificialFieldMethod.setDeclaringClass(addTo);
        addTo.addMethod(artificialFieldMethod);
        return artificialFieldMethod.makeRef();
    }

    @Override
    protected void replaceGetFieldsAndPutFieldsWithGeneratedMethods(SootClass old, SootClass replaceIn) {
        for (SootMethod sm : replaceIn.getMethods()) {
            // Exclude abstract methods and those generated by Mulib
            if (sm.isAbstract()
                    || isSyntheticMethod(sm)
                    // Exclude any other constructor as well ('this' cannot be null or aliasing in there)
                    || sm.getName().equals(init)) {
                continue;
            }
            JimpleBody jb = (JimpleBody) sm.retrieveActiveBody();
            UnitPatchingChain upc = jb.getUnits();
            Stmt[] upcAr = upc.toArray(Stmt[]::new);
            AssignStmt[] getFieldAndPutFieldInsns =
                    Arrays.stream(upcAr)
                            .filter(u -> u instanceof AssignStmt && u.containsFieldRef()
                                    && u.getFieldRef() instanceof InstanceFieldRef)
                            .toArray(AssignStmt[]::new);
            for (AssignStmt stmt : getFieldAndPutFieldInsns) {
                InstanceFieldRef fieldRef = (InstanceFieldRef) stmt.getFieldRef();
                Local base = (Local) fieldRef.getBase();
                boolean isGetfield = stmt.getRightOp() instanceof FieldRef;
                assert !isGetfield || stmt.getLeftOp() instanceof Local
                        : "Assumption violated; - only one side can be a field ref in Jimple";
                SootMethodRef fieldMethodRef;
                if (isGetfield) {
                    fieldMethodRef = fieldAccessorMethods.get(fieldRef.getField());
                } else {
                    fieldMethodRef = fieldSetterMethods.get(fieldRef.getField());
                }
                if (fieldMethodRef == null) {
                    continue;
                }
                InvokeExpr iie;
                if (isGetfield) {
                    // Replace GETFIELD
                    iie = Jimple.v().newVirtualInvokeExpr(base, fieldMethodRef);
                    AssignStmt assignFieldValueToLocal = Jimple.v().newAssignStmt(
                            stmt.getLeftOp(),
                            iie
                    );
                    upc.insertBefore(assignFieldValueToLocal, stmt);
                } else {
                    // Replace PUTFIELD
                    iie = Jimple.v().newVirtualInvokeExpr(
                            base,
                            fieldMethodRef,
                            stmt.getRightOp()
                    );
                    InvokeStmt putFieldCall = Jimple.v().newInvokeStmt(iie);
                    upc.insertBefore(putFieldCall, stmt);
                }
                upc.remove(stmt);
            }
        }
    }

    private boolean isSyntheticMethod(SootMethod sm) {
        // Synthetic methods themselves, symbolic execution-, transformation- and copying-constructors as
        // well as the label method
        return sm.getName().contains(_TRANSFORMATION_INDICATOR)
                // Exclude any constructor ('this' cannot be null in there)
                || (sm.getName().equals(init) &&
                (sm.getParameterTypes().contains(v.TYPE_SE)
                        || sm.getParameterTypes().contains(v.TYPE_MULIB_VALUE_COPIER)
                        || sm.getParameterTypes().contains(v.TYPE_MULIB_VALUE_TRANSFORMER)))
                // label method
                || (sm.getName().equals("label") && sm.getParameterTypes().equals(List.of(v.TYPE_OBJECT, v.TYPE_SOLVER_MANAGER)))
                // copy method
                || (sm.getName().equals("copy") && sm.getParameterTypes().equals(List.of(v.TYPE_MULIB_VALUE_COPIER)));
    }

    @Override
    protected void replaceStaticFieldInsnsWithGeneratedMethods(SootClass old, SootClass replaceIn) {
        for (SootMethod sm : replaceIn.getMethods()) {
            // Exclude abstract methods and synthetic methods; - we do not ever use static fields there that
            // need replacing.
            if (sm.isAbstract() || isSyntheticMethod(sm) || sm.getName().equals(clinit)) {
                continue;
            }
            JimpleBody jb = (JimpleBody) sm.retrieveActiveBody();
            UnitPatchingChain upc = jb.getUnits();
            Stmt[] upcAr = upc.toArray(Stmt[]::new);
            AssignStmt[] staticFieldInsns =
                    Arrays.stream(upcAr)
                            .filter(u -> u instanceof AssignStmt
                                    && u.containsFieldRef() && u.getFieldRef() instanceof StaticFieldRef
                                    && transformedClassNodes.values().stream().anyMatch(tcn -> tcn.getName().equals(u.getFieldRef().getField().getDeclaringClass().getName())))
                            .toArray(AssignStmt[]::new);
            if (staticFieldInsns.length == 0) {
                continue;
            }
            LocalSpawner ls = new LocalSpawner(jb);
            Optional<Local> sel = jb.getLocals().stream().filter(f -> f.getType().equals(v.TYPE_SE)).findFirst();
            if (sel.isEmpty()) {
                continue;
            }
            Local seLocal = sel.get();
            for (AssignStmt stmt : staticFieldInsns) {
                StaticFieldRef fieldRef = (StaticFieldRef) stmt.getFieldRef();
                boolean isStaticGet = stmt.getRightOp() instanceof FieldRef;
                assert !isStaticGet || stmt.getLeftOp() instanceof Local
                        : "Assumption violated; - only one side can be a field ref in Jimple";
                SootMethodRef fieldMethodRef;
                if (isStaticGet) {
                    fieldMethodRef = v.SM_SE_GET_STATIC_FIELD.makeRef();
                } else {
                    fieldMethodRef = v.SM_SE_SET_STATIC_FIELD.makeRef();
                }

                String fieldName = fieldRef.getField().getDeclaringClass().getName()
                        + "." + fieldRef.getField().getName();
                StringConstant fieldNameStringConstant = StringConstant.v(fieldName);
                InvokeExpr iie;
                if (isStaticGet) {
                    // Replace GETSTATIC
                    iie = Jimple.v().newVirtualInvokeExpr(
                            seLocal,
                            fieldMethodRef,
                            fieldNameStringConstant
                    );
                    Local toCast = ls.spawnNewStackLocal(v.TYPE_OBJECT);
                    AssignStmt toCastStmt = Jimple.v().newAssignStmt(
                            toCast,
                            iie
                    );
                    AssignStmt castStmt = Jimple.v().newAssignStmt(
                            stmt.getLeftOp(),
                            Jimple.v().newCastExpr(toCast, stmt.getLeftOp().getType())
                    );
                    upc.insertBefore(List.of(toCastStmt, castStmt), stmt);
                } else {
                    // Replace PUTSTATIC
                    iie = Jimple.v().newVirtualInvokeExpr(
                            seLocal,
                            fieldMethodRef,
                            fieldNameStringConstant,
                            stmt.getRightOp()
                    );
                    InvokeStmt putFieldCall = Jimple.v().newInvokeStmt(iie);
                    upc.insertBefore(putFieldCall, stmt);
                }
                upc.remove(stmt);
            }
        }
    }

    private static void addSuperCallToUpcIfNeeded(
            Local thisLocal,
            SootClass result,
            String methodName,
            List<Type> paramTypes,
            List<Value> params,
            Type returnType,
            UnitPatchingChain upc) {
        if (result.getSuperclass().getName().contains(_TRANSFORMATION_INDICATOR)) {
            // Add super-call
            InvokeStmt invokeSuperCall = Jimple.v().newInvokeStmt(
                    Jimple.v().newSpecialInvokeExpr(
                            thisLocal,
                            Scene.v().makeMethodRef(
                                    result.getSuperclass(),
                                    methodName,
                                    paramTypes,
                                    returnType,
                                    false
                            ),
                            params));
            upc.add(invokeSuperCall);
        }
    }

    @Override
    protected void generateBlockCacheInPartnerClassFieldsAndInitializeLazyFieldsAndGetFieldNameToSubstitutedVar(SootClass old, SootClass result) {
        // TODO Split up into finer template methods
        // TODO Regard Exception and RuntimeException dedicatedly
        boolean superClassIsTransformed = result.getSuperclass().getName().contains(_TRANSFORMATION_INDICATOR);

        // If we block the cache, we will also call to block the cache in the objects (indirectly) contained
        // in a field
        SootMethod blockCacheInPartnerClassFields;
        {
            final String methodName = _TRANSFORMATION_INDICATOR + "blockCacheInPartnerClassFields";
            final List<Type> paramTypes = List.of();
            final Type returnType = v.TYPE_VOID;
            blockCacheInPartnerClassFields = new SootMethod(methodName, paramTypes, returnType,  Modifier.PUBLIC);
            JimpleBody jb = Jimple.v().newBody(blockCacheInPartnerClassFields);
            blockCacheInPartnerClassFields.setActiveBody(jb);
            UnitPatchingChain upc = jb.getUnits();
            LocalSpawner localSpawner = new LocalSpawner(jb);
            Local thisLocal = localSpawner.spawnNewLocal(result.getType());
            IdentityStmt identityStmt = Jimple.v().newIdentityStmt(thisLocal, Jimple.v().newThisRef(result.getType()));
            upc.add(identityStmt);
            addSuperCallToUpcIfNeeded(thisLocal, result, methodName, paramTypes, List.of(), returnType, upc);
            Collection<SootField> oldFields = old.getFields();
            Stmt returnStmt = Jimple.v().newReturnVoidStmt();
            upc.add(returnStmt);
            Stmt next = returnStmt;
            for (SootField sf : oldFields) {
                SootField newField = getTransformedFieldForOldField(sf, result);
                if (!fieldWasTransformedAndIsNonStatic(newField, sf.getType(), newField.getType()) || !(sf.getType() instanceof RefLikeType)) {
                    continue;
                }
                Local getFieldLocal = localSpawner.spawnNewStackLocal(newField.getType());
                Stmt getFieldStmt = Jimple.v().newAssignStmt(getFieldLocal, Jimple.v().newInstanceFieldRef(thisLocal, newField.makeRef()));
                Stmt checkIsNull = Jimple.v().newIfStmt(Jimple.v().newEqExpr(NullConstant.v(), getFieldLocal), next);
                Stmt blockCacheStmt = Jimple.v().newInvokeStmt(Jimple.v().newInterfaceInvokeExpr(getFieldLocal, v.SM_PARTNER_CLASS_BLOCK_CACHE.makeRef()));
                upc.insertBeforeNoRedirect(getFieldStmt, next);
                upc.insertBeforeNoRedirect(checkIsNull, next);
                upc.insertBeforeNoRedirect(blockCacheStmt, next);
                next = getFieldStmt;
            }
            blockCacheInPartnerClassFields.setDeclaringClass(result);
            result.addMethod(blockCacheInPartnerClassFields);
        }

        {
            String methodName = _TRANSFORMATION_INDICATOR + "initializeLazyFields";
            List<Type> paramTypes = List.of(v.TYPE_SE);
            Type returnType = v.TYPE_VOID;
            SootMethod initializeLazyFields = new SootMethod(methodName, paramTypes, returnType, Modifier.PUBLIC);
            JimpleBody jb = Jimple.v().newBody(initializeLazyFields);
            initializeLazyFields.setActiveBody(jb);
            UnitPatchingChain upc = jb.getUnits();
            LocalSpawner localSpawner = new LocalSpawner(jb);
            Local thisLocal = localSpawner.spawnNewLocal(result.getType());
            Local seLocal = localSpawner.spawnNewLocal(v.TYPE_SE);
            upc.add(Jimple.v().newIdentityStmt(thisLocal, Jimple.v().newThisRef(result.getType())));
            upc.add(Jimple.v().newIdentityStmt(seLocal, Jimple.v().newParameterRef(v.TYPE_SE, 0)));
            addSuperCallToUpcIfNeeded(thisLocal, result, methodName, paramTypes, List.of(seLocal), returnType, upc);
            Collection<SootField> oldFields = old.getFields();
            SootField resultOuterClassField = null;
            if (old.isInnerClass()) {
                String outerClassFieldName = getOuterClassField(old);
                if (outerClassFieldName != null) {
                    resultOuterClassField = result.getField(outerClassFieldName, result.getOuterClass().getType());
                }

            }
            for (SootField sf : oldFields) {
                SootField newField = getTransformedFieldForOldField(sf, result);
                if (!fieldWasTransformedAndIsNonStatic(newField, sf.getType(), newField.getType())) {
                    continue;
                }
                if (newField == resultOuterClassField) {
                    // We have already set this
                    continue;
                }
                initializeFieldViaSymbolicExecution(thisLocal, seLocal, localSpawner, sf, newField, upc);
            }
            upc.add(Jimple.v().newReturnVoidStmt());
            initializeLazyFields.setDeclaringClass(result);
            result.addMethod(initializeLazyFields);
        }
        {
            // Generate getFieldNameToSubstitutedVar
            String methodName = _TRANSFORMATION_INDICATOR + "getFieldNameToSubstitutedVar";
            List<Type> paramTypes = List.of();
            Type returnType = v.SC_MAP.getType();

            SootMethod getFieldNameToSubstitutedVar =
                    new SootMethod(methodName, paramTypes, returnType, Modifier.PUBLIC);
            JimpleBody jb = Jimple.v().newBody(getFieldNameToSubstitutedVar);
            getFieldNameToSubstitutedVar.setActiveBody(jb);
            UnitPatchingChain upc = jb.getUnits();
            LocalSpawner localSpawner = new LocalSpawner(jb);
            Local thisLocal = localSpawner.spawnNewLocal(result.getType());
            IdentityStmt identityStmt = Jimple.v().newIdentityStmt(thisLocal, Jimple.v().newThisRef(result.getType()));
            upc.add(identityStmt);
            Local mapLocal = localSpawner.spawnNewLocal(v.SC_MAP.getType());
            if (!superClassIsTransformed) {
                NewExpr newExpr = Jimple.v().newNewExpr(v.SC_HASH_MAP.getType());
                SpecialInvokeExpr invokeSpecialExpr =
                        Jimple.v().newSpecialInvokeExpr(mapLocal, Scene.v().makeConstructorRef(v.SC_HASH_MAP, List.of()));
                upc.add(Jimple.v().newAssignStmt(mapLocal, newExpr));
                upc.add(Jimple.v().newInvokeStmt(invokeSpecialExpr));
            } else {
                upc.add(Jimple.v().newAssignStmt(
                        mapLocal,
                        Jimple.v().newSpecialInvokeExpr(
                                thisLocal,
                                Scene.v().makeMethodRef(result.getSuperclass(), methodName, paramTypes, returnType, false))
                ));
            }

            Collection<SootField> oldFields = old.getFields();
            for (SootField sf : oldFields) {
                SootField newField = getTransformedFieldForOldField(sf, result);
                if (!fieldWasTransformedAndIsNonStatic(newField, sf.getType(), newField.getType())) {
                    continue;
                }
                // Push name of field
                Local nameOfFieldLocal = localSpawner.spawnNewStackLocal(v.TYPE_STRING);
                upc.add(Jimple.v().newAssignStmt(nameOfFieldLocal, StringConstant.v(newField.getDeclaringClass().getName() + "." + newField.getName())));
                // Get field
                Local getFieldLocal = localSpawner.spawnNewStackLocal(newField.getType());
                upc.add(Jimple.v().newAssignStmt(getFieldLocal, Jimple.v().newInstanceFieldRef(thisLocal, newField.makeRef())));
                // Add field to Map
                upc.add(Jimple.v().newInvokeStmt(Jimple.v().newInterfaceInvokeExpr(mapLocal, v.SM_MAP_PUT.makeRef(), nameOfFieldLocal, getFieldLocal)));
            }

            upc.add(Jimple.v().newReturnStmt(mapLocal));
            getFieldNameToSubstitutedVar.setDeclaringClass(result);
            result.addMethod(getFieldNameToSubstitutedVar);
        }
    }

    protected final boolean calculateReflectionRequiredForField(SootField originalField) {
        if (reflectionRequiredSinceClassIsNotVisible(originalField.getDeclaringClass())) {
            return true;
        }
        if (originalField.getDeclaringClass().getPackageName().startsWith("java")) {
            return true;
        }
        int access = originalField.getModifiers();
        if (java.lang.reflect.Modifier.isStatic(access)) {
            return false;
        }
        if (config.TRANSF_LOAD_WITH_SYSTEM_CLASSLOADER) {
            return java.lang.reflect.Modifier.isPrivate(access) || java.lang.reflect.Modifier.isFinal(access);
        } else {
            // Otherwise, it is in another module and friendly as well as protected fields cannot be accessed
            // without reflection
            return !java.lang.reflect.Modifier.isPublic(access) || java.lang.reflect.Modifier.isFinal(access);
        }
    }

    @Override
    protected void generateAndAddSymbolicExecutionConstructor(SootClass old, SootClass result) {
        generateSpecializedConstructor(old, result, ChosenConstructor.SE_CONSTR);
    }

    @Override
    protected void generateAndAddTransformationConstructor(SootClass old, SootClass result) {
        generateSpecializedConstructor(old, result, ChosenConstructor.TRANSFORMATION_CONSTR);
    }

    @Override
    protected void generateAndAddCopyConstructor(SootClass old, SootClass result) {
        generateSpecializedConstructor(old, result, ChosenConstructor.COPY_CONSTR);
    }

    @Override
    protected void generateAndAddCopyMethod(SootClass old, SootClass result) {
        if (result.isAbstract()) {
            return;
        }
        // Create copy method
        SootMethod copyMethod =
                new SootMethod("copy", List.of(v.TYPE_MULIB_VALUE_COPIER), v.TYPE_OBJECT, Modifier.PUBLIC);

        // Create parameter locals for method
        JimpleBody b = Jimple.v().newBody(copyMethod);
        copyMethod.setActiveBody(b);
        LocalSpawner localSpawner = new LocalSpawner(b);
        // Create locals for body
        Local thisLocal = localSpawner.spawnNewLocal(result.getType());
        Local mvtLocal = localSpawner.spawnNewLocal(v.TYPE_MULIB_VALUE_COPIER);

        // Get unit chain to add instructions to
        UnitPatchingChain upc = b.getUnits();
        // Create identity statement for parameter locals
        upc.add(Jimple.v().newIdentityStmt(thisLocal, Jimple.v().newThisRef(result.getType())));
        upc.add(Jimple.v().newIdentityStmt(mvtLocal, Jimple.v().newParameterRef(v.TYPE_MULIB_VALUE_COPIER, 0)));

        Local stackResultOfCopyConstructor = createStmtsForConstructorCall(
                    result.getType(), localSpawner, upc,
                    List.of(result.getType(), v.TYPE_MULIB_VALUE_COPIER),
                    List.of(thisLocal, mvtLocal)
                );
        upc.add(Jimple.v().newReturnStmt(stackResultOfCopyConstructor));

        copyMethod.setDeclaringClass(result);
        result.addMethod(copyMethod);
    }

    @Override
    protected void generateAndAddLabelTypeMethod(SootClass old, SootClass result) {
        // Create label method
        SootMethod labelMethod =
                new SootMethod(
                        "label",
                        List.of(v.TYPE_OBJECT, v.TYPE_SOLVER_MANAGER),
                        v.TYPE_OBJECT,
                        Modifier.PUBLIC
                );

        // Create parameter locals for method
        JimpleBody b = Jimple.v().newBody(labelMethod);
        labelMethod.setActiveBody(b);
        LocalSpawner localSpawner = new LocalSpawner(b);
        // Create locals for body
        Local thisLocal = localSpawner.spawnNewLocal(result.getType());
        Local labelTo = localSpawner.spawnNewLocal(v.TYPE_OBJECT);
        Local smLocal = localSpawner.spawnNewLocal(v.TYPE_SOLVER_MANAGER);

        // Get unit chain to add instructions to
        UnitPatchingChain upc = b.getUnits();
        // Create identity statement for parameter locals
        upc.add(Jimple.v().newIdentityStmt(thisLocal, Jimple.v().newThisRef(result.getType())));
        upc.add(Jimple.v().newIdentityStmt(labelTo, Jimple.v().newParameterRef(v.TYPE_OBJECT, 0)));
        Stmt lastIdentity = Jimple.v().newIdentityStmt(smLocal, Jimple.v().newParameterRef(v.TYPE_SOLVER_MANAGER, 1));
        upc.add(lastIdentity);
        Local castedToLabelTo;
        if (!reflectionRequiredSinceClassIsNotVisible(old)) {
            castedToLabelTo = localSpawner.spawnNewLocal(old.getType());
            AssignStmt castObjectToActualType = Jimple.v().newAssignStmt(
                    castedToLabelTo,
                    Jimple.v().newCastExpr(labelTo, old.getType())
            );
            upc.add(castObjectToActualType);
        } else {
            castedToLabelTo = labelTo;
        }

        boolean reflectionRequired = calculateReflectionRequired(old);
        Local classLocal = null, fieldLocal = null, exceptionLocal = null;
        if (reflectionRequired) {
            classLocal = localSpawner.spawnNewLocal(v.TYPE_CLASS);
            fieldLocal = localSpawner.spawnNewLocal(v.TYPE_FIELD);
            exceptionLocal = localSpawner.spawnNewLocal(v.TYPE_EXCEPTION);
        }

        for (SootField oldField : old.getFields()) {
            SootField transformedField = getTransformedFieldForOldField(oldField, result);
            if (transformedField.isStatic()) {
                continue;
            }
            /* GET VALUE OF ORIGINAL FIELD */
            Local transformedValue = localSpawner.spawnNewStackLocal(transformedField.getType());
            // We can directly access the field of this
            AssignStmt getField = Jimple.v().newAssignStmt(
                    transformedValue,
                    Jimple.v().newInstanceFieldRef(thisLocal, transformedField.makeRef())
            );
            upc.add(getField);

            boolean reflectionRequiredToSetField =
                    calculateReflectionRequiredForField(oldField);

            /* LABEL VALUE */
            Local labeledValue;
            assert !(transformedField.getType() instanceof PrimType) : "Fields should always be transformed or be ignored objects";
            assert !(transformedField.getType() instanceof ArrayType) : "Array fields should have been transformed";
            Type ot = oldField.getType();
            boolean originalFieldIsPrimitive = isPrimitiveOrSprimitive(ot);
            if (originalFieldIsPrimitive) {
                assert ot instanceof PrimType;
                Local toCastPrimitiveValueWrapper = localSpawner.spawnNewStackLocal(v.TYPE_OBJECT);
                AssignStmt labelPrimitiveValue = Jimple.v().newAssignStmt(
                        toCastPrimitiveValueWrapper,
                        Jimple.v().newInterfaceInvokeExpr(
                                smLocal,
                                v.SM_SOLVER_MANAGER_GET_LABEL.makeRef(),
                                transformedValue
                        )
                );
                upc.add(labelPrimitiveValue);
                Type potentiallyWrappedType = wrapTypeIfNecessary(oldField.getType());
                if (!reflectionRequiredToSetField) {
                    // We still need to unwrap, if we do not use reflection
                    Local castedValue = localSpawner.spawnNewStackLocal(potentiallyWrappedType);
                    AssignStmt toCastValue = Jimple.v().newAssignStmt(
                            castedValue,
                            Jimple.v().newCastExpr(toCastPrimitiveValueWrapper, potentiallyWrappedType)
                    );
                    upc.add(toCastValue);

                    SootMethodRef used = getValueFromNumberMethodRef(ot);
                    labeledValue = localSpawner.spawnNewStackLocal(ot);
                    AssignStmt unwrap = Jimple.v().newAssignStmt(
                            labeledValue,
                            Jimple.v().newVirtualInvokeExpr(castedValue, used)
                    );
                    upc.add(unwrap);
                } else {
                    // We can simply cast since we pass this to Field.set(...)
                    labeledValue = localSpawner.spawnNewStackLocal(potentiallyWrappedType);
                    AssignStmt castValue = Jimple.v().newAssignStmt(
                            labeledValue,
                            Jimple.v().newCastExpr(toCastPrimitiveValueWrapper, potentiallyWrappedType)
                    );
                    upc.add(castValue);
                }
            } else {
                // Is sarray or partner class
                Local toCastLabeledLocal = localSpawner.spawnNewStackLocal(v.TYPE_OBJECT);
                AssignStmt labelValue = Jimple.v().newAssignStmt(
                        toCastLabeledLocal,
                        Jimple.v().newInterfaceInvokeExpr(
                                smLocal,
                                v.SM_SOLVER_MANAGER_GET_LABEL.makeRef(),
                                transformedValue
                        )
                );
                upc.add(labelValue);
                if (oldField.getType() instanceof PrimType
                        || (oldField.getType() instanceof ArrayType && !((((ArrayType) oldField.getType()).baseType instanceof RefType)
                                && reflectionRequiredSinceClassIsNotVisible(((RefType) ((ArrayType) oldField.getType()).baseType).getSootClass())))
                        || (oldField.getType() instanceof RefType && !reflectionRequiredSinceClassIsNotVisible(((RefType) oldField.getType()).getSootClass()))) {
                    labeledValue = localSpawner.spawnNewStackLocal(oldField.getType());
                    AssignStmt castValue = Jimple.v().newAssignStmt(
                            labeledValue,
                            Jimple.v().newCastExpr(toCastLabeledLocal, ot)
                    );
                    upc.add(castValue);
                } else {
                    labeledValue = toCastLabeledLocal;
                }
            }

            /* SET FIELD */
            Stmt firstStmtOfSettingValue;
            if (reflectionRequiredToSetField) {
                // Get field and class local, set field accessible
                firstStmtOfSettingValue =
                        addInstructionsToInitializeFieldAndSetAccessible(thisLocal, classLocal, fieldLocal, oldField, upc);
                // Set via Field.set(Object, Object)
                InvokeStmt setViaField = Jimple.v().newInvokeStmt(
                        Jimple.v().newVirtualInvokeExpr(fieldLocal, v.SM_FIELD_SET.makeRef(), castedToLabelTo, labeledValue)
                );
                upc.add(setViaField);
            } else {
                // Simply add the field
                firstStmtOfSettingValue = Jimple.v().newAssignStmt(
                        Jimple.v().newInstanceFieldRef(castedToLabelTo, oldField.makeRef()),
                        labeledValue
                );
                upc.add(firstStmtOfSettingValue);
            }

            if (originalFieldIsPrimitive) {
                // Can be null for objects that are represented in the solver or that are initialized lazily...
                // Add check for null of transformed value; - if Sprimitive is null, add respective neutral element
                // If transformedValue is not null, jump to after getField
                // Else push neutral element
                // And GOTO firstStmtOfSettingValue
                PrimType pt = (PrimType) ot;
                Unit stmtAfterGetField = upc.getSuccOf(getField);
                Stmt ifStmt =
                        Jimple.v().newIfStmt(Jimple.v().newNeExpr(NullConstant.v(), transformedValue), stmtAfterGetField);
                Value wrappedOrPrimitive = reflectionRequiredToSetField ? getPrimitiveWrapperNeutralValueForType(pt) : getPrimitiveNeutralValueForType(pt);
                AssignStmt pushNeutralElement =
                        Jimple.v().newAssignStmt(labeledValue, wrappedOrPrimitive);
                GotoStmt gotoSet = Jimple.v().newGotoStmt(firstStmtOfSettingValue);
                upc.insertAfter(List.of(ifStmt, pushNeutralElement, gotoSet), getField);
            }
        }

        ReturnStmt returnStmt = Jimple.v().newReturnStmt(castedToLabelTo);
        // For transformation constructor: If reflection is required, we must catch the exceptions
        if (reflectionRequired) {
            upc.add(Jimple.v().newGotoStmt(returnStmt));
            // We start the trap first thing after the last identity statement (i.e. the first "real" instruction)
            try {
                Unit successorOfSuperInit = upc.getSuccOf(lastIdentity);
                IdentityStmt exceptionStmt = Jimple.v().newIdentityStmt(exceptionLocal, Jimple.v().newCaughtExceptionRef());
                Local mulibExceptionStackLocal = localSpawner.spawnNewStackLocal(v.TYPE_MULIB_RUNTIME_EXCEPTION);
                AssignStmt newMulibRuntimeException =
                        Jimple.v().newAssignStmt(
                                mulibExceptionStackLocal,
                                Jimple.v().newNewExpr(v.TYPE_MULIB_RUNTIME_EXCEPTION)
                        );
                InvokeStmt invokeMulibRuntimeExceptionConstr =
                        Jimple.v().newInvokeStmt(Jimple.v().newSpecialInvokeExpr(
                                mulibExceptionStackLocal,
                                Scene.v().makeConstructorRef(v.SC_MULIB_RUNTIME_EXCEPTION, List.of(v.TYPE_EXCEPTION)),
                                exceptionLocal));
                upc.add(exceptionStmt);
                upc.add(newMulibRuntimeException);
                upc.add(invokeMulibRuntimeExceptionConstr);
                upc.add(Jimple.v().newThrowStmt(mulibExceptionStackLocal));
                Trap t = Jimple.v().newTrap(v.SC_EXCEPTION, successorOfSuperInit, returnStmt, exceptionStmt);
                b.getTraps().add(t);
            } catch (Exception e) {
                throw new MulibRuntimeException(e);
            }
        }
        upc.add(returnStmt);
        labelMethod.setDeclaringClass(result);
        result.addMethod(labelMethod);
    }

    private static Value getPrimitiveWrapperNeutralValueForType(PrimType pt) {
        Value neutral = getPrimitiveNeutralValueForType(pt);
        SootMethod wrapperCall;
        if (pt instanceof IntType) {
            wrapperCall = v.SM_INTEGER_VALUE_OF;
        } else if (pt instanceof LongType) {
            wrapperCall = v.SM_LONG_VALUE_OF;
        } else if (pt instanceof DoubleType) {
            wrapperCall = v.SM_DOUBLE_VALUE_OF;
        } else if (pt instanceof FloatType) {
            wrapperCall = v.SM_FLOAT_VALUE_OF;
        } else if (pt instanceof ShortType) {
            wrapperCall = v.SM_SHORT_VALUE_OF;
        } else if (pt instanceof ByteType) {
            wrapperCall = v.SM_BYTE_VALUE_OF;
        } else if (pt instanceof BooleanType) {
            wrapperCall = v.SM_BOOLEAN_VALUE_OF;
        } else {
            assert pt instanceof CharType;
            wrapperCall = v.SM_CHARACTER_VALUE_OF;
        }
        return Jimple.v().newStaticInvokeExpr(wrapperCall.makeRef(), neutral);
    }

    private static Value getPrimitiveNeutralValueForType(PrimType pt) {
        if (pt instanceof IntType || pt instanceof ShortType
                || pt instanceof ByteType || pt instanceof BooleanType
                || pt instanceof CharType) {
            return IntConstant.v(0);
        } else if (pt instanceof LongType) {
            return LongConstant.ZERO;
        } else if (pt instanceof DoubleType) {
            return DoubleConstant.ZERO;
        } else {
            assert pt instanceof FloatType;
            return FloatConstant.ZERO;
        }
    }

    @Override
    protected void generateAndAddOriginalClassMethod(SootClass old, SootClass result) {
        boolean reflectionRequiredToGetOriginalClass = reflectionRequiredSinceClassIsNotVisible(old);
        // Create method
        SootMethod originalClassMethod = new SootMethod(_TRANSFORMATION_INDICATOR + "getOriginalClass", List.of(), v.TYPE_CLASS, Modifier.PUBLIC);
        // Create body
        JimpleBody b = Jimple.v().newBody(originalClassMethod);
        originalClassMethod.setActiveBody(b);
        LocalSpawner localSpawner = new LocalSpawner(b);
        // Create locals for body
        Local thisLocal = localSpawner.spawnNewLocal(result.getType());
        // Get unit chain
        UnitPatchingChain upc = b.getUnits();
        // Create identity statement for SymbolicExecution parameter
        upc.add(Jimple.v().newIdentityStmt(thisLocal, Jimple.v().newThisRef(result.getType())));
        if (!reflectionRequiredToGetOriginalClass) {
            // Return class constant
            upc.add(Jimple.v().newReturnStmt(ClassConstant.fromType(old.getType())));
        } else {
            // Return via Class.forName(String)
            Local classLocal = localSpawner.spawnNewStackLocal(v.TYPE_CLASS);
            upc.add(Jimple.v().newAssignStmt(
                    classLocal,
                    Jimple.v().newStaticInvokeExpr(v.SM_CLASS_FOR_NAME.makeRef(), StringConstant.v(old.getName())))
            );
            upc.add(Jimple.v().newReturnStmt(classLocal));
        }
        originalClassMethod.setDeclaringClass(result);
        result.addMethod(originalClassMethod);
    }


    @Override
    protected void treatSpecialMethodCallsInClassNodesMethods(SootClass sc) {
        new DaciteProcedure().treatSpecialMethodCallsInClassNodesMethods(sc);
    }

    final static class DaciteProcedure {

        void treatSpecialMethodCallsInClassNodesMethods(SootClass sc) {
            // TODO Hardcoded for now
            final String methodOwnerToTreatIn = "dacite.core.defuse.DefUseAnalyser";
            final List<String> methodNamesToAccountFor = List.of(
                    "visitDef", "visitUse", "visitStaticFieldUse", "visitStaticFieldDef",
                    "visitFieldDef", "visitFieldUse",
                    "visitArrayUse", // two different signatures (int/Sint)
                    "visitArrayDef", // two different signatures (int/Sint)
                    "visitParameter",
                    "registerInterMethod" // two different signatures (one value/multiple values)
            );
            String registerInterMethod = "registerInterMethod";
            for (SootMethod sm : sc.getMethods()) {
                if (sm.isAbstract() || sm.isNative()) {
                    continue;
                }
                UnitPatchingChain upc = sm.retrieveActiveBody().getUnits();
                Stmt[] stmts = upc.toArray(Stmt[]::new);
                for (int i = 0; i < stmts.length; i++) {
                    Stmt s = stmts[i];
                    if (!s.containsInvokeExpr()) {
                        continue;
                    }
                    if (s instanceof AssignStmt) {
                        continue;
                    }
                    InvokeStmt invokeStmt = (InvokeStmt) s;
                    InvokeExpr invokeExpr = invokeStmt.getInvokeExpr();
                    SootMethodRef smr = invokeExpr.getMethodRef();
                    String methodName = smr.getName();
                    if (!smr.getDeclaringClass().getName().equals(methodOwnerToTreatIn)) {
                        continue;
                    }
                    if (!methodNamesToAccountFor.contains(methodName)) {
                        continue;
                    }
                    Value[] values;
                    if (methodName.startsWith("visitArray")) {
                        Value value = invokeExpr.getArg(2);
                        values = new Value[] { value };
                        Value index = invokeExpr.getArg(1);
                        if (index.getType().equals(v.TYPE_SINT)) {
                            SootClass defUseAnalyzerClass = smr.getDeclaringClass();
                            boolean use = methodName.contains("Use");
                            SootMethodRef newSmr = Scene.v().makeMethodRef(
                                    defUseAnalyzerClass,
                                    "visitArray" + (use ? "Use" : "Def"),
                                    List.of(v.TYPE_OBJECT, v.TYPE_SINT, v.TYPE_OBJECT, v.TYPE_INT, v.TYPE_INT, v.TYPE_STRING),
                                    v.TYPE_VOID,
                                    true
                            );
                            smr = newSmr;
                            invokeExpr.setMethodRef(newSmr);
                        }
                    } else if (registerInterMethod.equals(methodName)) {
                        Value value = invokeExpr.getArg(0);
                        if (value.getType() instanceof ArrayType) {
                            values = findValuesStoredInArray(stmts, i).toArray(Value[]::new);
                        } else {
                            values = new Value[]{value};
                        }
                    } else if (methodName.startsWith("visitField")) {
                        values = new Value[] { invokeExpr.getArg(1) };
                    } else if (methodName.startsWith("visitStaticField")) {
                        values = new Value[] { invokeExpr.getArg(0) };
                    } else if (methodName.equals("visitDef") || methodName.equals("visitUse")) {
                        values = new Value[]{invokeExpr.getArg(0)};
                    } else if (methodName.equals("visitParameter")) {
                        values = new Value[]{invokeExpr.getArg(0)};
                    } else {
                        throw new NotYetImplementedException(methodName);
                    }
                    for (Value value : values) {
                        if (!isWrapperType(value.getType())) {
                            continue;
                        }
                        // Is a wrapper type; - we want to replace the respective valueOf-call
                        // Find source of value, i.e., the wrapping valueOf-call
                        for (Stmt z : stmts) {
                            if (z == s) {
                                // Cannot be defined hereafter
                                break;
                            }
                            if (!z.getDefBoxes().stream().anyMatch(vb -> vb.getValue().equals(value))) {
                                continue;
                            }
                            // Is source; - must be an assign statement
                            AssignStmt source = (AssignStmt) z;
                            // Must contain invokeExpr
                            InvokeExpr sourceInvokeExpr = source.getInvokeExpr();
                            Value wrapped = sourceInvokeExpr.getArg(0);
                            if (!isPrimitiveOrSprimitive(wrapped.getType())) {
                                continue;
                            }
                            assert sourceInvokeExpr.getMethodRef().getName().equals("valueOf");
                            source.setRightOp(wrapped);
                            ((Local) source.getLeftOp()).setType(v.TYPE_OBJECT);
                        }
                    }
                }
            }
            return;
        }

        Collection<Value> findValuesStoredInArray(Stmt[] stmts, int positionOfInterMethodStmt) {
            Collection<Value> result = new HashSet<>();
            boolean foundSetParameter = false;
            // Find previous setParameter-method
            for (int i = positionOfInterMethodStmt - 1; i >= 0; i--) {
                Stmt s = stmts[i];
                if (!s.containsInvokeExpr()) {
                    continue;
                }
                InvokeExpr invokeExpr = s.getInvokeExpr();
                SootMethodRef smr = invokeExpr.getMethodRef();
                String declaringClassName = smr.getDeclaringClass().getName();
                String methodName = smr.getName();
                if (declaringClassName.equals("dacite.core.defuse.ParameterCollector") && methodName.equals("setParameter")) {
                    foundSetParameter = true;
                    break;
                } else if (declaringClassName.equals("dacite.core.defuse.ParameterCollector") && methodName.equals("push")) {
                    Value arg = invokeExpr.getArg(0);
                    result.add(arg);
                }
            }
            assert foundSetParameter : "setParameter-call indicating start of parameter collection not found";
            return result;
        }

        boolean isWrapperType(Type t) {
            if (!(t instanceof RefType)) {
                return false;
            }
            RefType rt = (RefType) t;
            String cn = rt.getClassName();
            if (cn.startsWith("de.wwu.mulib.model.classes.java.lang")) {
                cn = cn.replace("de.wwu.mulib.model.classes.", "").replace("Replacement", "").replace(_TRANSFORMATION_INDICATOR, "");
            }
            return cn.equals(Integer.class.getName()) || cn.equals(Long.class.getName())
                    || cn.equals(Double.class.getName()) || cn.equals(Float.class.getName())
                    || cn.equals(Short.class.getName()) || cn.equals(Byte.class.getName())
                    || cn.equals(Boolean.class.getName()) || cn.equals(Character.class.getName());
        }
    }

    private boolean reflectionRequiredSinceClassIsNotVisible(SootClass old) {
        int access = old.getModifiers();
        if (config.TRANSF_LOAD_WITH_SYSTEM_CLASSLOADER) {
            return Modifier.isPrivate(access);
        } else {
            // Otherwise, it is in another module and friendly as well as protected fields cannot be accessed
            // without reflection
            return !Modifier.isPublic(access);
        }
    }

    @Override
    protected void generateOrReplaceClinit(SootClass old, SootClass result) {
        SootMethod initializer = null;
        for (SootMethod sm : result.getMethods()) {
            if (sm.getName().equals(clinit)) {
                initializer = sm;
                break;
            }
        }
        if (initializer != null) {
            result.removeMethod(initializer);
        }
        initializer = new SootMethod(clinit, List.of(), v.TYPE_VOID, Modifier.PUBLIC | Modifier.STATIC);
        // Create parameter locals for method
        JimpleBody b = Jimple.v().newBody(initializer);
        initializer.setActiveBody(b);
        // Get unit chain to add instructions to
        UnitPatchingChain upc = b.getUnits();
        upc.add(Jimple.v().newReturnVoidStmt());

        initializer.setDeclaringClass(result);
        result.addMethod(initializer);
        enhanceClinitOrInitWithNullConditionalDefaults(result, b, true);
    }

    private void enhanceClinitOrInitWithNullConditionalDefaults(SootClass c, JimpleBody b, boolean regardStaticFields) {
        Set<SootField> fieldsToInitialize =
                c.getFields().stream()
                        .filter(f -> isPrimitiveOrSprimitive(f.getType())
                                // Finals are checked statically!
                                && !f.isFinal()
                                // Exclude synthetic fields
                                && !f.getName().contains(_TRANSFORMATION_INDICATOR)
                                && ((regardStaticFields && f.isStatic()) || (!regardStaticFields && !f.isStatic())))
                        .collect(Collectors.toSet());
        UnitPatchingChain upc = b.getUnits();
        Local thisLocal = regardStaticFields ? null : b.getThisLocal();
        LocalSpawner localSpawner = new LocalSpawner(b);

        Set<ReturnVoidStmt> returnVoids = upc.stream().filter(
                u -> u instanceof ReturnVoidStmt
        ).map(ReturnVoidStmt.class::cast).collect(Collectors.toSet());

        for (ReturnVoidStmt rv : returnVoids) {
            // For each of the ReturnVoidStmts: Add a null-check and initialize if needed
            Unit nextUnit = rv;
            for (SootField f : fieldsToInitialize) {
                Type t = f.getType();
                Local loadedField = localSpawner.spawnNewStackLocal(t);
                // Load field
                AssignStmt loadField = Jimple.v().newAssignStmt(
                        loadedField,
                        regardStaticFields ?
                                Jimple.v().newStaticFieldRef(f.makeRef())
                                :
                                Jimple.v().newInstanceFieldRef(thisLocal, f.makeRef())
                );
                // Check if field is not null
                IfStmt notNullCheck = Jimple.v().newIfStmt(
                        Jimple.v().newNeExpr(loadedField, NullConstant.v()),
                        nextUnit // If is not null, jump to next statement
                );
                SootFieldRef staticNeutralElementField;
                // If is null, initialize
                if (isIntOrSint(t)) {
                    staticNeutralElementField = v.SF_SINT_NEUTRAL.makeRef();
                } else if (isLongOrSlong(t)) {
                    staticNeutralElementField = v.SF_SLONG_NEUTRAL.makeRef();
                } else if (isDoubleOrSdouble(t)) {
                    staticNeutralElementField = v.SF_SDOUBLE_NEUTRAL.makeRef();
                } else if (isFloatOrSfloat(t)) {
                    staticNeutralElementField = v.SF_SFLOAT_NEUTRAL.makeRef();
                } else if (isShortOrSshort(t)) {
                    staticNeutralElementField = v.SF_SSHORT_NEUTRAL.makeRef();
                } else if (isByteOrSbyte(t)) {
                    staticNeutralElementField = v.SF_SBYTE_NEUTRAL.makeRef();
                } else if (isBoolOrSbool(t)) {
                    staticNeutralElementField = v.SF_SBOOL_NEUTRAL.makeRef();
                } else if (isCharOrSchar(t)) {
                    staticNeutralElementField = v.SF_SCHAR_NEUTRAL.makeRef();
                } else {
                    throw new NotYetImplementedException();
                }
                Local setNewValueTo = localSpawner.spawnNewStackLocal(t);
                AssignStmt getNeutral = Jimple.v().newAssignStmt(
                        setNewValueTo,
                        Jimple.v().newStaticFieldRef(staticNeutralElementField)
                );
                AssignStmt assignToField = Jimple.v().newAssignStmt(
                        regardStaticFields ?
                                Jimple.v().newStaticFieldRef(f.makeRef())
                                :
                                Jimple.v().newInstanceFieldRef(thisLocal, f.makeRef()),
                        setNewValueTo
                );
                upc.insertBefore(loadField, nextUnit);
                // Readd redirect to next unit! upc.insertBefore will change this
                notNullCheck.setTarget(nextUnit);
                upc.insertBeforeNoRedirect(assignToField, nextUnit);
                upc.insertBeforeNoRedirect(getNeutral, assignToField);
                upc.insertBeforeNoRedirect(notNullCheck, getNeutral);
                nextUnit = loadField;
            }
        }
    }

    @Override
    protected void generateNullChecksForMethods(SootClass old, SootClass result) {
        for (SootMethod sm : result.getMethods()) {
            if (sm.isAbstract()
                    || sm.isStatic()
                    || sm.getName().contains(_TRANSFORMATION_INDICATOR)
                    || sm.getName().equals(init)
                    || (sm.getName().equals("label") && sm.getParameterCount() == 2 && sm.getParameterType(1).equals(v.TYPE_SOLVER_MANAGER))
                    || (sm.getName().equals("copy") && sm.getParameterCount() == 1 && sm.getParameterType(0).equals(v.TYPE_MULIB_VALUE_COPIER))) {
                continue;
            }
            JimpleBody jb = (JimpleBody) sm.retrieveActiveBody();
            UnitPatchingChain upc = jb.getUnits();
            Local thisLocal = jb.getThisLocal();
            SootMethod nullCheckMethod = v.SM_PARTNER_CLASS_NULL_CHECK;
            InvokeStmt nullCheck = Jimple.v().newInvokeStmt(Jimple.v().newInterfaceInvokeExpr(thisLocal, nullCheckMethod.makeRef()));
            Stmt firstNonIdentityStmt = jb.getFirstNonIdentityStmt();
            upc.insertBefore(nullCheck, firstNonIdentityStmt);
        }
    }
    @Override
    protected void ensureInitializedLibraryTypeFieldsInConstructors(SootClass result) {
        for (SootMethod sm : result.getMethods()) {
            if (sm.getName().equals(init) && !(sm.getParameterCount() == 1 && sm.getParameterType(0).equals(v.TYPE_SE))) {
                replaceObjectConstructorCallIfNeeded(result, sm);
                enhanceClinitOrInitWithNullConditionalDefaults(result, (JimpleBody) sm.retrieveActiveBody(), false);
            }
        }
    }

    private void replaceObjectConstructorCallIfNeeded(SootClass c, SootMethod sm) {
        if (!c.getSuperclass().equals(v.SC_PARTNER_CLASS_OBJECT)) {
            return;
        }
        UnitPatchingChain upc = sm.retrieveActiveBody().getUnits();
        for (Unit u : upc) {
            if (u instanceof InvokeStmt && ((InvokeStmt) u).getInvokeExpr() instanceof SpecialInvokeExpr) {
                SpecialInvokeExpr sie = (SpecialInvokeExpr) ((InvokeStmt) u).getInvokeExpr();
                if (sie.getMethodRef().getDeclaringClass().equals(v.SC_OBJECT)) {
                    sie.setMethodRef(v.SM_PARTNER_CLASS_OBJECT_EMPTY_INIT.makeRef());
                }
            }
        }
    }

    @Override
    protected MulibClassLoader<SootClass> generateMulibClassLoader() {
        return new SootClassLoader(this);
    }

    @Override
    protected SootClass transformClassNode(SootClass toTransform) {
        // Create new SootClass with the transformation prefix
        SootClass result = new SootClass(addTransformationIndicatorToName(toTransform.getName()));
        // Set modifiers
        result.setModifiers(toTransform.getModifiers());
        // The class is already added and added to the set of resolved classes. This is done to ensure that
        // we can use it later on
        Scene.v().addClass(result);
        resolvedClasses.put(result.getName(), result);
        // Set super class and interfaces
        SootClass sootSuperClass = transformEnrichAndValidateIfNotSpecialCase(toTransform.getSuperclass().getName());
        if (sootSuperClass.equals(v.SC_OBJECT) && !toTransform.isInterface()) {
            // We rectify calls to the Object constructor for interfaces when dealing with constructors dedicatedly
            sootSuperClass = v.SC_PARTNER_CLASS_OBJECT;
        }
        result.setSuperclass(sootSuperClass);
        if (toTransform.isInterface()) {
            result.addInterface(v.SC_PARTNER_CLASS);
        }
        for (SootClass i : toTransform.getInterfaces()) {
            SootClass sootInterface = transformEnrichAndValidateIfNotSpecialCase(i.getName());
            result.addInterface(sootInterface);
        }
        // Set outer class
        SootClass outerClass = toTransform.getOuterClassUnsafe();
        if (outerClass != null) {
            SootClass transformedOuterClass = transformEnrichAndValidateIfNotSpecialCase(outerClass.getName());
            result.setOuterClass(transformedOuterClass);
        }
        // Set inner classes
        for (Tag t : toTransform.getTags()) {
            if (t instanceof InnerClassTag) {
                InnerClassTag innerClassTag = (InnerClassTag) t;
                String innerClassName = innerClassTag.getInnerClass().replace("/", ".");
                String outerClassName = innerClassTag.getOuterClass() == null ? null : innerClassTag.getOuterClass().replace("/", ".");
                if (!toTransform.getName().equals(outerClassName)) {
                    continue;
                }
                transformEnrichAndValidateIfNotSpecialCase(innerClassName);
                result.addTag(
                        new InnerClassTag(
                                addTransformationIndicatorToPath(innerClassTag.getInnerClass()),
                                addTransformationIndicatorToPath(innerClassTag.getOuterClass()),
                                addTransformationIndicatorToName(innerClassName),
                                innerClassTag.getAccessFlags()
                        )
                );
            }
        }

        // Transform fields
        for (SootField f : toTransform.getFields()) {
            SootField transformedField = transformField(f, result);
            result.addField(transformedField);
        }

        // Transform methods
        for (SootMethod m : toTransform.getMethods()) {
            if (m.isNative()) {
                SootMethod substituted = sootReplaceMethodWithOtherMethod.get(m);
                if (substituted == null) {
                    throw new MulibRuntimeException(m.getSignature() + " is native and should be transformed, " +
                            "but has no substitution");
                }
                continue;
            }
            SootMethod transformedMethod = transformMethod(m, result);
            result.addMethod(transformedMethod);
        }

        return result;
    }

    public static boolean isRefTypeOrArrayWithInnerRefType(Type t) {
        return t instanceof RefType
                || (t instanceof ArrayType && (getInnermostTypeInArray((ArrayType) t) instanceof RefType));
    }

    public boolean isToTransformRefTypeOrArrayWithInnerToTransformRefType(Type t) {
        RefType rt;
        if (t instanceof RefType) {
            rt = (RefType) t;
        } else if (t instanceof ArrayType) {
            Type temp = getInnermostTypeInArray((ArrayType) t);
            if (!(temp instanceof RefType)) {
                return false;
            }
            rt = (RefType) temp;
        } else {
            return false;
        }
        if (rt.getClassName().contains(_TRANSFORMATION_INDICATOR)) {
            // Already transformed
            return false;
        }

        return shouldBeTransformed(rt.getClassName());
    }


    private SootMethod transformMethod(final SootMethod toTransform, final SootClass declaringTransformedClass) {
        // Replace parameter types and return types
        List<Type> transformedParameterTypes = transformTypes(toTransform.getParameterTypes());
        Type transformedReturnType = transformType(toTransform.getReturnType());
        int modifiers = toTransform.getModifiers();
        SootMethod result = new SootMethod(
                toTransform.getName(),
                transformedParameterTypes,
                transformedReturnType,
                modifiers
        );
        result.setDeclaringClass(declaringTransformedClass);
        List<SootClass> transformedExceptions = result.getExceptions();
        for (SootClass e : toTransform.getExceptions()) {
            transformedExceptions.add(transformEnrichAndValidateIfNotSpecialCase(e.getName()));
        }
        if (toTransform.isAbstract()) {
            return result;
        }
        // Analyze which parts of the jimple body need to be replaced
        TaintAnalyzer gta = new TaintAnalyzer(config, this, toTransform, v);
        TaintAnalysis a = gta.analyze();

        JimpleBody transformedBody = Jimple.v().newBody(result);
        result.setActiveBody(transformedBody);
        JimpleBody toTransformBody = a.analyzedBody;

        // Replace types of locals
        for (Local l : toTransformBody.getLocals()) {
            // transformValue will only transform tainted values
            transformedBody.getLocals().add((Local) transformValue(l, a));
        }
        LocalSpawner ls = new LocalSpawner(transformedBody);
        Local seLocal = ls.spawnNewLocal(v.TYPE_SE);
        // We already replace all values. While transforming units, the values do not have to be transformed
        // anymore.
        for (ValueBox vb : toTransformBody.getUseAndDefBoxes()) {
            Value transformedValue = transformValue(vb.getValue(), a);
            vb.setValue(transformedValue);
        }

        // Replace units
        UnitPatchingChain upc = transformedBody.getUnits();
        MethodInfoContainer methodInfoContainer = new MethodInfoContainer(
                result,
                upc,
                ls,
                seLocal,
                a
        );
        StaticInvokeExpr seGetExpr = Jimple.v().newStaticInvokeExpr(v.SM_SE_GET.makeRef());
        AssignStmt seAssign = Jimple.v().newAssignStmt(seLocal, seGetExpr);
        boolean firstNonIdentityStatement = true;
        UnitPatchingChain toTransformChain = toTransformBody.getUnits();
        for (Unit u : toTransformChain) {

            if (!(u instanceof IdentityStmt) && firstNonIdentityStatement
                    && !(u instanceof JInvokeStmt && ((JInvokeStmt) u).getInvokeExpr() instanceof SpecialInvokeExpr) ) {
                // The SymbolicExecution instance is assigned only before the first non-identity statement is executed
                firstNonIdentityStatement = false;
                upc.add(seAssign);
            }

            TcArgs args = new TcArgs(methodInfoContainer, u);
            if (u instanceof GotoStmt) {
                transform((GotoStmt) u, args);
            } else if (u instanceof MonitorStmt) {
                transform((MonitorStmt) u, args);
            } else if (u instanceof ReturnStmt) {
                transform((ReturnStmt) u, args);
            } else if (u instanceof SwitchStmt) {
                transform((SwitchStmt) u, args);
            } else if (u instanceof BreakpointStmt) {
                transform((BreakpointStmt) u, args);
            } else if (u instanceof ReturnVoidStmt) {
                transform((ReturnVoidStmt) u, args);
            } else if (u instanceof InvokeStmt) {
                transform((InvokeStmt) u, args);
            } else if (u instanceof ThrowStmt) {
                transform((ThrowStmt) u, args);
            } else if (u instanceof RetStmt) {
                transform((RetStmt) u, args);
            } else if (u instanceof IfStmt) {
                transform((IfStmt) u, args);
            } else if (u instanceof NopStmt) {
                transform((NopStmt) u, args);
            } else if (u instanceof DefinitionStmt) {
                transform((DefinitionStmt) u, args);
            } else {
                throw new NotYetImplementedException(u.toString());
            }
        }

        // Replace types of exceptions
        for (Trap t : toTransformBody.getTraps()) {
            // This was already handled during the transformation of the method body
            t.setException(transformEnrichAndValidateIfNotSpecialCase(t.getException().getName()));
            transformedBody.getTraps().add(t);
        }

        toTransform.setSource(a.originalMethodSource);
        toTransform.releaseActiveBody();
        return result;
    }

    private static boolean isSarray(Type t) {
        return isSarraySarray(t) || isPartnerClassSarray(t) || isPrimitiveSarray(t);
    }

    private static boolean isSarraySarray(Type t) {
        return (t instanceof RefType) &&
                (((RefType) t).getClassName().equals(Sarray.SarraySarray.class.getName())
                        || ((RefType) t).getClassName().endsWith(_SARRAYSARRAY_POSTFIX));
    }

    private static boolean isPartnerClassSarray(Type t) {
        return (t instanceof RefType) &&
                (((RefType) t).getClassName().equals(Sarray.PartnerClassSarray.class.getName())
                        || (((RefType) t).getClassName().endsWith(_PARTNER_CLASSSARRAY_POSTFIX)));
    }

    private static boolean isPrimitiveSarray(Type t) {
        return isSintSarray(t) || isSlongSarray(t) || isSdoubleSarray(t) || isSfloatSarray(t)
                || isSshortSarray(t) || isSbyteSarray(t) || isSboolSarray(t) || isScharSarray(t);
    }

    private static boolean isSintSarray(Type t) {
        return (t instanceof RefType) && ((RefType) t).getClassName().equals(Sarray.SintSarray.class.getName());
    }

    private static boolean isSlongSarray(Type t) {
        return (t instanceof RefType) && ((RefType) t).getClassName().equals(Sarray.SlongSarray.class.getName());
    }

    private static boolean isSdoubleSarray(Type t) {
        return (t instanceof RefType) && ((RefType) t).getClassName().equals(Sarray.SdoubleSarray.class.getName());
    }

    private static boolean isSfloatSarray(Type t) {
        return (t instanceof RefType) && ((RefType) t).getClassName().equals(Sarray.SfloatSarray.class.getName());
    }

    private static boolean isSshortSarray(Type t) {
        return (t instanceof RefType) && ((RefType) t).getClassName().equals(Sarray.SshortSarray.class.getName());
    }

    private static boolean isSbyteSarray(Type t) {
        return (t instanceof RefType) && ((RefType) t).getClassName().equals(Sarray.SbyteSarray.class.getName());
    }

    private static boolean isSboolSarray(Type t) {
        return (t instanceof RefType) && ((RefType) t).getClassName().equals(Sarray.SboolSarray.class.getName());
    }

    public static boolean isScharSarray(Type t) {
        return (t instanceof RefType) && ((RefType) t).getClassName().equals(Sarray.ScharSarray.class.getName());
    }

    private static boolean isPrimitiveOrSprimitive(Type t) {
        return isIntOrSintSubtype(t) || isLongOrSlong(t)
                || isDoubleOrSdouble(t) || isFloatOrSfloat(t) || isBoolOrSbool(t);
    }

    private static boolean isLongOrSlong(Type t) {
        return t instanceof LongType
                || ((t instanceof RefType) && ((RefType) t).getClassName().equals(Slong.class.getName()));
    }

    private static boolean isIntOrSintSubtype(Type t) {
        return isIntOrSint(t) || isShortOrSshort(t) || isByteOrSbyte(t) || isCharOrSchar(t);
    }
    private static boolean isIntOrSint(Type t) {
        return t instanceof IntType
                || ((t instanceof RefType) && ((RefType) t).getClassName().equals(Sint.class.getName()))
                || ((t instanceof RefType) && ((RefType) t).getClassName().equals(Sint.ConcSint.class.getName()))
                || ((t instanceof RefType) && ((RefType) t).getClassName().equals(Sint.SymSint.class.getName()));
    }

    private static boolean isDoubleOrSdouble(Type t) {
        return t instanceof DoubleType
                || ((t instanceof RefType) && ((RefType) t).getClassName().equals(Sdouble.class.getName()))
                || ((t instanceof RefType) && ((RefType) t).getClassName().equals(Sdouble.ConcSdouble.class.getName()))
                || ((t instanceof RefType) && ((RefType) t).getClassName().equals(Sdouble.SymSdouble.class.getName()));
    }

    private static boolean isFloatOrSfloat(Type t) {
        return t instanceof FloatType
                || ((t instanceof RefType) && ((RefType) t).getClassName().equals(Sfloat.class.getName()))
                || ((t instanceof RefType) && ((RefType) t).getClassName().equals(Sfloat.ConcSfloat.class.getName()))
                || ((t instanceof RefType) && ((RefType) t).getClassName().equals(Sfloat.SymSfloat.class.getName()));
    }

    private static boolean isShortOrSshort(Type t) {
        return t instanceof ShortType
                || ((t instanceof RefType) && ((RefType) t).getClassName().equals(Sshort.class.getName()))
                || ((t instanceof RefType) && ((RefType) t).getClassName().equals(Sshort.ConcSshort.class.getName()))
                || ((t instanceof RefType) && ((RefType) t).getClassName().equals(Sshort.SymSshort.class.getName()));
    }

    private static boolean isByteOrSbyte(Type t) {
        return t instanceof ByteType
                || ((t instanceof RefType) && ((RefType) t).getClassName().equals(Sbyte.class.getName()))
                || ((t instanceof RefType) && ((RefType) t).getClassName().equals(Sbyte.ConcSbyte.class.getName()))
                || ((t instanceof RefType) && ((RefType) t).getClassName().equals(Sbyte.SymSbyte.class.getName()));
    }

    private static boolean isBoolOrSbool(Type t) {
        return t instanceof BooleanType
                || ((t instanceof RefType) && ((RefType) t).getClassName().equals(Sbool.class.getName()))
                || ((t instanceof RefType) && ((RefType) t).getClassName().equals(Sbool.ConcSbool.class.getName()))
                || ((t instanceof RefType) && ((RefType) t).getClassName().equals(Sbool.SymSbool.class.getName()));
    }

    private static boolean isCharOrSchar(Type t) {
        return t instanceof CharType
                || (t instanceof RefType) && ((RefType) t).getClassName().equals(Schar.class.getName())
                || ((t instanceof RefType) && ((RefType) t).getClassName().equals(Schar.ConcSchar.class.getName()))
                || ((t instanceof RefType) && ((RefType) t).getClassName().equals(Schar.SymSchar.class.getName()));
    }

    private void transform(GotoStmt g, TcArgs args) {
        // The jump address is changed (if necessary) when altering the jumped-to instruction
        args.addUnit(g);
    }

    private void transform(MonitorStmt m, TcArgs args) {
        assert !args.isTainted() && !args.isToWrap();
        if (m instanceof EnterMonitorStmt) {
            args.addUnit(m);
        } else if (m instanceof ExitMonitorStmt) {
            args.addUnit(m);
        } else {
            throw new NotYetImplementedException();
        }
    }

    private void transform(ReturnStmt r, TcArgs args) {
        // Jimple return statements only accept constants and locals.
        // Locals are already treated.
        // Constants still have to be wrapped.
        Value op = r.getOp();
        if (args.isToWrap() && !NullConstant.v().equals(op)) {
            // Find method to wrap with
            SootMethodRef used = constantWrapperMethodRef(args.newMethod().getReturnType()); // TODO class and string returns...
            // Create virtual call
            InvokeExpr invokeExpr = Jimple.v().newStaticInvokeExpr(used, op);
            assignNewValueRedirectAndAdd(invokeExpr, null, r, r.getOpBox(), args);
        }
        args.addUnit(r);
    }


    static class SwitchToIfElse {
        final Unit target;
        final int val;

        SwitchToIfElse(Unit target, int val) {
            this.target = target;
            this.val = val;
        }
    }
    private void transform(SwitchStmt s, TcArgs args) {
        if (!args.isTainted() && !args.isToWrap()) {
            args.addUnit(s);
            return;
        }

        List<SwitchToIfElse> substituteWithIfElse = new ArrayList<>();
        for (int i = 0; i < s.getTargets().size(); i++) {
            Unit target = s.getTarget(i);
            if (s instanceof TableSwitchStmt) {
                TableSwitchStmt tss = (TableSwitchStmt) s;
                assert tss.getTarget(i) != null;
                substituteWithIfElse.add(new SwitchToIfElse(tss.getTarget(i), i));
            } else if (s instanceof LookupSwitchStmt) {
                LookupSwitchStmt lss = (LookupSwitchStmt) s;
                substituteWithIfElse.add(new SwitchToIfElse(target, lss.getLookupValue(i)));
            }
        }
        boolean first = true;
        for (SwitchToIfElse sw : substituteWithIfElse) {
            Local wrapLocal = args.spawnStackLocal(v.TYPE_SINT);
            AssignStmt assign = Jimple.v().newAssignStmt(
                    wrapLocal,
                    Jimple.v().newStaticInvokeExpr(v.SM_SINT_CONCSINT.makeRef(), IntConstant.v(sw.val))
            );
            if (first) {
                s.redirectJumpsToThisTo(assign);
                first = false;
            }
            // Wrap value
            args.addUnit(assign);
            Local boolResult = args.spawnStackLocal(v.TYPE_BOOL);
            VirtualInvokeExpr virtualInvokeExpr;
            if (config.TRANSF_CFG_GENERATE_CHOICE_POINTS_WITH_ID) {
                virtualInvokeExpr = Jimple.v().newVirtualInvokeExpr(wrapLocal, v.SM_SINT_EQ_CHOICE_WITH_ID.makeRef(), s.getKey(), args.seLocal(), LongConstant.v(nextIdOfChoicePoint++));
            } else {
                virtualInvokeExpr = Jimple.v().newVirtualInvokeExpr(wrapLocal, v.SM_SINT_EQ_CHOICE.makeRef(), s.getKey(), args.seLocal());
            }
            // Call eq-method
            args.addUnit(Jimple.v().newAssignStmt(
                    boolResult,
                    virtualInvokeExpr
            ));
            // Conditional jump
            args.addUnit(Jimple.v().newIfStmt(Jimple.v().newEqExpr(boolResult, IntConstant.v(1)), sw.target));
        }
        args.addUnit(Jimple.v().newGotoStmt(s.getDefaultTarget()));
    }

    private void transform(BreakpointStmt b, TcArgs args) {
        assert !args.isToWrap() && !args.isTainted();
        args.addUnit(b);
    }

    private void transform(ReturnVoidStmt r, TcArgs args) {
        assert !args.isTainted() && !args.isToWrap();
        args.addUnit(r);
    }

    private InvokeExpr getInvokeExprIfIndicatorMethodExprElseNull(Stmt containingStmt, InvokeExpr invokeExpr, TcArgs args) {
        final String methodName = invokeExpr.getMethodRef().getName();
        final SootMethod frameworkMethod =
                v.getTransformedMethodOrNull(
                        methodName,
                        invokeExpr.getArgs(),
                        invokeExpr.getMethodRef(),
                        args
                );
        InvokeExpr result = null;
        if (methodName.equals(v.SM_MULIB_FREE_OBJECT.getName())
                || methodName.equals(v.SM_MULIB_NAMED_FREE_OBJECT.getName())) {
            // Is free array of any type or a partner class object
            List<Value> invokeArgs = new ArrayList<>();
            // Class constant of class to be initialized
            Value potentiallyUsedClassConstant;
            if (methodNameImpliesRememberedInitialization(methodName)) {
                assert invokeExpr.getArgs().size() == 2;
                invokeArgs.add(invokeExpr.getArgs().get(0));
                potentiallyUsedClassConstant = invokeExpr.getArgs().get(1);
            } else {
                assert invokeExpr.getArgs().size() == 1;
                potentiallyUsedClassConstant = invokeExpr.getArgs().get(0);
            }
            // If a sarray is initialized, we need to add further arguments for initializing a symbolic array
            Type typeOfClass;
            if (potentiallyUsedClassConstant instanceof ClassConstant) {
                typeOfClass = ((ClassConstant) potentiallyUsedClassConstant).toSootType();
            } else {
                typeOfClass = args.taintAnalysis().valueHolderToClassConstantType.get(potentiallyUsedClassConstant);
                if (typeOfClass == null) {
                    throw new MulibIllegalStateException("The class of the free array or free object must be known at compile time");
                }
            }
            boolean isSarray = typeOfClass instanceof ArrayType;
            if (isSarray) {
                Local symbolicLengthLocal = args.spawnStackLocal(v.TYPE_SINT);
                VirtualInvokeExpr getSymSintExpr = Jimple.v().newVirtualInvokeExpr(args.seLocal(), v.SM_SE_FREE_SINT.makeRef());
                AssignStmt assignSymbolicLength = Jimple.v().newAssignStmt(symbolicLengthLocal, getSymSintExpr);
                args.addUnit(assignSymbolicLength);
                containingStmt.redirectJumpsToThisTo(assignSymbolicLength);
                invokeArgs.add(symbolicLengthLocal);
            }

            if (isSarray) {
                if (!v.SM_SE_PRIMITIVE_SARRAY_INITS.contains(frameworkMethod)) {
                    // Is free array of object type (array of partner class object)
                    ClassConstant classConstantOfArray;
                    Type elementType;
                    if (!args.isTainted(potentiallyUsedClassConstant)) {
                        ArrayType transformedType = ((ArrayType) this.transformArrayType((ArrayType) typeOfClass, false));
                        elementType = transformedType.getElementType();
                    } else {
                        elementType = ((ArrayType) typeOfClass).getElementType();
                    }
                    classConstantOfArray = ClassConstant.fromType(elementType);
                    invokeArgs.add(classConstantOfArray);
                }
            } else {
                // Is free object
                Type transformedType = transformType(typeOfClass);
                invokeArgs.add(ClassConstant.fromType(transformedType));
            }
            if (isSarray) {
                invokeArgs.add(IntConstant.v(1)); // Default is symbolic!
            }
            result = Jimple.v().newVirtualInvokeExpr(args.seLocal(), frameworkMethod.makeRef(), invokeArgs);
        } else if (methodName.equals(v.SM_MULIB_PICK_FROM_OBJS.getName())) {
            assert invokeExpr.getArgCount() == 1;
            Value arg = invokeExpr.getArg(0);
            SootMethod sm;
            if (arg.getType() instanceof ArrayType) {
                if (((ArrayType) arg.getType()).getElementType() instanceof PrimType) {
                    PrimType pt = (PrimType) ((ArrayType) arg.getType()).getElementType();
                    if (pt instanceof IntType) {
                        sm = v.SM_SE_ALIASING_SYM_SINT_WITHIN_ARRAY;
                    } else if (pt instanceof DoubleType) {
                        sm = v.SM_SE_ALIASING_SYM_SDOUBLE_WITHIN_ARRAY;
                    } else if (pt instanceof FloatType) {
                        sm = v.SM_SE_ALIASING_SYM_SFLOAT_WITHIN_ARRAY;
                    } else if (pt instanceof LongType) {
                        sm = v.SM_SE_ALIASING_SYM_SLONG_WITHIN_ARRAY;
                    } else if (pt instanceof ShortType) {
                        sm = v.SM_SE_ALIASING_SYM_SSHORT_WITHIN_ARRAY;
                    } else if (pt instanceof ByteType) {
                        sm = v.SM_SE_ALIASING_SYM_SBYTE_WITHIN_ARRAY;
                    } else if (pt instanceof BooleanType) {
                        sm = v.SM_SE_ALIASING_SYM_SBOOL_WITHIN_ARRAY;
                    } else if (pt instanceof CharType) {
                        sm = v.SM_SE_ALIASING_SYM_SCHAR_WITHIN_ARRAY;
                    } else {
                        throw new NotYetImplementedException(pt.toString());
                    }
                } else {
                    sm = v.SM_SE_ALIASING_SYM_OBJECT_WITHIN_ARRAY;
                }
            } else {
                assert isSarray(arg.getType());
                if (isPrimitiveSarray(arg.getType())) {
                    if (isSintSarray(arg.getType())) {
                        sm = v.SM_SE_ALIASING_SYM_SINT_WITHIN_SARRAY;
                    } else if (isSdoubleSarray(arg.getType())) {
                        sm = v.SM_SE_ALIASING_SYM_SDOUBLE_WITHIN_SARRAY;
                    } else if (isSfloatSarray(arg.getType())) {
                        sm = v.SM_SE_ALIASING_SYM_SFLOAT_WITHIN_SARRAY;
                    } else if (isSlongSarray(arg.getType())) {
                        sm = v.SM_SE_ALIASING_SYM_SLONG_WITHIN_SARRAY;
                    } else if (isSshortSarray(arg.getType())) {
                        sm = v.SM_SE_ALIASING_SYM_SSHORT_WITHIN_SARRAY;
                    } else if (isSbyteSarray(arg.getType())) {
                        sm = v.SM_SE_ALIASING_SYM_SBYTE_WITHIN_SARRAY;
                    } else if (isSboolSarray(arg.getType())) {
                        sm = v.SM_SE_ALIASING_SYM_SBOOL_WITHIN_SARRAY;
                    } else if (isScharSarray(arg.getType())) {
                        sm = v.SM_SE_ALIASING_SYM_SCHAR_WITHIN_SARRAY;
                    } else {
                        throw new NotYetImplementedException();
                    }
                } else {
                    sm = v.SM_SE_ALIASING_SYM_OBJECT_WITHIN_SARRAY;
                }
            }
            result = Jimple.v().newVirtualInvokeExpr(args.seLocal(), sm.makeRef(), arg);
        } else if (methodName.equals("check") || methodName.equals("assume") || methodName.equals("checkAssume")) {
            SootMethod sm;
            if (methodName.equals("check")) {
                sm = v.SM_SE_CHECK;
            } else if (methodName.equals("assume")) {
                sm = v.SM_SE_ASSUME;
            } else {
                sm = v.SM_SE_CHECK_ASSUME;
            }
            Value val;
            if (!args.isTainted(invokeExpr.getArg(0))) {
                WrapPair wp = wrap(args, invokeExpr.getArgBox(0));
                containingStmt.redirectJumpsToThisTo(wp.newFirstStmt);
                val = wp.newValue;
            } else {
                val = invokeExpr.getArg(0);
            }
            result = Jimple.v().newVirtualInvokeExpr(args.seLocal(), sm.makeRef(), val);
        } else if (methodName.equals("isInSearch")) {
            result = Jimple.v().newVirtualInvokeExpr(args.seLocal(), v.SM_SE_IS_IN_SEARCH.makeRef());
        } else if (v.isIndicatorMethodName(methodName)) {
            // Is indicator method other than freeObject or rememberedFreeObject
            assert !v.SM_SE_PRIMITIVE_SARRAY_INITS.contains(frameworkMethod);
            if (v.LB_UB_INDICATORS_REMEMBER.contains(invokeExpr.getMethod()) || v.LB_UB_INDICATORS_WITHOUT_REMEMBER.contains(invokeExpr.getMethod())) {
                assert invokeExpr.getArgCount() == 2 || invokeExpr.getArgCount() == 3;
                // Has arguments for lower bound and upper bound
                ValueBox lb, ub;
                if (invokeExpr.getArgCount() == 2) {
                    lb = invokeExpr.getArgBox(0);
                    ub = invokeExpr.getArgBox(1);
                } else {
                    lb = invokeExpr.getArgBox(1);
                    ub = invokeExpr.getArgBox(2);
                }
                Stmt newFirstStmt = null;
                if (!args.isTainted(lb.getValue())) {
                    WrapPair wp = wrap(args, lb);
                    newFirstStmt = wp.newFirstStmt;
                    if (invokeExpr.getArgCount() == 2) {
                        invokeExpr.setArg(0, wp.newValue);
                    } else {
                        invokeExpr.setArg(1, wp.newValue);
                    }
                }
                if (!args.isTainted(ub.getValue())) {
                    WrapPair wp = wrap(args, ub);
                    if (newFirstStmt == null) {
                        newFirstStmt = wp.newFirstStmt;
                    }
                    if (invokeExpr.getArgCount() == 2) {
                        invokeExpr.setArg(1, wp.newValue);
                    } else {
                        invokeExpr.setArg(2, wp.newValue);
                    }
                }
                if (newFirstStmt != null) {
                    containingStmt.redirectJumpsToThisTo(newFirstStmt);
                }
            }
            // Primitive
            if (methodNameImpliesRememberedInitialization(methodName)) {
                assert invokeExpr.getArgs().size() == 1 || invokeExpr.getArgs().size() == 3;
                result = Jimple.v().newVirtualInvokeExpr(args.seLocal(), frameworkMethod.makeRef(), invokeExpr.getArgs());
            } else {
                assert invokeExpr.getArgs().size() == 0 || invokeExpr.getArgs().size() == 2;
                result = Jimple.v().newVirtualInvokeExpr(args.seLocal(), frameworkMethod.makeRef(), invokeExpr.getArgs());
            }
        }
        return result;
    }

    private void transform(InvokeStmt invoke, TcArgs args) {
        // If this InvokeStmt would produce a result, the InvokeExpr would be part of an AssignStmt
        assert !args.isToWrap();
        InvokeExpr possiblyTransformedIndicatorMethod = getInvokeExprIfIndicatorMethodExprElseNull(invoke, invoke.getInvokeExpr(), args);
        if (possiblyTransformedIndicatorMethod != null) {
            InvokeStmt newInvokeStmt = Jimple.v().newInvokeStmt(possiblyTransformedIndicatorMethod);
            invoke.redirectJumpsToThisTo(newInvokeStmt);
            args.addUnit(newInvokeStmt);
        } else if (args.isTainted()) {
            wrapInvokeExprArgsIfNeededRedirectAndAdd(invoke, invoke.getInvokeExpr(), args);
            args.addUnit(invoke);
        } else {
            adjustSignatureIfNeeded(invoke, invoke.getInvokeExpr(), args);
            args.addUnit(invoke);
        }
    }

    private void transform(ThrowStmt t, TcArgs args) {
        assert !args.isToWrap();
        // Does not need to be tainted, if, e.g., Exception or another ignored Exception is thrown
        args.addUnit(t);
    }

    private void transform(RetStmt r, TcArgs args) {
        assert !args.isToWrap();
        args.addUnit(r);
    }

    private void transform(IfStmt i, TcArgs args) {
        assert !args.isToWrap();
        ValueBox conditionBox = i.getConditionBox();
        ConditionExpr conditionExpr = (ConditionExpr) conditionBox.getValue();
        // Check if we have a null comparison or two references that are compared
        if ((conditionExpr.getOp1() instanceof Local
                && !isPrimitiveOrSprimitive(conditionExpr.getOp1().getType())
                && conditionExpr.getOp1().getType() instanceof RefLikeType)
                || conditionExpr.getOp1() instanceof NullConstant
                || conditionExpr.getOp1() instanceof ClassConstant) {
            // Comparison of two references
            assert (conditionExpr.getOp2() instanceof Local
                    && !isPrimitiveOrSprimitive(conditionExpr.getOp2().getType())
                    && conditionExpr.getOp2().getType() instanceof RefLikeType)
                    || conditionExpr.getOp2() instanceof NullConstant
                    || conditionExpr.getOp2() instanceof ClassConstant;
            assert conditionExpr instanceof NeExpr || conditionExpr instanceof EqExpr;
            Local refEqLocal = args.spawnStackLocal(v.TYPE_SBOOL);
            Stmt assignRefEq = Jimple.v().newAssignStmt(
                    refEqLocal,
                    Jimple.v().newVirtualInvokeExpr(args.seLocal(), v.SM_SE_REFERENCES_EQ.makeRef(), conditionExpr.getOp1(), conditionExpr.getOp2())
            );
            args.addUnit(assignRefEq);
            i.redirectJumpsToThisTo(assignRefEq);
            Local sboolToBoolLocal = args.spawnStackLocal(v.TYPE_BOOL);
            VirtualInvokeExpr virtualInvokeExpr;
            if (config.TRANSF_CFG_GENERATE_CHOICE_POINTS_WITH_ID) {
                virtualInvokeExpr = Jimple.v().newVirtualInvokeExpr(refEqLocal, v.SM_SBOOL_BOOL_CHOICE_S_WITH_ID.makeRef(), args.seLocal(), LongConstant.v(nextIdOfChoicePoint++));
            } else {
                virtualInvokeExpr = Jimple.v().newVirtualInvokeExpr(refEqLocal, v.SM_SBOOL_BOOL_CHOICE_S.makeRef(), args.seLocal());
            }
            Stmt assignBool = Jimple.v().newAssignStmt(
                    sboolToBoolLocal,
                    virtualInvokeExpr
            );
            args.addUnit(assignBool);
            i.setCondition(
                    conditionExpr instanceof EqExpr ?
                    Jimple.v().newEqExpr(sboolToBoolLocal, IntConstant.v(1))
                    :
                    Jimple.v().newEqExpr(sboolToBoolLocal, IntConstant.v(0))
            );
            args.addUnit(i);
            return;
        }
        if (!args.isTainted()) {
            args.addUnit(i);
            return;
        }
        // Since is tainted, one type must be Sbool
        final ValueBox lhsConditionExprBox = conditionExpr.getOp1Box();
        final ValueBox rhsConditionExprBox = conditionExpr.getOp2Box();
        Value lhsCondition = lhsConditionExprBox.getValue();
        Value rhsCondition = rhsConditionExprBox.getValue();
        boolean lhsIsBool = isBoolOrSbool(lhsCondition.getType());
        boolean rhsIsBool = isBoolOrSbool(rhsCondition.getType());
        boolean bothBool = lhsIsBool && rhsIsBool;
        SootMethodRef used;
        VirtualInvokeExpr virtualInvokeExpr;
        Stmt firstStatement = null;
        if (!bothBool && (lhsIsBool || rhsIsBool)) {
            // One is a constant int value representing true or false. The other is of type boolean
            assert conditionExpr instanceof NeExpr || conditionExpr instanceof EqExpr;
            boolean comparisonWithZero;
            // Find with which constant value we compare
            if (lhsIsBool) {
                assert rhsCondition instanceof IntConstant;
                assert ((IntConstant) rhsCondition).value == 0 || ((IntConstant) rhsCondition).value == 1;
            } else {
                assert lhsCondition instanceof IntConstant;
                assert ((IntConstant) lhsCondition).value == 0 || ((IntConstant) lhsCondition).value == 1;
            }
            comparisonWithZero = lhsIsBool ? ((IntConstant) rhsCondition).value == 0 : ((IntConstant) lhsCondition).value == 0;
            // Invert comparisonWithZero if condition is NeExpr
            comparisonWithZero = (conditionExpr instanceof NeExpr) != comparisonWithZero;
            if (config.TRANSF_CFG_GENERATE_CHOICE_POINTS_WITH_ID) {
                used = comparisonWithZero ? v.SM_SBOOL_NEGATED_BOOL_CHOICE_S_WITH_ID.makeRef() : v.SM_SBOOL_BOOL_CHOICE_S_WITH_ID.makeRef();
            } else {
                used = comparisonWithZero ? v.SM_SBOOL_NEGATED_BOOL_CHOICE_S.makeRef() : v.SM_SBOOL_BOOL_CHOICE_S.makeRef();
            }
            ValueBox nonConstantConditionExprBox = lhsIsBool ? lhsConditionExprBox : rhsConditionExprBox;
            Value nonConstantCondition = nonConstantConditionExprBox.getValue();
            if (!args.isTainted(nonConstantConditionExprBox.getValue())) {
                WrapPair wp = wrap(args, nonConstantConditionExprBox);
                firstStatement = wp.newFirstStmt;
                nonConstantCondition = wp.newValue;
            }

            // Must be Local since arg boxes used in J{Ne, Eq, ...}Expr only allow for subtypes of Immediate
            if (config.TRANSF_CFG_GENERATE_CHOICE_POINTS_WITH_ID) {
                virtualInvokeExpr = Jimple.v().newVirtualInvokeExpr((Local) nonConstantCondition, used, args.seLocal(), LongConstant.v(nextIdOfChoicePoint++));
            } else {
                virtualInvokeExpr = Jimple.v().newVirtualInvokeExpr((Local) nonConstantCondition, used, args.seLocal());
            }
        } else if (bothBool) {
            assert conditionExpr instanceof NeExpr || conditionExpr instanceof EqExpr;
            // We choose the method comparing two Sbools if both are Sbools
            if (config.TRANSF_CFG_GENERATE_CHOICE_POINTS_WITH_ID) {
                used = conditionExpr instanceof EqExpr ? v.SM_SBOOL_BOOL_CHOICE_WITH_ID.makeRef() : v.SM_SBOOL_NEGATED_BOOL_CHOICE_WITH_ID.makeRef();
            } else {
                used = conditionExpr instanceof EqExpr ? v.SM_SBOOL_BOOL_CHOICE.makeRef() : v.SM_SBOOL_NEGATED_BOOL_CHOICE.makeRef();
            }
//            assert args.isTainted(lhsCondition) || args.isTainted(rhsCondition);
            if (!args.isTainted(lhsCondition)) {
                WrapPair wp = wrap(args, lhsConditionExprBox);
                firstStatement = wp.newFirstStmt;
                lhsCondition = wp.newValue;
            }
            if (!args.isTainted(rhsCondition)) {
                WrapPair wp = wrap(args, rhsConditionExprBox);
                firstStatement = firstStatement == null ? wp.newFirstStmt : firstStatement;
                rhsCondition = wp.newValue;
            }
            if (config.TRANSF_CFG_GENERATE_CHOICE_POINTS_WITH_ID) {
                virtualInvokeExpr = Jimple.v().newVirtualInvokeExpr((Local) lhsCondition, used, rhsCondition, args.seLocal(), LongConstant.v(nextIdOfChoicePoint++));
            } else {
                virtualInvokeExpr = Jimple.v().newVirtualInvokeExpr((Local) lhsCondition, used, rhsCondition, args.seLocal());
            }
        } else {
            // Two int numbers are compared
            // Wrap constants if necessary or replace by zero-constant
            // -1 ==> There is no constant zero in the comparison
            // 1 ==> lhs is constant 0
            // 2 ==> rhs is constant 0
            byte zeroConstantType;
            if (lhsCondition instanceof IntConstant || rhsCondition instanceof IntConstant) {
                if (lhsCondition instanceof IntConstant) {
                    assert !(rhsCondition instanceof IntConstant);
                    zeroConstantType = (byte) (((IntConstant) lhsCondition).value == 0 ? 1 : -1);
                } else {
                    zeroConstantType = (byte) (((IntConstant) rhsCondition).value == 0 ? 2 : -1);
                }
            } else {
                zeroConstantType = -1;
            }
            if (!args.isTainted(lhsCondition)) {
//                assert zeroConstantType != 2;
                if (zeroConstantType == 1) {
                    lhsCondition = args.spawnStackLocal(IntType.v());
                    firstStatement = Jimple.v().newAssignStmt(lhsCondition, Jimple.v().newStaticFieldRef(v.SF_SINT_NEUTRAL.makeRef()));
                    args.addUnit(firstStatement);
                } else {
                    WrapPair wp = wrap(args, lhsConditionExprBox);
                    lhsCondition = wp.newValue;
                    firstStatement = wp.newFirstStmt;
                }
            }
            if (!args.isTainted(rhsCondition)) {
//                assert zeroConstantType != 1;
                if (zeroConstantType == 2) {
                    rhsCondition = args.spawnStackLocal(IntType.v());
                    Stmt assignStmt = Jimple.v().newAssignStmt(rhsCondition, Jimple.v().newStaticFieldRef(v.SF_SINT_NEUTRAL.makeRef()));
                    if (firstStatement == null) {
                        firstStatement = assignStmt;
                    }
                    args.addUnit(assignStmt);
                } else {
                    WrapPair wp = wrap(args, rhsConditionExprBox);
                    rhsCondition = wp.newValue;
                    if (firstStatement == null) {
                        firstStatement = wp.newFirstStmt;
                    }
                }
            }
            if (conditionExpr instanceof NeExpr) {
                used = (config.TRANSF_CFG_GENERATE_CHOICE_POINTS_WITH_ID ? v.SM_SINT_NOT_EQ_CHOICE_WITH_ID : v.SM_SINT_NOT_EQ_CHOICE).makeRef();
            } else if (conditionExpr instanceof EqExpr) {
                used = (config.TRANSF_CFG_GENERATE_CHOICE_POINTS_WITH_ID ? v.SM_SINT_EQ_CHOICE_WITH_ID : v.SM_SINT_EQ_CHOICE).makeRef();
            } else if (conditionExpr instanceof GeExpr) {
                used = (config.TRANSF_CFG_GENERATE_CHOICE_POINTS_WITH_ID ? v.SM_SINT_GTE_CHOICE_WITH_ID : v.SM_SINT_GTE_CHOICE).makeRef();
            } else if (conditionExpr instanceof GtExpr) {
                used = (config.TRANSF_CFG_GENERATE_CHOICE_POINTS_WITH_ID ? v.SM_SINT_GT_CHOICE_WITH_ID : v.SM_SINT_GT_CHOICE).makeRef();
            } else if (conditionExpr instanceof LeExpr) {
                used = (config.TRANSF_CFG_GENERATE_CHOICE_POINTS_WITH_ID ? v.SM_SINT_LTE_CHOICE_WITH_ID : v.SM_SINT_LTE_CHOICE).makeRef();
            } else if (conditionExpr instanceof LtExpr) {
                used = (config.TRANSF_CFG_GENERATE_CHOICE_POINTS_WITH_ID ? v.SM_SINT_LT_CHOICE_WITH_ID : v.SM_SINT_LT_CHOICE).makeRef();
            } else {
                throw new NotYetImplementedException(conditionExpr.toString());
            }
            if (config.TRANSF_CFG_GENERATE_CHOICE_POINTS_WITH_ID) {
                virtualInvokeExpr = Jimple.v().newVirtualInvokeExpr((Local) lhsCondition, used, rhsCondition, args.seLocal(), LongConstant.v(nextIdOfChoicePoint++));
            } else {
                virtualInvokeExpr = Jimple.v().newVirtualInvokeExpr((Local) lhsCondition, used, rhsCondition, args.seLocal());
            }
        }
        assignNewValueRedirectAndAdd(
                virtualInvokeExpr,
                firstStatement,
                i,
                i.getConditionBox(),
                l -> Jimple.v().newEqExpr(l, IntConstant.v(1)),
                args
        );
        args.addUnit(i);
    }

    private void transform(NopStmt n, TcArgs args) {
        assert !args.isToWrap() && !args.isTainted();
        args.addUnit(n);
    }

    private void transform(DefinitionStmt d, TcArgs args) {
        if (d instanceof IdentityStmt) {
            if (!(d.getRightOp().getType() instanceof RefType)) {
                ValueBox leftBox = d.getLeftOpBox();
                ValueBox rightBox = d.getRightOpBox();
                Local l = (Local) leftBox.getValue();
                ParameterRef pr = (ParameterRef) rightBox.getValue();
                l.setType(transformType(l.getType()));
                pr = new ParameterRef(transformType(pr.getType()), pr.getIndex());
                rightBox.setValue(pr);
            }
            args.addUnit(d);
        } else if (d instanceof AssignStmt) {
            transform((AssignStmt) d, args);
        } else {
            throw new NotYetImplementedException();
        }
    }

    private static class WrapPair {
        private final Value newValue;
        private final Stmt newFirstStmt;
        private WrapPair(Value newValue, Stmt newFirstStmt) {
            this.newValue = newValue;
            this.newFirstStmt = newFirstStmt;
        }
    }

    private WrapPair wrap(TcArgs args, ValueBox toWrapBox, Type t) {
        Value toWrap = toWrapBox.getValue();
        SootMethodRef wrapLhs = constantWrapperMethodRef(t);
        // If toWrap is not instanceof Immediate, spawn new stack local, so that the box in VirtualInvokeExpr
        // can definitely hold it
        Stmt firstStmt = null;
        if (!(toWrap instanceof Immediate)) {
            Local newLocal = args.spawnStackLocal(toWrap.getType());
            AssignStmt assignNonImmediateToWrapToLocal = Jimple.v().newAssignStmt(newLocal, toWrap);
            args.addUnit(assignNonImmediateToWrapToLocal);
            toWrap = newLocal;
            firstStmt = assignNonImmediateToWrapToLocal;
        }
        InvokeExpr wrapLhsExpr = Jimple.v().newStaticInvokeExpr(wrapLhs, toWrap);
        Local lhsWrapLocal = args.spawnStackLocal(transformType(t));
        AssignStmt assignLhs = Jimple.v().newAssignStmt(lhsWrapLocal, wrapLhsExpr);
        args.addUnit(assignLhs);
        toWrapBox.setValue(lhsWrapLocal);
        if (firstStmt == null) {
            firstStmt = assignLhs;
        }
        return new WrapPair(lhsWrapLocal, firstStmt);
    }

    private WrapPair wrap(TcArgs args, ValueBox toWrapBox) {
        return wrap(args, toWrapBox, toWrapBox.getValue().getType());
    }

    private void transform(AssignStmt a, TcArgs args) {
        // Special treatment of method calls
        if (!args.isTainted() && a.containsInvokeExpr()) {
            adjustSignatureIfNeeded(a, a.getInvokeExpr(), args);
            assert !args.isToWrap();
            args.addUnit(a);
            return;
        }

        if (!args.isToWrap() && !args.isTainted()) {
            args.addUnit(a);
            return;
        }
        assert args.isTainted(a.getLeftOp())
                || a.getLeftOp().getType() instanceof RefType
                || a.containsInvokeExpr() && v.isIndicatorMethodName(a.getInvokeExpr().getMethodRef().getName());
        final ValueBox variableBox = a.getLeftOpBox();
        final Value var = variableBox.getValue();
        final ValueBox valueBox = a.getRightOpBox();
        final Value value = valueBox.getValue();
        assert var instanceof Local || var instanceof Ref;
        if (args.isTainted()) {
            if ((value instanceof Local || value instanceof Constant) && var instanceof ArrayRef) {
                // Store in array
                ArrayRef toStoreWithin = (ArrayRef) var;
                boolean baseIsTainted = args.isTainted(toStoreWithin.getBase());
                if (!baseIsTainted) {
                    assert !args.isTainted(toStoreWithin.getIndex());
                    args.addUnit(a);
                    return;
                }
                // If base is tainted, the array is a Sarray
                Stmt firstStmt;
                Value index;
                if (!args.isTainted(toStoreWithin.getIndex())) {
                    WrapPair wp = wrap(args, toStoreWithin.getIndexBox());
                    firstStmt = wp.newFirstStmt;
                    index = wp.newValue;
                } else {
                    firstStmt = null;
                    index = toStoreWithin.getIndex();
                }
                Value toStore;
                if (!args.isTainted(value) && !(value.getType() instanceof RefType) && !NullConstant.v().equivTo(value)) {
                    Type typeToStore;
                    RefType sarrayType = (RefType) toStoreWithin.getBase().getType();
                    if (sarrayType.equals(v.TYPE_SBOOLSARRAY)) {
                        typeToStore = v.TYPE_SBOOL;
                    } else if (sarrayType.equals(v.TYPE_SBYTESARRAY)) {
                        typeToStore = v.TYPE_SBYTE;
                    } else if (sarrayType.equals(v.TYPE_SSHORTSARRAY)) {
                        typeToStore = v.TYPE_SSHORT;
                    } else if (sarrayType.equals(v.TYPE_SFLOATSARRAY)) {
                        typeToStore = v.TYPE_SFLOAT;
                    } else if (sarrayType.equals(v.TYPE_SDOUBLESARRAY)) {
                        typeToStore = v.TYPE_SDOUBLE;
                    } else if (sarrayType.equals(v.TYPE_SLONGSARRAY)) {
                        typeToStore = v.TYPE_SLONG;
                    } else if (sarrayType.equals(v.TYPE_SINTSARRAY)) {
                        typeToStore = v.TYPE_SINT;
                    } else if (sarrayType.equals(v.TYPE_SCHARSARRAY)) {
                        typeToStore = v.TYPE_SCHAR;
                    } else {
                        // Sarrays cannot be wrapped
                        throw new NotYetImplementedException(sarrayType.toString());
                    }
                    WrapPair wp = wrap(args, valueBox, typeToStore);
                    firstStmt = firstStmt == null ? wp.newFirstStmt : firstStmt;
                    toStore = wp.newValue;
                } else {
                    toStore = value;
                }

                VirtualInvokeExpr storeInArrayExpr =
                        Jimple.v().newVirtualInvokeExpr(
                                (Local) toStoreWithin.getBase(),
                                getSelectOrStoreBasedOnArrayBase((RefType) toStoreWithin.getBase().getType(), false).makeRef(),
                                index,
                                toStore,
                                args.seLocal()
                        );

                InvokeStmt storeInArrayStmt = Jimple.v().newInvokeStmt(storeInArrayExpr);
                args.addUnit(storeInArrayStmt);
                a.redirectJumpsToThisTo(firstStmt);
                // Return since here, we do not want to add the AssignStmt
                return;
            } else if (value instanceof Constant) {
                if (NullConstant.v().equivTo(value)) {
                    args.addUnit(a);
                    return;
                } else {
                    // Find method to wrap with
                    SootMethodRef used = constantWrapperMethodRef(var.getType());
                    // Create virtual call
                    InvokeExpr invokeExpr = Jimple.v().newStaticInvokeExpr(used, var);
                    assignNewValueRedirectAndAdd(invokeExpr, null, a, valueBox, args);
                }
            } else if (value instanceof Expr) {
                if (value instanceof InstanceOfExpr) {
                    InstanceOfExpr instanceOfExpr = (InstanceOfExpr) value;
                    Type checkIfType = instanceOfExpr.getCheckType();
                    ValueBox toCheckBox = instanceOfExpr.getOpBox();
                    Value toCheck = toCheckBox.getValue();
                    VirtualInvokeExpr virtualInvokeExpr =
                            Jimple.v().newVirtualInvokeExpr(args.seLocal(), v.SM_SE_INSTANCEOF.makeRef(), toCheck, ClassConstant.fromType(checkIfType));
                    assignNewValueRedirectAndAdd(virtualInvokeExpr, null, a, valueBox, args);
                } else if (value instanceof NewExpr) {
                    // Nothing to do
                } else if (value instanceof NewArrayExpr) {
                    NewArrayExpr nae = (NewArrayExpr) value;
                    Type t = nae.getBaseType();
                    SootMethod used;
                    if (t.equals(v.TYPE_SINT)) {
                        used = v.SM_SE_SINTSARRAY;
                    } else if (t.equals(v.TYPE_SLONG)) {
                        used = v.SM_SE_SLONGSARRAY;
                    } else if (t.equals(v.TYPE_SDOUBLE)) {
                        used = v.SM_SE_SDOUBLESARRAY;
                    } else if (t.equals(v.TYPE_SFLOAT)) {
                        used = v.SM_SE_SFLOATSARRAY;
                    } else if (t.equals(v.TYPE_SSHORT)) {
                        used = v.SM_SE_SSHORTSARRAY;
                    } else if (t.equals(v.TYPE_SBYTE)) {
                        used = v.SM_SE_SBYTESARRAY;
                    } else if (t.equals(v.TYPE_SBOOL)) {
                        used = v.SM_SE_SBOOLSARRAY;
                    } else if (t.equals(v.TYPE_SCHAR)) {
                        used = v.SM_SE_SCHARSARRAY;
                    } else {
                        Type type = nae.getBaseType();
                        if (type instanceof ArrayType) {
                            used = v.SM_SE_SARRAYSARRAY;
                        } else {
                            used = v.SM_SE_PARTNER_CLASSSARRAY;
                        }
                    }
                    Stmt firstStmt = null;
                    Value sizeValue = nae.getSize();
                    if (!args.isTainted(nae.getSize())) {
                        // Still has to be wrapped
                        WrapPair wp = wrap(args, nae.getSizeBox());
                        firstStmt = wp.newFirstStmt;
                        sizeValue = wp.newValue;
                    }
                    VirtualInvokeExpr sarrayInitVirtualExpr;
                    IntConstant falseConstant = IntConstant.v(0);
                    if (used == v.SM_SE_PARTNER_CLASSSARRAY || used == v.SM_SE_SARRAYSARRAY) {
                        ClassConstant classConstant = getWrapperAwareClassConstantForType(nae.getBaseType());
                        sarrayInitVirtualExpr = Jimple.v().newVirtualInvokeExpr(args.seLocal(), used.makeRef(), sizeValue, classConstant, falseConstant);
                    } else {
                        sarrayInitVirtualExpr = Jimple.v().newVirtualInvokeExpr(args.seLocal(), used.makeRef(), sizeValue, falseConstant);
                    }
                    Local toCastLocal = args.spawnStackLocal(used.getReturnType());
                    AssignStmt assignStmt = Jimple.v().newAssignStmt(toCastLocal, sarrayInitVirtualExpr);
                    if (firstStmt == null) {
                        firstStmt = assignStmt;
                    }
                    args.addUnit(assignStmt);
                    a.redirectJumpsToThisTo(firstStmt);
                    a.setRightOp(Jimple.v().newCastExpr(toCastLocal, a.getLeftOp().getType()));
                } else if (value instanceof NewMultiArrayExpr) {
                    NewMultiArrayExpr nae = (NewMultiArrayExpr) value;
                    Stmt firstStmt = null;
                    List<Value> sizes = new ArrayList<>();
                    for (int i = 0; i < nae.getSizeCount(); i++) {
                        Value size = nae.getSize(i);
                        if (!args.isTainted(size)) {
                            // Must be wrapped
                            WrapPair wp = wrap(args, nae.getSizeBox(i));
                            if (firstStmt == null) {
                                firstStmt = wp.newFirstStmt;
                            }
                            sizes.add(wp.newValue);
                        } else {
                            sizes.add(size);
                        }
                    }
                    NewArrayExpr sizesArray = Jimple.v().newNewArrayExpr(v.TYPE_SINT, IntConstant.v(sizes.size()));
                    Local sizesArrayLocal = args.spawnStackLocal(sizesArray.getType());
                    AssignStmt assignNewArrayExprToSizesLocal =
                            Jimple.v().newAssignStmt(sizesArrayLocal, sizesArray);
                    args.addUnit(assignNewArrayExprToSizesLocal);
                    for (int i = 0; i < sizes.size(); i++) {
                        ArrayRef toStoreIn = Jimple.v().newArrayRef(sizesArrayLocal, IntConstant.v(i));
                        AssignStmt assignSizeToSizeArray = Jimple.v().newAssignStmt(toStoreIn, sizes.get(i));
                        args.addUnit(assignSizeToSizeArray);
                    }

                    ArrayType elementType = ArrayType.v(nae.getBaseType().baseType, nae.getBaseType().numDimensions - 1);
                    VirtualInvokeExpr newSarraySarrayExpr =
                            Jimple.v().newVirtualInvokeExpr(
                                    args.seLocal(),
                                    v.SM_SE_MULTIDIM_SARRAYSARRAY.makeRef(), // Must be SarraySarray
                                    sizesArrayLocal,
                                    getWrapperAwareClassConstantForType(elementType)
                            );
                    Local toCastLocal = args.spawnStackLocal(v.SM_SE_MULTIDIM_SARRAYSARRAY.getReturnType());
                    AssignStmt assignStmt = Jimple.v().newAssignStmt(toCastLocal, newSarraySarrayExpr);
                    if (firstStmt == null) {
                        firstStmt = assignStmt;
                    }
                    args.addUnit(assignStmt);
                    a.redirectJumpsToThisTo(firstStmt);
                    Local castedLocal = args.spawnStackLocal(a.getLeftOp().getType());
                    AssignStmt cast = Jimple.v().newAssignStmt(castedLocal, Jimple.v().newCastExpr(toCastLocal, castedLocal.getType()));
                    args.addUnit(cast);
                    a.setRightOp(castedLocal);
                } else if (value instanceof UnopExpr) {
                    assert args.isTainted(value);
                    Type t = ((UnopExpr) value).getOp().getType();
                    SootMethodRef used;
                    Expr expr;
                    if (value instanceof NegExpr) {
                        if (isIntOrSintSubtype(t)) {
                            used = v.SM_SINT_NEG.makeRef();
                        } else if (isDoubleOrSdouble(t)) {
                            used = v.SM_SDOUBLE_NEG.makeRef();
                        } else if (isFloatOrSfloat(t)) {
                            used = v.SM_SFLOAT_NEG.makeRef();
                        } else if (isLongOrSlong(t)) {
                            used = v.SM_SLONG_NEG.makeRef();
                        } else {
                            throw new NotYetImplementedException(value.toString());
                        }
                        expr = Jimple.v().newVirtualInvokeExpr((Local) ((NegExpr) value).getOp(), used, args.seLocal());
                    } else if (value instanceof LengthExpr) {
                        LengthExpr lengthExpr = (LengthExpr) value;
                        assert args.isTainted(lengthExpr.getOp()) : "If a length expression is tainted, the array itself must be tainted!";
                        // If array local itself is tainted, we need to call the length method
                        expr = Jimple.v().newVirtualInvokeExpr((Local) lengthExpr.getOp(), v.SM_SARRAY_LENGTH.makeRef());
                    } else {
                        throw new NotYetImplementedException(value.toString());
                    }
                    // Create virtual call
                    assignNewValueRedirectAndAdd(expr, null, a, valueBox, args);
                } else if (value instanceof BinopExpr) {
                    BinopExpr b = (BinopExpr) value;
                    // In Java, we have Binops between two of the same types
                    // TODO still, this is a workaround, as for b.getType() "unknown" is returned, since Sbool etc. are not known to Soot
                    Type t = b.getOp1().getType();
                    SootMethodRef used;
                    if (b instanceof OrExpr) {
                        if (isIntOrSintSubtype(t)) {
                            used = v.SM_SINT_IOR.makeRef();
                        } else {
                            assert isLongOrSlong(t);
                            used = v.SM_SLONG_LOR.makeRef();
                        }
                    } else if (b instanceof DivExpr) {
                        if (isIntOrSintSubtype(t)) {
                            used = v.SM_SINT_DIV.makeRef();
                        } else if (isLongOrSlong(t)) {
                            used = v.SM_SLONG_DIV.makeRef();
                        } else if (isDoubleOrSdouble(t)) {
                            used = v.SM_SDOUBLE_DIV.makeRef();
                        } else if (isFloatOrSfloat(t)) {
                            used = v.SM_SFLOAT_DIV.makeRef();
                        } else {
                            throw new NotYetImplementedException();
                        }
                    } else if (b instanceof UshrExpr) {
                        if (isIntOrSintSubtype(t)) {
                            used = v.SM_SINT_IUSHR.makeRef();
                        } else {
                            assert isLongOrSlong(t);
                            used = v.SM_SLONG_LUSHR.makeRef();
                        }
                    } else if (b instanceof CmpExpr || b instanceof CmpgExpr || b instanceof CmplExpr) {
                        // ImmediateBox is used --> must be Local
                        ((Local) var).setType(v.TYPE_SINT);
                        if (isLongOrSlong(t)) {
                            used = v.SM_SLONG_CMP.makeRef();
                        } else if (isDoubleOrSdouble(t)) {
                            used = v.SM_SDOUBLE_CMP.makeRef();
                        } else if (isFloatOrSfloat(t)) {
                            used = v.SM_SFLOAT_CMP.makeRef();
                        } else {
                            throw new NotYetImplementedException(b.toString());
                        }
                    } else if (b instanceof RemExpr) {
                        if (isIntOrSintSubtype(t)) {
                            used = v.SM_SINT_MOD.makeRef();
                        } else if (isDoubleOrSdouble(t)) {
                            used = v.SM_SDOUBLE_MOD.makeRef();
                        } else if (isLongOrSlong(t)) {
                            used = v.SM_SLONG_MOD.makeRef();
                        } else if (isFloatOrSfloat(t)) {
                            used = v.SM_SFLOAT_MOD.makeRef();
                        } else {
                            throw new NotYetImplementedException(b.toString());
                        }
                    } else if (b instanceof XorExpr) {
                        if (isIntOrSintSubtype(t)) {
                            used = v.SM_SINT_IXOR.makeRef();
                        } else {
                            assert isLongOrSlong(t);
                            used = v.SM_SLONG_LXOR.makeRef();
                        }
                    } else if (b instanceof SubExpr) {
                        if (isIntOrSintSubtype(t)) {
                            used = v.SM_SINT_SUB.makeRef();
                        } else if (isLongOrSlong(t)) {
                            used = v.SM_SLONG_SUB.makeRef();
                        } else if (isDoubleOrSdouble(t)) {
                            used = v.SM_SDOUBLE_SUB.makeRef();
                        } else if (isFloatOrSfloat(t)) {
                            used = v.SM_SFLOAT_SUB.makeRef();
                        } else {
                            throw new NotYetImplementedException();
                        }
                    } else if (b instanceof ConditionExpr) {
                        throw new NotYetImplementedException();
                    } else if (b instanceof ShlExpr) {
                        if (isIntOrSintSubtype(t)) {
                            used = v.SM_SINT_ISHL.makeRef();
                        } else {
                            assert isLongOrSlong(t);
                            used = v.SM_SLONG_LSHL.makeRef();
                        }
                    } else if (b instanceof AddExpr) {
                        if (isIntOrSintSubtype(t)) {
                            used = v.SM_SINT_ADD.makeRef();
                        } else if (isLongOrSlong(t)) {
                            used = v.SM_SLONG_ADD.makeRef();
                        } else if (isDoubleOrSdouble(t)) {
                            used = v.SM_SDOUBLE_ADD.makeRef();
                        } else if (isFloatOrSfloat(t)) {
                            used = v.SM_SFLOAT_ADD.makeRef();
                        } else {
                            throw new NotYetImplementedException();
                        }
                    } else if (b instanceof MulExpr) {
                        if (isIntOrSintSubtype(t)) {
                            used = v.SM_SINT_MUL.makeRef();
                        } else if (isLongOrSlong(t)) {
                            used = v.SM_SLONG_MUL.makeRef();
                        } else if (isDoubleOrSdouble(t)) {
                            used = v.SM_SDOUBLE_MUL.makeRef();
                        } else if (isFloatOrSfloat(t)) {
                            used = v.SM_SFLOAT_MUL.makeRef();
                        } else {
                            throw new NotYetImplementedException();
                        }
                    } else if (b instanceof AndExpr) {
                        if (isIntOrSintSubtype(t)) {
                            used = v.SM_SINT_IAND.makeRef();
                        } else {
                            assert isLongOrSlong(t);
                            used = v.SM_SLONG_LAND.makeRef();
                        }
                    } else if (b instanceof ShrExpr) {
                        if (isIntOrSintSubtype(t)) {
                            used = v.SM_SINT_ISHR.makeRef();
                        } else {
                            assert isLongOrSlong(t);
                            used = v.SM_SLONG_LSHR.makeRef();
                        }
                    } else {
                        throw new NotYetImplementedException(b.toString());
                    }
                    ValueBox lhsBox = b.getOp1Box();
                    ValueBox rhsBox = b.getOp2Box();
                    Value lhs = lhsBox.getValue();
                    Value rhs = rhsBox.getValue();
                    Stmt firstStatement = null;
                    if (!args.isTainted(lhs)) {
                        WrapPair wp = wrap(args, lhsBox);
                        lhs = wp.newValue;
                        firstStatement = wp.newFirstStmt;
                    }
                    if (!args.isTainted(rhs)) {
                        WrapPair wp = wrap(args, rhsBox);
                        rhs = wp.newValue;
                        firstStatement = firstStatement == null ? wp.newFirstStmt : firstStatement;
                    }
                    // Create virtual call
                    // Lhs must be Local, since BinopExpr uses ImmediateBox
                    VirtualInvokeExpr virtualInvokeExpr = Jimple.v().newVirtualInvokeExpr((Local) lhs, used, rhs, args.seLocal());
                    assignNewValueRedirectAndAdd(virtualInvokeExpr, firstStatement, a, valueBox, args);
                } else if (value instanceof InvokeExpr) {
                    InvokeExpr invokeExpr = (InvokeExpr) value;
                    InvokeExpr possiblyTransformedIndicatorMethod = getInvokeExprIfIndicatorMethodExprElseNull(a, invokeExpr, args);

                    if (possiblyTransformedIndicatorMethod != null) {
                        AssignStmt newAssignStmt = Jimple.v().newAssignStmt(var, possiblyTransformedIndicatorMethod);
                        a.redirectJumpsToThisTo(newAssignStmt);
                        args.addUnit(newAssignStmt);
                        return; // Terminate immediately, we do not need the indicator method
                    } else {
                        wrapInvokeExprArgsIfNeededRedirectAndAdd(a, invokeExpr, args);
                    }
                } else if (value instanceof CastExpr) {
                    boolean isObjectCast = false;
                    Type typeToCast = ((CastExpr) value).getOp().getType();
                    Type castTo = ((CastExpr) value).getCastType();
                    SootMethodRef used;
                    if (!isIntOrSint(castTo) && (isIntOrSint(typeToCast) || isByteOrSbyte(typeToCast) || isShortOrSshort(typeToCast) || isCharOrSchar(typeToCast))) {
                        if (isDoubleOrSdouble(castTo)) {
                            used = v.SM_SINT_I2D.makeRef();
                        } else if (isFloatOrSfloat(castTo)) {
                            used = v.SM_SINT_I2F.makeRef();
                        } else if (isLongOrSlong(castTo)) {
                            used = v.SM_SINT_I2L.makeRef();
                        } else if (isByteOrSbyte(castTo)) {
                            used = v.SM_SINT_I2B.makeRef();
                        } else if (isShortOrSshort(castTo)) {
                            used = v.SM_SINT_I2S.makeRef();
                        } else if (isCharOrSchar(castTo)) {
                            used = v.SM_SINT_I2C.makeRef();
                        } else if (isIntOrSint(castTo)) {
                            // To preserve other units using var, we simply assign it
                            AssignStmt newAssignStmt = Jimple.v().newAssignStmt(var, ((CastExpr) value).getOp());
                            a.redirectJumpsToThisTo(newAssignStmt);
                            args.addUnit(newAssignStmt);
                            return;
                        } else {
                            throw new NotYetImplementedException(castTo.toString());
                        }
                    } else if (isLongOrSlong(typeToCast)) {
                        if (isIntOrSintSubtype(castTo)) {
                            used = v.SM_SLONG_L2I.makeRef();
                        } else if (isDoubleOrSdouble(castTo)) {
                            used = v.SM_SLONG_L2D.makeRef();
                        } else if (isFloatOrSfloat(castTo)) {
                            used = v.SM_SLONG_L2F.makeRef();
                        } else {
                            throw new NotYetImplementedException(castTo.toString());
                        }
                    } else if (isDoubleOrSdouble(typeToCast)) {
                        if (isIntOrSintSubtype(castTo)) {
                            used = v.SM_SDOUBLE_D2I.makeRef();
                        } else if (isLongOrSlong(castTo)) {
                            used = v.SM_SDOUBLE_D2L.makeRef();
                        } else if (isFloatOrSfloat(castTo)) {
                            used = v.SM_SDOUBLE_D2F.makeRef();
                        } else {
                            throw new NotYetImplementedException(castTo.toString());
                        }
                    } else if (isFloatOrSfloat(typeToCast)) {
                        if (isIntOrSintSubtype(castTo)) {
                            used = v.SM_SFLOAT_F2I.makeRef();
                        } else if (isDoubleOrSdouble(castTo)) {
                            used = v.SM_SFLOAT_F2D.makeRef();
                        } else if (isLongOrSlong(castTo)) {
                            used = v.SM_SFLOAT_F2L.makeRef();
                        } else {
                            throw new NotYetImplementedException(castTo.toString());
                        }
                    } else if (isByteOrSbyte(typeToCast)) {
                        // Soot for some reason sometimes inserts statements like int i = (int) <byte value>
                        args.addUnit(a);
                        return;
                    } else if (isShortOrSshort(typeToCast)) {
                        args.addUnit(a);
                        return;
                    } else if (isBoolOrSbool(typeToCast)) {
                        throw new NotYetImplementedException();
                    } else {
                        used = v.SM_SE_CAST_TO.makeRef();
                        isObjectCast = true;
                    }
                    ValueBox opBox = ((CastExpr) value).getOpBox();
                    Value op = opBox.getValue();
                    VirtualInvokeExpr virtualInvokeExpr;
                    Stmt firstStatement = null;
                    if (isObjectCast) {
                        // This is solely here to trigger the required functionality of the search framework
                        virtualInvokeExpr = Jimple.v().newVirtualInvokeExpr(
                                args.seLocal(),
                                v.SM_SE_CAST_TO.makeRef(),
                                ((CastExpr) value).getOp(),
                                ClassConstant.fromType(castTo)
                        );
                    } else {
                        if (!args.isTainted(op) && !NullConstant.v().equivTo(op)) {
                            WrapPair wp = wrap(args, opBox);
                            op = wp.newValue;
                            firstStatement = wp.newFirstStmt;
                        }
                        // Create virtual call
                        // op must be Local, since JCastExpr uses instance of ImmediateBox
                        virtualInvokeExpr = Jimple.v().newVirtualInvokeExpr((Local) op, used, args.seLocal());
                    }
                    Local castCallResult = args.spawnStackLocal(castTo);
                    AssignStmt castCall = Jimple.v().newAssignStmt(castCallResult, virtualInvokeExpr);
                    args.addUnit(castCall);
                    if (firstStatement == null) {
                        firstStatement = castCall;
                    }
                    CastExpr castExpr = Jimple.v().newCastExpr(castCallResult, castTo);
                    assignNewValueRedirectAndAdd(castExpr, firstStatement, a, valueBox, args);
                }
            } else if (value instanceof Ref) {
                if (!(value instanceof IdentityRef) && !(value instanceof FieldRef)) {
                    // Load from array
                    assert value instanceof ArrayRef;
                    // If the value is ArrayRef, it means that we select
                    assert var instanceof Local;
                    ArrayRef arrayRefValue = (ArrayRef) value;
                    boolean baseIsTainted = args.isTainted(arrayRefValue.getBase());
                    if (!baseIsTainted) {
                        assert !args.isTainted(arrayRefValue.getIndex());
                        args.addUnit(a);
                        return;
                    }

                    Stmt firstStmt = null;
                    Value index = arrayRefValue.getIndex();
                    if (!args.isTainted(arrayRefValue.getIndex())) {
                        // We still need to taint the value!
                        WrapPair wp = wrap(args, arrayRefValue.getIndexBox());
                        firstStmt = wp.newFirstStmt;
                        index = wp.newValue;
                    }
                    assert arrayRefValue.getBase().getType() instanceof RefType : "The array has not been transformed to " +
                            "a Sarray class but should be used to perform select or store operations";
                    SootMethodRef selectRef = getSelectOrStoreBasedOnArrayBase((RefType) arrayRefValue.getBase().getType(), true).makeRef();
                    Expr expr = Jimple.v().newVirtualInvokeExpr(
                            (Local) arrayRefValue.getBase(),
                            selectRef,
                            index, args.seLocal()
                    );
                    Local stillToCastLocal = args.spawnStackLocal(v.TYPE_OBJECT);
                    AssignStmt stillToCastAssign =
                            Jimple.v().newAssignStmt(stillToCastLocal, expr);
                    args.addUnit(stillToCastAssign);
                    firstStmt = firstStmt == null ? stillToCastAssign : firstStmt;
                    CastExpr castToCorrectValue = Jimple.v().newCastExpr(stillToCastLocal, var.getType());
                    assignNewValueRedirectAndAdd(castToCorrectValue, firstStmt, a, valueBox, args);
                }
                // Else nothing to do
            }
            // Nothing to do for Local
        }
        // If unit is to be wrapped, call respective method
        if (args.isToWrap()) {
            assert !args.isTainted(value);
            assert isPrimitiveOrSprimitive(var.getType()) : "Illegal type to be wrapped: " + var.getType();
            assert args.isTainted(var);
            WrapPair wp = wrap(args, valueBox, var.getType());
            assignNewValueRedirectAndAdd(wp.newValue, wp.newFirstStmt, a, valueBox, args);
        }
        args.addUnit(a);
    }

    private SootMethod getSelectOrStoreBasedOnArrayBase(RefType arrayType, boolean returnSelect) {
        if (arrayType.equals(v.TYPE_SINTSARRAY)) {
            return returnSelect ? v.SM_SINTSARRAY_SELECT : v.SM_SINTSARRAY_STORE;
        } else if (arrayType.equals(v.TYPE_SLONGSARRAY)) {
            return returnSelect ? v.SM_SLONGSARRAY_SELECT : v.SM_SLONGSARRAY_STORE;
        } else if (arrayType.equals(v.TYPE_SDOUBLESARRAY)) {
            return returnSelect ? v.SM_SDOUBLESARRAY_SELECT : v.SM_SDOUBLESARRAY_STORE;
        } else if (arrayType.equals(v.TYPE_SFLOATSARRAY)) {
            return returnSelect ? v.SM_SFLOATSARRAY_SELECT : v.SM_SFLOATSARRAY_STORE;
        } else if (arrayType.equals(v.TYPE_SSHORTSARRAY)) {
            return returnSelect ? v.SM_SSHORTSARRAY_SELECT : v.SM_SSHORTSARRAY_STORE;
        } else if (arrayType.equals(v.TYPE_SBYTESARRAY)) {
            return returnSelect ? v.SM_SBYTESARRAY_SELECT : v.SM_SBYTESARRAY_STORE;
        } else if (arrayType.equals(v.TYPE_SBOOLSARRAY)) {
            return returnSelect ? v.SM_SBOOLSARRAY_SELECT : v.SM_SBOOLSARRAY_STORE;
        } else if (arrayType.equals(v.TYPE_SCHARSARRAY)) {
            return returnSelect ? v.SM_SCHARSARRAY_SELECT : v.SM_SCHARSARRAY_STORE;
        } else if (arrayType.equals(v.TYPE_SARRAYSARRAY) || specificSarraySarrayTypes.contains(arrayType.getSootClass())) {
            return returnSelect ? v.SM_SARRAYSARRAY_SELECT : v.SM_SARRAYSARRAY_STORE;
        } else if (arrayType.equals(v.TYPE_PARTNER_CLASSSARRAY) || specificPartnerClassSarrayTypes.contains(arrayType.getSootClass())) {
            return returnSelect ? v.SM_PARTNER_CLASSSARRAY_SELECT : v.SM_PARTNER_CLASSSARRAY_STORE;
        } else {
            throw new NotYetImplementedException();
        }
    }

    // Checks whether the invoke expression (representing a method call) should be generalized or its inputs concretized.
    // If so, it adds the respective instructions. Otherwise, nothing is done
    private void adjustSignatureIfNeeded(Stmt originalStmt, InvokeExpr invokeExpr, TcArgs args) {
        assert originalStmt instanceof AssignStmt || originalStmt instanceof InvokeStmt;
        if (invokeExpr.getArgCount() == 0) {
            return;
        }
        if (args.taintAnalysis().generalizeSignature.contains(originalStmt)) {
            boolean shouldBeReplaced = false;
            SootMethodRef smr = invokeExpr.getMethodRef();
            List<SootMethod> alternativeSootMethods =
                    smr.getDeclaringClass().getMethods()
                            .stream()
                            .filter(sm -> sm.getName().equals(smr.getName()) && sm.getParameterCount() == smr.getParameterTypes().size())
                            .collect(Collectors.toList());
            List<Type> parameterTypes = smr.getParameterTypes();
            for (int i = 0; i < parameterTypes.size(); i++) {
                Type currentType = parameterTypes.get(i);
                // If there is a primitive that was substituted, we need to find a more generic implementation
                // since the class that is not transformed should not implement any methods with
                // Mulib framework-parameters.
                if (isPrimitiveOrSprimitive(currentType) && args.isTainted(invokeExpr.getArg(i))) {
                    shouldBeReplaced = true;
                    final int parameterFixed = i;
                    alternativeSootMethods =
                            alternativeSootMethods.stream()
                                    .filter(asm -> asm.getParameterType(parameterFixed).equals(v.TYPE_OBJECT))
                                    .collect(Collectors.toList());
                }
            }
            if (shouldBeReplaced) {
                if (alternativeSootMethods.isEmpty()) {
                    // No-op for now
//                    throw new MisconfigurationException("There are no valid methods to generalize to for " + smr);
                } else {
                    // Pick any
                    invokeExpr.setMethodRef(alternativeSootMethods.get(0).makeRef());
                }
            }
        } else if (args.taintAnalysis().concretizeInputs.contains(originalStmt)) {
            SootMethod referencedMethod = invokeExpr.getMethod();
            for (int i = 0; i < invokeExpr.getArgCount(); i++) {
                ValueBox vb = invokeExpr.getArgBox(i);
                Value val = vb.getValue();
                // If argument is ref and we should not transform, we do not need to concretize
                Type paramType = referencedMethod.getParameterType(i);
                if (paramType instanceof RefType && !shouldBeTransformed(((RefType) paramType).getClassName())) {
                    continue;
                }
                // If the value is not tainted, we do not need to concretize it
                // If the value is a reference type
                if (!args.isTainted(val) && !hasTransformedRefType(val.getType())) {
                    continue;
                }
                Local concretizeResult = args.spawnStackLocal(v.TYPE_OBJECT);
                AssignStmt concretizeCall = Jimple.v().newAssignStmt(
                        concretizeResult,
                        Jimple.v().newVirtualInvokeExpr(args.seLocal(), v.SM_SE_CONCRETIZE.makeRef(), val)
                );
                Type castTo = wrapTypeIfNecessary(paramType);
                Local castConcretization = args.spawnStackLocal(castTo);
                AssignStmt castCall = Jimple.v().newAssignStmt(
                        castConcretization,
                        Jimple.v().newCastExpr(concretizeResult, castTo)
                );
                args.addUnit(concretizeCall);
                originalStmt.redirectJumpsToThisTo(concretizeCall);
                args.addUnit(castCall);

                if (paramType instanceof PrimType) {
                    // After concretizing, a wrapper object is returned
                    Local unwrappedValue = args.spawnStackLocal(paramType);
                    SootMethodRef getValue = getValueFromNumberMethodRef(paramType);
                    AssignStmt unwrap = Jimple.v().newAssignStmt(
                            unwrappedValue,
                            Jimple.v().newVirtualInvokeExpr(castConcretization, getValue)
                    );
                    args.addUnit(unwrap);
                    vb.setValue(unwrappedValue);
                } else {
                    vb.setValue(castConcretization);
                }
            }
            // originalStmt will be added in the method calling this
        }
    }

    private boolean hasTransformedRefType(Type t) {
        if (t instanceof RefType) {
            RefType rt = (RefType) t;
            String className = rt.getClassName();
            String simpleClassName = className.substring(className.lastIndexOf('.') + 1);
            return simpleClassName.startsWith(_TRANSFORMATION_INDICATOR) || className.startsWith("de.wwu.mulib.substitutions");
        } else if (t instanceof ArrayType) {
            Type innermostType = getInnermostTypeInArray((ArrayType) t);
            return hasTransformedRefType(innermostType);
        }

        return false;
    }

    private void wrapInvokeExprArgsIfNeededRedirectAndAdd(Stmt redirectJumpsToFrom, InvokeExpr invokeExpr, TcArgs args) {
        boolean firstWrappingStatement = true;
        SootMethodRef calledMethod = invokeExpr.getMethodRef();
        // Check if inputs must be wrapped
        for (int i = 0; i < invokeExpr.getArgCount(); i++) {
            ValueBox vb = invokeExpr.getArgBox(i);
            Value v = vb.getValue();
            if (!args.isTainted(v) && !(v.getType() instanceof RefType) && !(v instanceof NullConstant)) {
                assert !(v.getType() instanceof ArrayType);
                // Wrap input
                // Get method ref for wrapping
                SootMethodRef smr = constantWrapperMethodRef(calledMethod.getParameterType(i));
                // Call method ref (e.g. SymbolicExecution.concSint(int, SymbolicExecution))
                InvokeExpr wrappingExpr = Jimple.v().newStaticInvokeExpr(smr, v);
                // Store result in stack local
                Local stackLocal = args.spawnStackLocal(smr.getReturnType());
                AssignStmt wrappingStmt = Jimple.v().newAssignStmt(stackLocal, wrappingExpr);
                // We set the new argument of the original invokeExpr to this new stackLocal
                vb.setValue(stackLocal);
                // Add unit
                args.addUnit(wrappingStmt);
                if (firstWrappingStatement) {
                    // If this is the first wrapping statement, we need to redirect jumps from the original
                    // assign statement to this
                    redirectJumpsToFrom.redirectJumpsToThisTo(wrappingStmt);
                    firstWrappingStatement = false;
                }
            }
        }
    }

    private void assignNewValueRedirectAndAdd(
            Value valueToCreateStackLocalFor,
            Stmt firstStatement,
            Unit redirectJumpsFrom,
            ValueBox changeValueFor,
            Function<Local, Value> transformStackLocal,
            TcArgs args) {
        // Create new stack local to store result in
        Local stackLocal = args.spawnStackLocal(valueToCreateStackLocalFor.getType());
        // Assign expression to new stack local
        AssignStmt assignStmt = Jimple.v().newAssignStmt(stackLocal, valueToCreateStackLocalFor);
        firstStatement = firstStatement == null ? assignStmt : firstStatement;
        // The new assigned value is now the stack local
        Value value = changeValueFor.getValue();
//        assert args.isToWrap() || (args.isTainted() && (args.isTainted(value) || value.getType() instanceof RefType));
        Value transformedStackLocal = transformStackLocal.apply(stackLocal);
        changeValueFor.setValue(transformedStackLocal);
        // Redirect jumps
        redirectJumpsFrom.redirectJumpsToThisTo(firstStatement);
        // Add assign
        args.addUnit(assignStmt);
        if (value instanceof Local && args.isTainted(value)) {
            AssignStmt a = Jimple.v().newAssignStmt(value, transformedStackLocal);
            args.addUnit(a);
        }
    }

    private void assignNewValueRedirectAndAdd(
            Value valueToCreateStackLocalFor,
            Stmt firstStatement,
            Unit redirectJumpsFrom,
            ValueBox changeValueFor,
            TcArgs args) {
        assignNewValueRedirectAndAdd(
                valueToCreateStackLocalFor,
                firstStatement,
                redirectJumpsFrom,
                changeValueFor,
                l -> l, // Just use identity
                args
        );
    }


    private final Set<Value> transformedValues = new HashSet<>();
    private Value transformValue(Value toTransform, TaintAnalysis a) {
        // Since we never replace ValueBoxes, useBoxes does not have to be adapted throughout here
        if (transformedValues.contains(toTransform)) {
            // ValueBox containing this Value has already been regarded
            return toTransform;
        }
        if (toTransform instanceof InvokeExpr
                // TODO invokedynamic
                && !(toTransform instanceof DynamicInvokeExpr)) {
            // Transform InvokeExprs that belong to classes that are substituted
            transformExpr((Expr) toTransform, a);
            transformedValues.add(toTransform);
            return toTransform;
        }
        if (isRefTypeOrArrayWithInnerRefType(toTransform.getType())) {
            // Check if value can be statically transformed: That is the case for values with a reference type
            if (!isToTransformRefTypeOrArrayWithInnerToTransformRefType(toTransform.getType())
                    // For the fields, we need to check if we need to transform the owner of the field
                    && !(toTransform instanceof FieldRef
                    && isToTransformRefTypeOrArrayWithInnerToTransformRefType(((FieldRef) toTransform).getField().getDeclaringClass().getType()))) {
                transformedValues.add(toTransform);
                return toTransform;
            }
            // If it is to transform, continue!
        } else if (!a.taintedValues.contains(toTransform)) {
            // This is in an else-if-block since the type of ref types must still be transformed to the partner class type
            if (!(toTransform instanceof InstanceOfExpr)) {
                transformedValues.add(toTransform);
                return toTransform;
            }
        }
        Value transformed;
        if (toTransform instanceof Constant) {
            transformed = transformConstant((Constant) toTransform, a);
        } else if (toTransform instanceof Expr) {
            transformed = transformExpr((Expr) toTransform, a);
        } else if (toTransform instanceof Local) {
            // We solely transform array to Sarray if toTransform is actually tainted
            transformed = transformLocal((Local) toTransform, a.taintedValues.contains(toTransform));
        } else if (toTransform instanceof Ref) {
            transformed = transformRef((Ref) toTransform, a);
        } else {
            throw new NotYetImplementedException();
        }
        transformedValues.add(transformed);
        return transformed;
    }

    private Value transformRef(Ref r, TaintAnalysis a) {
        Value transformed;
        if (r instanceof IdentityRef) {
            transformed = transformIdentityRef((IdentityRef) r);
        } else if (r instanceof ConcreteRef) {
            transformed = transformConcreteRef((ConcreteRef) r, a);
        } else {
            throw new NotYetImplementedException();
        }
        return transformed;
    }

    private Value transformIdentityRef(IdentityRef i) {
        Value transformed;
        if (i instanceof CaughtExceptionRef) {
            transformed = Jimple.v().newCaughtExceptionRef();
        } else if (i instanceof ThisRef) {
            Type transformedType = transformType(i.getType());
            transformed = new ThisRef((RefType) transformedType);
        } else if (i instanceof ParameterRef) {
            ParameterRef parameterRef = (ParameterRef) i;
            transformed = new ParameterRef(transformType(parameterRef.getType()), parameterRef.getIndex());
        } else {
            throw new NotYetImplementedException();
        }
        return transformed;
    }

    private Value transformConcreteRef(ConcreteRef c, TaintAnalysis a) {
        Value transformed;
        if (c instanceof ArrayRef) {
            ArrayRef arrayRef = (ArrayRef) c;
            arrayRef.setBase((Local) transformValue(arrayRef.getBase(), a));
            arrayRef.setIndex(transformValue(arrayRef.getIndex(), a));
            transformed = arrayRef;
        } else if (c instanceof FieldRef) {
            FieldRef fieldRef = (FieldRef) c;
            SootFieldRef sfr = fieldRef.getFieldRef();
            SootClass declaringClassOfField = decideOnWhetherStillToCreatePartnerClass(sfr.declaringClass());
            SootFieldRef newSfr =
                    new AbstractSootFieldRef(
                            declaringClassOfField,
                            sfr.name(),
                            transformType(sfr.type()),
                            sfr.isStatic()
                    );
            fieldRef.setFieldRef(newSfr);
            if (fieldRef instanceof InstanceFieldRef) {
                ((InstanceFieldRef) fieldRef).setBase(transformValue(((InstanceFieldRef) c).getBase(), a));
            }
            transformed = fieldRef;
        } else {
            throw new NotYetImplementedException();
        }
        return transformed;
    }

    private Value transformLocal(Local l, boolean arrayToSarray) {
        l.setType(transformType(l.getType(), arrayToSarray));
        return l;
    }

    private Value transformConstant(Constant c, TaintAnalysis a) {
        Constant transformed;
        if (c instanceof MethodHandle) {
//            MethodHandle methodHandle = (MethodHandle) c;
            throw new NotYetImplementedException();
        } else if (c instanceof MethodType) {
//            MethodType methodType = (MethodType) c;
            throw new NotYetImplementedException();
        } else if (c instanceof ClassConstant) {
            String newValue = ((ClassConstant) c).getValue();
            String adapted = newValue.substring(1, newValue.length()-1).replace("/", ".");
            if (shouldBeTransformed(adapted)) {
                newValue = addTransformationIndicatorToPath(newValue);
            }
            transformed = ClassConstant.v(newValue);
        } else if (c instanceof StringConstant
                || c instanceof NumericConstant
                || c instanceof NullConstant) {
            // Nothing we can do here, the constant is to be wrapped
            transformed = c;
        } else {
            throw new NotYetImplementedException();
        }
        return transformed;
    }

    private Value transformExpr(Expr e, TaintAnalysis a) {
        Value transformed;
        if (e instanceof InstanceOfExpr) {
            InstanceOfExpr instanceOfExpr = (InstanceOfExpr) e;
            instanceOfExpr.setCheckType(transformType(instanceOfExpr.getCheckType()));
            instanceOfExpr.setOp(transformValue(instanceOfExpr.getOp(), a));
            transformed = instanceOfExpr;
        } else if (e instanceof NewExpr) {
            NewExpr newExpr = (NewExpr) e;
            // Sanity check
            assert newExpr.getBaseType() == newExpr.getType();
            newExpr.setBaseType((RefType) transformType(newExpr.getBaseType()));
            transformed = newExpr;
        } else if (e instanceof NewArrayExpr) {
            NewArrayExpr newArrayExpr = (NewArrayExpr) e;
            newArrayExpr.setSize(transformValue(newArrayExpr.getSize(), a));
            Type transformedBaseType = newArrayExpr.getBaseType() instanceof ArrayType ?
                    transformArrayType((ArrayType) newArrayExpr.getBaseType(), false)
                    :
                    transformType(newArrayExpr.getBaseType());
            newArrayExpr.setBaseType(transformedBaseType);
            transformed = newArrayExpr;
        } else if (e instanceof NewMultiArrayExpr) {
            NewMultiArrayExpr newMultiArrayExpr = (NewMultiArrayExpr) e;
            for (int i = 0; i < newMultiArrayExpr.getSizeCount(); i++) {
                newMultiArrayExpr.setSize(i, transformValue(newMultiArrayExpr.getSize(i), a));
            }
            Type transformedBaseType = transformType(newMultiArrayExpr.getBaseType().baseType);
            newMultiArrayExpr.setBaseType(
                    ArrayType.v(transformedBaseType, newMultiArrayExpr.getBaseType().numDimensions));
            transformed = newMultiArrayExpr;
        } else if (e instanceof UnopExpr) {
            UnopExpr unopExpr = (UnopExpr) e;
            unopExpr.setOp(transformValue(unopExpr.getOp(), a));
            transformed = unopExpr;
        } else if (e instanceof BinopExpr) {
            BinopExpr binopExpr = (BinopExpr) e;
            binopExpr.setOp1(transformValue(binopExpr.getOp1(), a));
            binopExpr.setOp2(transformValue(binopExpr.getOp2(), a));
            transformed = binopExpr;
        } else if (e instanceof InvokeExpr) {
            InvokeExpr invokeExpr = (InvokeExpr) e;
            if (invokeExpr instanceof DynamicInvokeExpr) {
                DynamicInvokeExpr dynamicInvokeExpr = (DynamicInvokeExpr) invokeExpr;
                List<Value> bootstrapArguments = new ArrayList<>();
                for (int i = 0; i < dynamicInvokeExpr.getBootstrapArgCount(); i++) {
                    bootstrapArguments.add(dynamicInvokeExpr.getBootstrapArg(i));
                }
//                for (int i = 0; i < dynamicInvokeExpr.getArgCount(); i++) {
//                    dynamicInvokeExpr.setArg(i, transformValue(dynamicInvokeExpr.getArg(i)));
//                }
//                DynamicInvokeExpr newDynamicInvokeExpr =
//                        Jimple.v().newDynamicInvokeExpr(
//                                transformMethodRef(dynamicInvokeExpr.getBootstrapMethodRef()),
//                                bootstrapArguments,
//                                ,
//                                dynamicInvokeExpr.getHandleTag(),
//                                dynamicInvokeExpr.getArgs()
//                        );
//                invokeExpr = newDynamicInvokeExpr;
                // TODO
                throw new NotYetImplementedException();
            } else {
                if (!shouldBeTransformed(invokeExpr.getMethodRef().getDeclaringClass().getName())) {
                    // We check whether there is a replacement for the called method
                    SootMethod substituteBy = this.sootReplaceMethodWithOtherMethod.get(invokeExpr.getMethod());
                    if (substituteBy != null) {
                        // No transformation
                        invokeExpr.setMethodRef(substituteBy.makeRef());
                    }
                    return invokeExpr;
                }
                for (int i = 0; i < invokeExpr.getArgCount(); i++) {
                    invokeExpr.setArg(i, transformValue(invokeExpr.getArg(i), a));
                }
                SootMethodRef invokedRef = transformMethodRef(invokeExpr.getMethodRef());
                invokeExpr.setMethodRef(invokedRef);
            }
            transformed = invokeExpr;
        } else if (e instanceof CastExpr) {
            CastExpr castExpr = (CastExpr) e;
            castExpr.setOp(transformValue(castExpr.getOp(), a));
            assert castExpr.getType() == castExpr.getCastType();
            castExpr.setCastType(transformType(castExpr.getCastType()));
            transformed = castExpr;
        } else {
            throw new NotYetImplementedException(e.toString());
        }
        return transformed;
    }

    private SootMethodRef transformMethodRef(SootMethodRef toTransform) {
        SootClass declaringClassOfInvokedMethod = decideOnWhetherStillToCreatePartnerClass(toTransform.getDeclaringClass());
        String name = toTransform.getName();
        List<Type> newTransformedParameterTypes = transformTypes(toTransform.getParameterTypes());
        Type newTransformedReturnType = transformType(toTransform.getReturnType());
        boolean isStatic = toTransform.isStatic();
        return new SootMethodRefImpl(
                declaringClassOfInvokedMethod,
                name,
                newTransformedParameterTypes,
                newTransformedReturnType,
                isStatic
        );
    }

    private List<Type> transformTypes(List<Type> toTransform) {
        return toTransform.stream()
                .map(this::transformType)
                .collect(Collectors.toList());
    }

    private final Map<Type, Type> toTransformedType = new HashMap<>();

    private Type transformType(Type toTransform) {
        return transformType(toTransform, true);
    }

    private Type transformType(Type toTransform, boolean arrayToSarray) {
        Type result;
        if ((result = toTransformedType.get(toTransform)) != null) {
            return result;
        }
        if (toTransform instanceof IntType) {
            result = v.TYPE_SINT;
        } else if (toTransform instanceof LongType) {
            result = v.TYPE_SLONG;
        } else if (toTransform instanceof DoubleType) {
            result = v.TYPE_SDOUBLE;
        } else if (toTransform instanceof FloatType) {
            result = v.TYPE_SFLOAT;
        } else if (toTransform instanceof ShortType) {
            result = v.TYPE_SSHORT;
        } else if (toTransform instanceof ByteType) {
            result = v.TYPE_SBYTE;
        } else if (toTransform instanceof BooleanType) {
            result = v.TYPE_SBOOL;
        } else if (toTransform instanceof CharType) {
            result = v.TYPE_SCHAR;
        } else if (toTransform instanceof RefType) {
            RefType refType = (RefType) toTransform;
            if (!refType.getClassName().contains(_TRANSFORMATION_INDICATOR) && !isAlreadyTransformedOrToBeTransformedPath(refType.getClassName())) {
                addToClassesToTransform(refType.getClassName());
            }
            if (refType.getClassName().contains(_TRANSFORMATION_INDICATOR) || isIgnored(getClassForName(refType.getClassName(), classLoader))) {
                result = refType;
            } else {
                decideOnWhetherStillNeedsToBeAddedToTransformationQueue(refType.getClassName());
                result = RefType.v(addTransformationIndicatorToName(refType.getClassName()));
            }
        } else if (toTransform instanceof ArrayType) {
            result = transformArrayType((ArrayType) toTransform, arrayToSarray);
        } else if (toTransform instanceof VoidType) {
            result = toTransform;
        } else {
            throw new NotYetImplementedException(toTransform.toString());
        }
        toTransformedType.put(toTransform, result);
        return result;
    }

    private static Type getInnermostTypeInArray(ArrayType at) {
        Type current = at;
        while (current instanceof ArrayType) {
            current = ((ArrayType) current).baseType;
        }
        return current;
    }


    private static String getNormalBaseTypeNameForType(Type t) {
        assert !(t instanceof ArrayType);
        if (t instanceof IntType) {
            return "I";
        } else if (t instanceof LongType) {
            return "J";
        } else if (t instanceof DoubleType) {
            return "D";
        } else if (t instanceof FloatType) {
            return "F";
        } else if (t instanceof ShortType) {
            return "S";
        } else if (t instanceof ByteType) {
            return "B";
        } else if (t instanceof BooleanType) {
            return "Z";
        } else if (t instanceof CharType) {
            return "C";
        } else {
            assert t instanceof RefType;
            return "L" + ((RefType) t).getClassName() + ";";
        }
    }

    private final Map<String, SootClass> arrayTypeToSpecialPartnerClassSarray = new HashMap<>();
    private final Map<String, String> specializedArrayTypeNameToOriginalTypeName = new HashMap<>();
    private SootClass getSpecificPartnerClassSarrayClassForArrayType(ArrayType at) {
        assert !(at.getElementType() instanceof PrimType) : "We only need to spawn new classes for non-primitive sarrays";
        SootClass result = arrayTypeToSpecialPartnerClassSarray.get(at.toString());
        if (result == null) {
            result = _generateAndLoadSpecificPartnerClassSarrayClassForArrayType(at);
            String normalName = "[".repeat(at.numDimensions) + getNormalBaseTypeNameForType(at.baseType);
            arrayTypeToSpecialPartnerClassSarray.put(normalName, result);
            specializedArrayTypeNameToOriginalTypeName.put(result.getName(), normalName);
        }
        return result;
    }

    private final Collection<SootClass> specificSarraySarrayTypes = new HashSet<>();
    private final Collection<SootClass> specificPartnerClassSarrayTypes = new HashSet<>();
    private SootClass _generateAndLoadSpecificPartnerClassSarrayClassForArrayType(ArrayType at) {
        assert !(at.getElementType() instanceof PrimType) : "We only need to spawn new classes for non-primitive sarrays";
        SootClass result;
        String spawnedClassName;
        SootClass superClass;
        if (at.getElementType() instanceof ArrayType) {
            Type baseType = at.baseType;
            int numberDimensions = at.numDimensions;
            superClass = v.SC_SARRAYSARRAY;
            // As we transform it, it must be a ref type
            spawnedClassName =
                    ((RefType) transformType(baseType, true)).getClassName()
                            + _TRANSFORMATION_INDICATOR
                            + numberDimensions
                            + _SARRAYSARRAY_POSTFIX;
        } else {
            RefType refType = (RefType) at.getElementType();
            superClass = v.SC_PARTNER_CLASSSARRAY;
            spawnedClassName = addTransformationIndicatorToName(refType.getClassName()) + _PARTNER_CLASSSARRAY_POSTFIX;
        }
        result = Scene.v().makeSootClass(spawnedClassName);
        result.setModifiers(Modifier.PUBLIC);
        result.setSuperclass(superClass);
        _addSuitingConstructorsForGeneratedSarraySarray(superClass, result);
        if (at.getElementType() instanceof ArrayType) {
            specificSarraySarrayTypes.add(result);
        } else {
            specificPartnerClassSarrayTypes.add(result);
        }
        return result;
    }

    private static void _addSuitingConstructorsForGeneratedSarraySarray(SootClass superClass, SootClass newClass) {
        boolean isSarraySarray = superClass.equals(v.SC_SARRAYSARRAY);
        SootMethod transform1, transform2, newInstance, copyConstr;
        if (isSarraySarray) {
            transform1 = Scene.v().makeSootMethod(init, List.of(v.TYPE_SARRAY.getArrayType(), v.TYPE_CLASS), v.TYPE_VOID, Modifier.PUBLIC);
            transform2 = Scene.v().makeSootMethod(init, List.of(v.TYPE_SARRAY.getArrayType(), v.TYPE_CLASS, v.TYPE_MULIB_VALUE_TRANSFORMER), v.TYPE_VOID, Modifier.PUBLIC);
            newInstance = Scene.v().makeSootMethod(init, List.of(v.TYPE_SINT, v.TYPE_SE, v.TYPE_BOOL, v.TYPE_CLASS, v.TYPE_SBOOL), v.TYPE_VOID, Modifier.PUBLIC);
            SootMethod multiNewInstance = Scene.v().makeSootMethod(init, List.of(v.TYPE_SINT.getArrayType(), v.TYPE_SE, v.TYPE_CLASS), v.TYPE_VOID, Modifier.PUBLIC);
            newClass.addMethod(multiNewInstance);
            _createBodyOfInitJustCallingSuper(multiNewInstance);
            copyConstr = Scene.v().makeSootMethod(init, List.of(v.TYPE_MULIB_VALUE_COPIER, v.TYPE_SARRAYSARRAY), v.TYPE_VOID, Modifier.PUBLIC);
        } else {
            transform1 = Scene.v().makeSootMethod(init, List.of(v.TYPE_PARTNER_CLASS.getArrayType()), v.TYPE_VOID, Modifier.PUBLIC);
            transform2 = Scene.v().makeSootMethod(init, List.of(v.TYPE_PARTNER_CLASS.getArrayType(), v.TYPE_MULIB_VALUE_TRANSFORMER), v.TYPE_VOID, Modifier.PUBLIC);
            newInstance = Scene.v().makeSootMethod(init, List.of(v.TYPE_CLASS, v.TYPE_SINT, v.TYPE_SE, v.TYPE_BOOL, v.TYPE_SBOOL), v.TYPE_VOID, Modifier.PUBLIC);
            copyConstr = Scene.v().makeSootMethod(init, List.of(v.TYPE_MULIB_VALUE_COPIER, v.TYPE_PARTNER_CLASSSARRAY), v.TYPE_VOID, Modifier.PUBLIC);
        }
        newClass.addMethod(transform1);
        newClass.addMethod(transform2);
        newClass.addMethod(newInstance);
        newClass.addMethod(copyConstr);
        _createBodyOfInitJustCallingSuper(transform1);
        _createBodyOfInitJustCallingSuper(transform2);
        _createBodyOfInitJustCallingSuper(newInstance);
        _createBodyOfInitJustCallingSuper(copyConstr);
        SootMethod copy = Scene.v().makeSootMethod("copy", List.of(v.TYPE_MULIB_VALUE_COPIER), isSarraySarray ? v.TYPE_SARRAYSARRAY : v.TYPE_PARTNER_CLASSSARRAY, Modifier.PUBLIC);
        {
            JimpleBody b = Jimple.v().newBody(copy);
            copy.setActiveBody(b);
            UnitPatchingChain upc = b.getUnits();
            LocalSpawner ls = new LocalSpawner(b);
            Local thisLocal = ls.spawnNewLocal(newClass.getType());
            Local mvcLocal = ls.spawnNewLocal(v.TYPE_MULIB_VALUE_COPIER);
            upc.add(Jimple.v().newIdentityStmt(thisLocal, Jimple.v().newThisRef(newClass.getType())));
            upc.add(Jimple.v().newIdentityStmt(mvcLocal, Jimple.v().newParameterRef(mvcLocal.getType(), 0)));
            Local resultLocal = ls.spawnNewLocal(newClass.getType());
            upc.add(Jimple.v().newAssignStmt(resultLocal, Jimple.v().newNewExpr(newClass.getType())));
            upc.add(Jimple.v().newInvokeStmt(Jimple.v().newSpecialInvokeExpr(resultLocal, copyConstr.makeRef(), mvcLocal, thisLocal)));
            upc.add(Jimple.v().newReturnStmt(resultLocal));
        }
        newClass.addMethod(copy);

        // getOriginalClass is not transformed; - it is not used in the workflow
        // label-method does not need to be transformed; - it is not used in the workflow
    }

    private static void _createBodyOfInitJustCallingSuper(SootMethod sm) {
        JimpleBody b = Jimple.v().newBody(sm);
        sm.setActiveBody(b);
        UnitPatchingChain upc = b.getUnits();
        LocalSpawner ls = new LocalSpawner(b);
        SootClass c = sm.getDeclaringClass();
        SootClass s = c.getSuperclass();
        Local thisLocal = ls.spawnNewLocal(c.getType());
        upc.add(Jimple.v().newIdentityStmt(thisLocal, Jimple.v().newThisRef(c.getType())));
        int localNumber = 0;
        List<Local> paramLocals = new ArrayList<>();
        for (Type t : sm.getParameterTypes()) {
            Local l = ls.spawnNewLocal(t);
            paramLocals.add(l);
            upc.add(Jimple.v().newIdentityStmt(l, Jimple.v().newParameterRef(l.getType(), localNumber++)));
        }
        upc.add(Jimple.v().newInvokeStmt(
                Jimple.v().newSpecialInvokeExpr(thisLocal, s.getMethod(init, sm.getParameterTypes()).makeRef(), paramLocals))
        );
        upc.add(Jimple.v().newReturnVoidStmt());
    }

    private RefLikeType transformArrayType(final ArrayType arrayType, boolean toSarray) {
        RefLikeType result;
        Type t = arrayType.baseType;
        if (arrayType.numDimensions > 1) {
            if (toSarray) {
                result = getSpecificPartnerClassSarrayClassForArrayType(arrayType).getType();
            } else {
                int numDimensions = arrayType.numDimensions;
                Type innermostType = getInnermostTypeInArray(arrayType);
                Type transformedInnermostBaseType = transformType(innermostType);
                return ArrayType.v(transformedInnermostBaseType, numDimensions);
            }
        } else if (!(t instanceof PrimType)) {
            RefType refType = (RefType) t;
            if (refType.getClassName().contains(_TRANSFORMATION_INDICATOR) && !isAlreadyTransformedOrToBeTransformedPath(refType.getClassName())) {
                addToClassesToTransform(refType.getClassName());
            }
            result = toSarray ? getSpecificPartnerClassSarrayClassForArrayType(arrayType).getType() : ArrayType.v(transformType(arrayType.baseType), 1);
        } else if (isIntOrSint(t)) {
            result = toSarray ? v.TYPE_SINTSARRAY : ArrayType.v(v.TYPE_SINT, 1);
        } else if (isLongOrSlong(t)) {
            result = toSarray ? v.TYPE_SLONGSARRAY : ArrayType.v(v.TYPE_SLONG, 1);
        } else if (isDoubleOrSdouble(t)) {
            result = toSarray ? v.TYPE_SDOUBLESARRAY : ArrayType.v(v.TYPE_SDOUBLE, 1);
        } else if (isFloatOrSfloat(t)) {
            result = toSarray ? v.TYPE_SFLOATSARRAY : ArrayType.v(v.TYPE_SFLOAT, 1);
        } else if (isShortOrSshort(t)) {
            result = toSarray ? v.TYPE_SSHORTSARRAY : ArrayType.v(v.TYPE_SSHORT, 1);
        } else if (isByteOrSbyte(t)) {
            result = toSarray ? v.TYPE_SBYTESARRAY : ArrayType.v(v.TYPE_SBYTE, 1);
        } else if (isBoolOrSbool(t)) {
            result = toSarray ? v.TYPE_SBOOLSARRAY : ArrayType.v(v.TYPE_SBOOL, 1);
        } else if (isCharOrSchar(t)) {
            result = toSarray ? v.TYPE_SCHARSARRAY : ArrayType.v(v.TYPE_SCHAR, 1);
        } else {
            throw new NotYetImplementedException();
        }
        return result;
    }

    // If is ignored, return original, if already resolved, return resolved object, otherwise transformEnrichAndValidate
    private SootClass decideOnWhetherStillToCreatePartnerClass(SootClass original) {
        if (isIgnored(getClassForName(original.getName(), classLoader))) {
            return original;
        }
        SootClass result;
        if ((result = resolvedClasses.get(addTransformationIndicatorToName(original.getName()))) != null) {
            return result;
        }
        return transformEnrichAndValidateIfNotSpecialCase(original.getName());
    }

    private SootField transformField(SootField toTransform, SootClass declaringTransformedClass) {
        Type transformedType = transformType(toTransform.getType());
        SootField transformed = new SootField(
                toTransform.getName(),
                transformedType,
                toTransform.getModifiers());
        transformed.setDeclaringClass(declaringTransformedClass);
        return transformed;
    }

    private SootClass transformEnrichAndValidateIfNotSpecialCase(String toTransformName) {
        synchronized (syncObject) {
            SootClass result;
            if ((result = resolvedClasses.get(addTransformationIndicatorToName(toTransformName))) != null) {
                return result;
            }
            if (shouldBeTransformed(toTransformName)) {
                if (!isAlreadyTransformedOrToBeTransformedPath(toTransformName)) {
                    addToClassesToTransform(toTransformName);
                }
                Class<?> toTransform = getClassForName(toTransformName, classLoader);
                toTransform = this.config.TRANSF_REPLACE_TO_BE_TRANSFORMED_CLASS_WITH_SPECIFIED_CLASS.getOrDefault(toTransform, toTransform);
                toTransformName = toTransform.getName();
                assert transformedClassNodes.get(toTransformName) == null;
                result = transformEnrichAndValidate(toTransformName);
                assert result != null;
                assert transformedClassNodes.get(toTransformName) != null : "Setting class in transformedClassNodes failed! Config: " + config;
                return result;
            } else {
                return getClassNodeForName(toTransformName);
            }
        }
    }

    private SootMethodRef constantWrapperMethodRef(Type t) {
        SootMethodRef used;
        if (isIntOrSint(t)) {
            used = v.SM_SINT_CONCSINT.makeRef();
        } else if (isLongOrSlong(t)) {
            used = v.SM_SLONG_CONCSLONG.makeRef();
        } else if (isDoubleOrSdouble(t)) {
            used = v.SM_SDOUBLE_CONCSDOUBLE.makeRef();
        } else if (isFloatOrSfloat(t)) {
            used = v.SM_SFLOAT_CONCSFLOAT.makeRef();
        } else if (isShortOrSshort(t)) {
            used = v.SM_SSHORT_CONCSSHORT.makeRef();
        } else if (isByteOrSbyte(t)) {
            used = v.SM_SBYTE_CONCSBYTE.makeRef();
        } else if (isBoolOrSbool(t)) {
            used = v.SM_SBOOL_CONCSBOOL.makeRef();
        } else if (isCharOrSchar(t)) {
            used = v.SM_SCHAR_CONCSCHAR.makeRef();
        } else {
            throw new NotYetImplementedException(t.toString());
        }
        return used;
    }
}
