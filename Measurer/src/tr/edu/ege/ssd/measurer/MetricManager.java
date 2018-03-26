package tr.edu.ege.ssd.measurer;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class MetricManager {

	public static long TIME_OUT = 10000;

	private MetricMonitor monitor;
	// Counters
	private AtomicLong requested = new AtomicLong(0);
	private AtomicLong succesfull = new AtomicLong(0);
	private AtomicLong failure = new AtomicLong(0);
	// Timers
	private AtomicLong latency = new AtomicLong(0);
	private AtomicLong throughput = new AtomicLong(0);

	private long begin = 0;

	// Waiting
	private ConcurrentHashMap<String, Long> ongoing = new ConcurrentHashMap<String, Long>();

	public MetricManager(MetricMonitor monitor) {
		super();
		this.monitor = monitor;
	}

	public void requested(int hash, String uri) {
		if (begin == 0)
			this.begin = System.currentTimeMillis();
		requested.incrementAndGet();
		// put
		ongoing.put(Integer.toString(hash), new Long(System.currentTimeMillis()));
	}

	public void success(int hash) {
		succesfull.incrementAndGet();
		String key = Integer.toString(hash);
		Long start = ongoing.get(key);
		long current = System.currentTimeMillis();
		long diff = current - start.longValue();
		latency.addAndGet(diff);
		ongoing.remove(key);
		diff = ((current - begin) / 1000);
		diff = diff == 0 ? 1 : diff;
		throughput.set((long) (succesfull.get() / diff));
		updateUI();
	}

	public void updateUI() {
		long scss = succesfull.intValue() == 0 ? 1 : succesfull.get();
		this.monitor.update(ongoing.size(), succesfull.get(), failure.get(), throughput.get(), latency.get() / scss);
	}

	public void failure(int hash) {
		failure.incrementAndGet();
		ongoing.remove(Integer.toString(hash));
		updateUI();
	}

	public boolean isOngoing() {
		return this.ongoing.size() > 0;
	}

	public void clear() {
		this.begin = 0;
		this.latency.set(0);
		this.ongoing.clear();
		this.requested.set(0);
		this.succesfull.set(0);
		this.throughput.set(0);
		this.failure.set(0);
		updateUI();
	}
}