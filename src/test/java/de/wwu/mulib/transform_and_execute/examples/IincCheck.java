package de.wwu.mulib.transform_and_execute.examples;

import de.wwu.mulib.Mulib;
import de.wwu.mulib.throwables.MulibRuntimeException;

public class IincCheck {

    public static int exec() {
        int i = Mulib.freeInt();
        int j = i;
        i++;
        if (i <= j) {
            throw new MulibRuntimeException("Must not occur.");
        }
        return 1;
    }

}
