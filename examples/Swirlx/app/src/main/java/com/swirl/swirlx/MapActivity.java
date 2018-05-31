package com.swirl.swirlx;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Rect;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MotionEventCompat;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TwoLineListItem;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolygonOptions;
import com.swirl.Region;
import com.swirl.RegionManager;
import com.swirl.Settings;
import com.swirl.Signal;
import com.swirl.Swirl;
import com.swirl.SwirlListener;

import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

public class MapActivity extends FragmentActivity implements OnMapReadyCallback {

	private static final int FILTER_ALL      = 0;
	private static final int FILTER_GEO      = 1;
	private static final int FILTER_CONTROL  = 2;

	private View mCurrentFilter;
	private int filterValue;
	private VerticalSplitView mSplitView;
	private MapView mMapView;
	private GoogleMap mMap;
	private ListView mHistoryView;
	private HistoryListAdapter mHistoryAdapter;
	private List<HistoryItem> mHistoryData;
	private HistoryItem mSelectedHistoryItem;
	private SwirlListener swirlListener;
	private Marker mSwirlLocationMarker;
	private boolean mShowedCurrent;


	@Override protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_map);

		mShowedCurrent = false;

		swirlListener = new SwirlListener() {
			@Override protected void onLocationChange(Location location) {
				MapActivity.this.onLocationChange(location);
			}
			@Override protected void onRegionsChanged(RegionManager manager, Set<Region> entered, Set<Region> exited) {
				MapActivity.this.onRegionsChanged();
			}
		};

		onFilterChange(findViewById(R.id.filter_geo));

		mMapView = (MapView)findViewById(R.id.map_view);
		mMapView.onCreate(savedInstanceState);
		mMapView.getMapAsync(this);

		mSplitView = new VerticalSplitView(
				findViewById(R.id.split_view_container),
				mMapView,
				findViewById(R.id.history_container),
				findViewById(R.id.resize_view)
		);

		mHistoryData = new ArrayList<>();
		mHistoryAdapter = new HistoryListAdapter(this, mHistoryData);
		mHistoryView = (ListView)findViewById(R.id.history_view);
		mHistoryView.setAdapter(mHistoryAdapter);
		mHistoryView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				view.setSelected(true);
				onHistoryItemSelected(position);
			}
		});
	}

	@Override protected void onResume() {
		super.onResume();
		mMapView.onResume();
		Swirl.getInstance().addListener(swirlListener);

		updateMapAndHistory();
	}

	@Override protected void onPause() {
		super.onPause();
		mMapView.onPause();
		Swirl.getInstance().removeListener(swirlListener);
	}

	@Override protected void onDestroy() {
		super.onDestroy();
		mMapView.onDestroy();
	}

	@Override protected void onSaveInstanceState(Bundle savedInstanceState) {
		super.onSaveInstanceState(savedInstanceState);
		mMapView.onSaveInstanceState(savedInstanceState);
	}

	@Override public void onLowMemory() {
		super.onLowMemory();
		mMapView.onLowMemory();
	}

	public void onShowCurrent(View v) {
		showCurrent(true);
	}

	private boolean showCurrent(boolean animated) {
		ArrayList<JSONObject> history = Settings.getArrayList(Settings.LOCATION_HISTORY);
		if (history.size() > 0) {
			Location location = toLocation(history.get(0));
			LatLng position = new LatLng(location.getLatitude(), location.getLongitude());
			CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(position, 15);
			if (animated) {
				mMap.animateCamera(cameraUpdate);
			} else {
				mMap.moveCamera(cameraUpdate);
			}
			return true;
		}
		return false;
	}

	public void onFilterChange(View v) {
		if (v == mCurrentFilter) {
			return;
		}

		if (mCurrentFilter != null) {
			mCurrentFilter.setActivated(false);
		}

		mCurrentFilter = v;
		mCurrentFilter.setActivated(true);

		switch (mCurrentFilter.getId()) {
			default:
			case R.id.filter_all:      filterValue = FILTER_ALL;      break;
			case R.id.filter_geo:      filterValue = FILTER_GEO;      break;
			case R.id.filter_control:  filterValue = FILTER_CONTROL;  break;
		}

		updateMapAndHistory();
	}

	private void onLocationChange(Location location) {
		updateMapAndHistory();
	}

	private void onRegionsChanged() {
		updateMapAndHistory();
	}

	private void updateMapAndHistory() {
		updateMarkersAndCircles();
		updateHistory();
	}

	// =============================================================================================
	// History
	// =============================================================================================

	private void onHistoryItemSelected(int index) {
		if (mSelectedHistoryItem != null) {
			mSelectedHistoryItem.getCircle().setVisible(false);
		}

		if (index < 0) {
			mSelectedHistoryItem = null;
			return;
		}

		mSelectedHistoryItem = mHistoryData.get(index);
		mSelectedHistoryItem.getMarker().showInfoWindow();
		mSelectedHistoryItem.getCircle().setVisible(true);

		mMap.animateCamera(CameraUpdateFactory.newLatLng(mSelectedHistoryItem.getMarker().getPosition()));
	}

	private void updateHistory() {
		if (mMap == null || mHistoryData == null) {
			return;
		}

		mHistoryData.clear();
		mSelectedHistoryItem = null;

		ArrayList<JSONObject> history = Settings.getArrayList(Settings.LOCATION_HISTORY);
		int zIndex = history.size();
		for (JSONObject locationInfo : history) {
			HistoryItem item = historyItemForLocationInfo(locationInfo);
			mHistoryData.add(item);

			item.getMarker().setZIndex(zIndex);
			item.getCircle().setZIndex(zIndex);
			zIndex--;
		}

		mHistoryAdapter.notifyDataSetChanged();

		if (!mShowedCurrent) {
			if (showCurrent(false)) {
				mShowedCurrent = true;
			}
		}
	}

	private HistoryItem historyItemForLocationInfo(JSONObject locationInfo) {
		Location location = toLocation(locationInfo);

		LatLng position = new LatLng(location.getLatitude(), location.getLongitude());
		String date = dateString(new Date(location.getTime()), "M/d/yy, h:mm:ss a");
		String type = locationInfo.optString("type");
		long minTime = locationInfo.optLong("minTime");
		float minDistance = (float)locationInfo.optDouble("minDistance");

		MarkerOptions markerOptions = new MarkerOptions().position(position);
		markerOptions.title(String.format("%s @ %s", type, date));
		markerOptions.snippet(String.format("%.3f,%.3f (min: distance=%.3f, time=%d)", location.getLatitude(), location.getLongitude(), minDistance, minTime));

		final float scale = getResources().getDisplayMetrics().density;
		CircleOptions circleOptions = new CircleOptions().center(position).radius(minDistance).strokeWidth(1.5f * scale);

		boolean first = (mHistoryData.size() == 0);

		if (first) {
			markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
			markerOptions.draggable(true);

			circleOptions.strokeColor(Color.BLUE);
		} else if (type.equals("update")) {
			markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW));

			circleOptions.strokeColor(Color.argb(255, 200, 200, 0));
		} else if (type.startsWith("region")) {
			markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE));

			circleOptions.strokeColor(Color.argb(255, 255, 165, 0));
		} else {
			markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ROSE));

			circleOptions.strokeColor(Color.argb(255, 255, 0, 127));
		}

		Marker marker = mMap.addMarker(markerOptions);
		Circle circle = mMap.addCircle(circleOptions);
		circle.setVisible(false);

		if (first) {
			mSwirlLocationMarker = marker;
		}

		return new HistoryItem(locationInfo, location, marker, circle);
	}

	// =============================================================================================
	// Map View
	// =============================================================================================

	@Override public void onMapReady(final GoogleMap googleMap) {
		mMap = googleMap;
		mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

		if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
			mMap.setMyLocationEnabled(true);
		}

		mMap.setOnInfoWindowCloseListener(new GoogleMap.OnInfoWindowCloseListener() {
			@Override public void onInfoWindowClose(Marker marker) {
				if (mSelectedHistoryItem != null && marker.equals(mSelectedHistoryItem.getMarker())) {
					onHistoryItemSelected(-1);
				}
			}
		});
		mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
			@Override public boolean onMarkerClick(Marker marker) {
				for (int index = 0; index < mHistoryData.size(); index++) {
					if (marker.equals(mHistoryData.get(index).getMarker())) {
						onHistoryItemSelected(index);
						return true;
					}
				}
				return false;
			}
		});
		mMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
			@Override public void onMarkerDragStart(Marker marker) {}
			@Override public void onMarkerDrag(Marker marker) {}
			@Override public void onMarkerDragEnd(Marker marker) {
				if (marker.equals(mSwirlLocationMarker)) {
					onSwirlLocationPinDragEnd();
				}
			}
		});

		updateMapAndHistory();
	}

	private void onSwirlLocationPinDragEnd() {
		LatLng position = mSwirlLocationMarker.getPosition();

		Location location = new Location("manual");
		location.setTime(System.currentTimeMillis());
		location.setLatitude(position.latitude);
		location.setLongitude(position.longitude);
		location.setAccuracy(10.0f);

		Swirl.getInstance().post(location);
	}

	private void updateMarkersAndCircles() {
		if (mMap == null) {
			return;
		}

		mMap.clear();
		mSwirlLocationMarker = null;

		addOverlayForRegion(RegionManager.getInstance().getMonitoredArea());

		for (Region region : RegionManager.getInstance().getMonitoredRegions()) {
			if (filterValue == FILTER_ALL ||
					(filterValue == FILTER_GEO     && region.getLocation() != null) ||
					(filterValue == FILTER_CONTROL && region.getLocation() == null)) {
				addOverlayForRegion(region);
			}
		}
	}

	private void addOverlayForRegion(Region region) {
		if (region == null) {
			return;
		}

		LatLng center = new LatLng(region.getCenter().getLatitude(), region.getCenter().getLongitude());
		boolean entered = false;

		final float scale = getResources().getDisplayMetrics().density;
		CircleOptions circle = new CircleOptions().center(center).radius(region.getRadius()).strokeWidth(1.5f * scale);
		MarkerOptions marker = new MarkerOptions().position(center);

		if (region.equals(RegionManager.getInstance().getMonitoredArea())) {
			marker.title("monitoredArea");
			marker.snippet(region.toString());
			marker.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET));

			circle.strokeColor(Color.BLACK);
			circle.strokeWidth(2.0f * scale);
		} else {
			if (region.getLocation() != null) {
				marker.title(region.getLocation().getName());
				marker.snippet(region.getLocation().toString());
			} else {
				marker.title("control");
				marker.snippet(region.toString());
			}

			if (RegionManager.getInstance().getEnteredRegions().contains(region)) {
				entered = true;

				marker.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));

				circle.strokeColor(Color.RED);
				circle.strokeWidth(2.0f * scale);

				if (region.getPolyCount() == 0) {
					circle.fillColor(Color.argb(3, 255, 0, 0));
				} else {
					circle.fillColor(Color.TRANSPARENT);
				}
			} else if (region.getType() == Signal.TYPE_GEOFENCE) {
				marker.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));

				circle.strokeColor(Color.argb(255, 0, 180, 0));

				if (region.getPolyCount() == 0) {
					circle.fillColor(Color.argb(3, 0, 255, 0));
				} else {
					circle.fillColor(Color.TRANSPARENT);
				}
			} else {
				marker.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA));

				int magenta = Color.MAGENTA;
				circle.strokeColor(magenta);
				circle.fillColor(Color.argb(3, Color.red(magenta), Color.green(magenta), Color.blue(magenta)));
			}
		}

		if (region.getPolyCount() > 0) {
			PolygonOptions polygon = new PolygonOptions();
			polygon.strokeWidth(circle.getStrokeWidth());
			polygon.strokeColor(circle.getStrokeColor());

			for (double[] latlong : region.getPolyCoords()) {
				polygon.add(new LatLng(latlong[0], latlong[1]));
			}

			circle.strokeWidth(Math.max(scale, (circle.getStrokeWidth() - scale)));

			mMap.addPolygon(polygon);
		}

		mMap.addCircle(circle);
		mMap.addMarker(marker);
	}

	// =============================================================================================
	// Util
	// =============================================================================================

	private static HashMap<String,DateFormat> dateFormatters = new HashMap<>();
	public static DateFormat formatter(String format, Locale locale, TimeZone timezone) {
		DateFormat formatter;
		if ((formatter = dateFormatters.get(format+locale.toString())) == null) {
			formatter = new SimpleDateFormat(format, locale);
			dateFormatters.put(format+locale.toString(), formatter);
		}
		formatter.setTimeZone(timezone);
		return formatter;
	}
	public static String dateString(Date date, String format) {
		return formatter(format, Locale.US, TimeZone.getDefault()).format(date);
	}

	private static Location toLocation(String provider, double latitude, double longitude, long time, float accuracy) {
		Location location = new Location(provider);
		location.setLatitude(latitude);
		location.setLongitude(longitude);
		location.setTime(time);
		location.setAccuracy(accuracy);
		return location;
	}

	private static Location toLocation(JSONObject json) {
		return toLocation("com.swirl", json.optDouble("latitude", 0.0),
				json.optDouble("longitude", 0.0),
				json.optLong("timestamp", 0),
				(float)json.optDouble("accuracy", 0.0));
	}


	class VerticalSplitView implements View.OnTouchListener {
		private View mContainerView;
		private View mTopView;
		private View mBottomView;
		private View mResizeView;

		private int mDownY;
		private int mDownTopHeight;
		private int mDownBottomHeight;

		public VerticalSplitView(View container, View topView, View bottomView, View resizeView) {
			mContainerView = container;
			mTopView = topView;
			mBottomView = bottomView;
			mResizeView = resizeView;

			mResizeView.setOnTouchListener(this);

			mContainerView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
				@Override public void onGlobalLayout() {
					mContainerView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
					onFirstLayout();
				}
			});
		}

		private void onFirstLayout() {
			int resizeHeight = mResizeView.getHeight();
			int remainingHeight = (mContainerView.getHeight() - resizeHeight);
			int newTopHeight = (int)(0.6 * remainingHeight);
			int newBottomHeight = (remainingHeight - newTopHeight);

			mTopView.getLayoutParams().height = newTopHeight;
			mBottomView.getLayoutParams().height = newBottomHeight;
		}

		@Override public boolean onTouch(View v, MotionEvent event) {
			switch (MotionEventCompat.getActionMasked(event)) {
				case MotionEvent.ACTION_DOWN:  return onTouchDown(event);
				case MotionEvent.ACTION_MOVE:  return onTouchMove(event);
			}

			return true;
		}

		private boolean onTouchDown(MotionEvent ev) {
			int x = (int)ev.getRawX();
			int y = (int)ev.getRawY();

			Rect rect = new Rect();
			mResizeView.getGlobalVisibleRect(rect);

			if (!rect.contains(x, y)) {
				return false;
			}

			mDownY = y;
			mDownTopHeight = mTopView.getHeight();
			mDownBottomHeight = mBottomView.getHeight();

			return true;
		}

		private boolean onTouchMove(MotionEvent ev) {
			int dy = ((int)ev.getRawY() - mDownY);

			if (mDownTopHeight+dy < 0) {
				dy = -mDownTopHeight;
			} else if (mDownBottomHeight-dy < 0) {
				dy = mDownBottomHeight;
			}

			int newTopHeight = (mDownTopHeight + dy);
			int newBottomHeight = (mDownBottomHeight - dy);

			mTopView.getLayoutParams().height = newTopHeight;
			mBottomView.getLayoutParams().height = newBottomHeight;

			mContainerView.requestLayout();

			return true;
		}
	}

	class HistoryListAdapter extends BaseAdapter {
		private Context context;
		private List<HistoryItem> items;

		public HistoryListAdapter(Context context, List<HistoryItem> items) {
			this.context = context;
			this.items = items;
		}

		@Override public int getCount() {
			return items.size();
		}

		@Override public Object getItem(int position) {
			return items.get(position);
		}

		@Override public long getItemId(int position) {
			return 0;
		}

		@Override public View getView(int position, View convertView, ViewGroup parent) {
			HistoryItem item = items.get(position);

			TwoLineListItem listItem;
			if (convertView == null) {
				LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				listItem = (TwoLineListItem)inflater.inflate(android.R.layout.simple_list_item_2, null);
			} else {
				listItem = (TwoLineListItem)convertView;
			}

			final float scale = context.getResources().getDisplayMetrics().density;
			((ViewGroup.MarginLayoutParams)listItem.getText1().getLayoutParams()).topMargin = (int)(2 * scale + 0.5f);
			listItem.getText1().setHeight((int)(24 * scale + 0.5f));
			listItem.getText2().setHeight((int)(20 * scale + 0.5f));
			listItem.setMinimumHeight((int)(50 * scale + 0.5f));

			listItem.getText1().setText(item.getMarker().getTitle());
			listItem.getText2().setText(item.getMarker().getSnippet());

			return listItem;
		}
	}

	class HistoryItem {
		private JSONObject info;
		private Location location;
		private Marker marker;
		private Circle circle;

		public HistoryItem(JSONObject info, Location location, Marker marker, Circle circle) {
			this.info = info;
			this.location = location;
			this.marker = marker;
			this.circle = circle;
		}

		public JSONObject getInfo() {
			return info;
		}

		public Location getLocation() {
			return location;
		}

		public Marker getMarker() {
			return marker;
		}

		public Circle getCircle() {
			return circle;
		}
	}
}
