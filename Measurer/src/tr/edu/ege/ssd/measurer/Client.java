package tr.edu.ege.ssd.measurer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Phaser;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Request.CommitListener;
import org.eclipse.jetty.client.api.Response.CompleteListener;
import org.eclipse.jetty.client.api.Result;

public class Client implements Runnable, CompleteListener, CommitListener {

	private static final int CONCURRENT_REQUEST = 10;
	public static boolean log = true;
	ScheduledExecutorService executorService = Executors.newScheduledThreadPool(CONCURRENT_REQUEST);
	Phaser phaser = new Phaser();

	public enum UNIT {
		BIG, NORMAL, LIST, ICON
	}

	private HttpClient httpClient;
	private MetricManager metricManager;
	private UNIT type = UNIT.BIG;

	public Client(MetricManager metricManger) throws Exception {
		super();
		this.metricManager = metricManger;
		if (log)
			System.out.println("Client created");
		// init httpclient
		prepareHTTPClient();
	}

	@Override
	public void onComplete(Result result) {
		if (!result.isFailed()) {
			if (result.getResponse().getStatus() == 200) {
				if (log) {
					metricManager.success(result.getRequest().hashCode());
					System.out.println("Success: " + result.getRequest().getURI());
				}
				return;
			}
			metricManager.failure(result.getRequest().hashCode());
			if (log)
				System.err.println("Failure: " + result.getRequest().getURI());
		}
	}

	@Override
	public void onCommit(Request request) {
		metricManager.requested(request.hashCode(), request.getURI().toString());
	}

	@Override
	public void run() {
		clearSession();
		try {
			System.out.println("Preparing lists");
			List<String> loadURIs = Paths.loadURIs();
			// int from = (int) Paths.randomWithRange(0, 1000);
			// List<String> subList = loadURIs.subList(from, from + 1000);
			// System.out.println("Lists prepared");
			// visit(subList);
			visit(loadURIs);
			phaser.arriveAndAwaitAdvance();
			metricManager.updateUI();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void clearSession() {
		this.phaser = new Phaser();
		if (httpClient.isStopped()) {
			try {
				this.executorService = Executors.newScheduledThreadPool(CONCURRENT_REQUEST);
				httpClient.start();
				Thread.sleep(1000);
				this.metricManager.clear();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void visit(List<String> subList) {
		subList.forEach(uri -> request(prepareURI(uri, UNIT.LIST, false)));
		subList.forEach(uri -> request(prepareURI(uri, UNIT.NORMAL, false)));
		subList.forEach(uri -> request(prepareURI(uri, UNIT.ICON, true)));
	}

	public void request(String uri) {
		final Client client = this;
		Runnable runnable = () -> {
			if (httpClient.isStopped()) {
				System.out.println(String.format("HTTP Client is stopped. Leaving requested %s", uri));
				return;
			}
			Request request = httpClient.newRequest(uri).timeout(MetricManager.TIME_OUT, TimeUnit.MILLISECONDS)
					.onRequestCommit(client);
			try {
				if (log)
					System.out.println(String.format("Requesting %s", uri));
				ContentResponse send = request.send();
				if (send.getStatus() == 200) {
					metricManager.success(send.getRequest().hashCode());
				} else {
					metricManager.failure(send.getRequest().hashCode());
					String pathname = "error.txt";
					save2File(send, pathname);
				}
			} catch (Exception e) {
				metricManager.failure(request.hashCode());
				System.err.println(e.getMessage() + " -> " + uri);
				e.printStackTrace();
			}
			phaser.arriveAndDeregister();
		};
		phaser.register();
		if (!httpClient.isStopped())
			this.executorService.schedule(runnable, 100, TimeUnit.MILLISECONDS);
	}

	private void save2File(ContentResponse send, String pathname) throws FileNotFoundException, IOException {
		if (!new File(pathname).exists()) {
			FileOutputStream fos = new FileOutputStream(new File(pathname));
			fos.write(send.getContent());
			fos.close();
		}
	}

	private void prepareHTTPClient() throws Exception {
		httpClient = new HttpClient();
		httpClient.setMaxConnectionsPerDestination(CONCURRENT_REQUEST);
		httpClient.setConnectTimeout(MetricManager.TIME_OUT);
		httpClient.setResponseBufferSize(10000000);
		httpClient.start();
		Thread.sleep(1000);
	}

	public void visitWellcomepage() {
		Paths.WELLCOME_URIs.stream().forEach(uri -> request(prepareURI(uri)));
	}

	public List<String> visitCategory() throws InterruptedException {
		List<String> randomCategoryItems = Paths.getRandomCategory();
		for (int i = 1; i < 3; i++) {
			int from = Paths.IMAGE_COUNT_ON_EACH_PAGE * (i - 1);
			int to = (Paths.IMAGE_COUNT_ON_EACH_PAGE * i) - 1;
			IntStream.range(from, to).forEach(uri -> request(prepareURI(randomCategoryItems.get(uri))));
			waitSome();
			visitImage(randomCategoryItems.subList(from, to));
		}
		return randomCategoryItems;
	}

	private void visitImage(String uri) {
		request(uri);
		//
		Paths.RECC_URIs.get(uri).stream().forEach(rec -> request(rec));
	}

	private List<String> visitImage(List<String> uris) {
		int i = (int) Paths.randomWithRange(0, uris.size());
		visitImage(uris.get(i));
		List<String> list = Paths.RECC_URIs.get(uris.get(i));
		list.forEach(reccUri -> request(prepareURI(reccUri, true)));
		return list;
	}

	private String prepareQuery(boolean isGray) {
		return prepareQuery(this.type, isGray);
	}

	private String prepareQuery(UNIT current, boolean isGray) {
		String pattern = "?width=%d&height=%d";
		String query = "";
		switch (current) {
		case BIG:
			query = String.format(pattern, 1024, 768);
			break;
		case NORMAL:
			query = String.format(pattern, 800, 600);
			break;
		case LIST:
			query = String.format(pattern, 600, 600);
			break;
		case ICON:
			query = String.format(pattern, 256, 256);
			break;
		}
		if (isGray)
			query = query + "&color=GRAY";
		return query;
	}

	private String prepareURI(String uri, boolean isGray) {
		return uri + this.prepareQuery(isGray);
	}

	private String prepareURI(String uri, UNIT unit, boolean isGray) {
		return uri + this.prepareQuery(unit, isGray);
	}

	private String prepareURI(String uri) {
		return prepareURI(uri, false);
	}

	private void waitSome() throws InterruptedException {
		Thread.sleep(MetricManager.TIME_OUT);
	}

	public void forceStop() {
		try {
			this.executorService.shutdown();
			this.httpClient.stop();
			Thread.sleep(1000);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		while (!this.executorService.isShutdown() && !this.httpClient.isStopped()) {
			System.out.println(String.format("Waiting for stop of Executor:%b and HTTPClient:%b",
					this.executorService.isShutdown(), this.httpClient.isStopped()));
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		if (log) {
			String message = String.format("Stopped: Executor:%b and HTTPClient:%b", this.executorService.isShutdown(),
					this.httpClient.isStopped());
			System.out.println(message);
		}
	}
}