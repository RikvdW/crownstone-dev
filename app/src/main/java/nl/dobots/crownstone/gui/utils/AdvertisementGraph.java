package nl.dobots.crownstone.gui.utils;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;
import org.achartengine.tools.PanListener;
import org.achartengine.tools.ZoomEvent;
import org.achartengine.tools.ZoomListener;

import java.util.Date;

import nl.dobots.bluenet.ble.base.structs.CrownstoneServiceData;

/**
 *
 *
 * @author Dominik Egger <dominik@dobots.nl>
 */
public class AdvertisementGraph implements ZoomListener, PanListener {
	private static final String TAG = AdvertisementGraph.class.getCanonicalName();

	public static final int STATISTICS_X_TIME = 5;

	private Activity _activity;

	private GraphicalView _graphView;

	private boolean _zoomApplied = false;
	private boolean _panApplied = false;
	private int resetCounter = -1;

	private long _maxPowerUsage;
	private long _minPowerUsage;
	private long _maxAccumulatedEnergy;
	private long _minAccumulatedEnergy;

	private int _resetCounterSeries;

	private XYMultipleSeriesRenderer _multipleSeriesRenderer;
	private XYMultipleSeriesDataset _dataSet;

	private static final PointStyle[] listOfPointStyles = new PointStyle[] { PointStyle.CIRCLE, PointStyle.POINT, PointStyle.DIAMOND,
			PointStyle.SQUARE, PointStyle.TRIANGLE, PointStyle.X };

	private static final int[] listOfSeriesColors = new int[] { 0xFF00BFFF, Color.DKGRAY, Color.GREEN, Color.YELLOW,
			Color.MAGENTA, Color.CYAN, Color.WHITE };

	private long _liveMinTime;
	private long _maxTime;
	private long _minTemp;
	private long _maxTemp;
	private int _currentSeries = 0;
	private int _temperatureSeries;
	private int _switchStateSeries;
	private int _powerUsageSeries;
	private int _accumulatedEnergySeries;
	private int _relayStateSeries;
	private int _pwmSeries;

	public AdvertisementGraph(Activity activity) {
		_activity = activity;
		createGraph();
	}

	public void setView(RelativeLayout layGraph) {
		layGraph.addView(_graphView);
	}

	public void removeView(RelativeLayout layGraph) {
		layGraph.removeView(_graphView);
	}

	public void onServiceData(String name, CrownstoneServiceData serviceData) {
//		onSwitchState(serviceData.getSwitchState());
		onPwm(serviceData.getPwm());
		onRelayState(serviceData.getRelayState());
		onTemperature(serviceData.getTemperature());
		onPowerUsage(serviceData.getPowerUsage());
		onAccumulatedEnergy(serviceData.getAccumulatedEnergy());

		String[] split = name.split("_");
		if (split.length > 1) {
			int counter = Integer.valueOf(split[split.length - 1]);
			if (resetCounter == -1) {
				resetCounter = counter;
			} else if (counter != resetCounter) {
				onResetCounterChange();
				resetCounter = counter;
			}
		}

		updateRange();
	}

	void onSwitchState(int switchState) {

		if (switchState > 100) {
			switchState = 100;
		}

		// add new point
//		TimeSeries series = (TimeSeries)_dataSet.getSeriesAt(_switchStateSeries);
//		series.add(new Date(), switchState);
		XYSeries series = _dataSet.getSeriesAt(_switchStateSeries);
		series.add(new Date().getTime(), switchState);
	}

	void onPwm(int pwm) {

		if (pwm > 100) {
			pwm = 100;
		}

		// add new point
//		TimeSeries series = (TimeSeries)_dataSet.getSeriesAt(_switchStateSeries);
//		series.add(new Date(), pwm);
		XYSeries series = _dataSet.getSeriesAt(_pwmSeries);
		series.add(new Date().getTime(), pwm);
	}

	void onRelayState(boolean relayState) {

		// add new point
//		TimeSeries series = (TimeSeries)_dataSet.getSeriesAt(_switchStateSeries);
//		series.add(new Date(), relayState);
		XYSeries series = _dataSet.getSeriesAt(_relayStateSeries);
		series.add(new Date().getTime(), relayState ? 1 : 0);
	}

	void onResetCounterChange() {

		// add new point
//		TimeSeries series = (TimeSeries)_dataSet.getSeriesAt(_switchStateSeries);
//		series.add(new Date(), relayState);
		XYSeries series = _dataSet.getSeriesAt(_resetCounterSeries);
		series.add(new Date().getTime(), 0);
		series.add(new Date().getTime(), 1);
		series.add(new Date().getTime(), 0);
	}

	void onTemperature(int temperature) {

		// add new point
//		TimeSeries series = (TimeSeries)_dataSet.getSeriesAt(_temperatureSeries);
//		series.add(new Date(), temperature);
		XYSeries series = _dataSet.getSeriesAt(_temperatureSeries);
		series.add(new Date().getTime(), temperature);

		// update y-axis range
		if (temperature > _maxTemp) {
			_maxTemp = (long)(temperature + (temperature - _minTemp) * 0.2);
		}
		if (temperature < _minTemp) {
			_minTemp = Math.min(0, (long)(temperature - (_maxTemp - temperature) * 0.2));
		}
	}

	void onPowerUsage(int powerUsage) {

		// add new point
		XYSeries series = _dataSet.getSeriesAt(_powerUsageSeries);
		series.add(new Date().getTime(), powerUsage);

		// update y-axis range
		if (powerUsage > _maxPowerUsage) {
			_maxPowerUsage = (long)(powerUsage + (powerUsage - _minPowerUsage) * 0.2);
		}
		if (powerUsage < _minPowerUsage) {
			_minPowerUsage = Math.min(0, (long)(powerUsage - (_maxPowerUsage - powerUsage) * 0.2));
		}
	}

	void onAccumulatedEnergy(int accumulatedEnergy) {

		// add new point
		XYSeries series = _dataSet.getSeriesAt(_accumulatedEnergySeries);
		series.add(new Date().getTime(), accumulatedEnergy);

		// update y-axis range
		if (accumulatedEnergy > _maxAccumulatedEnergy) {
			_maxAccumulatedEnergy = (long)(accumulatedEnergy + (accumulatedEnergy - _minAccumulatedEnergy) * 0.2);
		}
		if (accumulatedEnergy < _minAccumulatedEnergy) {
			_minAccumulatedEnergy = Math.min(0, (long)(accumulatedEnergy - (_maxAccumulatedEnergy - accumulatedEnergy) * 0.2));
		}
	}


	public void updateRange() {

		// update x-axis range
		_maxTime = new Date().getTime() + 1 * 60 * 1000;
		_liveMinTime = _maxTime - STATISTICS_X_TIME * 60 * 1000;

		// update range
		if (!(_zoomApplied || _panApplied)) {

//			_multipleSeriesRenderer.setInitialRange(new double[]{_liveMinTime, _maxTime, 0, 100}, _switchStateSeries);
//			_multipleSeriesRenderer.setRange(new double[]{_liveMinTime, _maxTime, 0, 100}, _switchStateSeries);

			_multipleSeriesRenderer.setInitialRange(new double[]{_liveMinTime, _maxTime, 0, 100}, _pwmSeries);
			_multipleSeriesRenderer.setRange(new double[]{_liveMinTime, _maxTime, 0, 100}, _pwmSeries);

			_multipleSeriesRenderer.setInitialRange(new double[]{_liveMinTime, _maxTime, 0, 1}, _relayStateSeries);
			_multipleSeriesRenderer.setRange(new double[]{_liveMinTime, _maxTime, 0, 1}, _relayStateSeries);

			_multipleSeriesRenderer.setInitialRange(new double[]{_liveMinTime, _maxTime, 0, 1}, _resetCounterSeries);
			_multipleSeriesRenderer.setRange(new double[]{_liveMinTime, _maxTime, 0, 1}, _resetCounterSeries);

			_multipleSeriesRenderer.setInitialRange(new double[]{_liveMinTime, _maxTime, _minTemp, _maxTemp}, _temperatureSeries);
			_multipleSeriesRenderer.setRange(new double[]{_liveMinTime, _maxTime, _minTemp, _maxTemp}, _temperatureSeries);

			_multipleSeriesRenderer.setInitialRange(new double[]{_liveMinTime, _maxTime, _minPowerUsage, _maxPowerUsage}, _powerUsageSeries);
			_multipleSeriesRenderer.setRange(new double[]{_liveMinTime, _maxTime, _minPowerUsage, _maxPowerUsage}, _powerUsageSeries);

			_multipleSeriesRenderer.setInitialRange(new double[]{_liveMinTime, _maxTime, _minAccumulatedEnergy, _maxAccumulatedEnergy}, _accumulatedEnergySeries);
			_multipleSeriesRenderer.setRange(new double[]{_liveMinTime, _maxTime, _minAccumulatedEnergy, _maxAccumulatedEnergy}, _accumulatedEnergySeries);
		}

		// redraw
		if (_activity != null) {
			_activity.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					_graphView.repaint();
				}
			});
		}
	}


	private void createResetCounterSeries() {

		_resetCounterSeries = _currentSeries++;

		// create time series (series with x = timestamp, y = temperature)
//		TimeSeries series = new TimeSeries("SwitchState");
		XYSeries series = new XYSeries("Resets", _resetCounterSeries);

		_dataSet.addSeries(series);

		// create new renderer for the new series
		XYSeriesRenderer renderer = new XYSeriesRenderer();
		_multipleSeriesRenderer.addSeriesRenderer(renderer);

		renderer.setPointStyle(PointStyle.POINT);
		renderer.setColor(Color.RED);
		renderer.setFillPoints(false);
		renderer.setDisplayChartValues(true);
//		renderer.setDisplayChartValuesDistance(50);
		renderer.setChartValuesTextSize(convertDpToPixel(10, _activity));
		renderer.setShowLegendItem(true);

		renderer.setLineWidth(5);

//		_multipleSeriesRenderer.setYAxisAlign(Paint.Align.LEFT, _resetCounterSeries);
//		_multipleSeriesRenderer.setYLabelsAlign(Paint.Align.LEFT, _resetCounterSeries);
//		_multipleSeriesRenderer.setAxisTitleTextSize(30f);
		_multipleSeriesRenderer.setYLabelsColor(_resetCounterSeries, Color.TRANSPARENT);
//		_multipleSeriesRenderer.setYTitle("Relay State", 1);
	}

	private void createRelayStateSeries() {

		_relayStateSeries = _currentSeries++;

		// create time series (series with x = timestamp, y = temperature)
//		TimeSeries series = new TimeSeries("SwitchState");
		XYSeries series = new XYSeries("RelayState", _relayStateSeries);

		_dataSet.addSeries(series);

		// create new renderer for the new series
		XYSeriesRenderer renderer = new XYSeriesRenderer();
		_multipleSeriesRenderer.addSeriesRenderer(renderer);

//		renderer.setPointStyle(listOfPointStyles[_relayStateSeries]);
		renderer.setPointStyle(PointStyle.POINT);
		renderer.setColor(listOfSeriesColors[_relayStateSeries]);
		renderer.setFillPoints(false);
		renderer.setDisplayChartValues(true);
//		renderer.setDisplayChartValuesDistance(50);
		renderer.setChartValuesTextSize(convertDpToPixel(10, _activity));
		renderer.setShowLegendItem(true);

//		_multipleSeriesRenderer.setYAxisAlign(Paint.Align.RIGHT, _relayStateSeries);
//		_multipleSeriesRenderer.setYLabelsAlign(Paint.Align.LEFT, _relayStateSeries);
//		_multipleSeriesRenderer.setAxisTitleTextSize(0);
		_multipleSeriesRenderer.setYLabelsColor(_relayStateSeries, Color.TRANSPARENT);
//		_multipleSeriesRenderer.setYTitle("Relay State", 1);
	}

	private void createPowerUsageSeries() {

		_powerUsageSeries = _currentSeries++;

		_minPowerUsage = 0;
		_maxPowerUsage = 100;

//		_minTime = Long.MAX_VALUE;
//		_maxTime = Long.MIN_VALUE;

		// create time series (series with x = timestamp, y = temperature)
//		TimeSeries series = new TimeSeries("Temperature");
		XYSeries series = new XYSeries("PowerUsage", _powerUsageSeries);
		_dataSet.addSeries(series);

		// create new renderer for the new series
		XYSeriesRenderer renderer = new XYSeriesRenderer();
		_multipleSeriesRenderer.addSeriesRenderer(renderer);

		renderer.setPointStyle(listOfPointStyles[_powerUsageSeries % listOfPointStyles.length]);
		renderer.setColor(listOfSeriesColors[_powerUsageSeries]);
		renderer.setFillPoints(false);
		renderer.setDisplayChartValues(false);
//		renderer.setDisplayChartValuesDistance(50);
		renderer.setChartValuesTextSize(convertDpToPixel(10, _activity));
		renderer.setShowLegendItem(true);

		_multipleSeriesRenderer.setYAxisAlign(Paint.Align.RIGHT, _powerUsageSeries);
		_multipleSeriesRenderer.setYLabelsAlign(Paint.Align.LEFT, _powerUsageSeries);
		_multipleSeriesRenderer.setAxisTitleTextSize(convertDpToPixel(10, _activity));
		_multipleSeriesRenderer.setYLabelsColor(_powerUsageSeries, listOfSeriesColors[_powerUsageSeries]);
//		_multipleSeriesRenderer.setYTitle("PowerUsage", 0);

//		_currentPointStyle++;
//		_currentSeriesColor++
	}

	private void createAccumulatedEnergySeries() {

		_accumulatedEnergySeries = _currentSeries++;

		_minAccumulatedEnergy = 0;
		_maxAccumulatedEnergy = 100;

//		_minTime = Long.MAX_VALUE;
//		_maxTime = Long.MIN_VALUE;

		// create time series (series with x = timestamp, y = temperature)
//		TimeSeries series = new TimeSeries("Temperature");
		XYSeries series = new XYSeries("AccumulatedEnergy", _accumulatedEnergySeries);
		_dataSet.addSeries(series);

		// create new renderer for the new series
		XYSeriesRenderer renderer = new XYSeriesRenderer();
		_multipleSeriesRenderer.addSeriesRenderer(renderer);

		renderer.setPointStyle(listOfPointStyles[_accumulatedEnergySeries]);
		renderer.setColor(listOfSeriesColors[_accumulatedEnergySeries]);
		renderer.setFillPoints(false);
		renderer.setDisplayChartValues(true);
//		renderer.setDisplayChartValuesDistance(50);
		renderer.setChartValuesTextSize(convertDpToPixel(10, _activity));
		renderer.setShowLegendItem(true);

		_multipleSeriesRenderer.setYAxisAlign(Paint.Align.RIGHT, _accumulatedEnergySeries);
		_multipleSeriesRenderer.setYLabelsAlign(Paint.Align.RIGHT, _accumulatedEnergySeries);
		_multipleSeriesRenderer.setAxisTitleTextSize(convertDpToPixel(10, _activity));
		_multipleSeriesRenderer.setYLabelsColor(_accumulatedEnergySeries, listOfSeriesColors[_accumulatedEnergySeries]);
//		_multipleSeriesRenderer.setYTitle("PowerUsage", 0);

//		_currentPointStyle++;
//		_currentSeriesColor++
	}

	private void createTemperatureSeries() {

		_temperatureSeries = _currentSeries++;

		_minTemp = 20;
		_maxTemp = 50;
//
//		_minTime = Long.MAX_VALUE;
//		_maxTime = Long.MIN_VALUE;

		// create time series (series with x = timestamp, y = temperature)
//		TimeSeries series = new TimeSeries("Temperature");
		XYSeries series = new XYSeries("Temperature", _temperatureSeries);

		_dataSet.addSeries(series);

		// create new renderer for the new series
		XYSeriesRenderer renderer = new XYSeriesRenderer();
		_multipleSeriesRenderer.addSeriesRenderer(renderer);

		renderer.setPointStyle(listOfPointStyles[_temperatureSeries]);
		renderer.setColor(listOfSeriesColors[_temperatureSeries]);
		renderer.setFillPoints(false);
		renderer.setDisplayChartValues(true);
//		renderer.setDisplayChartValuesDistance(50);
		renderer.setChartValuesTextSize(convertDpToPixel(10, _activity));
		renderer.setShowLegendItem(true);

		_multipleSeriesRenderer.setYAxisAlign(Paint.Align.LEFT, _temperatureSeries);
		_multipleSeriesRenderer.setYLabelsAlign(Paint.Align.RIGHT, _temperatureSeries);
		_multipleSeriesRenderer.setAxisTitleTextSize(convertDpToPixel(10, _activity));
		_multipleSeriesRenderer.setYLabelsColor(_temperatureSeries, listOfSeriesColors[_temperatureSeries]);
//		_multipleSeriesRenderer.setYTitle("Temperature [°C]", 0);
	}

	private void createSwitchStateSeries() {

		_switchStateSeries = _currentSeries++;

		// create time series (series with x = timestamp, y = temperature)
//		TimeSeries series = new TimeSeries("SwitchState");
		XYSeries series = new XYSeries("SwitchState", _switchStateSeries);

		_dataSet.addSeries(series);

		// create new renderer for the new series
		XYSeriesRenderer renderer = new XYSeriesRenderer();
		_multipleSeriesRenderer.addSeriesRenderer(renderer);

		renderer.setPointStyle(listOfPointStyles[_switchStateSeries]);
		renderer.setColor(listOfSeriesColors[_switchStateSeries]);
		renderer.setFillPoints(false);
		renderer.setDisplayChartValues(true);
//		renderer.setDisplayChartValuesDistance(50);
		renderer.setChartValuesTextSize(convertDpToPixel(10, _activity));
		renderer.setShowLegendItem(true);

		_multipleSeriesRenderer.setYAxisAlign(Paint.Align.LEFT, _switchStateSeries);
		_multipleSeriesRenderer.setYLabelsAlign(Paint.Align.LEFT, _switchStateSeries);
		_multipleSeriesRenderer.setAxisTitleTextSize(convertDpToPixel(10, _activity));
		_multipleSeriesRenderer.setYLabelsColor(_switchStateSeries, listOfSeriesColors[_switchStateSeries]);
//		_multipleSeriesRenderer.setYTitle("Switch State", 1);
	}

	private void createPwmSeries() {

		_pwmSeries = _currentSeries++;

		// create time series (series with x = timestamp, y = temperature)
//		TimeSeries series = new TimeSeries("SwitchState");
		XYSeries series = new XYSeries("Pwm", _pwmSeries);

		_dataSet.addSeries(series);

		// create new renderer for the new series
		XYSeriesRenderer renderer = new XYSeriesRenderer();
		_multipleSeriesRenderer.addSeriesRenderer(renderer);

		renderer.setPointStyle(listOfPointStyles[_pwmSeries]);
		renderer.setColor(listOfSeriesColors[_pwmSeries]);
		renderer.setFillPoints(false);
		renderer.setDisplayChartValues(true);
//		renderer.setDisplayChartValuesDistance(50);
		renderer.setChartValuesTextSize(convertDpToPixel(10, _activity));
		renderer.setShowLegendItem(true);

		_multipleSeriesRenderer.setYAxisAlign(Paint.Align.LEFT, _pwmSeries);
		_multipleSeriesRenderer.setYLabelsAlign(Paint.Align.LEFT, _pwmSeries);
		_multipleSeriesRenderer.setAxisTitleTextSize(convertDpToPixel(10, _activity));
		_multipleSeriesRenderer.setYLabelsColor(_pwmSeries, listOfSeriesColors[_pwmSeries]);
//		_multipleSeriesRenderer.setYTitle("Switch State", 1);
	}

	void createGraph() {

		// get graph renderer
		_multipleSeriesRenderer = getRenderer(6);
		_dataSet = new XYMultipleSeriesDataset();

		createTemperatureSeries();
		createResetCounterSeries();
//		createSwitchStateSeries();
		createPwmSeries();
		createRelayStateSeries();
		createPowerUsageSeries();
		createAccumulatedEnergySeries();

//		_maxTime = new Date().getTime();
//		_liveMinTime = new Date().getTime() - STATISTICS_X_TIME * 60 * 1000;

//		_multipleSeriesRenderer.setInitialRange(new double[] {_liveMinTime, _maxTime, _minTemp, _maxTemp}, _temperatureSeries);

		// create graph
		_graphView = ChartFactory.getTimeChartView(_activity, _dataSet, _multipleSeriesRenderer, null);
		_graphView.addZoomListener(this, false, true);
		_graphView.addPanListener(this);
		_graphView.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (_activity instanceof ViewPagerActivity) {
					switch (event.getAction()) {
						case MotionEvent.ACTION_DOWN: {
							((ViewPagerActivity) _activity).disableTouch(true);
							break;
						}
						case MotionEvent.ACTION_UP: {
							((ViewPagerActivity) _activity).disableTouch(false);
							break;
						}
					}
				}
				return false;
			}
		});

	}

	public static float convertDpToPixel(float dp, Context context){
		Resources resources = context.getResources();
		DisplayMetrics metrics = resources.getDisplayMetrics();
		float px = dp * ((float)metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
		return px;
	}

	public static float convertPixelsToDp(float px, Context context){
		Resources resources = context.getResources();
		DisplayMetrics metrics = resources.getDisplayMetrics();
		float dp = px / ((float)metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
		return dp;
	}

	/**
	 * Create graph renderer
	 *
	 * @return renderer object
	 */
	public XYMultipleSeriesRenderer getRenderer(int series) {
		XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer(series);

		// set minimum for y axis to 0
//		renderer.setYAxisMin(0);
//		renderer.setYAxisMax(100);

		// scrolling enabled
//		renderer.setPanEnabled(true, false);
		renderer.setPanEnabled(true, true);
		// limits for scrolling (minx, maxx, miny, maxy)
		// zoom buttons (in, out, 1:1)
		renderer.setZoomButtonsVisible(true);
		// enable zoom
//		renderer.setZoomEnabled(true, false);
		renderer.setZoomEnabled(true, true);

		// set labels text size
		renderer.setLabelsTextSize(convertDpToPixel(10, _activity));

		// hide legend
//		renderer.setShowLegend(false);
		renderer.setShowLegend(true);
		renderer.setLegendTextSize(convertDpToPixel(10, _activity));
		renderer.setLegendHeight((int)convertDpToPixel(44, _activity));

		// set margins
//		renderer.setMargins(new int[] {30, 80, 50, 70});
		renderer.setMargins(new int[] {
				(int)convertDpToPixel(10, _activity),
				(int)convertDpToPixel(27, _activity),
				(int)convertDpToPixel(17, _activity),
				(int)convertDpToPixel(24, _activity)
		});

//		renderer.setApplyBackgroundColor(true);
//		renderer.setBackgroundColor(Color.WHITE);
//		renderer.setMarginsColor(Color.WHITE);

		renderer.setMarginsColor(Color.argb(0x00, 0x01, 0x01, 0x01));
		// todo: need to get background colour of activity, transparent is not good enough
//		renderer.setMarginsColor(((ColorDrawable) _layGraph.getBackground()).getColor());

		renderer.setXAxisMin(new Date().getTime() - STATISTICS_X_TIME * 60 * 1000);

		renderer.setZoomButtonsVisible(false);
		renderer.setExternalZoomEnabled(true);

//		XYSeriesRenderer r = new XYSeriesRenderer();

		// set color
//		r.setColor(Color.GREEN);

		// set fill below line
//		r.setFillBelowLine(true);

//		renderer.addSeriesRenderer(r);
		return renderer;
	}

	public void zoomIn() {
		_zoomApplied = true;
		_graphView.zoomIn();
	}

	public void zoomOut() {
		_graphView.zoomOut();
		_zoomApplied = true;
	}
	public void resetZoom() {
		_zoomApplied = false;
		_panApplied = false;
		updateRange();
	}

	@Override
	public void zoomApplied(ZoomEvent zoomEvent) {
		_zoomApplied = true;
	}

	@Override
	public void zoomReset() {
		_zoomApplied = false;
	}

	@Override
	public void panApplied() {
		_panApplied = true;
	}

}
