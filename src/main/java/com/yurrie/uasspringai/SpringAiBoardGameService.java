
package com.yurrie.uasspringai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Primary
public class SpringAiBoardGameService implements BoardGameService {

    private final ChatClient chatClient;

    public SpringAiBoardGameService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @Override
    public Answer askQuestion(Question question) {

        String answerText = chatClient.prompt()
                .user(question.question())
                .call()
                .content();

        return new Answer(answerText);
    }
}
