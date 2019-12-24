package com.practica2;
public class Administrativo implements Personal {
    public void trabajar(){
        while(true){
            //replantear. puede que al peekear la segunda vez haya uno distinto
            if(Almazon.pedidos.peek() != null && Almazon.pedidos.peek().isPagado()) Almazon.hayPedido.set(true);
        }
    }

}
