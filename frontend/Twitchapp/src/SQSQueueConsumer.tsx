import { SQSClient, ReceiveMessageCommand, DeleteMessageCommand } from '@aws-sdk/client-sqs';
import { useEffect } from 'react';
import { AlertTypes } from './App';
import { getIsUserLoggedIn, getUserId, getUserIdInNumericalFormat } from './services/session';

let triggerAlertMessage: any;

const client = new SQSClient({
  region: import.meta.env.VITE_AWS_REGION,
  credentials: {
    accessKeyId: import.meta.env.VITE_AWS_ACCESS_ID,
    secretAccessKey: import.meta.env.VITE_AWS_ACCESS_KEY,
  },
});

const command = new ReceiveMessageCommand({
  QueueUrl: 'https://sqs.us-east-1.amazonaws.com/697006387197/Dummy',
  MaxNumberOfMessages: 10,
  WaitTimeSeconds: 10,
});

function isJsonString(str: any) {
    try {
        JSON.parse(str);
    } catch (e) {
        return false;
    }
    return true;
}

function receiveMessages() {
  client.send(command).then((response) => {
    if (response.Messages) {
      response.Messages.forEach((message) => {

        if(message != undefined && message.Body != undefined && isJsonString(message.Body)){
            console.log(message.Body);
            let data = JSON.parse(message.Body).Message;
            if(data != undefined && isJsonString(data)){
                data = JSON.parse(data);
                if(data.hasOwnProperty("userId") && data.hasOwnProperty("channelName") && data.hasOwnProperty("channelId") ){
                    if(getUserId() != undefined && getUserId() != false && getIsUserLoggedIn() != undefined && getIsUserLoggedIn() != false ){
                        if(data.userId === getUserIdInNumericalFormat()){
                            triggerAlertMessage("A Clip have been generated in Channel '" + data.channelName + "'", AlertTypes.INFO, 5000); 
                        }
                    }  
                }
            }
        }

        // Delete the message from the queue
        deleteMessage(message);
      });
    }
  }).catch((err) => {
    console.error(err);
  });
}

function deleteMessage(message: any) {
  const deleteCommand = new DeleteMessageCommand({
    QueueUrl: 'https://sqs.us-east-1.amazonaws.com/697006387197/Dummy',
    ReceiptHandle: message.ReceiptHandle,
  });

  client.send(deleteCommand).catch((err) => {
    debugger;
    console.error(err);
  });
}

export const SQSQueueConsumerInitate = ({showAlertMessage}: any) => {
    triggerAlertMessage = showAlertMessage;
    setInterval(receiveMessages, 1000);

  return null;
}

