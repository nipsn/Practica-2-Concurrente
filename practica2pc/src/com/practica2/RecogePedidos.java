package com.practica2;

public class RecogePedidos implements Personal {
    public void trabajar(){
        while(true){
            if(Almazon.hayPedido.get()){
                Almazon.p.add(Almazon.pedidos.poll());
            }
        }
    }
}
