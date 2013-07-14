package org.mili.amqp.service;

import org.mili.amqp.schemas.EchoPayloadResponseT;
import org.mili.amqp.schemas.EchoPayloadT;
import org.mili.amqp.schemas.ObjectFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBElement;

public class EchoPayloadService {

    private static Logger logger = LoggerFactory.getLogger(EchoPayloadService.class);

    int hitCount;


    public JAXBElement<EchoPayloadResponseT> submitEchoPayload(EchoPayloadT echoPayload)  {

        EchoPayloadResponseT  response = new EchoPayloadResponseT();
        JAXBElement<EchoPayloadResponseT> responseT = new ObjectFactory().createEchoPayloadResponse(response);


        logger.info("hit count is " + (++hitCount));

        String responseStr = getStringWithLengthAndFilledWithCharacter(echoPayload.getPayloadSize(), 'X');

        response.setPayload(responseStr);

        try {
            Thread.sleep(echoPayload.getSleepMilliseconds());
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return responseT;
        /**
         throw new ETFaultDetails("[THROWING EXCEPTION AS DEFAULT]");
         **/
    }


    protected String getStringWithLengthAndFilledWithCharacter(int length, char charToFill) {
        char[] array = new char[length];
        int pos = 0;
        while (pos < length) {
            array[pos] = charToFill;
            pos++;
        }
        return new String(array);
    }


}
