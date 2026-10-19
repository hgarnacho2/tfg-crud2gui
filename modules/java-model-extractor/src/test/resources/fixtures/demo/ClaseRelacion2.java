package com.demo;

public class ClaseRelacion2 {
    private final ClaseHijo1 objetoCompuesto;
    public ClaseRelacion2(){objetoCompuesto=new ClaseHijo1();}
    public ClaseRelacion2(int p1,int p2,int p3,int h1,int h2){objetoCompuesto=new ClaseHijo1(p1,p2,p3,h1,h2);}
    public ClaseRelacion2(ClaseHijo1 objetoCompuesto){this.objetoCompuesto=objetoCompuesto;}
    public ClaseHijo1 getObjetoCompuesto(){return objetoCompuesto;}
}
