package com.demo;

import java.util.ArrayList;
import java.util.List;

public class ClaseGestora {
    private int atributoClaseGestora_1;
    private int atributoClaseGestora_2;
    private int atributoClaseGestora_3;
    private ClasePadre1 objetoMiembro;
    private List<ClaseRelacionN> relaciones;
    public ClaseGestora(){ atributoClaseGestora_1=0; atributoClaseGestora_2=0; atributoClaseGestora_3=0; objetoMiembro=null; relaciones=new ArrayList<>(); }
    public ClaseGestora(int a1,int a2,int a3){ this(); atributoClaseGestora_1=a1; atributoClaseGestora_2=a2; atributoClaseGestora_3=a3; }
    public ClaseGestora(int a1,int a2,int a3,ClasePadre1 objeto){ this(a1,a2,a3); objetoMiembro=objeto; }
    public ClaseGestora(int a1,int a2,int a3,ClasePadre1 objeto,List<ClaseRelacionN> relaciones){ this(a1,a2,a3,objeto); if(relaciones!=null) for(ClaseRelacionN r:relaciones) addRelacion(r); }
    public int getAtributoClaseGestora_1(){return atributoClaseGestora_1;}
    public void setAtributoClaseGestora_1(int v){atributoClaseGestora_1=v;}
    public int getAtributoClaseGestora_2(){return atributoClaseGestora_2;}
    public void setAtributoClaseGestora_2(int v){atributoClaseGestora_2=v;}
    public int getAtributoClaseGestora_3(){return atributoClaseGestora_3;}
    public void setAtributoClaseGestora_3(int v){atributoClaseGestora_3=v;}
    public ClasePadre1 getObjetoMiembro(){return objetoMiembro;}
    public void setObjetoMiembro(ClasePadre1 v){objetoMiembro=v;}
    public List<ClaseRelacionN> getRelaciones(){return new ArrayList<>(relaciones);}
    public void addRelacion(ClaseRelacionN r){if(r!=null){relaciones.add(r); r.setGestora(this);}}
    public void removeRelacion(ClaseRelacionN r){if(relaciones.remove(r)) r.setGestora(null);}
}
