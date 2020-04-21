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
package org.example.user;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.example.client.CAClient;
import org.example.config.Config;
import org.example.util.Util;

/**
 * 
 * @author Balaji Kadambi
 *
 */

public class RegisterEnrollUser {
	public static Map<String,UserContext> userContextMap = new HashMap<String,UserContext>();
	public static void main(String args[]) {
		try {
			//Util.cleanUp();
			String caUrl = Config.CA_ORG1_URL;
			CAClient caClient = new CAClient(caUrl, null);
			// Enroll Admin to Org1MSP
			UserContext adminUserContext = new UserContext();
			adminUserContext.setName(Config.ADMIN);
			adminUserContext.setAffiliation(Config.ORG1);
			adminUserContext.setMspId(Config.ORG1_MSP);
			caClient.setAdminUserContext(adminUserContext);
			adminUserContext = caClient.enrollAdminUser(Config.ADMIN, Config.ADMIN_PASSWORD);

			// Register and Enroll user to Org1MSP
			UserContext userContext = new UserContext();
			// first argument is the name
			String name = args[0];
			userContext.setName(name);
			// second argument is the affiliation
			userContext.setAffiliation(args[1]);
			// third argument is msp of organization
			userContext.setMspId(args[2]);

			String eSecret = caClient.registerUser(name, args[1]);
			Logger.getLogger(RegisterEnrollUser.class.getName()).log(Level.INFO, eSecret);
			userContext = caClient.enrollUser(userContext, eSecret);
			userContextMap.put(userContext.name, userContext);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
