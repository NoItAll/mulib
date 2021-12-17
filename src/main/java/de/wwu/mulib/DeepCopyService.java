package de.wwu.mulib;

import com.google.gson.Gson;
import de.wwu.mulib.exceptions.MulibRuntimeException;
import de.wwu.mulib.substitutions.primitives.Sprimitive;

import java.io.*;

// In the future, this should be replaced with lazy copiers of the original input wrapping the object.
public class DeepCopyService {
    private Gson gson = new Gson();

    public Object deepCopy(Object toCopy) {
        if (toCopy == null) {
            return null;
        } else if (toCopy instanceof Sprimitive) {
            return toCopy;
        } else if (toCopy instanceof Serializable) {
            try {
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
                objectOutputStream.writeObject(toCopy);
                ByteArrayInputStream byteArrayInputStream =
                        new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
                ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);
                return objectInputStream.readObject();
            } catch (IOException | ClassNotFoundException e) {
                throw new MulibRuntimeException("Deep copy failed", e);
            }
        }
        return gson.fromJson(gson.toJson(toCopy), toCopy.getClass());
    }
}
