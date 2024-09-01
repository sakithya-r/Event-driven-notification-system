package com.aws.sqs_producer.service;

import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class SqsService {


    @Autowired
    private SqsClient sqsClient;
    
    @Autowired
    private ObjectMapper objectMapper;
    

    public String sendMessage(Object messageObject, String queueUrl) {
        try {
            String messageBody = objectMapper.writeValueAsString(messageObject);

            SendMessageRequest sendMsgRequest = SendMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .messageBody(messageBody)
                    .build();
            sqsClient.sendMessage(sendMsgRequest);
            return "Message sent to the SQS service";

          
        } catch (Exception e) {
            e.printStackTrace();
            return "Message sending failed";
        }
    }
}
