package pwr.project.getrawdata;

import java.util.ArrayList;

public class Srednia {

	ArrayList<WynikiGPS> wynikiGPS;			//"Pozdrowienia dla dr'a Greblickiego :**, WE <33333333 AK!
	ArrayList<WynikiACC> wynikiACC;			//OOOO MACARENA!!
	ArrayList<WynikiGPS> srednieWlasciweGPS;
	ArrayList<WynikiGPS> srednieZeSredniejGPS;
	ArrayList<WynikiACC> srednieWlasciweACC;
	ArrayList<WynikiACC> srednieZeSredniejACC;
	ArrayList<WynikiACC> bladACC;
	ArrayList<WynikiGPS> bladGPS;
	float sredniaGPS;
	float sredniaACC;
	float blad;
	int indeksGPS;
	int indeksACC;
	boolean czyPelnaGPS = false;
	boolean czyPelnaACC = false;
	static final int ILOSC = 2000;
	int ileDanych = 0;

	Srednia() {
			wynikiGPS = new ArrayList<WynikiGPS>();
			wynikiACC = new ArrayList<WynikiACC>();
		srednieZeSredniejACC = new ArrayList<WynikiACC>();
		srednieWlasciweACC = new ArrayList<WynikiACC>();
		srednieZeSredniejGPS = new ArrayList<WynikiGPS>();
		srednieWlasciweGPS = new ArrayList<WynikiGPS>();
		bladACC = new ArrayList<WynikiACC>();
		bladGPS = new ArrayList<WynikiGPS>();
		indeksGPS = -1;
		indeksACC = -1;
		for(int i = 0; i < ILOSC; i++){
			wynikiGPS.add(new WynikiGPS(0, 0));
			wynikiACC.add(new WynikiACC(0, 0, 0));
		
		}
		
		

	}

	void pobieranieDanychGPS(float a, float b) {
		WynikiGPS gps = new WynikiGPS(a, b);
		if(indeksGPS == ILOSC - 1){
			indeksGPS = -1;
			czyPelnaGPS = true;
		}
		wynikiGPS.set(++indeksGPS, gps);
		if(czyPelnaGPS){
			liczenieSredniejGPS();
		}
	}

	void pobieranieDanychACC(float a, float b, float c) {
		WynikiACC acc = new WynikiACC(a, b,c);
		if(indeksACC == ILOSC - 1){
			indeksACC = -1;
			czyPelnaACC = true;
		}
		wynikiACC.set(++indeksACC,acc);
		if(czyPelnaACC){
			liczenieSredniejACC();
		}
	}
	
	void liczenieSredniejGPS(){
		float sumDl=0, sumSzer=0, srDl=0, srSzer=0; 
		
		for(WynikiGPS k: wynikiGPS)
		{
			sumDl+=k.dl;
			sumSzer+=k.szer;
		}
		srDl=sumDl/ILOSC;
		srSzer=sumSzer/ILOSC;
		srednieWlasciweGPS.add(new WynikiGPS(srDl, srSzer));
		if(srednieZeSredniejGPS.size()==0)
			srednieZeSredniejGPS.add(srednieWlasciweGPS.get(0));
		else{
			WynikiGPS w = wynikiGPS.get(indeksGPS);
			srDl = ((ILOSC*srednieZeSredniejGPS.get(srednieZeSredniejGPS.size()-1).dl) + w.dl)/(ILOSC + 1);
			srSzer = ((ILOSC*srednieZeSredniejGPS.get(srednieZeSredniejGPS.size()-1).szer) + w.szer)/(ILOSC + 1);
			srednieZeSredniejGPS.add(new WynikiGPS(srDl, srSzer));
		}
		float bladDl = srednieWlasciweGPS.get(srednieWlasciweGPS.size()-1).dl-srednieZeSredniejGPS.get(srednieZeSredniejGPS.size()-1).dl;
		float bladSzer = srednieWlasciweGPS.get(srednieWlasciweGPS.size()-1).szer-srednieZeSredniejGPS.get(srednieZeSredniejGPS.size()-1).szer;
		
		bladGPS.add(new WynikiGPS(bladDl, bladSzer));
	}
	
	float getSrednia(){
		return bladACC.get(bladACC.size()-1).x;
	}
	
	void liczenieSredniejACC(){
		float sumX=0, sumY=0,sumZ=0, srX=0, srY=0,srZ=0; 
		
		for(WynikiACC k: wynikiACC)
		{
			sumX+=k.x;
			sumY+=k.y;
			sumZ+=k.z;
		}
		srX=sumX/ILOSC;
		srY=sumY/ILOSC;
		srZ=sumZ/ILOSC;
		srednieWlasciweACC.add(new WynikiACC(srX, srY, srZ));
		if(srednieZeSredniejACC.size()==0)
			srednieZeSredniejACC.add(srednieWlasciweACC.get(0));
		else{
			WynikiACC w = wynikiACC.get(indeksACC);
			srX = ((ILOSC*srednieZeSredniejACC.get(srednieZeSredniejACC.size()-1).x) + w.x)/(ILOSC + 1);
			srY = ((ILOSC*srednieZeSredniejACC.get(srednieZeSredniejACC.size()-1).y) + w.y)/(ILOSC + 1);
			srZ = ((ILOSC*srednieZeSredniejACC.get(srednieZeSredniejACC.size()-1).z) + w.z)/(ILOSC + 1);
			srednieZeSredniejACC.add(new WynikiACC(srX, srY, srZ));
		}
		
		float bladX = srednieWlasciweACC.get(srednieWlasciweACC.size()-1).x-srednieZeSredniejACC.get(srednieZeSredniejACC.size()-1).x;
		float bladY = srednieWlasciweACC.get(srednieWlasciweACC.size()-1).y-srednieZeSredniejACC.get(srednieZeSredniejACC.size()-1).y;
		float bladZ = srednieWlasciweACC.get(srednieWlasciweACC.size()-1).z-srednieZeSredniejACC.get(srednieZeSredniejACC.size()-1).z;
		
		bladACC.add(new WynikiACC(bladX, bladY,bladZ));
		ileDanych++;
	}
}

class WynikiGPS {
	float dl;
	float szer;

	WynikiGPS(float a, float b) {
		dl = a;
		szer = b;
	}
}

class WynikiACC {
	float x;
	float y;
	float z;

	WynikiACC(float a, float b, float c) {
		x = a;
		y = b;
		z = c;
	}

}