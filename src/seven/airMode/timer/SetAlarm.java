/*
 * Copyright (C) 2011 The Seven Open Source Project
 *	
   Email:breakprisonsona@gmail.com
    Autthor:Seven
    Company:7Bench
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package seven.airMode.timer;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.text.format.DateFormat;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TimePicker;
import android.widget.Toast;

/**
 * Manages each alarm
 */
public class SetAlarm extends PreferenceActivity
        implements TimePickerDialog.OnTimeSetListener,
        Preference.OnPreferenceChangeListener {

	private static final String TAG = " SetAlarm ";
    private EditTextPreference mLabel;
    private CheckBoxPreference mEnabledPref;
    private Preference mTimePref;
    //private AlarmPreference mAlarmPref;
    private CheckBoxPreference mVibratePref;
    private RepeatPreference mRepeatPref;

    private int     mId;
    private int     mHour;
    private int     mMinutes;
    private boolean mTimePickerCancelled;
    private Alarm   mOriginalAlarm;
    
    private String open_str;
    private String close_str;
    private String save_open_label;
    private String save_close_label;

    /**
     * Set an alarm.  Requires an Alarms.ALARM_ID to be passed in as an
     * extra. FIXME: Pass an Alarm object like every other Activity.
     */
    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        // Override the default content view.
        setContentView(R.layout.set_alarm);
        open_str = this.getString(R.string.open_str);
        close_str = this.getString(R.string.colse_str);
        save_open_label = this.getString(R.string.save_open_label);
        save_close_label = this.getString(R.string.save_close_labe);
        addPreferencesFromResource(R.xml.alarm_prefs);

        // Get each preference so we can retrieve the value later.
        mLabel = (EditTextPreference) findPreference("label");
        mLabel.setShouldDisableView(true);
        /*
        mLabel.setOnPreferenceChangeListener(
                new Preference.OnPreferenceChangeListener() {
                    public boolean onPreferenceChange(Preference p,
                            Object newValue) {
                        String val = (String) newValue;
                        // Set the summary based on the new label.
                        p.setSummary(val);
                        if (val != null && !val.equals(mLabel.getText())) {
                            // Call through to the generic listener.
                            return SetAlarm.this.onPreferenceChange(p,
                                newValue);
                        }
                        return true;
                    }
                });
                */
        mEnabledPref = (CheckBoxPreference) findPreference("enabled");
        mEnabledPref.setOnPreferenceChangeListener(
                new Preference.OnPreferenceChangeListener() {
                    public boolean onPreferenceChange(Preference p,
                            Object newValue) {
                        // Pop a toast when enabling alarms.
                        if (!mEnabledPref.isChecked()) {
                            popAlarmSetToast(SetAlarm.this, mHour, mMinutes,
                                mRepeatPref.getDaysOfWeek());
                        }
                        return SetAlarm.this.onPreferenceChange(p, newValue);
                    }
                });
        mTimePref = findPreference("time");
        //去掉singtone
        //mAlarmPref = (AlarmPreference) findPreference("alarm");
       // mAlarmPref.setOnPreferenceChangeListener(this);
        mVibratePref = (CheckBoxPreference) findPreference("vibrate");
        //mVibratePref.setOnPreferenceChangeListener(this);
        mVibratePref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			
			@Override
			public boolean onPreferenceClick(Preference preference) {
				if(mVibratePref.isChecked()){
					mLabel.setText(open_str);
		            mLabel.setSummary(open_str);
				}else{
					mLabel.setText(close_str);
		            mLabel.setSummary(close_str);
				}
				return false;
			}
		});
        mRepeatPref = (RepeatPreference) findPreference("setRepeat");
        mRepeatPref.setOnPreferenceChangeListener(this);

        Intent i = getIntent();
        mId = i.getIntExtra(Alarms.ALARM_ID, -1);
        	LogHelper.d(TAG +"In SetAlarm, alarm id = " + mId);
        Alarm alarm = null;
        if (mId == -1) {
            // No alarm id means create a new alarm.
            alarm = new Alarm();
        } else {
            /* load alarm details from database */
            alarm = Alarms.getAlarm(getContentResolver(), mId);
            // Bad alarm, bail to avoid a NPE.
            if (alarm == null) {
                finish();
                return;
            }
        }
        mOriginalAlarm = alarm;

        updatePrefs(mOriginalAlarm);

        // We have to do this to get the save/cancel buttons to highlight on
        // their own.
        getListView().setItemsCanFocus(true);

        // Attach actions to each button.
        Button b = (Button) findViewById(R.id.alarm_save);
        b.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    saveAlarm();
                    finish();
                }
        });
        final Button revert = (Button) findViewById(R.id.alarm_revert);
        revert.setEnabled(false);
        revert.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    int newId = mId;
                    updatePrefs(mOriginalAlarm);
                    // "Revert" on a newly created alarm should delete it.
                    if (mOriginalAlarm.id == -1) {
                        Alarms.deleteAlarm(SetAlarm.this, newId);
                    } else {
                        saveAlarm();
                    }
                    revert.setEnabled(false);
                }
        });
        b = (Button) findViewById(R.id.alarm_delete);
        if (mId == -1) {
            b.setEnabled(false);
        } else {
            b.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    deleteAlarm();
                }
            });
        }

        // The last thing we do is pop the time picker if this is a new alarm.
        if (mId == -1) {
            // Assume the user hit cancel
            mTimePickerCancelled = true;
            showTimePicker();
        }
    }

    // Used to post runnables asynchronously.
    private static final Handler sHandler = new Handler();

    public boolean onPreferenceChange(final Preference p, Object newValue) {
        // Asynchronously save the alarm since this method is called _before_
        // the value of the preference has changed.
        sHandler.post(new Runnable() {
            public void run() {
                // Editing any preference (except enable) enables the alarm.
                if (p != mEnabledPref) {
                    mEnabledPref.setChecked(true);
                }
                saveAlarmAndEnableRevert();
            }
        });
        return true;
    }

    private void updatePrefs(Alarm alarm) {
        mId = alarm.id;
        mEnabledPref.setChecked(alarm.enabled);
        
        mHour = alarm.hour;
        mMinutes = alarm.minutes;
        mRepeatPref.setDaysOfWeek(alarm.daysOfWeek);
        mVibratePref.setChecked(alarm.vibrate);
        if(alarm.vibrate){
        	mLabel.setText(open_str);
            mLabel.setSummary(open_str);
        	//.label = "Open AirMode.";
        }else{
        	mLabel.setText(close_str);
            mLabel.setSummary(close_str);
        	//alarm.label = "Close AirMode.";
        }
        
        // Give the alert uri to the preference.
        //mAlarmPref.setAlert(alarm.alert);
        
        
        updateTime();
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
            Preference preference) {
        if (preference == mTimePref) {
            showTimePicker();
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public void onBackPressed() {
        // In the usual case of viewing an alarm, mTimePickerCancelled is
        // initialized to false. When creating a new alarm, this value is
        // assumed true until the user changes the time.
        if (!mTimePickerCancelled) {
            saveAlarm();
        }
        finish();
    }

    private void showTimePicker() {
        new TimePickerDialog(this, this, mHour, mMinutes,
                DateFormat.is24HourFormat(this)).show();
    }

    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        // onTimeSet is called when the user clicks "Set"
        mTimePickerCancelled = false;
        mHour = hourOfDay;
        mMinutes = minute;
        updateTime();
        // If the time has been changed, enable the alarm.
        mEnabledPref.setChecked(true);
        // Save the alarm and pop a toast.
        popAlarmSetToast(this, saveAlarmAndEnableRevert());
    }

    private void updateTime() {
    	LogHelper.d(TAG +"updateTime " + mId);
        mTimePref.setSummary(Alarms.formatTime(this, mHour, mMinutes,
                mRepeatPref.getDaysOfWeek()));
    }

    private long saveAlarmAndEnableRevert() {
        // Enable "Revert" to go back to the original Alarm.
        final Button revert = (Button) findViewById(R.id.alarm_revert);
        revert.setEnabled(true);
        return saveAlarm();
    }

    private long saveAlarm() {
    	
        Alarm alarm = new Alarm();
        alarm.id = mId;
        alarm.enabled = mEnabledPref.isChecked();
        alarm.hour = mHour;
        alarm.minutes = mMinutes;
        alarm.daysOfWeek = mRepeatPref.getDaysOfWeek();
        alarm.vibrate = mVibratePref.isChecked();
        if(alarm.vibrate){
        	alarm.label = save_open_label;
        }else{
        	alarm.label = save_close_label;
        }
        //alarm.label = mLabel.getText();
       // alarm.alert = mAlarmPref.getAlert();

        long time;
        if (alarm.id == -1) {
            time = Alarms.addAlarm(this, alarm);
            // addAlarm populates the alarm with the new id. Update mId so that
            // changes to other preferences update the new alarm.
            mId = alarm.id;
        } else {
            time = Alarms.setAlarm(this, alarm);
        }
        return time;
    }

    private void deleteAlarm() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.delete_alarm))
                .setMessage(getString(R.string.delete_alarm_confirm))
                .setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface d, int w) {
                                Alarms.deleteAlarm(SetAlarm.this, mId);
                                finish();
                            }
                        })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    /**
     * Display a toast that tells the user how long until the alarm
     * goes off.  This helps prevent "am/pm" mistakes.
     */
    static void popAlarmSetToast(Context context, int hour, int minute,
                                 Alarm.DaysOfWeek daysOfWeek) {
        popAlarmSetToast(context,
                Alarms.calculateAlarm(hour, minute, daysOfWeek)
                .getTimeInMillis());
    }

    private static void popAlarmSetToast(Context context, long timeInMillis) {
        String toastText = formatToast(context, timeInMillis);
        Toast toast = Toast.makeText(context, toastText, Toast.LENGTH_LONG);
        ToastMaster.setToast(toast);
        toast.show();
    }

    /**
     * format "Alarm set for 2 days 7 hours and 53 minutes from
     * now"
     */
    static String formatToast(Context context, long timeInMillis) {
        long delta = timeInMillis - System.currentTimeMillis();
        long hours = delta / (1000 * 60 * 60);
        long minutes = delta / (1000 * 60) % 60;
        long days = hours / 24;
        hours = hours % 24;

        String daySeq = (days == 0) ? "" :
                (days == 1) ? context.getString(R.string.day) :
                context.getString(R.string.days, Long.toString(days));

        String minSeq = (minutes == 0) ? "" :
                (minutes == 1) ? context.getString(R.string.minute) :
                context.getString(R.string.minutes, Long.toString(minutes));

        String hourSeq = (hours == 0) ? "" :
                (hours == 1) ? context.getString(R.string.hour) :
                context.getString(R.string.hours, Long.toString(hours));

        boolean dispDays = days > 0;
        boolean dispHour = hours > 0;
        boolean dispMinute = minutes > 0;

        int index = (dispDays ? 1 : 0) |
                    (dispHour ? 2 : 0) |
                    (dispMinute ? 4 : 0);

        String[] formats = context.getResources().getStringArray(R.array.alarm_set);
        return String.format(formats[index], daySeq, hourSeq, minSeq);
    }
}
