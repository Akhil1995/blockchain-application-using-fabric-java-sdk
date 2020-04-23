package org.example.chaincode.invocation;

import java.util.ArrayList;
import java.util.List;

import org.hyperledger.fabric.protos.ledger.rwset.Rwset;
import org.hyperledger.fabric.protos.ledger.rwset.kvrwset.KvRwset.KVRWSet;
import org.hyperledger.fabric.sdk.BlockInfo.EndorserInfo;

public class TxnInfo {
	private String txn_id;
	private long timestamp;
	private List<KVRWSet> rwsetlist;
	private List<String> callArgs;
	private List<EndorserInfo> endorserList;
	
	public TxnInfo(String txn_id, long timestamp, List<String> args) {
		this.txn_id = txn_id;
		this.timestamp = timestamp;
		this.rwsetlist = new ArrayList<>();
		this.callArgs = new ArrayList<String>();
		this.callArgs.addAll(args);
		this.endorserList = new ArrayList<>();
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

	public List<KVRWSet> getRwsetlist() {
		return rwsetlist;
	}

	public void setRwsetlist(List<KVRWSet> rwsetlist) {
		this.rwsetlist = rwsetlist;
	}

	public List<String> getCallArgs() {
		return callArgs;
	}

	public void setCallArgs(List<String> callArgs) {
		this.callArgs = callArgs;
	}
	
	public List<EndorserInfo> getEndorserList() {
		return endorserList;
	}

	public void setEndorserList(List<EndorserInfo> endorserList) {
		this.endorserList = endorserList;
	}
	@Override
	public String toString() {
		return "TxnInfo [txn_id=" + txn_id + ", timestamp=" + timestamp + ", rwsetlist=" + rwsetlist + ", callArgs="
				+ callArgs + ", toString()=" + super.toString() + "]";
	}
}
