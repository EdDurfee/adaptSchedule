package tcsp;

import stp.STN;
import interval.Interval;
import interval.IntervalSet;

/**
 * Intended to be a simple data structure for storing TCSP
 * @author User
 *
 */
public class TCSP {
	IntervalSet[][] temporalNetwork;
	
	public TCSP(int size){
		temporalNetwork = new IntervalSet[size][size];
		for(int i=0;i<size;i++){
			for(int j=i+1;j<size;j++){
				temporalNetwork[i][j] = new IntervalSet();
			}
		}
	}
	
	public void add(STN stp){
		for(int i=0;i<temporalNetwork.length;i++){
			for(int j=i+1;j<temporalNetwork.length;j++){
				temporalNetwork[i][j].add(new Interval(0-stp.getBound(j, i), stp.getBound(i, j)));
			}
		}
	}
	
	
}
