package model;

public class SlidePartData {
	public short sequenceNumber;
	public short sessionNumber;
	public byte[] data;
	public boolean start;
	public boolean end;
	public short numPack;
	public int maxPacketSize;
	
	public SlidePartData() {
	}
}