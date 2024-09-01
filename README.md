# Producer

Producer is the producer program for adding the message to the SQS .

To run the Producer application 
## 1.Add the application AWS creds to the application.properties file

aws.accessKeyId= &lt; yourAccessKey &gt;

aws.secretAccessKey= &lt;yourSecretAccessKey&gt;

aws.sqs.queueUrl=&lt;your-SQS-url&gt;

Add the region in the Config class.

## 2.Start the springboot application in the local system

Trigger the endpoint localhost:8080/sqs/send

## 3.Use postman to trigger a producer action with sample payload

<code>{
    "eventType": "MarketingEvent",
    "message": {
        "default": "This is a default marketing message.",
        "email": "I am a email from the AWS SNS notification System ",
        "sms": "50% off your next purchase! Offer valid for a limited time. Visit our website for details."
    }
}</code>

The above payload triggers a marketing event i.e. The subscribers under the SNS topic configured in the lambda function will receive the message as configured for their protocol(SMS, Email etc.,)

<code>{
  "eventType": "TransactionalEvent",
  "SMS": {
    "phoneNumber": <your-registered-mobile>,
    "message": "Welcome to our service!"
  },
  "Email": {
    "email": <gamil>,
    "subject": "Welcome to Our Service!",
    "message": "Thank you for registering!"
  },
  "addToMarketing": false
}</code>

This event triggers a transactional event i.e. The specified mobile number and email will only receive the messages.
If the addToMarketing is enabled then the user phone number and email is added to the SNS topic for further processing of sending marketing messages,
else only messages or sent to the specified mobile number and email through SNS and SES services.


# lambda

This is the function that processes the SQS messages and invokes the required SNS/SES services.
Add the environmnet variables for source EMAIL and SNS_TOPIC_ARN.
