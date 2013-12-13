package factory.spacesImpl;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;

import org.mozartspaces.capi3.FifoCoordinator;
import org.mozartspaces.capi3.LindaCoordinator;
import org.mozartspaces.core.Capi;
import org.mozartspaces.core.ContainerReference;
import org.mozartspaces.core.DefaultMzsCore;
import org.mozartspaces.core.Entry;
import org.mozartspaces.core.MzsConstants;
import org.mozartspaces.core.MzsCore;
import org.mozartspaces.core.MzsCoreException;
import org.mozartspaces.core.TransactionReference;

import factory.entities.GingerBread;
import factory.entities.GingerBread.State;
import factory.utils.Utils;

public class QAEmployee {

	private MzsCore core;
	private float defectRate = 0.2f; // TODO: Set at startup
	private Long id = 1L;
	
	public QAEmployee(MzsCore core, Long id) {
		this.core = core;
		this.id = id;
	}

	public void run() {
		Capi capi = new Capi(core);
		
		boolean needsCheck = false;
		
		while (true) {
			try {
				ContainerReference chargeContainer = capi.lookupContainer("charges", new URI(Server.spaceURL), MzsConstants.RequestTimeout.INFINITE, null);
				ContainerReference gingerbreadsContainer = capi.lookupContainer("gingerbreads", new URI(Server.spaceURL), MzsConstants.RequestTimeout.INFINITE, null);
				ContainerReference qaPassedContainer = capi.lookupContainer("qaPassed", new URI(Server.spaceURL), MzsConstants.RequestTimeout.INFINITE, null);
		
				TransactionReference tx = capi.createTransaction(MzsConstants.RequestTimeout.INFINITE, new URI(Server.spaceURL));
				
				// Get next charge
				System.out.println("WAIT FOR NEXT");
				Long chargeId =  (Long) capi.take(chargeContainer, FifoCoordinator.newSelector(), MzsConstants.RequestTimeout.INFINITE, tx).get(0);
				
				// Get gingerbreads of this charge
				GingerBread tpl = new GingerBread();
				tpl.setChargeId(chargeId);
				ArrayList<GingerBread> testList = capi.take(gingerbreadsContainer, LindaCoordinator.newSelector(tpl, MzsConstants.Selecting.COUNT_ALL), MzsConstants.RequestTimeout.INFINITE, tx);
				
				// Shuffle to have a random selection for testing
				Collections.shuffle(testList);
				
				// Mark as controlled or garbage based on the defectRate
				if (Math.random() < defectRate && needsCheck) {
					for (GingerBread tested : testList) {
						tested.setState(State.GARBAGE);
						System.out.println("GARBAGE");
					}
				}
				else {
					for (GingerBread tested : testList) {
						tested.setState(State.CONTROLLED);
						System.out.println("Is OK!");
					}
				}
				
				boolean eatedSkipped = false; // We need to skip the first one because we did eat it
				if (!needsCheck) {
					eatedSkipped = true; // No check means nothing eaten
				}
				for (GingerBread tested : testList) {
					tested.setQaId(id);
					if (!eatedSkipped) {
						tested.setState(State.EATEN);
					}
					capi.write(new Entry(tested), gingerbreadsContainer, MzsConstants.RequestTimeout.INFINITE, tx);
					// Ready for delivery
					System.out.println(tested);
					if (eatedSkipped && tested.getState().equals(State.CONTROLLED)) {
						capi.write(new Entry(tested.getId()), qaPassedContainer, MzsConstants.RequestTimeout.INFINITE, tx);
					}
					eatedSkipped = true;
				}
				
				try {
					capi.commitTransaction(tx);
				}
				catch (MzsCoreException e) {
					capi.rollbackTransaction(tx);
				}
				
				// Toggle needs check state
				needsCheck = !needsCheck;
				
			} catch (MzsCoreException e) {
				e.printStackTrace();
			} catch (URISyntaxException e) {
				e.printStackTrace();
			}
		}
		
	}
	
	
	public static void main(String[] args) {
		System.setProperty("mozartspaces.configurationFile", "mozartspaces-client.xml");
		MzsCore core = DefaultMzsCore.newInstanceWithoutSpace();
		new QAEmployee(core, Utils.getStartupId(args)).run();
	}

}
