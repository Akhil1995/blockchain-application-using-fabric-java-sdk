package org.example.chaincode.invocation;

import java.util.ArrayList;
import java.util.List;

public class TxnInfo {
	private String txn_id;
	private long timestamp;
	private List<TxnRead> reads;
	private List<TxnWrite> writes;
	private List<String> callArgs;
	private List<String> endorsers;
	private String chaincode;
	private String creatorId;
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
		this.reads = new ArrayList<>();
		this.writes = new ArrayList<>();
		this.callArgs = new ArrayList<String>();
		this.callArgs.addAll(args);
		this.endorsers = new ArrayList<>();
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
		return endorsers;
	}

	public void setEndorserList(List<String> endorserList) {
		this.endorsers = endorserList;
	}
	public List<TxnRead> getReadlist() {
		return reads;
	}

	public void setReadlist(List<TxnRead> readlist) {
		this.reads = readlist;
	}

	public List<TxnWrite> getWritelist() {
		return writes;
	}

	public void setWritelist(List<TxnWrite> writelist) {
		this.writes = writelist;
	}

	@Override
	public String toString() {
		return "TxnInfo [txn_id=" + txn_id + ", timestamp=" + timestamp + ", readlist=" + reads + ", writelist="
				+ writes + ", callArgs=" + callArgs + ", endorserList=" + endorsers + ", chaincode=" + chaincode
				+ ", blockHeight=" + blockHeight + "]";
	}
	public String getCreatorId() {
		return creatorId;
	}

	public void setCreatorId(String creatorId) {
		this.creatorId = creatorId;
	}
}
