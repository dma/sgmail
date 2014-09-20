package com.subgraph.sgmail.internal.autoconf;

import org.xbill.DNS.*;

public class GetMX {
	

	public static String getMX(String name) {
		Record[] records;		
		try {
			records = new Lookup(name, Type.MX).run(); 
			if (records != null) {
				
			
				String mxHost = null;
				int priority = -1;

				for (int i = 0; i < records.length; i++) {
					MXRecord mx = (MXRecord) records[i];
					if (priority < 0) {
						mxHost = mx.getTarget().toString();
						priority = mx.getPriority();
					}
					if (mx.getPriority() < priority) {
						mxHost = mx.getTarget().toString();
						priority = mx.getPriority();
					}
				}
				return mxHost;
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			System.out.println("Lookup failed of "+name);
			e.printStackTrace();
		}

		return null;
	}


	
}
