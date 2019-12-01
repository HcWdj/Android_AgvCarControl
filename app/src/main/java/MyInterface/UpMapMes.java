package MyInterface;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

public interface UpMapMes {

    /**
     * @param name 图像名
     * @param mapMes 站点信息
     * @return
     */
    @FormUrlEncoded
    @POST("/MyTest/UpMapMes")
    Call<ResponseBody> uploadMes(@Field("NAME") String name, @Field("MAPMES") String mapMes);
}
