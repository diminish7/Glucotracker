package com.rushdevo.glucotracker;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.DatePicker;
import android.widget.ListView;
import android.widget.TextView;

import com.rushdevo.glucotracker.data.GlucoseRecord;
import com.rushdevo.glucotracker.data.GlucotrackerData;

/**
 * @author jasonrush
 * List of glucose records
 */
public class TrackerList extends ListActivity {
	private Calendar startDateCal;
	private Calendar stopDateCal;
	
	private GlucotrackerData dataDelegate;
	private List<GlucoseRecord> records;
	private ListView listView;
	private TextView footer;
	private GlucoseRecordAdapter listAdapter;
	
	private SimpleDateFormat formatter;
	private Integer average;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.tracker_list);
		this.listView = getListView();
		this.formatter = new SimpleDateFormat("M/d/yyyy");
        this.dataDelegate = new GlucotrackerData(this);
        initializeDates();
        resetTitle();
        queryRecords();
        calculateAverage();
        footer = new TextView(this);
        setFooterText();
        footer.setGravity(Gravity.CENTER);
        listView.addFooterView(footer);
        listAdapter = new GlucoseRecordAdapter(this, android.R.layout.simple_list_item_1, this.records);
        setListAdapter(listAdapter);
	}
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	super.onCreateOptionsMenu(menu);
    	MenuInflater inflater = getMenuInflater();
    	inflater.inflate(R.menu.list_menu, menu);
    	return true;
    }
	
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch (item.getItemId()) {
    	case R.id.settings:
    		startActivity(new Intent(this, Settings.class));
    		return true;
    	case R.id.change_dates:
    		return displayDateRangeDialog();
    	case R.id.graph:
    		// TODO
    		return true;
    	}
    	return false;
    }

	/////// GETTERS AND SETTERS ////////////
	
	public void setStartDate(Calendar startDate) {
		this.startDateCal = startDate;
	}

	public Calendar getStartDate() {
		return startDateCal;
	}

	public void setStopDate(Calendar stopDate) {
		this.stopDateCal = stopDate;
	}

	public Calendar getStopDate() {
		return stopDateCal;
	}
	
	//////////// HELPERS ///////////////
	/**
	 * Display the dialog for changing the date range
	 */
	private Boolean displayDateRangeDialog() {
		AlertDialog.Builder dialog = new AlertDialog.Builder(this);
		
		dialog.setTitle(R.string.change_dates_title);
		
		LayoutInflater factory = LayoutInflater.from(this);
		final View dateChangerView = factory.inflate(R.layout.date_changer, null);
		dialog.setView(dateChangerView);
		
		final DatePicker startPicker = (DatePicker)dateChangerView.findViewById(R.id.start_date);
		startPicker.updateDate(startDateCal.get(Calendar.YEAR), startDateCal.get(Calendar.MONTH), startDateCal.get(Calendar.DAY_OF_MONTH));
		final DatePicker stopPicker = (DatePicker)dateChangerView.findViewById(R.id.stop_date);
		stopPicker.updateDate(stopDateCal.get(Calendar.YEAR), stopDateCal.get(Calendar.MONTH), stopDateCal.get(Calendar.DAY_OF_MONTH));
		
		dialog.setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            	// Update the start and stop dates
            	startDateCal.set(Calendar.YEAR, startPicker.getYear());
            	startDateCal.set(Calendar.MONTH, startPicker.getMonth());
            	startDateCal.set(Calendar.DAY_OF_MONTH, startPicker.getDayOfMonth());
            	
            	stopDateCal.set(Calendar.YEAR, stopPicker.getYear());
            	stopDateCal.set(Calendar.MONTH, stopPicker.getMonth());
            	stopDateCal.set(Calendar.DAY_OF_MONTH, stopPicker.getDayOfMonth());
            	
            	// Query records within the start and stop date
            	queryRecords();
            	// Refresh the list
            	updateList();
            	// Update the title to include the new date range
            	resetTitle();
            	// Update the average
            	calculateAverage();
            	setFooterText();
            }
        });
		
		dialog.setNegativeButton(R.string.alert_dialog_cancel, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				// Canceled, NOOP (default behavior)
			}
		});
		
		dialog.show();
		return true;
	}
	
	/**
	 * Initialize the dates for the query. Default to current month
	 */
	private void initializeDates() {
		startDateCal = Calendar.getInstance();
		startDateCal.setTime(new Date());
		startDateCal.set(Calendar.DAY_OF_MONTH, 1);
		
		stopDateCal = Calendar.getInstance();
		stopDateCal.setTime(new Date());
		stopDateCal.set(Calendar.DAY_OF_MONTH, stopDateCal.getActualMaximum(Calendar.DAY_OF_MONTH));
	}
	
	/**
	 * Query the records between this.startDate and this.endDate
	 */
	private void queryRecords() {
		this.records = dataDelegate.getGlucoseRecords(startDateCal.getTime(), stopDateCal.getTime());
	}
	
	/**
	 * Calculate the average for the set of GlucoseRecords
	 */
	private void calculateAverage() {
		if (records.isEmpty()) {
			this.average = 0;
		} else {
			Integer total = 0;
			for (GlucoseRecord record : records) {
				total += record.getBloodSugar();
			}
			this.average = Math.round(total / new Float(records.size()));
		}
	}
	
	/**
	 * Update the list view with whatever records stores right now
	 */
	private void updateList() {
    	listAdapter.setRecords(records);
    	listAdapter.notifyDataSetChanged();
	}
	
	/**
	 * Reset the title to include the dates
	 */
	private void resetTitle() {
		StringBuilder title = new StringBuilder(getResources().getString(R.string.app_name));
		if (startDateCal != null && stopDateCal != null) {
			title.append(" (");
			title.append(formatter.format(startDateCal.getTime()));
			title.append(" - ");
			title.append(formatter.format(stopDateCal.getTime()));
			title.append(")");
		}
		setTitle(title);
	}
	
	/**
	 * Set the footer text to the current average
	 */
	private void setFooterText() {
		footer.setText("Average: " + this.average);
	}
}
