package factory.jmsImpl;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

import org.apache.qpid.transport.util.Logger;
import org.w3c.dom.Text;

public class JMSBakerIngredientsTopicListener implements MessageListener {

	private JMSBakerInstance baker;
	private Logger logger = Logger.get(getClass());
	
	
	public JMSBakerIngredientsTopicListener(JMSBakerInstance baker) {
		this.baker = baker;
	}

	public void onMessage(Message message) {
		this.logger.info("Received message", (Object[]) null);
		try {
			if (message instanceof TextMessage) {
				String txt = ((TextMessage) message).getText();
				if (txt != null && txt.equals("INGREDIENTS_READY")) {
					this.baker.sendRequestForIngredients();
				}
			}
		}
		catch (JMSException e) {
			e.printStackTrace();
		}
	}

}
