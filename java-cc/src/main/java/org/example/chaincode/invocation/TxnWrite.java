package org.example.chaincode.invocation;

public class TxnWrite {
	private String key;
	private long blockNumber;
	private String valueWritten;
	
	@Override
	public String toString() {
		return "TxnWrite [key=" + key + ", blockNumber=" + blockNumber + ", valueWritten=" + valueWritten+"]";
	}
	public String getKey() {
		return key;
	}
	public void setKey(String key) {
		this.key = key;
	}
	public long getBlockNumber() {
		return blockNumber;
	}
	public void setBlockNumber(long blockNumber) {
		this.blockNumber = blockNumber;
	}
	public String getValueWritten() {
		return valueWritten;
	}
	public void setValueWritten(String valueRead) {
		this.valueWritten = valueRead;
	}
	public TxnWrite(String key,long blockNumber){
		this.key = key;
		this.blockNumber = blockNumber;
	}
	public TxnWrite(){
	}
}
