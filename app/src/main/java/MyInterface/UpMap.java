package MyInterface;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

public interface UpMap {
    @FormUrlEncoded
    @POST("/MyTest/UpMap")
    Call<ResponseBody> upmap(@Field("NAME") String name, @Field("MAPMES") String mapMes);
}
