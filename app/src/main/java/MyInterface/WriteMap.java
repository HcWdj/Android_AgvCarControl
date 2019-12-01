package MyInterface;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

public interface WriteMap{
    @FormUrlEncoded
    @POST("/MyTest/AddTable")
    Call<ResponseBody> write(@Field("NUMBER") String number, @Field("ROW") String row, @Field("MESSAGE") String message);
}
