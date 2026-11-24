
package com.yurrie.uasspringai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.evaluation.RelevancyEvaluator;
import org.springframework.ai.evaluation.EvaluationRequest;
import org.springframework.ai.evaluation.EvaluationResponse;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

@Service
public class SelfEvaluatingBoardGameService implements BoardGameService {

    private final ChatClient chatClient;
    private final RelevancyEvaluator evaluator;

    public SelfEvaluatingBoardGameService(
            ChatClient.Builder chatClientBuilder) {

        this.chatClient = chatClientBuilder.build();

        this.evaluator = new RelevancyEvaluator(chatClientBuilder);
    }

    @Override
    @Retryable(
            retryFor = AnswerNotRelevantException.class,
            maxAttempts = 3
    )
    public Answer askQuestion(Question question) {

        String answerText = chatClient.prompt()
                .user(question.question())
                .call()
                .content();

        evaluateRelevancy(question, answerText);

        return new Answer(answerText);
    }

    @Recover
    public Answer recover(AnswerNotRelevantException exception) {

        return new Answer(
                "I'm sorry, I wasn't able to answer the question."
        );
    }

    private void evaluateRelevancy(
            Question question,
            String answerText) {

        EvaluationRequest evaluationRequest =
                new EvaluationRequest(
                        question.question(),
                        answerText
                );

        EvaluationResponse evaluationResponse =
                evaluator.evaluate(evaluationRequest);

        if (!evaluationResponse.isPass()) {

            throw new AnswerNotRelevantException(
                    question.question(),
                    answerText
            );
        }
    }
}
