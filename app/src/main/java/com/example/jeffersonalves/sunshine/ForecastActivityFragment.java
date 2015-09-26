package com.example.jeffersonalves.sunshine;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import Rest.WheaterApi;
import json.ItemCity;
import json.RootWheather;
import retrofit.Call;
import retrofit.GsonConverterFactory;
import retrofit.Response;
import retrofit.Retrofit;

/**
 * A placeholder fragment containing a simple view.
 */
public class ForecastActivityFragment extends Fragment {

    ArrayList<String> arrayList = new ArrayList<String>();
    ArrayAdapter<String> arrayAdapter;
    FetchWheatherTask fetchWheatherTask;

    final String LOG_CAT = ForecastActivityFragment.class.getSimpleName();

    public ForecastActivityFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onStart() {
        super.onStart();
        refreshItems();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.forecastfragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.action_refresh) {

            refreshItems();

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View viewRoot = inflater.inflate(R.layout.fragment_main, container, false);
        final ListView listView = (ListView) viewRoot.findViewById(R.id.listview_forecast);

        arrayAdapter = new ArrayAdapter<String>(
                getActivity(),
                R.layout.list_item_forecast,
                R.id.list_item_forecast_textview,
                arrayList
        );

        listView.setAdapter(arrayAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                String forecast = arrayAdapter.getItem(position);

                Intent detailActivity = new Intent(getActivity(), DetailActivity.class)
                        .putExtra(Intent.EXTRA_TEXT, forecast);
                startActivity(detailActivity);

            }
        });

        return viewRoot;
    }

    /**
     * Utilizado para atualizar os itens da tela e segundo plano
     */
    private void refreshItems(){

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());

        String defaulLocal = sharedPreferences.getString(getString(R.string.pref_location_key),
                getString(R.string.pref_location_default));

        fetchWheatherTask = new FetchWheatherTask();
        fetchWheatherTask.execute(defaulLocal);

    }

    public class FetchWheatherTask extends AsyncTask<String, Void, ArrayList<String>> {

        @Override
        protected ArrayList<String> doInBackground(String... params) {

            if (params.length == 0)
                return null;

            Gson gson = new GsonBuilder().create();
            List<RootWheather> rootWheathers = new ArrayList<RootWheather>();

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(WheaterApi.BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            final WheaterApi wheaterApi = retrofit.create(WheaterApi.class);

            Call<RootWheather> call = wheaterApi.listWheater(params[0]);

            try {

                GregorianCalendar gc = new GregorianCalendar();
                Response<RootWheather> rootWheather = call.execute();
                ArrayList<String> resultStrs = new ArrayList<String>();

                for (int i = 0; i < rootWheather.body().list.length; i++) {

                    ItemCity item = rootWheather.body().list[i];

                    gc.add(GregorianCalendar.DATE, 1);
                    Date time = gc.getTime();

                    String day = getReadableDateString(time.getTime());
                    String highAndLow = formatHighLows(item.temp.max, item.temp.min);

                    resultStrs.add(day + " - " + item.weather[0].description + " - " + highAndLow);
                }

                return resultStrs;

            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(ArrayList<String> strings) {
            if (!strings.isEmpty()) {
                arrayAdapter.clear();
                arrayAdapter.addAll(strings);
            }
        }
    }

    /* The date/time conversion code is going to be moved outside the asynctask later,
      * so for convenience we're breaking it out into its own method now.
      */
    private String getReadableDateString(long time) {
        // Because the API returns a unix timestamp (measured in seconds),
        // it must be converted to milliseconds in order to be converted to valid date.
        SimpleDateFormat shortenedDateFormat = new SimpleDateFormat("EEE MMM dd");
        return shortenedDateFormat.format(time);
    }

    /**
     * Prepare the weather high/lows for presentation.
     */
    private String formatHighLows(double high, double low) {
        // For presentation, assume the user doesn't care about tenths of a degree.

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String unitType = sharedPreferences.getString(getString(R.string.pref_metric_list_key),
                getString(R.string.pref_metric_list_default));

        if(unitType.equals(getResources().getStringArray(R.array.pref_metric_list_values)[1])){
            high = (high * 1.8) + 32;
            low = (low * 1.8) + 32;
        }
        else if(!unitType.equals(getResources().getStringArray(R.array.pref_metric_list_values)[0])){
            Log.d(LOG_CAT, "Unit type not found" + unitType);
        }

        long roundedHigh = Math.round(high);
        long roundedLow = Math.round(low);

        String highLowStr = roundedHigh + "/" + roundedLow;
        return highLowStr;
    }

}
