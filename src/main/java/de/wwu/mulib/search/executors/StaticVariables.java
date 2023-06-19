package de.wwu.mulib.search.executors;

import de.wwu.mulib.exceptions.MulibRuntimeException;
import de.wwu.mulib.transformations.MulibValueTransformer;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

public class StaticVariables {

    private final Map<String, Object> fieldNamesToInitialValues;
    private Map<String, Object> staticFieldsToValues;

    public StaticVariables(MulibValueTransformer mulibValueTransformer, Map<Field, Field> transformedToOriginalStaticFields) {
        this.staticFieldsToValues = new HashMap<>();
        this.fieldNamesToInitialValues = new HashMap<>();
        for (Map.Entry<Field, Field> entry : transformedToOriginalStaticFields.entrySet()) {
            Field originalField = entry.getValue();
            originalField.setAccessible(true);
            assert Modifier.isStatic(originalField.getModifiers()) : "No static field";
            try {
                Object value = originalField.get(null);
                this.fieldNamesToInitialValues.put(
                        entry.getKey().getDeclaringClass().getName() + "." + entry.getKey().getName(),
                        mulibValueTransformer.transform(value)
                );
            } catch (IllegalAccessException e) {
                throw new MulibRuntimeException(e);
            }
        }
    }

    private StaticVariables(Map<String, Object> fieldNamesToInitialValues) {
        this.fieldNamesToInitialValues = fieldNamesToInitialValues;
    }

    public Object getStaticField(String fieldName, SymbolicExecution se) {
        if (staticFieldsToValues == null) {
            staticFieldsToValues = new HashMap<>();
        }
        return staticFieldsToValues.computeIfAbsent(
                fieldName,
                k -> se.getMulibValueCopier().copy(fieldNamesToInitialValues.get(fieldName))
        );
    }

    public void setStaticField(String fieldName, Object value) {
        if (staticFieldsToValues == null) {
            staticFieldsToValues = new HashMap<>();
        }
        staticFieldsToValues.put(fieldName, value);
    }

    public void renew() {
        if (staticFieldsToValues == null) {
            return;
        }
        staticFieldsToValues.clear();
    }

    public StaticVariables copyFromPrototype() {
        return new StaticVariables(fieldNamesToInitialValues);
    }

}
