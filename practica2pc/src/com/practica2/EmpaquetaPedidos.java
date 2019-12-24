package com.practica2;

public class EmpaquetaPedidos implements Personal {
    public void trabajar(){
        while(true){
            if(!Almazon.p.isFull() && !Almazon.p.isSucia()){
                Almazon.cinta.offer(Almazon.p.poll());
            }
        }
    }
}
