package tr.edu.ege.ssd.measurer;

import java.io.IOException;

import org.jfree.ui.RefineryUtilities;

public class ClientLeader {

	private MetricManager metricManager;
	private MetricMonitor monitor;
	private Client client;

	public ClientLeader() throws Exception {
		super();
		// init ui
		monitor = new MetricMonitor("Throughput vs Latency", this);
		monitor.pack();
		RefineryUtilities.centerFrameOnScreen(monitor);
		monitor.setVisible(true);

		// init metric manager.
		metricManager = new MetricManager(monitor);

		// init clients
		initClients();
	}

	public static void main(String[] args) throws IOException, Exception {
		if (args.length > 0) {
			Paths.HOST = args[0];
		}
		new ClientLeader();
	}

	private void initClients() throws Exception, InterruptedException {
		client = new Client(metricManager);
	}

	public void forceStop() {
		client.forceStop();
		this.monitor.setStartable(true);
	}

	public void run() throws InterruptedException {
		System.out.println("Starting...");
		Thread t = new Thread(client);
		t.start();
	}
}