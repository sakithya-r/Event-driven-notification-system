package lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.PublishResult;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder;
import com.amazonaws.services.simpleemail.model.Destination;
import com.amazonaws.services.simpleemail.model.Message;
import com.amazonaws.services.simpleemail.model.Body;
import com.amazonaws.services.simpleemail.model.Content;
import com.amazonaws.services.simpleemail.model.SendEmailRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class NotificationHandler implements RequestHandler<SQSEvent, String> {

    private static final AmazonSNS snsClient = AmazonSNSClientBuilder.defaultClient();
    private static final AmazonSimpleEmailService sesClient = AmazonSimpleEmailServiceClientBuilder.defaultClient();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String handleRequest(SQSEvent event, Context context) {
        for (SQSEvent.SQSMessage message : event.getRecords()) {
            try {
                JsonNode jsonNode = objectMapper.readTree(message.getBody());
                String eventType = jsonNode.path("eventType").asText(null);

                if ("TransactionalEvent".equals(eventType)) {
                    // Handle TransactionalEvent
                    handleTransactionalEvent(jsonNode, context);
                } else if ("MarketingEvent".equals(eventType)) {
                    // Handle MarketingEvent
                    handleMarketingMessage(jsonNode, context);
                } else {
                    context.getLogger().log("Unknown event type: " + eventType);
                }

            } catch (Exception e) {
                context.getLogger().log("Error processing message: " + e.getMessage());
            }
        }

        return "Processed " + event.getRecords().size() + " messages.";
    }

    private void handleTransactionalEvent(JsonNode jsonNode, Context context) {

        JsonNode smsNode = jsonNode.path("SMS");
        JsonNode emailNode = jsonNode.path("Email");
        boolean addToMarketing = jsonNode.path("addToMarketing").asBoolean(false);

        String phoneNumber = smsNode.path("phoneNumber").asText(null);
        String smsMessage = smsNode.path("message").asText("Welcome to our service!");

        if (phoneNumber != null && !phoneNumber.isEmpty()) {
            sendSMS(phoneNumber, smsMessage, context);
            if (addToMarketing) {
                subscribePhoneNumberToMarketing(phoneNumber, context);
            }
        } else {
            context.getLogger().log("No phone number provided. Skipping SMS.");
        }

        String emailAddress = emailNode.path("email").asText(null);
        String emailSubject = emailNode.path("subject").asText("Welcome to Our Service!");
        String emailBody = emailNode.path("message").asText("Thank you for registering!");

        if (emailAddress != null && isValidEmail(emailAddress)) {
            sendEmail(emailAddress, emailSubject, emailBody, context);
        } else {
            context.getLogger().log("Invalid or missing email address. Skipping email.");
        }
    }

    private void handleMarketingMessage(JsonNode jsonNode, Context context) {
        // Access the topic ARN from environment variables
        String topicArn = System.getenv("SNS_TOPIC_ARN");
        JsonNode messageNode = jsonNode.path("message");

        if (topicArn != null && !topicArn.isEmpty()) {
            if (messageNode != null && messageNode.isObject()) {
                // Send pre-formatted message to SNS topic
                sendToTopic(topicArn, messageNode.toString(), context);
            } else {
                context.getLogger().log("Message format is incorrect or missing.");
            }
        } else {
            context.getLogger().log("No topic ARN provided in environment variables.");
        }
    }

    private void sendSMS(String phoneNumber, String message, Context context) {
        try {
            PublishRequest publishRequest = new PublishRequest()
                .withMessage(message)
                .withPhoneNumber(phoneNumber);
            PublishResult publishResult = snsClient.publish(publishRequest);
            context.getLogger().log("SMS sent with Message ID: " + publishResult.getMessageId());
        } catch (Exception e) {
            context.getLogger().log("Error sending SMS: " + e.getMessage());
        }
    }

    private void sendEmail(String toAddress, String subject, String body, Context context) {
        try {
            // Fetch source email from environment variable
            String sourceEmail = System.getenv("SES_SOURCE_EMAIL");

            if (sourceEmail == null || sourceEmail.isEmpty()) {
                context.getLogger().log("Source email is not provided in environment variables.");
                return;
            }

            SendEmailRequest request = new SendEmailRequest()
                .withDestination(new Destination().withToAddresses(toAddress))
                .withMessage(new Message()
                    .withBody(new Body().withText(new Content().withCharset("UTF-8").withData(body)))
                    .withSubject(new Content().withCharset("UTF-8").withData(subject)))
                .withSource(sourceEmail); // Use source email from environment variables

            sesClient.sendEmail(request);
            context.getLogger().log("Email sent to: " + toAddress);
        } catch (Exception e) {
            context.getLogger().log("Error sending email: " + e.getMessage());
        }
    }

    private void sendToTopic(String topicArn, String message, Context context) {
        try {
            PublishRequest publishRequest = new PublishRequest()
                .withTopicArn(topicArn)
                .withMessage(message)
                .withMessageStructure("json"); // Indicates the message is a JSON structure
            PublishResult publishResult = snsClient.publish(publishRequest);
            context.getLogger().log("Message sent to topic with Message ID: " + publishResult.getMessageId());
        } catch (Exception e) {
            context.getLogger().log("Error sending message to topic: " + e.getMessage());
        }
    }

    private void subscribePhoneNumberToMarketing(String phoneNumber, Context context) {
        try {
            String marketingTopicArn = System.getenv("SNS_TOPIC_ARN");

            if (marketingTopicArn != null && !marketingTopicArn.isEmpty()) {
                
                snsClient.subscribe(marketingTopicArn, "sms", phoneNumber);
                context.getLogger().log("Phone number " + phoneNumber + " subscribed to marketing topic: " + marketingTopicArn);
            } else {
                context.getLogger().log("Marketing SNS topic ARN is not provided in environment variables.");
            }
        } catch (Exception e) {
            context.getLogger().log("Error subscribing phone number to marketing topic: " + e.getMessage());
        }
    }

    private boolean isValidEmail(String email) {
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
        return email != null && email.matches(emailRegex);
    }
}
