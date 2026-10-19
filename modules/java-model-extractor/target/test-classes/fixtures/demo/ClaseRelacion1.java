package com.demo;

public class ClaseRelacion1 {
    private ClaseHijo1 objetoAgregado;
    public ClaseRelacion1(){objetoAgregado=null;}
    public ClaseRelacion1(ClaseHijo1 objetoAgregado){this.objetoAgregado=objetoAgregado;}
    public ClaseHijo1 getObjetoAgregado(){return objetoAgregado;}
    public void setObjetoAgregado(ClaseHijo1 v){objetoAgregado=v;}
}
