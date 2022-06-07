package de.wwu.mulib.transformations.soot_transformations;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.exceptions.MisconfigurationException;
import de.wwu.mulib.exceptions.MulibRuntimeException;
import de.wwu.mulib.exceptions.NotYetImplementedException;
import de.wwu.mulib.substitutions.Sarray;
import de.wwu.mulib.substitutions.primitives.*;
import de.wwu.mulib.transformations.AbstractMulibTransformer;
import de.wwu.mulib.transformations.MulibClassFileWriter;
import de.wwu.mulib.transformations.MulibClassLoader;
import soot.*;
import soot.jimple.*;
import soot.options.Options;
import soot.tagkit.InnerClassTag;
import soot.tagkit.Tag;
import soot.util.Chain;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static de.wwu.mulib.transformations.StringConstants._TRANSFORMATION_PREFIX;
import static de.wwu.mulib.transformations.StringConstants.init;
import static de.wwu.mulib.transformations.TransformationUtility.determineNestHostFieldName;
import static de.wwu.mulib.transformations.TransformationUtility.getClassForName;

public class SootMulibTransformer extends AbstractMulibTransformer<SootClass> {

    private static class TcArgs {
        private final MethodInfoContainer methodInfoContainer;
        private final boolean _isTainted;
        private final boolean _isToWrap;

        private TcArgs(
                MethodInfoContainer methodInfoContainer,
                Unit u) {
            this.methodInfoContainer = methodInfoContainer;
            this._isTainted = methodInfoContainer.taintAnalysis.tainted.contains(u);
            this._isToWrap = methodInfoContainer.taintAnalysis.toWrap.contains(u);
        }

        private Local spawnStackLocal(Type t) {
            return methodInfoContainer.spawnNewStackLocal(t);
        }

        private Local seLocal() {
            return methodInfoContainer.seLocal;
        }

        private Local thisLocal() {
            return methodInfoContainer.thisLocal;
        }

        private TaintAnalysis taintAnalysis() {
            return methodInfoContainer.taintAnalysis;
        }

        private boolean isTainted() {
            return _isTainted;
        }

        private boolean isToWrap() {
            return _isToWrap;
        }

        private boolean isTainted(Value value) {
            return methodInfoContainer.taintAnalysis.taintedValues.contains(value);
        }

        private SootMethod newMethod() {
            return methodInfoContainer.newMethod;
        }

        private void addUnit(Unit u) {
            methodInfoContainer.toAddTo.add(u);
        }

        private JimpleBody newBody() {
            return methodInfoContainer.newBody;
        }

        private JimpleBody oldBody() {
            return methodInfoContainer.oldBody;
        }

        private SootMethod oldMethod() {
            return methodInfoContainer.oldMethod;
        }
    }

    private enum ChosenConstructor {
        SE_CONSTR, COPY_CONSTR, TRANSFORMATION_CONSTR
    }

    private class MulibTransformationInformation {
        /// TODO Enum switch to either store copy-constructor, se-constructor, or transformation-constructor
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
            isInnerNonStatic = !transformed.isStatic() && transformed.isInnerClass();
            shouldBeTransformed = SootMulibTransformer.this.shouldBeTransformed(original.getName().replace(".", "/"));
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
                            List.of(original.getType(), v.TYPE_MULIB_VALUE_TRANSFORMER)
                            :
                            List.of();
            parameterTypesOfCopyConstructor =
                    shouldBeTransformed ?
                            List.of(transformed.getType(), v.TYPE_MULIB_VALUE_TRANSFORMER)
                            :
                            List.of();
        }

        private boolean shouldBeTransformed() {
            return shouldBeTransformed;
        }

        private Local getAdditionalLocal(ChosenConstructor cc, LocalSpawner localSpawner) {
            if (cc == ChosenConstructor.SE_CONSTR) {
                return null;
            } else if (cc == ChosenConstructor.COPY_CONSTR) {
                return localSpawner.spawnNewLocal(transformed.getType());
            } else if (cc == ChosenConstructor.TRANSFORMATION_CONSTR) {
                return localSpawner.spawnNewLocal(original.getType());
            } else {
                throw new NotYetImplementedException();
            }
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
        private final JimpleBody newBody;
        private final UnitPatchingChain toAddTo;
        private final LocalSpawner localSpawner;
        private final Local seLocal;
        private final Local thisLocal;
        private final SootMethod oldMethod;
        private final JimpleBody oldBody;
        private final TaintAnalysis taintAnalysis;

        private MethodInfoContainer(
                final SootMethod newMethod,
                final JimpleBody newBody,
                final UnitPatchingChain toAddTo,
                final LocalSpawner localSpawner,
                final Local seLocal,
                final Local thisLocal,
                final SootMethod oldMethod,
                final JimpleBody oldBody,
                final TaintAnalysis taintAnalysis) {
            this.newMethod = newMethod;
            this.newBody = newBody;
            this.oldMethod = oldMethod;
            this.oldBody = oldBody;
            this.toAddTo = toAddTo;
            this.seLocal = seLocal;
            this.thisLocal = thisLocal;
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

        private LocalSpawner(JimpleBody body) {
            this.body = body;
            Chain<Local> existingLocals = body.getLocals();
            for (Local l : existingLocals) {
                Type t = l.getType();
                if (t instanceof RefLikeType) {
                    nRefLocals++;
                } else if (t instanceof IntType) {
                    nIntLocals++;
                } else if (t instanceof LongType) {
                    nLongLocals++;
                } else if (t instanceof DoubleType) {
                    nDoubleLocals++;
                } else if (t instanceof FloatType) {
                    nFloatLocals++;
                } else if (t instanceof ShortType) {
                    nShortLocals++;
                } else if (t instanceof ByteType) {
                    nByteLocals++;
                } else if (t instanceof BooleanType) {
                    nBoolLocals++;
                } else if (t instanceof VoidType) {
                    throw new MulibRuntimeException("Void type as local variable");
                } else {
                    throw new NotYetImplementedException();
                }
            }
        }

        private Local spawnNewStackLocal(Type t) {
            return _spawnNewLocal("$", t);
        }

        private Local _spawnNewLocal(String prefix, Type t) {
            Local result;
            if (t instanceof RefLikeType) {
                result = Jimple.v().newLocal(prefix + "r" + nRefLocals, t);
                nRefLocals++;
            } else if (t instanceof IntType) {
                result = Jimple.v().newLocal(prefix + "i" + nIntLocals, t);
                nIntLocals++;
            } else if (t instanceof LongType) {
                result = Jimple.v().newLocal(prefix + "j" + nLongLocals, t);
                nLongLocals++;
            } else if (t instanceof DoubleType) {
                result = Jimple.v().newLocal(prefix + "d" + nDoubleLocals, t);
                nDoubleLocals++;
            } else if (t instanceof FloatType) {
                result = Jimple.v().newLocal(prefix + "f" + nFloatLocals, t);
                nFloatLocals++;
            } else if (t instanceof ShortType) {
                result = Jimple.v().newLocal(prefix + "s" + nShortLocals, t);
                nShortLocals++;
            } else if (t instanceof ByteType) {
                result = Jimple.v().newLocal(prefix + "b" + nByteLocals, t);
                nByteLocals++;
            } else if (t instanceof BooleanType) {
                result = Jimple.v().newLocal(prefix + "z" + nBoolLocals, t);
                nBoolLocals++;
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

    private static final String JAVA_CLASS_PATH;
    private static final String DEFAULT_SOOT_JCP;
    static {
        JAVA_CLASS_PATH = System.getProperty("java.class.path").replace("build/resources/test", "build");
        DEFAULT_SOOT_JCP = Scene.defaultJavaClassPath();
    }
    private final SootMulibClassesAndMethods v;

    /**
     * Constructs an instance of MulibTransformer according to the configuration.
     *
     * @param config The configuration.
     */
    public SootMulibTransformer(MulibConfig config) {
        super(config);
        //// TODO Synchronize with synchronization object; since soot works on singletons everywhere
        G.reset();
        Options.v().set_soot_classpath(JAVA_CLASS_PATH + ":" + DEFAULT_SOOT_JCP);
        v = new SootMulibClassesAndMethods();
    }

    @Override
    public MulibClassFileWriter<SootClass> generateMulibClassFileWriter() {
        return new SootClassFileWriter();
    }

    @Override
    protected String getNameToLoadOfClassNode(SootClass classNode) {
        return classNode.getName().replace("/", ".");
    }

    @Override
    protected boolean isInterface(SootClass classNode) {
        return classNode.isInterface();
    }

    private final Map<String, SootClass> resolvedClasses = new HashMap<>();
    @Override
    protected SootClass getClassNodeForName(String name) {
        SootClass c;
        // For some reason Scene.v().loadClass(String,int) cannot resolve properly,
        // this is a workaround. TODO
        if ((c = resolvedClasses.get(name)) != null) {
            return c;
        }
        Class<?> clazz = getClassForName(name);
        c = Scene.v().forceResolve(name, SootClass.BODIES);
        c.setModifiers(clazz.getModifiers());
        resolvedClasses.put(name, c);
        return c;
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
        if (cc == ChosenConstructor.SE_CONSTR) {
           seOrMvtLocal = localSpawner.spawnNewLocal(v.TYPE_SE);
        } else {
            seOrMvtLocal = localSpawner.spawnNewLocal(v.TYPE_MULIB_VALUE_TRANSFORMER);
        }
        // Get unit chain to add instructions to
        UnitPatchingChain upc = b.getUnits();
        // Create identity statement for parameter locals
        upc.add(Jimple.v().newIdentityStmt(thisLocal, Jimple.v().newThisRef(result.getType())));
        int localNumber = 0;
        if (additionalLocal != null) {
            upc.add(Jimple.v().newIdentityStmt(additionalLocal, Jimple.v().newParameterRef(additionalLocal.getType(), localNumber++)));
        }

        if (thisOuterLocal != null && cc == ChosenConstructor.SE_CONSTR) {
            upc.add(Jimple.v().newIdentityStmt(thisOuterLocal, Jimple.v().newParameterRef(resultData.transformedOuterClassType, localNumber++)));
        }

        if (cc == ChosenConstructor.SE_CONSTR) {
            upc.add(Jimple.v().newIdentityStmt(seOrMvtLocal, Jimple.v().newParameterRef(v.TYPE_SE, localNumber++)));
        } else {
            upc.add(Jimple.v().newIdentityStmt(seOrMvtLocal, Jimple.v().newParameterRef(v.TYPE_MULIB_VALUE_TRANSFORMER, localNumber++)));
        }
        // Add super-constructor call
        SootMethodRef refOfInit =
                result.getSuperclass().getMethod(init, superData.getParameterTypesOfConstr(cc)).makeRef();
        SpecialInvokeExpr initExpr =
                Jimple.v().newSpecialInvokeExpr(
                        thisLocal,
                        refOfInit,
                        superData.getParameterValuesOfConstrExceptThis(cc, additionalLocal, thisOuterLocal, seOrMvtLocal)
                );
        InvokeStmt invokeSuperConstructorStmt = Jimple.v().newInvokeStmt(initExpr);
        upc.add(invokeSuperConstructorStmt);

        if (cc == ChosenConstructor.TRANSFORMATION_CONSTR) {
            VirtualInvokeExpr registerCopy =
                    Jimple.v().newVirtualInvokeExpr(seOrMvtLocal, v.SM_MULIB_VALUE_TRANSFORMER_REGISTER_COPY.makeRef(), additionalLocal, thisLocal);
            upc.add(Jimple.v().newInvokeStmt(registerCopy));
        }

        // If this is an inner non-static class, we already set the field of its outer class
        SootField resultOuterClassField = null;
        if (thisOuterLocal != null && cc == ChosenConstructor.SE_CONSTR) {
            String outerClassFieldName = getOuterClassField(old);
            resultOuterClassField = result.getField(outerClassFieldName, result.getOuterClass().getType());
            FieldRef resultOuterClassFieldRef = Jimple.v().newInstanceFieldRef(thisLocal, resultOuterClassField.makeRef());
            upc.add(Jimple.v().newAssignStmt(resultOuterClassFieldRef, thisOuterLocal));
        }

        // For transformation-constructor: We might need reflection to initialize all fields
        boolean reflectionRequired = false;
        if (cc == ChosenConstructor.TRANSFORMATION_CONSTR) {
            for (SootField f : old.getFields()) {
                if (calculateReflectionRequiredForFieldInNonStaticMethod(f.getModifiers())) {
                    reflectionRequired = true;
                    break;
                }
            }
        }
        Local classLocal = null, fieldLocal = null, exceptionLocal = null;
        if (reflectionRequired) {
            classLocal = localSpawner.spawnNewLocal(v.TYPE_CLASS);
            fieldLocal = localSpawner.spawnNewLocal(v.TYPE_FIELD);
            exceptionLocal = localSpawner.spawnNewLocal(v.TYPE_EXCEPTION);
        }

        // Only used for TRANSFORMATION_CONSTR
        Local seLocal = null;
        // For SymbolicExecution-Constructor: make null check
        // This is why we already create the return statement (but do only add it at the end of this
        ReturnVoidStmt returnStmt = Jimple.v().newReturnVoidStmt();
        if (cc == ChosenConstructor.SE_CONSTR) {
            IfStmt nullIfStmt = Jimple.v().newIfStmt(Jimple.v().newEqExpr(seOrMvtLocal, NullConstant.v()), returnStmt);
            upc.add(nullIfStmt);
        } else if (cc == ChosenConstructor.TRANSFORMATION_CONSTR) {
            // We need to additionally add an instance of SymbolicExecution here
            // TODO Enhance constructor signature accordingly instead
            seLocal = localSpawner.spawnNewLocal(v.TYPE_SE);
            AssignStmt assignSeLocal = Jimple.v().newAssignStmt(
                    seLocal,
                    Jimple.v().newStaticInvokeExpr(v.SM_SE_GET.makeRef())
            );
            upc.add(assignSeLocal);
        }

        // Initialize fields
        for (SootField oldField : old.getFields()) {
            SootField transformedField = getTransformedFieldForOldField(oldField, result);
            if (transformedField == resultOuterClassField) {
                // We have already set this
                continue;
            }
            if (transformedField.isStatic()) {
                // We do this in <clinit>
                continue;
            }

            if (cc == ChosenConstructor.SE_CONSTR) {
                initializeFieldViaSymbolicExecution(thisLocal, seOrMvtLocal, localSpawner, transformedField, upc);
            } else if (cc == ChosenConstructor.TRANSFORMATION_CONSTR) {
                initializeFieldViaTransformation(
                        thisLocal, additionalLocal, seOrMvtLocal, seLocal, classLocal, fieldLocal, exceptionLocal,
                        localSpawner, oldField, transformedField, old, upc);
            } else if (cc == ChosenConstructor.COPY_CONSTR) {
                throw new NotYetImplementedException();
            } else {
                throw new NotYetImplementedException(cc.name());
            }
        }

        // For transformation constructor: If reflection is required, we must catch the exceptions
        if (cc == ChosenConstructor.TRANSFORMATION_CONSTR && reflectionRequired) {
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
        upc.add(returnStmt);

        newConstructorOfResult.setDeclaringClass(result);
        result.addMethod(newConstructorOfResult);
    }

    private SootField getTransformedFieldForOldField(SootField oldField, SootClass oldClass) {
        Type originalType = transformType(oldField.getType());
        return oldClass.getField(oldField.getName(), originalType);
    }

    private ClassConstant getWrapperAwareClassConstantForType(Type t) {
        return ClassConstant.v("L" + getClassAwareOfWrapper(t).getName().replace(".", "/") + ";");
    }

    private Type getWrapperAwareTypeForType(Type t) {
        return Scene.v().getRefType(getClassAwareOfWrapper(t).getName().replace("/", "."));
    }

    private Class<?> getClassAwareOfWrapper(Type t) {
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
            } else {
                throw new NotYetImplementedException();
            }
            return clazz;
        } else {
            try {
                return Class.forName(((RefType) t).getClassName());
            } catch (ClassNotFoundException e) {
                throw new MulibRuntimeException("Class not found", e);
            }
        }
    }

    private void initializeFieldViaTransformation(
            Local thisLocal,
            Local originalLocal,
            Local mvtLocal,
            Local seLocal,
            Local classLocal,
            Local fieldLocal,
            Local exceptionLocal,
            LocalSpawner localSpawner,
            SootField originalField,
            SootField transformedField,
            SootClass original,
            UnitPatchingChain upc) {
        assert mvtLocal.getType() == v.TYPE_MULIB_VALUE_TRANSFORMER;
        boolean reflectionRequiredForField = calculateReflectionRequiredForFieldInNonStaticMethod(transformedField.getModifiers());
        Type transformedType = transformedField.getType();
        Type originalType = originalField.getType();
        Local originalValue = localSpawner.spawnNewStackLocal(originalType);
        Local transformedValue = localSpawner.spawnNewStackLocal(transformedType);
        // Assign value to field; we already define it here so that we can jump to it
        AssignStmt assignToField = Jimple.v().newAssignStmt(
                Jimple.v().newInstanceFieldRef(thisLocal, transformedField.makeRef()),
                transformedValue
        );
        /* GET VALUE FROM ORIGINAL FIELD */
        if (reflectionRequiredForField) {
            // If reflection is required to get the value from the original object, we need to first get the
            // field object from the class object.
            ClassConstant classConstant = getWrapperAwareClassConstantForType(originalType);
            AssignStmt initClassVar = Jimple.v().newAssignStmt(
                    classLocal,
                    classConstant
            );
            upc.add(initClassVar);
            AssignStmt initFieldVar = Jimple.v().newAssignStmt(
                    fieldLocal,
                    Jimple.v().newVirtualInvokeExpr(classLocal, v.SM_CLASS_GET_DECLARED_FIELD.makeRef(), StringConstant.v(originalField.getName()))
            );
            upc.add(initFieldVar);
            InvokeStmt setAccessible = Jimple.v().newInvokeStmt(Jimple.v().newVirtualInvokeExpr(
                    fieldLocal, v.SM_FIELD_SET_ACCESSIBLE.makeRef(), IntConstant.v(1)
            ));
            upc.add(setAccessible);
            // Then, we execute Field.get(originalObject) to get the value (if there is a wrapper object involved, we still
            // need to unpack)
            Local tempFieldObjectLocal = localSpawner.spawnNewStackLocal(v.TYPE_OBJECT);
            AssignStmt tempObjectFieldVal = Jimple.v().newAssignStmt(
                    tempFieldObjectLocal,
                    Jimple.v().newVirtualInvokeExpr(fieldLocal, v.SM_FIELD_GET.makeRef(), originalLocal)
            );
            upc.add(tempObjectFieldVal);
            Type typeOfFieldGet = getWrapperAwareTypeForType(originalType);
            // Cast
            Local tempFieldValLocal = localSpawner.spawnNewStackLocal(typeOfFieldGet);
            AssignStmt castedTempFieldVal = Jimple.v().newAssignStmt(
                    tempFieldValLocal,
                    Jimple.v().newCastExpr(tempFieldObjectLocal, typeOfFieldGet)
            );
            upc.add(castedTempFieldVal);
            AssignStmt initFieldVal;
            if (originalType instanceof PrimType) {
                // We still have to unwrap
                SootMethodRef toCall;
                Type toWrap = originalType;
                if (toWrap instanceof IntType) {
                    toCall = v.SM_INTEGER_GETVAL.makeRef();
                } else if (toWrap instanceof LongType) {
                    toCall = v.SM_LONG_GETVAL.makeRef();
                } else if (toWrap instanceof DoubleType) {
                    toCall = v.SM_DOUBLE_GETVAL.makeRef();
                } else if (toWrap instanceof FloatType) {
                    toCall = v.SM_FLOAT_GETVAL.makeRef();
                } else if (toWrap instanceof ShortType) {
                    toCall = v.SM_SHORT_GETVAL.makeRef();
                } else if (toWrap instanceof ByteType) {
                    toCall = v.SM_BYTE_GETVAL.makeRef();
                } else if (toWrap instanceof BooleanType) {
                    toCall = v.SM_BOOLEAN_GETVAL.makeRef();
                } else {
                    throw new NotYetImplementedException(String.valueOf(tempFieldValLocal.getType()));
                }
                initFieldVal = Jimple.v().newAssignStmt(
                        originalValue,
                        Jimple.v().newVirtualInvokeExpr(tempFieldValLocal, toCall)
                );
            } else {
                initFieldVal = Jimple.v().newAssignStmt(
                        originalValue,
                        tempFieldValLocal
                );
            }


            upc.add(initFieldVal);
        } else {
            // If we do not use reflection, we can simply execute a get
            AssignStmt getField = Jimple.v().newAssignStmt(
                    originalValue,
                    Jimple.v().newInstanceFieldRef(originalLocal, originalField.makeRef())
            );
            upc.add(getField);
        }

        /* WRAP THE FIELD VALUE OR CREATE A NEW OBJECT FROM IT */
        if (isPrimitiveOrSprimitive(transformedType)) {
            // If the value is primitive, we can just wrap it
            SootMethodRef wrapper = constantWrapperMethodRef(transformedType);
            AssignStmt wrapConstant = Jimple.v().newAssignStmt(
                    transformedValue,
                    Jimple.v().newVirtualInvokeExpr(seLocal, wrapper, originalValue)
            );
            upc.add(wrapConstant);
        } else if (isSarray(transformedType)) {
            throw new NotYetImplementedException();
        } else {
            // Is partner class
            // Check if originalValue == null
            // TODO also check if should be transformed
            AssignStmt assignNull = Jimple.v().newAssignStmt(transformedValue, NullConstant.v());
            IfStmt nullCheck = Jimple.v().newIfStmt(Jimple.v().newEqExpr(originalValue, NullConstant.v()), assignNull);
            // Check if retrieved original value is null (we add assignNull at the end)
            upc.add(nullCheck);

            // If the value is not null, we calculate whether the value has already been created
            VirtualInvokeExpr callAlreadyCreated =
                    Jimple.v().newVirtualInvokeExpr(mvtLocal, v.SM_MULIB_VALUE_TRANSFORMER_ALREADY_CREATED.makeRef(), originalValue);
            Local stackAlreadyCreated = localSpawner.spawnNewStackLocal(v.TYPE_BOOL); // TODO or int?
            AssignStmt computeIfAlreadyCreated =
                    Jimple.v().newAssignStmt(stackAlreadyCreated, callAlreadyCreated);
            upc.add(computeIfAlreadyCreated);
            ConditionExpr wasAlreadyCreatedExpr = Jimple.v().newEqExpr(stackAlreadyCreated, IntConstant.v(1)); // Is true?
            // If the object was already created, we jump to get the copy from the value transformer
            VirtualInvokeExpr getCopy =
                    Jimple.v().newVirtualInvokeExpr(mvtLocal, v.SM_MULIB_VALUE_TRANSFORMER_GET_COPY.makeRef(), originalLocal);
            Local stackLocalOfAlreadyCreatedObject = localSpawner.spawnNewStackLocal(v.TYPE_OBJECT);
            AssignStmt assignCopy = Jimple.v().newAssignStmt(stackLocalOfAlreadyCreatedObject, getCopy);
            CastExpr castedToExpr = Jimple.v().newCastExpr(stackLocalOfAlreadyCreatedObject, transformedType);
            AssignStmt assignCastedCopy = Jimple.v().newAssignStmt(transformedValue, castedToExpr);
            IfStmt alreadyCreatedCheck =
                    Jimple.v().newIfStmt(wasAlreadyCreatedExpr, assignCopy);
            // Again, we need to add all statements for assigning the copy after treating the false-case
            upc.add(alreadyCreatedCheck);

            // If there was no copy, we initialize a new object using the constructor
            Local stackLocalOfNewObject =
                    createStmtsForConstructorCall(
                            (RefType) transformedType,
                            localSpawner,
                            upc,
                            List.of(originalLocal.getType(), v.TYPE_MULIB_VALUE_TRANSFORMER),
                            List.of(originalLocal, mvtLocal)
                    );
            upc.add(Jimple.v().newAssignStmt(transformedValue, stackLocalOfNewObject));
            upc.add(Jimple.v().newGotoStmt(assignToField));

            upc.add(assignCopy);
            upc.add(assignCastedCopy);
            upc.add(Jimple.v().newGotoStmt(assignToField));

            // If the retrieved original value is null, we assign null
            upc.add(assignNull);
        }

        upc.add(assignToField);
    }

    private void initializeFieldViaSymbolicExecution(
            Local thisLocal,
            Local seLocal,
            LocalSpawner localSpawner,
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
            Local stackLocalForNew =
                    createStmtsForConstructorCall((RefType) t, localSpawner, upc, List.of(v.TYPE_SE), List.of(seLocal));
            AssignStmt assignToField = Jimple.v().newAssignStmt(fieldRef, stackLocalForNew);
            upc.add(assignToField);
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
            List<Value> constructorArguments) { // TODO make lazy
        String className = transformedType.getClassName();
        String originalClassName = className.replace(_TRANSFORMATION_PREFIX, "");
        Local stackLocalForNew = localSpawner.spawnNewStackLocal(transformedType);
        NewExpr newExpr = Jimple.v().newNewExpr(transformedType);
        AssignStmt assignNewToStackLocal = Jimple.v().newAssignStmt(stackLocalForNew, newExpr);
        SootClass sootClassToInitialize = transformEnrichAndValidateIfNotSpecialCase(originalClassName);
        SootMethodRef invokeSpecialRef;
        SpecialInvokeExpr invokeConstructorExpr;
        if (shouldBeTransformed(originalClassName.replace(".", "/"))) {
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
//        throw new NotYetImplementedException(); //// TODO
    }

    @Override
    protected void generateAndAddCopyMethod(SootClass old, SootClass result) {
//        throw new NotYetImplementedException(); //// TODO
    }

    @Override
    protected void generateAndAddLabelTypeMethod(SootClass old, SootClass result) {
//        throw new NotYetImplementedException(); //// TODO
    }

    @Override
    protected void generateAndAddOriginalClassMethod(SootClass old, SootClass result) {
        // Create method
        SootMethod originalClassMethod = new SootMethod("getOriginalClass", List.of(), v.TYPE_CLASS, Modifier.PUBLIC);
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
        // Return class constant
        upc.add(Jimple.v().newReturnStmt(ClassConstant.fromType(old.getType())));
        originalClassMethod.setDeclaringClass(result);
        result.addMethod(originalClassMethod);
    }

    @Override
    protected void generateOrEnhanceClinit(SootClass old, SootClass result) {
//        throw new NotYetImplementedException(); //// TODO
    }

    @Override
    protected void ensureInitializedLibraryTypeFieldsInConstructors(SootClass result) {
//        throw new NotYetImplementedException(); //// TODO
    }

    @Override
    protected MulibClassLoader<SootClass> generateMulibClassLoader() {
        return new SootClassLoader(this);
    }


    public static String addPrefixToPath(String addTo) {
        return _addPrefix(false, addTo);
    }

    public static String addPrefixToName(String addTo) {
        return _addPrefix(true, addTo);
    }

    private static String _addPrefix(boolean useDot, String addTo) {
        if (addTo == null) {
            return null;
        }
        int actualNameIndex = addTo.lastIndexOf(useDot ? '.' : '/') + 1;
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

    @Override
    protected SootClass transformClassNode(SootClass toTransform) {
        /// TODO implements partnerclass
        // Create new SootClass with the transformation prefix
        SootClass result = new SootClass(addPrefixToName(toTransform.getName()));
        // Set modifiers
        result.setModifiers(toTransform.getModifiers());
        // Set super class and interfaces
        SootClass sootSuperClass = transformEnrichAndValidateIfNotSpecialCase(toTransform.getSuperclass().getName());
        // The class is already added and added to the set of resolved classes. This is done to ensure that
        // we can use it later on
        Scene.v().addClass(result);
        resolvedClasses.put(result.getName(), result);
        result.setSuperclass(sootSuperClass);
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
                String outerClassName = innerClassTag.getOuterClass().replace("/", ".");
                if (!toTransform.getName().equals(outerClassName)) {
                    continue;
                }
                transformEnrichAndValidateIfNotSpecialCase(innerClassName);
                result.addTag(
                        new InnerClassTag(
                                addPrefixToPath(innerClassTag.getInnerClass()),
                                addPrefixToPath(innerClassTag.getOuterClass()),
                                addPrefixToName(innerClassTag.getName()),
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
            SootMethod transformedMethod = transformMethod(m, result);
            result.addMethod(transformedMethod);
        }

        return result;
    }

    private SootMethod transformMethod(final SootMethod toTransform, final SootClass declaringTransformedClass) {
        // Replace parameter types and return types
        List<Type> transformedParameterTypes = transformTypes(toTransform.getParameterTypes());
        Type transformedReturnType = transformType(toTransform.getReturnType());
        SootMethod result = new SootMethod(
                toTransform.getName(),
                transformedParameterTypes,
                transformedReturnType,
                toTransform.getModifiers()
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

        // Replace types of exceptions
//        b.getTraps(); TODO
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
                transformedBody,
                upc,
                ls,
                seLocal,
                result.isStatic() ? null : toTransformBody.getThisLocal(),
                toTransform,
                toTransformBody,
                a
        );
        StaticInvokeExpr seGetExpr = Jimple.v().newStaticInvokeExpr(v.SM_SE_GET.makeRef());
        AssignStmt seAssign = Jimple.v().newAssignStmt(seLocal, seGetExpr);
        boolean firstNonIdentityStatement = true;
        UnitPatchingChain toTransformChain = toTransformBody.getUnits();
        for (Unit u : toTransformChain) {

            if (!(u instanceof IdentityStmt) && firstNonIdentityStatement) {
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
        toTransform.setSource(a.originalMethodSource);
        toTransform.releaseActiveBody(); //// TODO Synchronize on toTransform
        return result;
    }

    /* TODO isXOrSX methods are a workaround until we decide how to deal with mutability in Soot. Perhaps transform local etc. later on */

    private static boolean isSarray(Type t) {
        return isSarraySarray(t) || isPartnerClassSarray(t) || isPrimitiveSarray(t);
    }

    private static boolean isSarraySarray(Type t) {
        return (t instanceof RefType) && ((RefType) t).getClassName().equals(Sarray.SarraySarray.class.getName());
    }

    private static boolean isPartnerClassSarray(Type t) {
        return (t instanceof RefType) && ((RefType) t).getClassName().equals(Sarray.PartnerClassSarray.class.getName());
    }

    private static boolean isPrimitiveSarray(Type t) {
        return isSintSarray(t) || isSlongSarray(t) || isSdoubleSarray(t) || isSfloatSarray(t)
                || isSshortSarray(t) || isSbyteSarray(t) || isSboolSarray(t);
    }

    private static boolean isSintSarray(Type t) {
        return (t instanceof RefType) && ((RefType) t).getClassName().equals(Sint.class.getName());
    }

    private static boolean isSlongSarray(Type t) {
        return (t instanceof RefType) && ((RefType) t).getClassName().equals(Slong.class.getName());
    }

    private static boolean isSdoubleSarray(Type t) {
        return (t instanceof RefType) && ((RefType) t).getClassName().equals(Sdouble.class.getName());
    }

    private static boolean isSfloatSarray(Type t) {
        return (t instanceof RefType) && ((RefType) t).getClassName().equals(Sfloat.class.getName());
    }

    private static boolean isSshortSarray(Type t) {
        return (t instanceof RefType) && ((RefType) t).getClassName().equals(Sshort.class.getName());
    }

    private static boolean isSbyteSarray(Type t) {
        return (t instanceof RefType) && ((RefType) t).getClassName().equals(Sbyte.class.getName());
    }

    private static boolean isSboolSarray(Type t) {
        return (t instanceof RefType) && ((RefType) t).getClassName().equals(Sbool.class.getName());
    }

    private static boolean isActualPrimitive(Type t) {
        return t instanceof IntType || t instanceof LongType || t instanceof DoubleType || t instanceof FloatType
                || t instanceof ShortType || t instanceof ByteType || t instanceof BooleanType;
    }

    private static boolean isPrimitiveOrSprimitive(Type t) {
        return isIntOrSintSubtype(t) || isLongOrSlong(t) || isDoubleOrSdouble(t) || isFloatOrSfloat(t) || isBoolOrSbool(t);
    }

    private static boolean isLongOrSlong(Type t) {
        return t instanceof LongType
                || ((t instanceof RefType) && ((RefType) t).getClassName().equals(Slong.class.getName()));
    }

    private static boolean isIntOrSintSubtype(Type t) {
        return isIntOrSint(t) || isShortOrSshort(t) || isByteOrSbyte(t);
    }
    private static boolean isIntOrSint(Type t) {
        return t instanceof IntType
                || ((t instanceof RefType) && ((RefType) t).getClassName().equals(Sint.class.getName()));
    }

    private static boolean isDoubleOrSdouble(Type t) {
        return t instanceof DoubleType
                || ((t instanceof RefType) && ((RefType) t).getClassName().equals(Sdouble.class.getName()));
    }

    private static boolean isFloatOrSfloat(Type t) {
        return t instanceof FloatType
                || ((t instanceof RefType) && ((RefType) t).getClassName().equals(Sfloat.class.getName()));
    }

    private static boolean isShortOrSshort(Type t) {
        return t instanceof ShortType
                || ((t instanceof RefType) && ((RefType) t).getClassName().equals(Sshort.class.getName()));
    }

    private static boolean isByteOrSbyte(Type t) {
        return t instanceof ByteType
                || ((t instanceof RefType) && ((RefType) t).getClassName().equals(Sbyte.class.getName()));
    }

    private static boolean isBoolOrSbool(Type t) {
        return t instanceof BooleanType
                || ((t instanceof RefType) && ((RefType) t).getClassName().equals(Sbool.class.getName()));
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
        if (args.isToWrap()) {
            // Find method to wrap with
            SootMethodRef used = constantWrapperMethodRef(args.newMethod().getReturnType()); // TODO class and string returns...
            // Create virtual call
            VirtualInvokeExpr virutalInvokeExpr = Jimple.v().newVirtualInvokeExpr(args.seLocal(), used, op);
            createStackLocalAssignExprRedirectAndAdd(virutalInvokeExpr, null, r, r.getOpBox(), args);
        }
        args.addUnit(r);
    }

    private void transform(SwitchStmt s, TcArgs args) {
        if (!args.isTainted() && !args.isToWrap()) {
            args.addUnit(s);
            return;
        }
        // TODO args.isTainted and args.isWrapped
        if (s instanceof TableSwitchStmt) {
            throw new NotYetImplementedException();
//            args.addUnit(s);
        } else if (s instanceof LookupSwitchStmt) {
            throw new NotYetImplementedException();
//            args.addUnit(s);
        } else {
            throw new NotYetImplementedException();
        }
    }

    private void transform(BreakpointStmt b, TcArgs args) {
        assert !args.isToWrap() && !args.isTainted();
        args.addUnit(b);
    }

    private void transform(ReturnVoidStmt r, TcArgs args) {
        assert !args.isTainted() && !args.isToWrap();
        args.addUnit(r);
    }

    private InvokeExpr getInvokeExprIfIndicatorMethodExprElseNull(InvokeExpr invokeExpr, TcArgs args) {
        String methodName = invokeExpr.getMethodRef().getName();
        InvokeExpr result = null;
        if (v.isIndicatorMethodName(methodName)) {
            SootMethod frameworkMethod = v.getTransformedMethodForIndicatorMethodName(methodName);
            if (methodName.startsWith("named")) {
                result = Jimple.v().newVirtualInvokeExpr(args.seLocal(), frameworkMethod.makeRef(), invokeExpr.getArgs());
            } else {
                result = Jimple.v().newVirtualInvokeExpr(args.seLocal(), frameworkMethod.makeRef());
            }
        }
        return result;
    }

    private void transform(InvokeStmt invoke, TcArgs args) {
        // If this InvokeStmt would produce a result, the InvokeExpr would be part of an AssignStmt
        assert !args.isToWrap();
        InvokeExpr possiblyTransformedIndicatorMethod = getInvokeExprIfIndicatorMethodExprElseNull(invoke.getInvokeExpr(), args);
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
        assert !args.isToWrap(); //// TODO validate in TaintAnalyzer
        args.addUnit(r);
    }

    private void transform(IfStmt i, TcArgs args) {
        assert !args.isToWrap();
        if (!args.isTainted()) {
            args.addUnit(i);
            return;
        }
        // Since is tainted, one type must be Sbool
        ValueBox conditionBox = i.getConditionBox();
        ConditionExpr conditionExpr = (ConditionExpr) conditionBox.getValue();
        ValueBox lhsConditionExprBox = conditionExpr.getOp1Box();
        ValueBox rhsConditionExprBox = conditionExpr.getOp2Box();
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
                comparisonWithZero = ((IntConstant) rhsCondition).value == 0;
            } else {
                assert lhsCondition instanceof IntConstant;
                assert ((IntConstant) lhsCondition).value == 0 || ((IntConstant) lhsCondition).value == 1;
                comparisonWithZero = ((IntConstant) rhsCondition).value == 0;
            }
            // Invert comparisonWithZero if condition is NeExpr
            comparisonWithZero = (conditionExpr instanceof NeExpr) != comparisonWithZero;
            used = comparisonWithZero ? v.SM_SBOOL_NEGATED_BOOL_CHOICE_S.makeRef() : v.SM_SBOOL_BOOL_CHOICE_S.makeRef();
            // Must be Local since arg boxes used in J{Ne, Eq, ...}Expr only allow for subtypes of Immediate
            virtualInvokeExpr = Jimple.v().newVirtualInvokeExpr((Local) (lhsIsBool ? lhsCondition : rhsCondition), used, args.seLocal());
        } else if (bothBool) {
            assert conditionExpr instanceof NeExpr || conditionExpr instanceof EqExpr;
            // We choose the method comparing two Sbools if both are Sbools
            used = conditionExpr instanceof EqExpr ? v.SM_SBOOL_BOOL_CHOICE.makeRef() : v.SM_SBOOL_NEGATED_BOOL_CHOICE.makeRef();
            virtualInvokeExpr = Jimple.v().newVirtualInvokeExpr((Local) lhsCondition, used, rhsCondition, args.seLocal());
        } else {
            // Two int numbers are compared
            // Wrap constants if necessary
            if (!args.isTainted(lhsCondition)) {
                WrapPair wp = wrap(args, lhsConditionExprBox);
                lhsCondition = wp.newValue;
                firstStatement = wp.newFirstStmt;
            }
            if (!args.isTainted(rhsCondition)) {
                WrapPair wp = wrap(args, rhsConditionExprBox);
                rhsCondition = wp.newValue;
                firstStatement = firstStatement == null ? wp.newFirstStmt : firstStatement;
            }
            if (conditionExpr instanceof NeExpr) {
                used = v.SM_SINT_NOT_EQ_CHOICE.makeRef();
            } else if (conditionExpr instanceof EqExpr) {
                used = v.SM_SINT_EQ_CHOICE.makeRef();
            } else if (conditionExpr instanceof GeExpr) {
                used = v.SM_SINT_GTE_CHOICE.makeRef();
            } else if (conditionExpr instanceof GtExpr) {
                used = v.SM_SINT_GT_CHOICE.makeRef();
            } else if (conditionExpr instanceof LeExpr) {
                used = v.SM_SINT_LTE_CHOICE.makeRef();
            } else if (conditionExpr instanceof LtExpr) {
                used = v.SM_SINT_LT_CHOICE.makeRef();
            } else {
                throw new NotYetImplementedException(conditionExpr.toString());
            }
            virtualInvokeExpr = Jimple.v().newVirtualInvokeExpr((Local) lhsCondition, used, rhsCondition, args.seLocal());
        }
        createStackLocalAssignExprRedirectAndAdd(
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

    private WrapPair wrap(TcArgs args, ValueBox toWrapBox) {
        Value toWrap = toWrapBox.getValue();
        SootMethodRef wrapLhs = constantWrapperMethodRef(toWrap.getType());
        VirtualInvokeExpr wrapLhsExpr = Jimple.v().newVirtualInvokeExpr(args.seLocal(), wrapLhs, toWrap);
        Local lhsWrapLocal = args.spawnStackLocal(toWrap.getType());
        AssignStmt assignLhs = Jimple.v().newAssignStmt(lhsWrapLocal, wrapLhsExpr);
        args.addUnit(assignLhs);
        toWrapBox.setValue(lhsWrapLocal);
        return new WrapPair(lhsWrapLocal, assignLhs);
    }

    private void transform(AssignStmt a, TcArgs args) {
        // Special treatment of method calls
        if (!args.isTainted() && a.containsInvokeExpr()) {
            adjustSignatureIfNeeded(a, a.getInvokeExpr(), args);
            if (args.isToWrap()) {
                throw new NotYetImplementedException(); /// TODO
            }
            args.addUnit(a);
            return;
        }
        if (!args.isToWrap() && !args.isTainted()) {
            args.addUnit(a);
            return;
        }
        assert args.taintAnalysis().taintedValues.contains(a.getLeftOp());
        ValueBox variableBox = a.getLeftOpBox();
        Value var = variableBox.getValue();
        ValueBox valueBox = a.getRightOpBox();
        Value value = valueBox.getValue();
        assert var instanceof Local || var instanceof Ref;
        assert ((args.isTainted() && args.isTainted(value))
                || (args.isTainted() && a.containsInvokeExpr() && v.getTransformedMethodForIndicatorMethodName(a.getInvokeExpr().getMethodRef().getName()) != null))
                || args.isToWrap();
        if (args.isTainted()) {
            if (value instanceof Constant) {
                // Find method to wrap with
                SootMethodRef used = constantWrapperMethodRef(var.getType());
                // Create virtual call
                VirtualInvokeExpr virtualInvokeExpr = Jimple.v().newVirtualInvokeExpr(args.seLocal(), used, var);
                createStackLocalAssignExprRedirectAndAdd(virtualInvokeExpr, null, a, valueBox, args);
            } else if (value instanceof Expr) {
                if (value instanceof InstanceOfExpr) {
                    InstanceOfExpr instanceOfExpr = (InstanceOfExpr) value;
                    Type checkIfType = instanceOfExpr.getCheckType();
                    ValueBox toCheckBox = instanceOfExpr.getOpBox();
                    Value toCheck = toCheckBox.getValue();
                    VirtualInvokeExpr virtualInvokeExpr =
                            Jimple.v().newVirtualInvokeExpr(args.seLocal(), v.SM_SE_INSTANCEOF.makeRef(), toCheck, ClassConstant.fromType(checkIfType));
                    createStackLocalAssignExprRedirectAndAdd(virtualInvokeExpr, null, a, valueBox, args);
                } else if (value instanceof NewExpr) {
                    // Nothing to do
                } else if (value instanceof NewArrayExpr) {
                    // Nothing to do for now, TODO free arrays
                } else if (value instanceof NewMultiArrayExpr) {
                    // Nothing to do for now, TODO free arrays
                } else if (value instanceof UnopExpr) {
                    Type t = ((UnopExpr) value).getOp().getType();
                    SootMethodRef used;
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
                    } else if (value instanceof LengthExpr) {
                        // TODO free arrays
                        throw new NotYetImplementedException(value.toString());
                    } else {
                        throw new NotYetImplementedException(value.toString());
                    }
                    // Create virtual call
                    VirtualInvokeExpr virtualInvokeExpr = Jimple.v().newVirtualInvokeExpr((Local) ((NegExpr) value).getOp(), used, args.seLocal());
                    createStackLocalAssignExprRedirectAndAdd(virtualInvokeExpr, null, a, valueBox, args);
                } else if (value instanceof BinopExpr) {
                    BinopExpr b = (BinopExpr) value;
                    // In Java, we have Binops between two of the same types
                    // TODO still, this is a workaround, as for b.getType() "unknown" is returned, since Sbool etc. are not known to Soot
                    Type t = b.getOp1().getType();
                    SootMethodRef used;
                    if (b instanceof OrExpr) {
                        throw new NotYetImplementedException();
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
                        throw new NotYetImplementedException();
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
                        if (isIntOrSint(t)) {
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
                        throw new NotYetImplementedException();
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
                        throw new NotYetImplementedException();
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
                        throw new NotYetImplementedException();
                    } else if (b instanceof ShrExpr) {
                        throw new NotYetImplementedException();
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
                    createStackLocalAssignExprRedirectAndAdd(virtualInvokeExpr, firstStatement, a, valueBox, args);
                } else if (value instanceof InvokeExpr) {
                    InvokeExpr invokeExpr = (InvokeExpr) value;
                    InvokeExpr possiblyTransformedIndicatorMethod = getInvokeExprIfIndicatorMethodExprElseNull(invokeExpr, args);
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
                    if (isIntOrSint(typeToCast)) {
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
                        } else if (isIntOrSint(castTo)) { // TODO Also see CMP, a byte is generated there...
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
                        // Soot for some reason sometimes inserts statements like int i = (int) <bytevalue>
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
                        if (!args.isTainted(op)) {
                            WrapPair wp = wrap(args, opBox);
                            op = wp.newValue;
                            firstStatement = wp.newFirstStmt;
                        }
                        // Create virtual call
                        // op must be Local, since JCastExpr uses instance of ImmediateBox
                        virtualInvokeExpr = Jimple.v().newVirtualInvokeExpr((Local) op, used, args.seLocal());
                    }
                    createStackLocalAssignExprRedirectAndAdd(virtualInvokeExpr, firstStatement, a, valueBox, args);
                }
            } else if (value instanceof Ref) {
                if (value instanceof IdentityRef || value instanceof FieldRef) {
                    // Nothing to do
                } else {
                    // ArrayRef
                    throw new NotYetImplementedException(value.toString());
                }
            } else if (value instanceof Local) {
                // Nothing to do
            }
        }
        var = a.getLeftOp();
        // If unit is to be wrapped, call respective method
        if (args.isToWrap()) {
            // Only primitives can be wrapped, arrays and objects should not occur here
            assert var.getType() instanceof IntType || var.getType() instanceof LongType
                    || var.getType() instanceof DoubleType || var.getType() instanceof ShortType
                    || var.getType() instanceof ByteType || var.getType() instanceof BooleanType;
            SootMethodRef smr = constantWrapperMethodRef(var.getType());
            VirtualInvokeExpr invokeExpr = Jimple.v().newVirtualInvokeExpr(args.seLocal(), smr, value);
            createStackLocalAssignExprRedirectAndAdd(invokeExpr, null, a, a.getLeftOpBox(), args);
            return;
        }
        args.addUnit(a);
    }

    private void adjustSignatureIfNeeded(Stmt originalStmt, InvokeExpr invokeExpr, TcArgs args) {
        if (invokeExpr.getArgCount() == 0) {
            return;
        }
        SootMethodRef smr = invokeExpr.getMethodRef();
        List<SootMethod> alternativeSootMethods =
                smr.getDeclaringClass().getMethods()
                        .stream()
                        .filter(sm -> sm.getName().equals(smr.getName()) && sm.getParameterCount() == smr.getParameterTypes().size())
                        .collect(Collectors.toList());
        boolean shouldBeReplaced = false;
        List<Type> parameterTypes = smr.getParameterTypes();
        if (args.taintAnalysis().generalizeSignature.contains(originalStmt)) {
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
                    throw new MisconfigurationException("There are no valid methods to generalize to for " + smr);
                } else {
                    // Pick any
                    invokeExpr.setMethodRef(alternativeSootMethods.get(0).makeRef());
                }
            }
        } else if (args.taintAnalysis().concretizeInputs.contains(originalStmt)) {
            throw new NotYetImplementedException(); /// TODO
        }
    }

    private void wrapInvokeExprArgsIfNeededRedirectAndAdd(Stmt redirectJumpsToFrom, InvokeExpr invokeExpr, TcArgs args) {
        boolean firstWrappingStatement = true;
        SootMethodRef calledMethod = invokeExpr.getMethodRef();
        // Check if inputs must be wrapped
        for (int i = 0; i < invokeExpr.getArgCount(); i++) {
            ValueBox vb = invokeExpr.getArgBox(i);
            Value v = vb.getValue();
            if (!args.isTainted(v)) {
                // Wrap input
                // Get method ref for wrapping
                SootMethodRef smr = constantWrapperMethodRef(calledMethod.getParameterType(i));
                // Call method ref (e.g. SymbolicExecution.concSint(int, SymbolicExecution))
                VirtualInvokeExpr wrappingExpr = Jimple.v().newVirtualInvokeExpr(args.seLocal(), smr, v);
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

    private Value createStackLocalAssignExprRedirectAndAdd(
            Expr expressionToCreateStackLocalFor,
            Stmt firstStatement,
            Unit redirectJumpsFrom,
            ValueBox setStackLocalAsValue,
            Function<Local, Value> transformStackLocal,
            TcArgs args) {
        // Create new stack local to store result in
        Local stackLocal = args.spawnStackLocal(expressionToCreateStackLocalFor.getType());
        // Assign expression to new stack local
        AssignStmt assignStmt = Jimple.v().newAssignStmt(stackLocal, expressionToCreateStackLocalFor);
        firstStatement = firstStatement == null ? assignStmt : firstStatement;
        // The new assigned value is now the stack local
        setStackLocalAsValue.setValue(transformStackLocal.apply(stackLocal));
        // Redirect jumps
        redirectJumpsFrom.redirectJumpsToThisTo(firstStatement);
        // Add assign
        args.addUnit(assignStmt);
        return stackLocal;
    }

    private Local createStackLocalAssignExprRedirectAndAdd(
            Expr expressionToCreateStackLocalFor,
            Stmt firstStatement,
            Unit redirectJumpsFrom,
            ValueBox setStackLocalAsValue,
            TcArgs args) {
        return (Local) createStackLocalAssignExprRedirectAndAdd(
                expressionToCreateStackLocalFor,
                firstStatement,
                redirectJumpsFrom,
                setStackLocalAsValue,
                l -> l, // Just use identity
                args
        );
    }


    private final Set<Value> transformedValues = new HashSet<>();
    private Value transformValue(Value toTransform, TaintAnalysis a) {
        if (!a.taintedValues.contains(toTransform)) {
            return toTransform;
        }
        // Since we never replace ValueBoxes, useBoxes does not have to be adapted throughout here
        if (transformedValues.contains(toTransform)) {
            // ValueBox containing this Value has already been regarded
            return toTransform;
        }
        Value transformed;
        if (toTransform instanceof Constant) {
            transformed = transformConstant((Constant) toTransform, a);
        } else if (toTransform instanceof Expr) {
            transformed = transformExpr((Expr) toTransform, a);
        } else if (toTransform instanceof Local) {
            transformed = transformLocal((Local) toTransform);
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

    private Value transformLocal(Local l) {
        l.setType(transformType(l.getType()));
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
            if (shouldBeTransformed(newValue)) {
                newValue = addPrefixToPath(newValue);
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
            instanceOfExpr.setCheckType(transformType(instanceOfExpr.getType()));
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
            newArrayExpr.setBaseType(transformType(newArrayExpr.getBaseType()));
            transformed = newArrayExpr;
        } else if (e instanceof NewMultiArrayExpr) {
            NewMultiArrayExpr newMultiArrayExpr = (NewMultiArrayExpr) e;
            for (int i = 0; i < newMultiArrayExpr.getSizeCount(); i++) {
                newMultiArrayExpr.setSize(i, transformValue(newMultiArrayExpr.getSize(i), a));
            }
            newMultiArrayExpr.setBaseType((ArrayType) transformType(newMultiArrayExpr.getBaseType()));
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
        SootMethodRef invokedRef = new SootMethodRefImpl(
                declaringClassOfInvokedMethod,
                name,
                newTransformedParameterTypes,
                newTransformedReturnType,
                isStatic
        );
        return invokedRef;
    }

    private List<Type> transformTypes(List<Type> toTransform) {
        List<Type> transformedParameterTypes = toTransform.stream()
                .map(this::transformType)
                .collect(Collectors.toList());
        return transformedParameterTypes;
    }

    private final Map<Type, Type> toTransformedType = new HashMap<>();
    private Type transformType(Type toTransform) {
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
        } else if (toTransform instanceof RefType) {
            RefType refType = (RefType) toTransform;
            if (isIgnored(getClassForName(refType.getClassName()))) {
                result = refType;
            } else {
                decideOnWhetherStillNeedsToBeAddedToTransformationQueue(refType.getClassName());
                result = RefType.v(addPrefixToName(refType.getClassName()));
            }
        } else if (toTransform instanceof ArrayType) {
            ArrayType arrayType = (ArrayType) toTransform;
            result = ArrayType.v(transformType(arrayType.baseType), arrayType.numDimensions);
        } else if (toTransform instanceof VoidType) {
            result = toTransform;
        } else {
            throw new NotYetImplementedException(toTransform.toString());
        }
        toTransformedType.put(toTransform, result);
        return result;
    }

    // If is ignored, return original, if already resolved, return resolved object, otherwise transformEnrichAndValidate
    private SootClass decideOnWhetherStillToCreatePartnerClass(SootClass original) {
        if (isIgnored(getClassForName(original.getName()))) {
            return original;
        }
        SootClass result;
        if ((result = resolvedClasses.get(addPrefixToName(original.getName()))) != null) {
            return result;
        }
        return transformEnrichAndValidateIfNotSpecialCase(original.getName());
    }

    private SootField transformField(SootField toTransform, SootClass declaringTransformedClass) {
        SootField transformed = new SootField(
                toTransform.getName(),
                transformType(toTransform.getType()),
                toTransform.getModifiers());
        transformed.setDeclaringClass(declaringTransformedClass);
        return transformed;
    }

    private void decideOnWhetherStillNeedsToBeAddedToTransformationQueue(String name) {
        if (shouldBeTransformed(name) && isAlreadyTransformedOrToBeTransformedPath(name)) {
            decideOnAddToClassesToTransform(name);
        }
    }

    private SootClass transformEnrichAndValidateIfNotSpecialCase(String toTransformName) {
        SootClass result;
        if ((result = resolvedClasses.get(addPrefixToName(toTransformName))) != null) {
            return result;
        }
        if (shouldBeTransformed(toTransformName)) {
            if (!isAlreadyTransformedOrToBeTransformedPath(toTransformName)) {
                decideOnAddToClassesToTransform(toTransformName);
            }
            return transformEnrichAndValidate(toTransformName);
        } else {
            return getClassNodeForName(toTransformName);
        }
    }

    private SootMethodRef constantWrapperMethodRef(Type t) {
        SootMethodRef used;
        if (isIntOrSint(t)) {
            used = v.SM_SE_CONCSINT.makeRef();
        } else if (isLongOrSlong(t)) {
            used = v.SM_SE_CONCSLONG.makeRef();
        } else if (isDoubleOrSdouble(t)) {
            used = v.SM_SE_CONCSDOUBLE.makeRef();
        } else if (isFloatOrSfloat(t)) {
            used = v.SM_SE_CONCSFLOAT.makeRef();
        } else if (isShortOrSshort(t)) {
            used = v.SM_SE_CONCSSHORT.makeRef();
        } else if (isByteOrSbyte(t)) {
            used = v.SM_SE_CONCSBYTE.makeRef();
        } else if (isBoolOrSbool(t)) {
            used = v.SM_SE_CONCSBOOL.makeRef();
        } else {
            throw new NotYetImplementedException(t.toString());
        }
        return used;
    }
}
