package de.wwu.mulib.search.executors;

import de.wwu.mulib.exceptions.MulibRuntimeException;
import de.wwu.mulib.transformations.MulibValueTransformer;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class StaticVariables {

    private final Map<String, Object> staticFieldsToInitialValues;
    private final Map<SymbolicExecution, Map<String, Object>> staticFieldsForSymbolicExecutions;

    public StaticVariables(MulibValueTransformer mulibValueTransformer, Map<Field, Field> transformedToOriginalStaticFields) {
        this.staticFieldsForSymbolicExecutions = new ConcurrentHashMap<>();
        staticFieldsToInitialValues = new HashMap<>();
        for (Map.Entry<Field, Field> entry : transformedToOriginalStaticFields.entrySet()) {
            Field originalField = entry.getValue();
            originalField.setAccessible(true);
            assert Modifier.isStatic(originalField.getModifiers()) : "No static field";
            try {
                Object value = originalField.get(null);
                staticFieldsToInitialValues.put(
                        entry.getKey().getDeclaringClass().getName() + "." + entry.getKey().getName(),
                        mulibValueTransformer.transform(value)
                );
            } catch (IllegalAccessException e) {
                throw new MulibRuntimeException(e);
            }
        }
    }

    public Object getStaticField(String fieldName, SymbolicExecution se) {
        Map<String, Object> staticFieldsForSe = staticFieldsForSymbolicExecutions.computeIfAbsent(se, k -> new HashMap<>());
        Object result = staticFieldsForSe.computeIfAbsent(
                fieldName,
                k -> se.getMulibValueCopier().copy(staticFieldsToInitialValues.get(fieldName))
        );
        return result;
    }

    public void setStaticField(String fieldName, Object value, SymbolicExecution se) {
        Map<String, Object> staticFieldsForSe = staticFieldsForSymbolicExecutions.computeIfAbsent(se, k -> new HashMap<>());
        staticFieldsForSe.put(fieldName, value);
    }

    public void tearDown(SymbolicExecution se) {
        staticFieldsForSymbolicExecutions.remove(se);
    }

}
