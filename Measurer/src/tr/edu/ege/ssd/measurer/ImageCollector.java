package tr.edu.ege.ssd.measurer;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ImageCollector implements Runnable {

	static String PATH = "e:/images/wallpaper/";
	private BlockingQueue<String> queue;
	private boolean stop = false;
	
	public ImageCollector(BlockingQueue<String> queue) {
		super();
		this.queue = queue;
	}

	@Override
	public void run() {
		parseImageURIs();
		stop  = true;
	}

	private void parseImageURIs() {
		BufferedReader br = null;
		try {
			System.out.println(String.format("Parsing images from file %s", PATH));
			br = new BufferedReader(new FileReader(PATH+"ress.txt"));
			// read for passing headers
			br.readLine();
			String line = null;
			while ((line = br.readLine()) != null) {
				String[] split = line.split("##");
				String uri = split[0];
				boolean offer = false;
				while(!offer){					
					offer = this.queue.offer(uri);
					if(!offer)
						Thread.sleep(5000);
				}
				System.out.println(String.format("\t%s put in queue ", uri));
			}
			br.close();
			System.out.println(String.format("%d images parsed", this.queue.size()));
		} catch (Exception e) {
			e.printStackTrace();
			try {
				br.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}

	public boolean isStop() {
		return stop;
	}

	public void setStop(boolean stop) {
		this.stop = stop;
	}

	public static void main(String[] args) throws InterruptedException {
		ArrayBlockingQueue<String> queue = new ArrayBlockingQueue<String>(100,true);
		ScheduledExecutorService service = Executors.newScheduledThreadPool(5);
		ImageCollector collector = new ImageCollector(queue);
		service.scheduleAtFixedRate(collector, 1, 1, TimeUnit.SECONDS);
//		queue.put("http://wallpaperswide.com/download/fantasy_32-wallpaper-1920x1080.jpg");
		String uri = null;
		while ((uri = queue.take()) != null && !collector.isStop()) {
			service.schedule(new ImageRetriever(uri),1,TimeUnit.SECONDS);			
		}
		System.out.println("Finished.!!");
	}
}

class ImageRetriever implements Callable<Boolean> {
	private String uri;
	private String filename;

	public ImageRetriever(String uri) {
		super();
		this.uri = uri;
		this.filename = ImageCollector.PATH + uri.substring(uri.lastIndexOf("/") + 1);
	}

	@Override
	public Boolean call() throws Exception {
		Boolean result = Boolean.FALSE;
		BufferedInputStream in = null;
		FileOutputStream fout = null;
		File targetFile = new File(filename);
		if(targetFile.exists()){			
			System.out.println(String.format("File already exists %s" , filename));
			return Boolean.TRUE;
		}
		try {
			in = new BufferedInputStream(new URL(uri).openStream());
			fout = new FileOutputStream(filename);

			final byte data[] = new byte[1024];
			int count;
			while ((count = in.read(data, 0, 1024)) != -1) {
				fout.write(data, 0, count);
			}
			System.out.println(String.format("wrote to file:%s", filename));
			result = Boolean.TRUE;			
		} catch (Exception e) {
			FileWriter errors = new FileWriter("e:/images/wallpaper/errors.txt",true);
			errors.write(uri + System.lineSeparator());
			errors.close();
			System.err.println("Cannot retireve " + uri);
			System.err.println(e.getMessage());
		}finally {
			if (in != null) {
				in.close();
			}
			if (fout != null) {
				fout.close();
			}
		}
		return result;
	}
}