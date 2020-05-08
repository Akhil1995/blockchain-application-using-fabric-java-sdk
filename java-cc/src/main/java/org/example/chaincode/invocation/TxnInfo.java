package org.example.chaincode.invocation;

import java.util.ArrayList;
import java.util.List;

public class TxnInfo {
	private String txn_id;
	private long timestamp;
	private List<TxnRead> readlist;
	private List<TxnWrite> writelist;
	private List<String> callArgs;
	private List<String> endorserList;
	private String chaincode;
	private long blockHeight;
	
	public long getBlockHeight() {
		return blockHeight;
	}

	public void setBlockHeight(long blockHeight) {
		this.blockHeight = blockHeight;
	}

	public String getChaincode() {
		return chaincode;
	}

	public void setChaincode(String chaincode) {
		this.chaincode = chaincode;
	}

	public TxnInfo(String txn_id, long timestamp, List<String> args,String chaincode) {
		this.txn_id = txn_id;
		this.timestamp = timestamp;
		this.readlist = new ArrayList<>();
		this.writelist = new ArrayList<>();
		this.callArgs = new ArrayList<String>();
		this.callArgs.addAll(args);
		this.endorserList = new ArrayList<>();
		this.chaincode = chaincode;
	}

	public String getTxn_id() {
		return txn_id;
	}

	public void setTxn_id(String txn_id) {
		this.txn_id = txn_id;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public List<String> getCallArgs() {
		return callArgs;
	}

	public void setCallArgs(List<String> callArgs) {
		this.callArgs = callArgs;
	}
	
	public List<String> getEndorserList() {
		return endorserList;
	}

	public void setEndorserList(List<String> endorserList) {
		this.endorserList = endorserList;
	}
	public List<TxnRead> getReadlist() {
		return readlist;
	}

	public void setReadlist(List<TxnRead> readlist) {
		this.readlist = readlist;
	}

	public List<TxnWrite> getWritelist() {
		return writelist;
	}

	public void setWritelist(List<TxnWrite> writelist) {
		this.writelist = writelist;
	}

	@Override
	public String toString() {
		return "TxnInfo [txn_id=" + txn_id + ", timestamp=" + timestamp + ", readlist=" + readlist + ", writelist="
				+ writelist + ", callArgs=" + callArgs + ", endorserList=" + endorserList + ", chaincode=" + chaincode
				+ ", blockHeight=" + blockHeight + "]";
	}
}
