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

package org.springframework.ai.autoconfigure.baichuan;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Geng Rong
 */
@ConfigurationProperties(BaichuanCommonProperties.CONFIG_PREFIX)
public class BaichuanCommonProperties extends BaichuanParentProperties {

	public static final String CONFIG_PREFIX = "spring.ai.baichuan";

	public static final String DEFAULT_BASE_URL = "https://api.baichuan-ai.com";

	public BaichuanCommonProperties() {
		super.setBaseUrl(DEFAULT_BASE_URL);
	}

}
