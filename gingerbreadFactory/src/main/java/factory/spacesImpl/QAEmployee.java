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
	private float defectRate;
	private Long id = 1L;
	
	public QAEmployee(MzsCore core, Long id, float defectRate) {
		this.core = core;
		this.id = id;
		this.defectRate = defectRate;
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
				//System.out.println("WAIT FOR NEXT");
				Long chargeId =  (Long) capi.take(chargeContainer, FifoCoordinator.newSelector(), MzsConstants.RequestTimeout.INFINITE, tx).get(0);
				
				// Get gingerbreads of this charge
				GingerBread tpl = new GingerBread();
				tpl.setChargeId(chargeId);
				ArrayList<GingerBread> testList = capi.take(gingerbreadsContainer, LindaCoordinator.newSelector(tpl, MzsConstants.Selecting.COUNT_ALL), MzsConstants.RequestTimeout.INFINITE, tx);
				
				
				// Shuffle to have a random selection for testing
				Collections.shuffle(testList);
				
				// Mark as controlled or garbage based on the defectRate
				if (defectRate > 0.01 && Math.random() < defectRate && needsCheck) {
					for (GingerBread tested : testList) {
						tested.setState(State.GARBAGE);
						//System.out.println("GARBAGE");
					}
				}
				else {
					for (GingerBread tested : testList) {
						tested.setState(State.CONTROLLED);
						//System.out.println("Is OK!");
					}
				}
				
				boolean eatedSkipped = false; // We need to skip the first one because we did eat it
				if (!needsCheck || defectRate < 0.01) {
					eatedSkipped = true; // No check means nothing eaten
				}
				
				ArrayList<Entry> gbEntries = new ArrayList<Entry>();
				ArrayList<Entry> passed = new ArrayList<Entry>();

				for (GingerBread tested : testList) {
					tested.setQaId(id);
					if (!eatedSkipped) {
						tested.setState(State.EATEN);
					}
					gbEntries.add(new Entry(tested));

					// Ready for delivery
					if (eatedSkipped && tested.getState().equals(State.CONTROLLED)) {
						passed.add(new Entry(tested.getId()));
					}
					eatedSkipped = true;
				}
				
				capi.write(gbEntries, gingerbreadsContainer, MzsConstants.RequestTimeout.INFINITE, tx);
				capi.write(passed, qaPassedContainer, MzsConstants.RequestTimeout.INFINITE, tx);
				
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
		SpaceUtils.parseFactoryID(args, 2);
		SpaceUtils.setupBenchmark(core); // Only for benchmark
		new QAEmployee(core, Utils.getStartupId(args), Utils.getStartupDefectRate(args)).run();
	}

}
