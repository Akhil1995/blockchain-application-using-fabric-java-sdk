/****************************************************** 
 *  Copyright 2018 IBM Corporation 
 *  Licensed under the Apache License, Version 2.0 (the "License"); 
 *  you may not use this file except in compliance with the License. 
 *  You may obtain a copy of the License at 
 *  http://www.apache.org/licenses/LICENSE-2.0 
 *  Unless required by applicable law or agreed to in writing, software 
 *  distributed under the License is distributed on an "AS IS" BASIS, 
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 *  See the License for the specific language governing permissions and 
 *  limitations under the License.
 */ 
package org.example.chaincode.invocation;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.example.client.CAClient;
import org.example.client.ChannelClient;
import org.example.client.FabricClient;
import org.example.config.Config;
import org.example.user.UserContext;
import org.example.util.Util;
import org.hyperledger.fabric.sdk.BlockInfo;
import org.hyperledger.fabric.sdk.BlockInfo.EnvelopeInfo;
import org.hyperledger.fabric.sdk.BlockInfo.EnvelopeType;
import org.hyperledger.fabric.sdk.BlockInfo.TransactionEnvelopeInfo;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.EventHub;
import org.hyperledger.fabric.sdk.Orderer;
import org.hyperledger.fabric.sdk.Peer;
import org.hyperledger.fabric.sdk.ProposalResponse;
import org.hyperledger.fabric.sdk.TransactionInfo;
import org.hyperledger.fabric.sdk.User;

import com.google.gson.Gson;
import com.google.protobuf.InvalidProtocolBufferException;

/**
 * 
 * @author Balaji Kadambi
 *
 */

public class QueryChaincode {

	private static final byte[] EXPECTED_EVENT_DATA = "!".getBytes(UTF_8);
	private static final String EXPECTED_EVENT_NAME = "event";
	private static Gson gson = new Gson();
	private static Map<String,TxnInfo> transactionMap = new HashMap<>();
	private static TxnInfo getTxnInfoFromBlock(BlockInfo blk,String tx_id) {
		System.out.println(blk.getBlockNumber());
		for(EnvelopeInfo en: blk.getEnvelopeInfos()) {
			if(en.getType() == EnvelopeType.TRANSACTION_ENVELOPE && en.getTransactionID().equals(tx_id)) {
				TransactionEnvelopeInfo txenin = (TransactionEnvelopeInfo) en;
				for(BlockInfo.TransactionEnvelopeInfo.TransactionActionInfo actinfo : txenin.getTransactionActionInfos()) {
					List<String> callArgs = new ArrayList<>();
					// add list of arguments used when the chaincode was called
					for(int j=0;j<actinfo.getChaincodeInputArgsCount();j++) {
						callArgs.add(new String(actinfo.getChaincodeInputArgs(j)));
					}
					TxnInfo txn_info = new TxnInfo(tx_id,txenin.getTimestamp().getTime(),callArgs);
					// get list of endorsers
					for(int j=0;j<actinfo.getEndorsementsCount();j++)
						txn_info.getEndorserList().add(actinfo.getEndorsementInfo(j));
					actinfo.getTxReadWriteSet().getNsRwsetInfos().forEach(rwset->{
						try {
							// add all reads/writes that happened to this 
							if(rwset.getRwset().getWritesCount() > 0 || rwset.getRwset().getReadsCount()>0)
								txn_info.getRwsetlist().add(rwset.getRwset());
							rwset.getRwset().getReadsList().forEach(read->{
								//System.out.println(read.getAllFields());
								System.out.println("Read set:");
								System.out.println(read.getKey());
							});
							rwset.getRwset().getWritesList().forEach(write->{
								//System.out.println(write.getAllFields());'
								System.out.println("Write set:");
								System.out.println(write.getKey());
								byte[] writeLen = new byte[write.getValue().size()];
								write.getValue().copyTo(writeLen, 0);
								System.out.println(new String(writeLen));
							});
						} catch (InvalidProtocolBufferException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					});
					transactionMap.put(tx_id, txn_info);
					return txn_info;
				}
			}
		}
		return null;
	}
	public static void main(String args[]) {
		try {
            //Util.cleanUp();
			String caUrl = Config.CA_ORG1_URL;
			CAClient caClient = new CAClient(caUrl, null);
			// Enroll Admin to Org1MSP
			String ccName = args[0];
			String fcnName = "query";
			String tx_id = args[1];
			String[] sArgs = new String[args.length-2];
			for(int i=2;i<args.length;i++)
				sArgs[i-2] = args[i];
			UserContext adminUserContext = new UserContext();
			adminUserContext.setName(Config.ADMIN);
			adminUserContext.setAffiliation(Config.ORG1);
			adminUserContext.setMspId(Config.ORG1_MSP);
			caClient.setAdminUserContext(adminUserContext);
			adminUserContext = caClient.enrollAdminUser(Config.ADMIN, Config.ADMIN_PASSWORD);
			
			FabricClient fabClient = new FabricClient(adminUserContext);
			
			ChannelClient channelClient = fabClient.createChannelClient(Config.CHANNEL_NAME);
			Channel channel = channelClient.getChannel();
			Peer peer = fabClient.getInstance().newPeer(Config.ORG1_PEER_0, Config.ORG1_PEER_0_URL);
			EventHub eventHub = fabClient.getInstance().newEventHub("eventhub01", "grpc://localhost:7053");
			Orderer orderer = fabClient.getInstance().newOrderer(Config.ORDERER_NAME, Config.ORDERER_URL);
			channel.addPeer(peer);
			channel.addEventHub(eventHub);
			channel.addOrderer(orderer);
			channel.initialize();
			Logger.getLogger(QueryChaincode.class.getName()).log(Level.INFO, "Querying chaincode ...");
			User usercontext = Util.readUserContext(Config.ORG1, sArgs[0]);
			if(usercontext==null) {
				Logger.getLogger(InvokeChaincode.class.getName()).log(Level.SEVERE,"User not registered");
				return;
			}
			BlockInfo tx_block = channel.queryBlockByTransactionID(peer, tx_id, usercontext);
			// get the keys written by this transaction...
			getTxnInfoFromBlock(tx_block, tx_id);
			TxnInfo first_txn = transactionMap.get(tx_id);
			// get a list of keys for this transaction and get their history....
			Queue<String> keyQueue = new LinkedList<>();
			// hashset for keeping track of keys that were already queried
			Set<String> keySet = new HashSet<>();
			first_txn.getRwsetlist().forEach(kvrwset->{
				kvrwset.getWritesList().forEach(write->{
					keyQueue.offer(write.getKey());
				});
			});
			while(!keyQueue.isEmpty()) {
				String key = keyQueue.poll();
				sArgs[1] = key;
				keySet.add(key);
				Collection<ProposalResponse>  responsesQuery = channelClient.queryByChainCode(ccName, fcnName, sArgs,usercontext);
				String payload = null;
				for (ProposalResponse pres : responsesQuery) {
					String stringResponse = new String(pres.getChaincodeActionResponsePayload());
					payload = stringResponse;
					Logger.getLogger(QueryChaincode.class.getName()).log(Level.INFO, stringResponse);
				}
				String[] historyKeys = gson.fromJson(payload,String[].class);
				HistoryDTO[] dtokeys = new HistoryDTO[historyKeys.length];
				BlockInfo[] txInfo = new BlockInfo[historyKeys.length];
				int iter = 0;
				boolean checkForFirstTransaction = false;
				for(String x:historyKeys) {
					dtokeys[iter] = gson.fromJson(x, HistoryDTO.class);
					// we do not need history of the key before our transaction, so query blocks only from then
					if(checkForFirstTransaction) {
						txInfo[iter] = channel.queryBlockByTransactionID(peer, dtokeys[iter].getTx_id(), usercontext);
						TxnInfo txn = getTxnInfoFromBlock(txInfo[iter], dtokeys[iter].getTx_id());
						if(txn != null) {
							txn.getRwsetlist().forEach(kvrwset->{
								kvrwset.getWritesList().forEach(kvwrite->{
									if(!keySet.contains(kvwrite.getKey())) {
										keyQueue.offer(kvwrite.getKey());
										keySet.add(kvwrite.getKey());
									}
								});
							});
						}
					}
					else {
						if(dtokeys[iter].getTx_id().equals(tx_id)) {
							checkForFirstTransaction= true;
						}
					}
					iter++;
				}
			}
			System.out.println(transactionMap.keySet());
			// build adjacency list for directed acyclic graph
//			Thread.sleep(10000);
//			String[] args1 = {"CAR1"};
//			Logger.getLogger(QueryChaincode.class.getName()).log(Level.INFO, "Querying for a car - " + args1[0]);
//			
//			Collection<ProposalResponse>  responses1Query = channelClient.queryByChainCode("fabcar", "queryCar", args1);
//			for (ProposalResponse pres : responses1Query) {
//				String stringResponse = new String(pres.getChaincodeActionResponsePayload());
//				Logger.getLogger(QueryChaincode.class.getName()).log(Level.INFO, stringResponse);
//			}		
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
