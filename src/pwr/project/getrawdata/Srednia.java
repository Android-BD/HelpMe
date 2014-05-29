package pwr.project.getrawdata;

import java.util.LinkedList;

public class Srednia<T extends Wyniki> {


	LinkedList<T> wynikiPomiarow;
	float sredniaGPS;
	float sredniaACC;
	static final int ILOSC_MINIMUM = 10;


	Srednia() {
		wynikiPomiarow = new LinkedList<T>();
	}




	float sredniaCalkowitaZWieluPomiarow(T a){
		if(wynikiPomiarow.size() < ILOSC_MINIMUM){
			wynikiPomiarow.add(a);
		} else{
			wynikiPomiarow.poll();
			wynikiPomiarow.add(a);
		}


		return getSredniaCalkowita(wynikiPomiarow);
	}

	private float getSredniaCalkowita(LinkedList<T> p){
		float wynik = 0;
		double suma = 0;
		for (T a: p){
			suma += a.dlugoscWektora();
		}
		wynik = (float)suma/p.size();
		return wynik;
	}
}

abstract class Wyniki{
	abstract double dlugoscWektora();
}
class WynikiGPS extends Wyniki {
	float dl;
	float szer;

	WynikiGPS(float a, float b) {
		dl = a;
		szer = b;
	}

	@Override
	double dlugoscWektora() {
		return 0;
	}
}

class WynikiACC extends Wyniki {
	float x;
	float y;
	float z;

	WynikiACC(float a, float b, float c) {
		x = a;
		y = b;
		z = c;
	}
	
	public double dlugoscWektora(){
		return Math.sqrt(x*x+y*y+z*z);
	}
	

}
