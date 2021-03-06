package com.jayway.restassured.path.json;

import com.jayway.restassured.mapper.ObjectDeserializationContext;
import com.jayway.restassured.path.json.config.JsonPathConfig;
import com.jayway.restassured.path.json.mapping.JsonPathObjectDeserializer;
import com.jayway.restassured.path.json.support.Greeting;
import com.jayway.restassured.path.json.support.Winner;
import org.apache.commons.lang3.StringUtils;
import org.hamcrest.Matchers;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.jayway.restassured.path.json.JsonPath.from;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class JsonPathObjectDeserializationTest {

    private static final String GREETING = "{ \"greeting\" : { \n" +
            "                \"firstName\" : \"John\", \n" +
            "                \"lastName\" : \"Doe\" \n" +
            "               }\n" +
            "}";

    private static final String LOTTO = "{\"lotto\":{\n" +
            "    \"lottoId\":5,\n" +
            "    \"winning-numbers\":[2,45,34,23,7,5,3],\n" +
            "    \"winners\":[{\n" +
            "      \"winnerId\":23,\n" +
            "      \"numbers\":[2,45,34,23,3,5]\n" +
            "    },{\n" +
            "      \"winnerId\":54,\n" +
            "      \"numbers\":[52,3,12,11,18,22]\n" +
            "    }]\n" +
            "  }\n" +
            "}";

    @Test public void
    json_path_supports_custom_deserializer() {
        // Given
        final AtomicBoolean customDeserializerUsed = new AtomicBoolean(false);

        final JsonPath jsonPath = new JsonPath(GREETING).using(new JsonPathConfig().defaultObjectDeserializer(new JsonPathObjectDeserializer() {
            public <T> T deserialize(ObjectDeserializationContext ctx) {
                customDeserializerUsed.set(true);
                final String json = ctx.getDataToDeserialize().asString();
                final Greeting greeting = new Greeting();
                greeting.setFirstName(StringUtils.substringBetween(json, "\"firstName\":\"", "\""));
                greeting.setLastName(StringUtils.substringBetween(json, "\"lastName\":\"", "\""));
                return (T) greeting;
            }
        }));

        // When
        final Greeting greeting = jsonPath.getObject("", Greeting.class);

        // Then
        assertThat(greeting.getFirstName(), equalTo("John"));
        assertThat(greeting.getLastName(), equalTo("Doe"));
        assertThat(customDeserializerUsed.get(), is(true));
    }

    @Test public void
    json_path_supports_custom_deserializer_with_static_configuration() {
        // Given
        final AtomicBoolean customDeserializerUsed = new AtomicBoolean(false);

        JsonPath.config = new JsonPathConfig().defaultObjectDeserializer(new JsonPathObjectDeserializer() {
            public <T> T deserialize(ObjectDeserializationContext ctx) {
                customDeserializerUsed.set(true);
                final String json = ctx.getDataToDeserialize().asString();
                final Greeting greeting = new Greeting();
                greeting.setFirstName(StringUtils.substringBetween(json, "\"firstName\":\"", "\""));
                greeting.setLastName(StringUtils.substringBetween(json, "\"lastName\":\"", "\""));
                return (T) greeting;
            }
        });

        final JsonPath jsonPath = new JsonPath(GREETING);

        // When
        try {
            final Greeting greeting = jsonPath.getObject("", Greeting.class);

            // Then
            assertThat(greeting.getFirstName(), equalTo("John"));
            assertThat(greeting.getLastName(), equalTo("Doe"));
            assertThat(customDeserializerUsed.get(), is(true));
        } finally {
            JsonPath.reset();
        }
    }

    @Test public void
    extracting_first_lotto_winner_to_java_object() {
        // When
        final Winner winner = from(LOTTO).getObject("lotto.winners[0]", Winner.class);

        // Then
        assertThat(winner.getNumbers(), hasItems(2,45,34,23,3,5));
        assertThat(winner.getWinnerId(), is(23));
    }

    @Test public void
    getting_numbers_greater_than_ten_for_lotto_winner_with_id_equal_to_23() {
        // When
        List<Integer> numbers = from(LOTTO).getList("lotto.winners.find { it.winnerId == 23 }.numbers.findAll { it > 10 }", Integer.class);

        // Then
        assertThat(numbers, hasItems(45, 34, 23));
        assertThat(numbers, hasSize(3));
    }
}
