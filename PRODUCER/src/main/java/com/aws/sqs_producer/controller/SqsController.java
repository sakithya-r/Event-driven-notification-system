package com.aws.sqs_producer.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import com.aws.sqs_producer.service.SqsService;

@RestController
@RequestMapping("/sqs")
public class SqsController {
	
	
	@Value("${aws.sqs.queueUrl}")
	private String queueUrl;
	
	@Autowired
    private SqsService sqsService;

    @PostMapping("/send")
    public String sendMessage(@RequestBody Object message) {
        return sqsService.sendMessage(message, queueUrl);
    }
}
