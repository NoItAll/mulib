package de.wwu.mulib.transformations.soot_transformations;

import de.wwu.mulib.throwables.MulibRuntimeException;
import org.objectweb.asm.ClassWriter;
import soot.util.backend.SootASMClassWriter;

import static de.wwu.mulib.transformations.StringConstants._TRANSFORMATION_INDICATOR;

/**
 * Adaptation of ClassWriter to fit the transformation prefix.
 */
public class MulibSootClassWriter extends SootASMClassWriter {

    /**
     * Constructs a new {@link ClassWriter} object.
     *
     * @param flags option flags that can be used to modify the default behavior of this class. See {@link #COMPUTE_MAXS},
     *              {@link #COMPUTE_FRAMES}.
     */
    public MulibSootClassWriter(int flags) {
        super(flags);
    }

    @Override
    protected String getCommonSuperClass(final String type1, final String type2) {
        if (type1.contains(_TRANSFORMATION_INDICATOR) || type2.contains(_TRANSFORMATION_INDICATOR)) {
            if (type1.contains(_TRANSFORMATION_INDICATOR) && type2.contains(_TRANSFORMATION_INDICATOR)) {
                String t1 = type1.replace(_TRANSFORMATION_INDICATOR, "");
                String t2 = type2.replace(_TRANSFORMATION_INDICATOR, "");
                try {
                    Class<?> c1 = Class.forName(t1.replace("/", "."));
                    Class<?> c2 = Class.forName(t2.replace("/", "."));
                    if (c1.isAssignableFrom(c2)) {
                        return type1;
                    } else if (c2.isAssignableFrom(c1)) {
                        return type2;
                    }
                } catch (ClassNotFoundException e) {
                    throw new MulibRuntimeException(e);
                }
            }
            return Object.class.getName().replace(".", "/");
        } else {
            return super.getCommonSuperClass(type1, type2);
        }
    }
}
