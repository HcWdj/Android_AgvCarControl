package MyInterface;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

public interface DelMapMes {
    @FormUrlEncoded
    @POST("/MyTest/DeleteTable")
    Call<ResponseBody> delMes(@Field("NAME") String name);
}
