package pruebas;

public class pruebas {

public static void main(String[] args) {

	  int num=888;



      int c=0; //numeros

      int resultado=0;

      int contador=0;

      int divisores=0;

      while(contador!=num ){

    	  divisores=0;

          c++;

    	  

          for(int i=1;i<=c;i++){

              if(c%i==0){

                  divisores++;

              }

          }



          if(divisores==2 || c==1){

              contador++;

              resultado=c;
           //   System.out.println(c);

          }

         

      }



      System.out.println(resultado);

}

}
