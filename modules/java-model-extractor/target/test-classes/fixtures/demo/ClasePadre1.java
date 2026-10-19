package com.demo;

public class ClasePadre1 {
    private int atributoClasePadre1_1;
    private int atributoClasePadre1_2;
    private int atributoClasePadre1_3;
    public ClasePadre1(){ this.atributoClasePadre1_1=0; this.atributoClasePadre1_2=0; this.atributoClasePadre1_3=0; }
    public ClasePadre1(int a1){ this(); this.atributoClasePadre1_1=a1; }
    public ClasePadre1(int a1,int a2,int a3){ this.atributoClasePadre1_1=a1; this.atributoClasePadre1_2=a2; this.atributoClasePadre1_3=a3; }
    public int getAtributoClasePadre1_1(){return atributoClasePadre1_1;}
    public void setAtributoClasePadre1_1(int v){atributoClasePadre1_1=v;}
    public int getAtributoClasePadre1_2(){return atributoClasePadre1_2;}
    public void setAtributoClasePadre1_2(int v){atributoClasePadre1_2=v;}
    public int getAtributoClasePadre1_3(){return atributoClasePadre1_3;}
    public void setAtributoClasePadre1_3(int v){atributoClasePadre1_3=v;}
}
