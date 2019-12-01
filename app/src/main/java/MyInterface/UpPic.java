package MyInterface;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

public interface UpPic {
    @FormUrlEncoded
    @POST("/MyTest/UploadPic")
    Call<ResponseBody> update(@Field("UP") String[] pictureNumber);
}
