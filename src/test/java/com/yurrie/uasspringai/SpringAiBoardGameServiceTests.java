
package com.yurrie.uasspringai;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.evaluation.RelevancyEvaluator;
import org.springframework.ai.chat.evaluation.FactCheckingEvaluator;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.evaluation.EvaluationRequest;
import org.springframework.ai.evaluation.EvaluationResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class SpringAiBoardGameServiceTests {

    @Autowired
    private BoardGameService boardGameService;

    @Autowired
    private ChatClient.Builder chatClientBuilder;

    private RelevancyEvaluator relevancyEvaluator;

    private FactCheckingEvaluator factCheckingEvaluator;

    @BeforeEach
    public void setup() {

        this.relevancyEvaluator =
                RelevancyEvaluator.builder()
                        .chatClientBuilder(chatClientBuilder)
                        .promptTemplate(new PromptTemplate("""
                            Your task is to evaluate whether the response
                            directly answers the user's question.

                            Answer YES if relevant.
                            Answer NO if not relevant.

                            Question:
                            {query}

                            Response:
                            {response}

                            Context:
                            {context}

                            Answer:
                            """))
                        .build();

        this.factCheckingEvaluator =
                FactCheckingEvaluator.builder(chatClientBuilder)
                        .build();
    }

    @Test
    public void evaluateRelevancy() {

        String userText = "Why is the sky blue?";

        Question question = new Question(userText);

        Answer answer = boardGameService.askQuestion(question);

        EvaluationRequest evaluationRequest =
                new EvaluationRequest(
                        userText,
                        answer.answer()
                );

        EvaluationResponse response =
                relevancyEvaluator.evaluate(evaluationRequest);

        Assertions.assertThat(response.isPass())
                .withFailMessage(
                        "Answer is not relevant to the question."
                )
                .isTrue();
    }

    @Test
    public void evaluateFactualAccuracy() {

        String context = """
                The sky appears blue because molecules in Earth's
                atmosphere scatter shorter wavelengths of sunlight
                more strongly than longer wavelengths.
                Blue light is scattered more than red light.
                This phenomenon is called Rayleigh scattering.
                """;

        String userText = "Why is the sky blue?";

        Question question = new Question(userText);

        Answer answer = boardGameService.askQuestion(question);

        EvaluationRequest evaluationRequest =
                new EvaluationRequest(
                        context,
                        List.of(),
                        answer.answer()
                );

        EvaluationResponse response =
                factCheckingEvaluator.evaluate(evaluationRequest);

        Assertions.assertThat(response.isPass())
                .withFailMessage("""
                        The answer:
                        %s
                        is not considered factually correct.
                        """, answer.answer())
                .isTrue();
    }
}
