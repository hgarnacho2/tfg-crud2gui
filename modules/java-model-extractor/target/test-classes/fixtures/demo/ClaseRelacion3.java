package com.demo;

public class ClaseRelacion3 {
    private ClaseHijo1 objetoAsociado;
    public ClaseRelacion3(){objetoAsociado=null;}
    public ClaseRelacion3(ClaseHijo1 objetoAsociado){this.objetoAsociado=objetoAsociado;}
    public ClaseHijo1 getObjetoAsociado(){return objetoAsociado;}
    public void setObjetoAsociado(ClaseHijo1 v){objetoAsociado=v;}
}
