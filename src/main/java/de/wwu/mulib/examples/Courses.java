package de.wwu.mulib.examples;
import  java.util.ArrayList;

import static de.wwu.mulib.Mulib.*;

public class Courses {
    private ArrayList<Assignment> assignments = new ArrayList<Assignment>();
    final static int DATABASES = 0,
            MATH = 1,
            ACCOUNTING = 2,
            PROGRAMMING = 3;
    final static int MILLER = 0,
            BROWN = 1,
            SMITH = 2;

    private CanTeach[] canTeach = {
            new CanTeach(MILLER,DATABASES),
            new CanTeach(MILLER,PROGRAMMING),
            new CanTeach(BROWN,MATH),
            new CanTeach(BROWN,PROGRAMMING),
            new CanTeach(SMITH,MATH),
            new CanTeach(SMITH,ACCOUNTING)};

//    enum Course{
//        DATABASES, MATH, ACCOUNTING, PROGRAMMING
//    }
//
//    enum Teacher{
//        MILLER, BROWN, SMITH
//    }

    public static class CanTeach{
        int teacher;
        int course;
        CanTeach(int t, int c){
            teacher = t; course = c;
        }
    }

    public static class Assignment{
        int teacher;
        int course;
        int time;

        Assignment(int t, int c, int tm) {
            teacher = t; course = c; time = tm;
        }
    }

    public void freeProf(int t, int time, ArrayList<Assignment> assignments) {
        for(Assignment a: assignments)
            assume((a.teacher != t) || (a.time != time));
    }

    public void freeCourse(int c, ArrayList<Assignment> assignments) {
        for(Assignment a: assignments)
            assume(a.course != c);
    }

    public boolean checkSkill(int t, int c) {
        for(CanTeach ct: canTeach)
            if ((ct.teacher == t) && (ct.course == c))
                return true;
        return false;
    }

    public void newAssignment() {
        int t = freeInt();
        int c = freeInt();
        int time = freeInt();
        assume(checkSkill(t, c));
        assume(time == 8 || time == 10);
        freeProf(t,time,assignments);
        freeCourse(c,assignments);
        assignments.add(new Assignment(t,c,time));
    }

    public static ArrayList<Assignment> schedule() {
        Courses courses = new Courses();
        while (courses.assignments.size() < 4)
            courses.newAssignment();
        return courses.assignments;
    }

}
