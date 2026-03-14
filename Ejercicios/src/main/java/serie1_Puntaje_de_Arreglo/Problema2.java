package serie1_Puntaje_de_Arreglo;

public class Problema2 {
	public static int[] secondMinMax(int[] numbers) {
		
		int max= numbers[0];
		int secondMax= numbers[0];
		int min = numbers[0];
		int secondMin = numbers[0];
		
		
		for(int i: numbers) {
			if(i > max) {
				secondMax = max;
				max = i;
			}else if(i > secondMax && i < max) {
				secondMax = i;
			}
			
			if (i < min) {
				secondMin = min;
				min = i;
			}else if(i < secondMin && i > min) {
				secondMin = i;
			}
			
		}
		
		
		return new int[] {secondMax,secondMin};
	}
}
