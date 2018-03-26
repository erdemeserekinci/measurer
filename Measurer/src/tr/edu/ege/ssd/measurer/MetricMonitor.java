package tr.edu.ege.ssd.measurer;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.StandardChartTheme;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.DialShape;
import org.jfree.chart.plot.MeterInterval;
import org.jfree.chart.plot.MeterPlot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.Range;
import org.jfree.data.general.DefaultValueDataset;
import org.jfree.data.general.ValueDataset;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.XYDataset;
import org.jfree.ui.ApplicationFrame;

/**
 * A demonstration application showing a time series chart where you can
 * dynamically add (random) data by clicking on a button.
 *
 */
public class MetricMonitor extends ApplicationFrame {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/** The time series data. */
	private TimeSeries series;

	private DefaultValueDataset gaugeDataset;

	private double TIME_OUT = MetricManager.TIME_OUT;

	private MeterPlot latencyGauge;

	private JTextField jtxtRequest;
	private JTextField jtxtSuccess;
	private JTextField jtxtFailure;
	private JTextField jtxtLatency;
	private JTextField jtxtThroughput;

	private JTextField jtxtHost;

	private JButton jbtnStartStop;

	private ClientLeader leader;

	/**
	 * Constructs a new demonstration application.
	 *
	 * @param title
	 *            the frame title.
	 */
	public MetricMonitor(final String title,ClientLeader leader) {
		super(title);
		this.leader = leader;
		StandardChartTheme theme = new StandardChartTheme("JFree/Shadow");
		Color color = new Color(173, 216, 230);
		theme.setPlotBackgroundPaint(color);
		theme.setChartBackgroundPaint(color.brighter());		
		ChartFactory.setChartTheme(theme);
		
		// line chart
		this.series = new TimeSeries("Random Data");
		final TimeSeriesCollection timeSeriesDataset = new TimeSeriesCollection(this.series);
		final JFreeChart lineChart = createLineChart(timeSeriesDataset);
		final ChartPanel linePanel = new ChartPanel(lineChart);

		this.gaugeDataset = new DefaultValueDataset(0);
		JFreeChart gaugeChart = createGaugeChart(gaugeDataset);
		ChartPanel gaugeChartPanel = new ChartPanel(gaugeChart);
		
		JPanel jpnlPureMetrics = createPureMetricsPanel();
		
		final JPanel content = new JPanel(new BorderLayout());
		content.add(linePanel, BorderLayout.NORTH);
		content.add(gaugeChartPanel, BorderLayout.WEST);
		content.add(jpnlPureMetrics,BorderLayout.SOUTH);
		setContentPane(content);		
	}

	private JPanel createPureMetricsPanel() {
		Font titleFont = new Font("Arial", Font.ITALIC, 12);
		Font textFont = new Font("Arial",Font.BOLD,14);
		
		jtxtRequest = new JTextField("0");
		TitledBorder border = new TitledBorder("Request");
		jtxtRequest.setBorder(border);
		border.setTitleFont(titleFont);
		jtxtRequest.setFont(textFont);
		jtxtRequest.setEditable(false);
		
		jtxtSuccess = new JTextField("0");
		border = new TitledBorder("Success");		
		jtxtSuccess.setBorder(border);
		border.setTitleFont(titleFont);
		jtxtSuccess.setFont(textFont);
		jtxtSuccess.setEditable(false);
		
		jtxtFailure = new JTextField("0");
		border = new TitledBorder("Failure");
		jtxtFailure.setBorder(border);
		border.setTitleFont(titleFont);
		jtxtFailure.setFont(textFont);
		jtxtFailure.setEditable(false);
		
		jtxtThroughput = new JTextField("0");
		border = new TitledBorder("Throughput");
		jtxtThroughput.setBorder(border);
		border.setTitleFont(titleFont);
		jtxtThroughput.setFont(textFont);
		jtxtThroughput.setEditable(false);
		
		jtxtLatency = new JTextField("0");
		border = new TitledBorder("Latency");
		border.setTitleFont(titleFont);
		jtxtLatency.setBorder(border);
		jtxtLatency.setFont(textFont);
		jtxtLatency.setEditable(false);
		
		JPanel jpnlController = new JPanel(new BorderLayout());
		jtxtHost = new JTextField();
		jtxtHost.setText(Paths.HOST);
		jbtnStartStop = new JButton("Start");
		jbtnStartStop.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				if(jbtnStartStop.getText().equals("Start")){
					try {
						if(!Paths.HOST.equals(jtxtHost.getText())){
							Paths.HOST = jtxtHost.getText();
							Paths.loadURIs();
						}
						leader.run();
					} catch (Exception e1) {
						e1.printStackTrace();
					}
					setStartable(false);
				}
				else{ 
					leader.forceStop();
					setStartable(true);
				}
			}
		});
		jpnlController.add(jtxtHost,BorderLayout.CENTER);
		jpnlController.add(jbtnStartStop,BorderLayout.EAST);

		JPanel jpnlPureMetrics = new JPanel(new GridLayout(6, 1));
		jpnlPureMetrics.add(jtxtRequest);
		jpnlPureMetrics.add(jtxtSuccess);
		jpnlPureMetrics.add(jtxtFailure);
		jpnlPureMetrics.add(jtxtThroughput);
		jpnlPureMetrics.add(jtxtLatency);
		jpnlPureMetrics.add(jpnlController);
		return jpnlPureMetrics;
	}

	/**
	 * Creates a sample chart.
	 * 
	 * @param dataset
	 *            the dataset.
	 * 
	 * @return A sample chart.
	 */
	private JFreeChart createLineChart(final XYDataset dataset) {
		final JFreeChart result = ChartFactory.createTimeSeriesChart("Latency per Request", "Time", "Value", dataset,
				true, true, false);
		final XYPlot plot = result.getXYPlot();
		ValueAxis axis = plot.getDomainAxis();
		axis.setAutoRange(true);
		axis.setFixedAutoRange(60000.0); // 60 seconds
		axis = plot.getRangeAxis();
		axis.setRange(0.0, 10000.0);
		return result;
	}

	// ****************************************************************************
	// * JFREECHART DEVELOPER GUIDE *
	// * The JFreeChart Developer Guide, written by David Gilbert, is available
	// *
	// * to purchase from Object Refinery Limited: *
	// * *
	// * http://www.object-refinery.com/jfreechart/guide.html *
	// * *
	// * Sales are used to provide funding for the JFreeChart project - please *
	// * support us so that we can continue developing free software. *
	// ****************************************************************************
	private void addLine(double value) {
		final Millisecond now = new Millisecond();
		Number number = this.series.getValue(now);
		if(number==null){
			this.series.addOrUpdate(now, value);
		}
	}

	private void setLatency(double value) {
		if (value < TIME_OUT) {
			this.gaugeDataset.setValue(value);
			latencyGauge.setNeedlePaint(Color.BLACK);
		} else {
			this.gaugeDataset.setValue(TIME_OUT);
			latencyGauge.setNeedlePaint(Color.RED);
		}
	}

	private JFreeChart createGaugeChart(ValueDataset valuedataset) {
		latencyGauge = new MeterPlot(valuedataset);
		latencyGauge.setRange(new Range(0.0D, TIME_OUT));
		Color low = new Color(153, 204, 255);
		Color middle = new Color(255,255,153);
		Color high = new Color(255,0,0);
		latencyGauge.addInterval(new MeterInterval("Hummingbird", new Range(0.0D, 1000D), Color.BLACK,
				new BasicStroke(2.0F), low));
		latencyGauge.addInterval(new MeterInterval("Sparrow", new Range(1000D, 2500D), Color.BLACK, new BasicStroke(2.0F),
				middle));
		latencyGauge.addInterval(new MeterInterval("Stork", new Range(2500D, TIME_OUT), Color.BLACK, new BasicStroke(2.0F),
				high));

		latencyGauge.setNeedlePaint(Color.black);
		latencyGauge.setDialBackgroundPaint(Color.white);
		latencyGauge.setDialOutlinePaint(Color.black);
		latencyGauge.setDialShape(DialShape.CHORD);
		latencyGauge.setMeterAngle(150);
		latencyGauge.setTickLabelsVisible(true);
		latencyGauge.setTickLabelFont(new Font("Arial", 1, 20));
		latencyGauge.setTickLabelPaint(Color.black);
		latencyGauge.setTickSize(5D);
		latencyGauge.setTickPaint(Color.gray);
		latencyGauge.setValuePaint(Color.black);
		latencyGauge.setValueFont(new Font("Arial", 1, 20));
		JFreeChart jfreechart = new JFreeChart("Mean Latency", JFreeChart.DEFAULT_TITLE_FONT, latencyGauge, true);
		return jfreechart;
	}
	
	public synchronized void update(long request,long success,long failure, long throughput,long latency){
		this.jtxtRequest.setText(Long.toString(request));
		this.jtxtSuccess.setText(Long.toString(success));
		this.jtxtFailure.setText(Long.toString(failure));
		this.jtxtThroughput.setText(Long.toString(throughput));
		this.jtxtLatency.setText(Long.toString(latency));
		this.addLine(latency);
		this.setLatency(latency);
	}
	
	public void setStartable(boolean b) {
		if(b){
			jtxtHost.setEnabled(true);
			jbtnStartStop.setText("Start");
		}else{
			jtxtHost.setEnabled(false);
			jbtnStartStop.setText("Stop");			
		}		
	}
}