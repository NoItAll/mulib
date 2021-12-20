package de.wwu.mulib.transformations;

import de.wwu.mulib.exceptions.MulibRuntimeException;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import static de.wwu.mulib.transformations.StringConstants._TRANSFORMATION_PREFIX;

public class MulibClassWriter extends ClassWriter {
    public MulibClassWriter(int flags) {
        super(flags);
    }

    public MulibClassWriter(ClassReader classReader, int flags) {
        super(classReader, flags);
    }


    @Override
    protected String getCommonSuperClass(final String type1, final String type2) {
        if (type1.contains(_TRANSFORMATION_PREFIX) || type2.contains(_TRANSFORMATION_PREFIX)) {
            if (type1.contains(_TRANSFORMATION_PREFIX) && type2.contains(_TRANSFORMATION_PREFIX)) {
                String t1 = type1.replace(_TRANSFORMATION_PREFIX, "");
                String t2 = type2.replace(_TRANSFORMATION_PREFIX, "");
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