package com.demo;

public class ClaseHijo1 extends ClasePadre1 {
    private int atributoClaseHijo1_1;
    private int atributoClaseHijo1_2;
    public ClaseHijo1(){ super(); this.atributoClaseHijo1_1=0; this.atributoClaseHijo1_2=0; }
    public ClaseHijo1(int a1,int a2){ super(); this.atributoClaseHijo1_1=a1; this.atributoClaseHijo1_2=a2; }
    public ClaseHijo1(int p1,int p2,int p3,int h1,int h2){ super(p1,p2,p3); this.atributoClaseHijo1_1=h1; this.atributoClaseHijo1_2=h2; }
    public int getAtributoClaseHijo1_1(){return atributoClaseHijo1_1;}
    public void setAtributoClaseHijo1_1(int v){atributoClaseHijo1_1=v;}
    public int getAtributoClaseHijo1_2(){return atributoClaseHijo1_2;}
    public void setAtributoClaseHijo1_2(int v){atributoClaseHijo1_2=v;}
}
