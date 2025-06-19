import retrofit2.http.GET
import retrofit2.http.Query

interface NewsApiService {
    @GET("v2/everything")
    suspend fun getNFTNews(
        @Query("q") query: String = "NFT",
        @Query("apiKey") apiKey: String
    ): NewsResponse
}
