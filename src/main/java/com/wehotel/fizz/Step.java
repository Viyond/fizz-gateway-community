/*
 *  Copyright (C) 2020 the original author or authors.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.wehotel.fizz;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import com.alibaba.fastjson.JSON;
import com.wehotel.fizz.input.Input;
import com.wehotel.fizz.input.InputConfig;
import com.wehotel.fizz.input.InputContext;
import com.wehotel.fizz.input.InputFactory;
import com.wehotel.fizz.input.InputType;

import reactor.core.publisher.Mono;

/**
 * 
 * @author linwaiwai
 * @author francis
 *
 */
public class Step {

	private String name; 
	
	// 是否在执行完当前step就返回
	private boolean stop; 
	
	private Map<String, Object> dataMapping;
	
	private Map<String, InputConfig> requestConfigs = new HashMap<String, InputConfig>();

	public static class Builder {
		public Step read(Map<String, Object> config) {
			Step step = new Step();
			List<Map> requests= (List<Map>) config.get("requests");
			for(Map requestConfig: requests) {
				InputConfig inputConfig = InputFactory.createInputConfig(requestConfig);
				step.addRequestConfig((String)requestConfig.get("name"), inputConfig);
			}
			return step;
		}
	}
	
	private StepContext<String, Object> stepContext;
	private StepResponse lastStepResponse = null;
	private Map<String, Input> inputs = new HashMap<String, Input>();
	public void beforeRun(StepContext<String, Object> stepContext2, StepResponse response ) {
		stepContext = stepContext2;
		lastStepResponse = response;
		StepResponse stepResponse = new StepResponse(this, null, new HashMap<String, Map<String, Object>>());
		stepContext.put(name, stepResponse);
		Map<String, InputConfig> configs = this.getRequestConfigs();
		for(String configName :configs.keySet()) {
			InputConfig inputConfig = configs.get(configName);
			InputType type = inputConfig.getType();
			Input input = InputFactory.createInput(type.toString());
			input.setConfig(inputConfig);
			input.setName(configName);
			input.setStepResponse(stepResponse);
			InputContext context = new InputContext(stepContext, lastStepResponse);
			input.beforeRun(context); 
			inputs.put(input.getName(), input);
		}
	}

	public List<Mono> run() {
		List<Mono> monos = new ArrayList<Mono>();  
		for(String name :inputs.keySet()) {
			Input input = inputs.get(name);
			if (input.needRun(stepContext)) {
				Mono<Map> singleMono = input.run();
				monos.add(singleMono);
			}
		}
		return monos;	
	}

	public void afeterRun() {
		
	}
	
	public InputConfig addRequestConfig(String name,  InputConfig requestConfig) {
		return requestConfigs.put(name, requestConfig);
	}
 

	public Map<String, InputConfig> getRequestConfigs() {
		return requestConfigs;
	}


	public String getName() {
		if (name == null) {
			return name = "step" + (int)(Math.random()*100);
		}
		return name;
	}


	public void setName(String name) {
		this.name = name;
	}

	public boolean isStop() {
		return stop;
	}

	public void setStop(boolean stop) {
		this.stop = stop;
	}

	public Map<String, Object> getDataMapping() {
		return dataMapping;
	}

	public void setDataMapping(Map<String, Object> dataMapping) {
		this.dataMapping = dataMapping;
	}


}

