package de.wwu.mulib.search.executors;

import de.wwu.mulib.throwables.MulibRuntimeException;
import de.wwu.mulib.transformations.MulibValueTransformer;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

/**
 * Maintains a transformed version of the relevant static variables within the search region.
 * Each {@link MulibExecutor} receives a copy via {@link StaticVariables#copyFromPrototype()} that they maintain.
 * The initially transformed values are stored and will be copied upon first accessing them.
 */
public class StaticVariables {

    private final Map<String, Object> fieldNamesToInitialValues;
    private Map<String, Object> staticFieldsToValues;
    private MulibValueCopier mulibValueCopier;

    /**
     * Constructs a new instance, retrieves static values from the fields and transforms them into a search region
     * representation.
     * @param mulibValueTransformer The transformer used for transforming the static field values into the search
     *                              region representation
     * @param transformedToOriginalStaticFields Key-value pairs in the format (transformed static field, original static field)
     */
    public StaticVariables(
            MulibValueTransformer mulibValueTransformer,
            Map<Field, Field> transformedToOriginalStaticFields) {
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

    /**
     * Returns the value stored in a "static field"
     * @param fieldName The field name. Should follow the format: packageName.className.fieldName
     * @return The value
     */
    public Object getStaticField(String fieldName) {
        if (staticFieldsToValues == null) {
            staticFieldsToValues = new HashMap<>();
        }
        return staticFieldsToValues.computeIfAbsent(
                fieldName,
                k -> mulibValueCopier.copy(fieldNamesToInitialValues.get(fieldName))
        );
    }

    /**
     * Sets the mulib value copier
     * @param mulibValueCopier The copier
     */
    public void setMulibValueCopier(MulibValueCopier mulibValueCopier) {
        this.mulibValueCopier = mulibValueCopier;
    }

    /**
     * Stores a value into a "static field"
     * @param fieldName The field name. Should follow the format: packageName.className.fieldName
     * @param value The value
     */
    public void setStaticField(String fieldName, Object value) {
        if (staticFieldsToValues == null) {
            staticFieldsToValues = new HashMap<>();
        }
        staticFieldsToValues.put(fieldName, value);
    }

    /**
     * Resets all initialized static fields
     */
    public void reset() {
        if (staticFieldsToValues == null) {
            return;
        }
        staticFieldsToValues.clear();
    }

    /**
     * Copies an instance from the prototype.
     * @return An instance containing the same initial values that will be copied upon the first read-access
     */
    public StaticVariables copyFromPrototype() {
        return new StaticVariables(fieldNamesToInitialValues);
    }

}
