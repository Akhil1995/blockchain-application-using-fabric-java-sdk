package org.example.chaincode.invocation;

public class TxnRead {
	private String key;
	private long blockNumber;
	private String valueRead;
	@Override
	public String toString() {
		return "TxnRead [key=" + key + ", blockNumber=" + blockNumber + ", valueRead=" + valueRead + "]";
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
	public String getValueRead() {
		return valueRead;
	}
	public void setValueRead(String valueRead) {
		this.valueRead = valueRead;
	}
	public TxnRead(String key,long blockNumber){
		this.key = key;
		this.blockNumber = blockNumber;
	}
}
