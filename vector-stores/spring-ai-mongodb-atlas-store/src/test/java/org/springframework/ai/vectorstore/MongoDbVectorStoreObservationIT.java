/*
 * Copyright 2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.ai.vectorstore;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.observation.conventions.SpringAiKind;
import org.springframework.ai.observation.conventions.VectorStoreProvider;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.vectorstore.observation.DefaultVectorStoreObservationConvention;
import org.springframework.ai.vectorstore.observation.VectorStoreObservationDocumentation.HighCardinalityKeyNames;
import org.springframework.ai.vectorstore.observation.VectorStoreObservationDocumentation.LowCardinalityKeyNames;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.util.MimeType;

import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.mongodb.client.MongoClient;

import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.tck.TestObservationRegistry;
import io.micrometer.observation.tck.TestObservationRegistryAssert;

/**
 * @author Christian Tzolov
 * @author Soby Chacko
 * @author Thomas Vitale
 */
@Testcontainers
@EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = ".+")
public class MongoDbVectorStoreObservationIT {

	@Container
	private static MongoDBAtlasContainer container = new MongoDBAtlasContainer();

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withUserConfiguration(Config.class)
		.withPropertyValues("spring.data.mongodb.database=springaisample",
				String.format("spring.data.mongodb.uri=" + container.getConnectionString()));

	List<Document> documents = List.of(
			new Document(getText("classpath:/test/data/spring.ai.txt"), Map.of("meta1", "meta1")),
			new Document(getText("classpath:/test/data/time.shelter.txt")),
			new Document(getText("classpath:/test/data/great.depression.txt"), Map.of("meta2", "meta2")));

	public static String getText(String uri) {
		var resource = new DefaultResourceLoader().getResource(uri);
		try {
			return resource.getContentAsString(StandardCharsets.UTF_8);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@BeforeEach
	public void beforeEach() {
		contextRunner.run(context -> {
			MongoTemplate mongoTemplate = context.getBean(MongoTemplate.class);
			mongoTemplate.getCollection("vector_store").deleteMany(new org.bson.Document());
		});
	}

	@Test
	void observationVectorStoreAddAndQueryOperations() {

		contextRunner.run(context -> {

			VectorStore vectorStore = context.getBean(VectorStore.class);

			TestObservationRegistry observationRegistry = context.getBean(TestObservationRegistry.class);

			vectorStore.add(documents);

			Thread.sleep(5000);

			TestObservationRegistryAssert.assertThat(observationRegistry)
				.doesNotHaveAnyRemainingCurrentObservation()
				.hasObservationWithNameEqualTo(DefaultVectorStoreObservationConvention.DEFAULT_NAME)
				.that()
				.hasContextualNameEqualTo("mongodb add")
				.hasLowCardinalityKeyValue(LowCardinalityKeyNames.DB_OPERATION_NAME.asString(), "add")
				.hasLowCardinalityKeyValue(LowCardinalityKeyNames.DB_SYSTEM.asString(),
						VectorStoreProvider.MONGODB.value())
				.hasLowCardinalityKeyValue(LowCardinalityKeyNames.SPRING_AI_KIND.asString(),
						SpringAiKind.VECTOR_STORE.value())
				.hasHighCardinalityKeyValue(HighCardinalityKeyNames.DB_VECTOR_QUERY_CONTENT.asString(), "none")
				.hasHighCardinalityKeyValue(HighCardinalityKeyNames.DB_VECTOR_DIMENSION_COUNT.asString(), "1536")
				.hasHighCardinalityKeyValue(HighCardinalityKeyNames.DB_COLLECTION_NAME.asString(),
						MongoDBAtlasVectorStore.DEFAULT_VECTOR_COLLECTION_NAME)
				.hasHighCardinalityKeyValue(HighCardinalityKeyNames.DB_NAMESPACE.asString(), "none")
				.hasHighCardinalityKeyValue(HighCardinalityKeyNames.DB_VECTOR_FIELD_NAME.asString(), "embedding")
				.hasHighCardinalityKeyValue(HighCardinalityKeyNames.DB_VECTOR_SIMILARITY_METRIC.asString(), "none")
				.hasHighCardinalityKeyValue(HighCardinalityKeyNames.DB_VECTOR_QUERY_TOP_K.asString(), "none")
				.hasHighCardinalityKeyValue(HighCardinalityKeyNames.DB_VECTOR_QUERY_SIMILARITY_THRESHOLD.asString(),
						"none")

				.hasBeenStarted()
				.hasBeenStopped();

			observationRegistry.clear();

			List<Document> results = vectorStore
				.similaritySearch(SearchRequest.query("What is Great Depression").withTopK(1));

			assertThat(results).isNotEmpty();

			TestObservationRegistryAssert.assertThat(observationRegistry)
				.doesNotHaveAnyRemainingCurrentObservation()
				.hasObservationWithNameEqualTo(DefaultVectorStoreObservationConvention.DEFAULT_NAME)
				.that()
				.hasContextualNameEqualTo("mongodb query")
				.hasLowCardinalityKeyValue(LowCardinalityKeyNames.DB_OPERATION_NAME.asString(), "query")
				.hasLowCardinalityKeyValue(LowCardinalityKeyNames.DB_SYSTEM.asString(),
						VectorStoreProvider.MONGODB.value())
				.hasLowCardinalityKeyValue(LowCardinalityKeyNames.SPRING_AI_KIND.asString(),
						SpringAiKind.VECTOR_STORE.value())

				.hasHighCardinalityKeyValue(HighCardinalityKeyNames.DB_VECTOR_QUERY_CONTENT.asString(),
						"What is Great Depression")
				.hasHighCardinalityKeyValue(HighCardinalityKeyNames.DB_VECTOR_DIMENSION_COUNT.asString(), "1536")
				.hasHighCardinalityKeyValue(HighCardinalityKeyNames.DB_COLLECTION_NAME.asString(),
						MongoDBAtlasVectorStore.DEFAULT_VECTOR_COLLECTION_NAME)
				.hasHighCardinalityKeyValue(HighCardinalityKeyNames.DB_NAMESPACE.asString(), "none")
				.hasHighCardinalityKeyValue(HighCardinalityKeyNames.DB_VECTOR_FIELD_NAME.asString(), "embedding")
				.hasHighCardinalityKeyValue(HighCardinalityKeyNames.DB_VECTOR_SIMILARITY_METRIC.asString(), "none")
				.hasHighCardinalityKeyValue(HighCardinalityKeyNames.DB_VECTOR_QUERY_TOP_K.asString(), "1")
				.hasHighCardinalityKeyValue(HighCardinalityKeyNames.DB_VECTOR_QUERY_SIMILARITY_THRESHOLD.asString(),
						"0.0")

				.hasBeenStarted()
				.hasBeenStopped();

		});
	}

	@SpringBootConfiguration
	@EnableAutoConfiguration
	public static class Config {

		@Bean
		public TestObservationRegistry observationRegistry() {
			return TestObservationRegistry.create();
		}

		@Bean
		public VectorStore vectorStore(MongoTemplate mongoTemplate, EmbeddingModel embeddingModel,
				ObservationRegistry observationRegistry) {
			return new MongoDBAtlasVectorStore(mongoTemplate, embeddingModel,
					MongoDBAtlasVectorStore.MongoDBVectorStoreConfig.builder()
						.withMetadataFieldsToFilter(List.of("country", "year"))
						.build(),
					true, observationRegistry, null);
		}

		@Bean
		public MongoTemplate mongoTemplate(MongoClient mongoClient, MongoCustomConversions mongoCustomConversions) {
			MongoTemplate mongoTemplate = new MongoTemplate(mongoClient, "springaisample");
			MappingMongoConverter converter = (MappingMongoConverter) mongoTemplate.getConverter();
			converter.setCustomConversions(mongoCustomConversions);
			((MongoMappingContext) converter.getMappingContext())
				.setSimpleTypeHolder(mongoCustomConversions.getSimpleTypeHolder());
			converter.afterPropertiesSet();
			return mongoTemplate;
		}

		@Bean
		public EmbeddingModel embeddingModel() {
			return new OpenAiEmbeddingModel(new OpenAiApi(System.getenv("OPENAI_API_KEY")));
		}

		@Bean
		public Converter<MimeType, String> mimeTypeToStringConverter() {
			return new Converter<MimeType, String>() {
				@Override
				public String convert(MimeType source) {
					return source.toString();
				}
			};
		}

		@Bean
		public Converter<String, MimeType> stringToMimeTypeConverter() {
			return new Converter<String, MimeType>() {
				@Override
				public MimeType convert(String source) {
					return MimeType.valueOf(source);
				}
			};
		}

		@Bean
		public MongoCustomConversions mongoCustomConversions() {
			return new MongoCustomConversions(Arrays.asList(mimeTypeToStringConverter(), stringToMimeTypeConverter()));
		}

	}

}