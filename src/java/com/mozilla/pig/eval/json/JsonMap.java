/**
 * Copyright 2010 Mozilla Foundation
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
 
package com.mozilla.pig.eval.json;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.pig.EvalFunc;
import org.apache.pig.data.BagFactory;
import org.apache.pig.data.DataBag;
import org.apache.pig.data.Tuple;
import org.apache.pig.data.TupleFactory;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

public class JsonMap extends EvalFunc<Map<String, Object>> {

	public static enum ERRORS { JSONParseError, JSONMappingError };
	
	private static final BagFactory bagFactory = BagFactory.getInstance();
	private static final TupleFactory tupleFactory = TupleFactory.getInstance();
	private final ObjectMapper jsonMapper = new ObjectMapper();
	
	/**
	 * Converts List objects to DataBag to keep Pig happy
	 * @param l
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private DataBag convertListToBag(List<Object> l) {
		DataBag dbag = bagFactory.newDefaultBag();
		Tuple t = tupleFactory.newTuple();
		for (Object o : l) {
			if (o instanceof List) {
				dbag.addAll(convertListToBag((List<Object>)o));
			} else {
				t.append(o);
			}
		}
		
		if (t.size() > 0) {
			dbag.add(t);
		}
		
		return dbag;
	}
	
	@SuppressWarnings("unchecked")
	public Map<String, Object> exec(Tuple input) throws IOException {
		if (input == null || input.size() == 0) {
			return null;
		}

		try {
			reporter.progress();
			Map<String,Object> values = jsonMapper.readValue((String)input.get(0), new TypeReference<Map<String,Object>>() { });
			Map<String,Object> safeValues = new HashMap<String,Object>();
			for (Map.Entry<String, Object> entry : values.entrySet()) {
				Object v = entry.getValue();
				if (v != null && v instanceof List) {
					DataBag db = convertListToBag((List<Object>)v);
					safeValues.put(entry.getKey(), db);
				} else {
					safeValues.put(entry.getKey(), entry.getValue());
				}
			}

			return safeValues;
		} catch(JsonParseException e) {
			pigLogger.warn(this, "JSON Parse Error", ERRORS.JSONParseError);
		} catch(JsonMappingException e) {
			pigLogger.warn(this, "JSON Mapping Error", ERRORS.JSONMappingError);
		}
		
		return null;
	}

}
