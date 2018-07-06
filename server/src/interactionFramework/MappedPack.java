package interactionFramework;

public class MappedPack {
	public String activity;
	public Boolean gt;//true if the constraint is greater than some time, false if less than
	public Boolean keyS;//true if the constraint is between start of the key activity
	public Boolean valueS;//true if the constraint is between start of the value activity
	public Boolean sync; 
	
	public MappedPack(String inputActivity, Boolean inputGt, Boolean inputKeyS, Boolean inputValueS, Boolean inputSync){
		activity = inputActivity;
		gt = inputGt;
		keyS = inputKeyS;
		valueS = inputValueS;
		sync = inputSync;
	}
}
