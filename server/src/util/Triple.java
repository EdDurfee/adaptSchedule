package util;

public class Triple<S1, S2, S3> {

	
	private S1 left;
	private S2 middle;
	private S3 right;
	
	public Triple(S1 l, S2 m, S3 r){
		left = l;
		middle = m;
		right = r;
	}
	
	public S1 getLeft(){
		return left;
	}
	
	public S2 getMiddle(){
		return middle;
	}
	
	public S3 getRight(){
		return right;
	}
	
	
}
