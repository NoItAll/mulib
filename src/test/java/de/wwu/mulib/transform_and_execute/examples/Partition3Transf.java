package de.wwu.mulib.transform_and_execute.examples;

import de.wwu.mulib.Mulib;

public class Partition3Transf {
    int m;
    int[] a;
    int sum;
    int[] mapping;

    Partition3Transf() {
        int n = 9;
        m = n/3;   // number of triples taken from input values
        a = new int[n];
        for(int i=0; i<n; i++) a[i] = i;
        sum = 0;   // sum of all inputs
        for (int i = 0; i < a.length; i++) {
            sum += a[i];
        }
    }


    public int[] generateMapping(){
        int n = 9;
        mapping = new int[n];
        for(int i=0; i<n; i++) {
            int triple = Mulib.trackedFreeInt("triple" + i);
            if (triple <= 0 || triple > m)
                throw Mulib.fail();
            else mapping[i] = triple;
        }
        return mapping;
    }

    public void checkMapping() {
        int n = 9;
        for(int j=1; j<=m; j++) {
            int sum3 = 0;
            int occurrences = 0;
            for(int i=0; i<n; i++)
                if(mapping[i] == j) {
                    occurrences++;
                    sum3 += a[i]; }
            if (occurrences != 3 || sum3 != sum/m) throw Mulib.fail();
        }
    }

    public static int[] exec() {
        Partition3Transf p = new Partition3Transf();

        p.mapping = p.generateMapping(); // mapping of inputs to triples
        p.checkMapping();
        return p.mapping;

    }
}
