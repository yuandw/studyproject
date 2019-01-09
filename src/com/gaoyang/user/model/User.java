package com.gaoyang.user.model;


public class User {
    int age;
    int a;
    int b;
    int height;
    long c;
    String code;
    String str;
    public int getAge() {
        return age;
    }

    public int getHeight() {
        return height;
    }

    public int getA() {
        return a;
    }

    public int getB() {
        return b;
    }

    public long getC() {
        return c;
    }

    public String getCode() {
        return code;
    }

    public void setStr(String str) {
        this.str = str;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public void setA(int a) {
        this.a = a;
    }

    public void setB(int b) {
        this.b = b;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public void setC(long c) {
        this.c = c;
    }

    public void setCode(String code) {
        this.code = code;
    }
    void add(){
        System.out.println(a+b);
    }
    public static void main(String[] args) {
     User user =new User();
     user.setA(5);
     user.setB(7);
     user.add();
    }
}
