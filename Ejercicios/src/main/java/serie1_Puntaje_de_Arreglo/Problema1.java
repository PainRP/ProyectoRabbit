package serie1_Puntaje_de_Arreglo;

public class Problema1 {
	public static int score(int[] numbers) {
		int total = 0;
		
		for(int num: numbers) {
			if(num%2==0 || num == 0) {
				total = total+1;
			}else if(num == 5) {
				total = total +5;
			} else {
				total = total+3;
			}
		}
		
		return total;
	}
	
	//La complejidad temporal sería de O(n), debido a que es un bucle for-each
	//La complejidad espacial sería de O(1), debido a que no se crea nada más, solo una variabble
}
