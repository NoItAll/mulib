package de.wwu.mulib.transform_and_execute.examples;

import static de.wwu.mulib.Mulib.assume;
import static de.wwu.mulib.Mulib.rememberedFreeInt;
public class SendMoreMoney {


    public static int[] search() {
        int s = rememberedFreeInt("s", 1, 9);
        int e = rememberedFreeInt("e", 0, 9);
        int n = rememberedFreeInt("n", 0, 9);
        int d = rememberedFreeInt("d", 0, 9);
        int m = rememberedFreeInt("m", 1, 9);
        int o = rememberedFreeInt("o", 0, 9);
        int r = rememberedFreeInt("r", 0, 9);
        int y = rememberedFreeInt("y", 0, 9);
        int[] vars = {s, e, n, d, m, o, r, y};
        for(int i=0; i<vars.length; i++) {
            for(int j=0; j<i; j++)
                assume(vars[i] != vars[j]);
        }
        assume( s*1000+e*100+n*10+d +
                m*1000+o*100+r*10+e ==
                m*10000+o*1000+n*100+e*10+y);
        return vars;
    }

}
