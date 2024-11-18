/*
 * Copyright 2023-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.ai.baichuan.metadata;

import org.springframework.ai.baichuan.api.BaichuanApi;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.util.Assert;

/**
 * @author Geng Rong
 */
public class BaichuanUsage implements Usage {

	private final BaichuanApi.Usage usage;

	protected BaichuanUsage(BaichuanApi.Usage usage) {
		Assert.notNull(usage, "Baichaun Usage must not be null");
		this.usage = usage;
	}

	public static BaichuanUsage from(BaichuanApi.Usage usage) {
		return new BaichuanUsage(usage);
	}

	protected BaichuanApi.Usage getUsage() {
		return this.usage;
	}

	@Override
	public Long getPromptTokens() {
		return getUsage().promptTokens().longValue();
	}

	@Override
	public Long getGenerationTokens() {
		return getUsage().completionTokens().longValue();
	}

	@Override
	public Long getTotalTokens() {
		return getUsage().totalTokens().longValue();
	}

	@Override
	public String toString() {
		return getUsage().toString();
	}

}
