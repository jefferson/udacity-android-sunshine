package Rest;

import json.RootWheather;
import retrofit.Call;
import retrofit.http.GET;
import retrofit.http.Query;

/**
 * Created by JeffersonAlves on 24/09/2015.
 */
public interface WheaterApi {

    String BASE_URL = "http://api.openweathermap.org";

    @GET("/data/2.5/forecast/daily?units=metric&cnt=7&lang=pt")
    Call<RootWheather> listWheater(@Query("q") String cidade);
}
