package com.demo;

public class ClaseRelacionN {
    private int atributoClaseRelacionN_1;
    private int atributoClaseRelacionN_2;
    private ClaseGestora gestora;
    public ClaseRelacionN(){atributoClaseRelacionN_1=0; atributoClaseRelacionN_2=0; gestora=null;}
    public ClaseRelacionN(int a1,int a2){this(); atributoClaseRelacionN_1=a1; atributoClaseRelacionN_2=a2;}
    public ClaseRelacionN(int a1,int a2,ClaseGestora gestora){this(a1,a2); this.gestora=gestora;}
    public int getAtributoClaseRelacionN_1(){return atributoClaseRelacionN_1;}
    public void setAtributoClaseRelacionN_1(int v){atributoClaseRelacionN_1=v;}
    public int getAtributoClaseRelacionN_2(){return atributoClaseRelacionN_2;}
    public void setAtributoClaseRelacionN_2(int v){atributoClaseRelacionN_2=v;}
    public ClaseGestora getGestora(){return gestora;}
    public void setGestora(ClaseGestora v){gestora=v;}
}
