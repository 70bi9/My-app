package com.example.data

import com.example.BuildConfig
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// --- Gemini REST JSON Models ---

@JsonClass(generateAdapter = true)
data class GeminiGenerateRequest(
    val contents: List<GeminiContent>,
    val generationConfig: GeminiGenerationConfig? = null,
    val systemInstruction: GeminiContent? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiPart(
    val text: String? = null,
    val inlineData: GeminiInlineData? = null
)

@JsonClass(generateAdapter = true)
data class GeminiInlineData(
    val mimeType: String,
    val data: String // Base64 encoded JPEG
)

@JsonClass(generateAdapter = true)
data class GeminiGenerationConfig(
    val responseMimeType: String? = "application/json",
    val temperature: Float? = 0.2f
)

@JsonClass(generateAdapter = true)
data class GeminiGenerateResponse(
    val candidates: List<GeminiCandidate>? = null
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    val content: GeminiContent? = null
)

// --- Retrofit Service ---

interface GeminiApiService {
    // gemini-3.5-flash endpoint
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiGenerateRequest
    ): GeminiGenerateResponse
}

object GeminiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(loggingInterceptor)
        .build()

    val service: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }

    // Helper to request the Gemini analysis
    suspend fun analyzeMangaPage(
        base64Jpeg: String,
        isUltraAccuracy: Boolean = false
    ): GeminiAnalysisResult {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return GeminiAnalysisResult.failure("API Key is missing. Please configure it in AI Studio Secrets panel.")
        }

        val prompt = if (isUltraAccuracy) {
            "Analyze this manga page for Ultra Accuracy Mode. Detect characters and assign their exact official anime colors (from your database of works like Blue Lock, Naruto, One Piece, etc.). Return a JSON."
        } else {
            "Analyze this manga page. Detect characters, clothes, hair, eyes, and background description. Return results in JSON format."
        }

        val systemPrompt = """
            You are MangaColor AI Brain, simulating Vision Transformer, CLIP, Florence-2, and Qwen-VL.
            Identify characters present on the page. Match against famous anime works (like Blue Lock, Dragon Ball, One Piece, Naruto, Jujutsu Kaisen, etc.).
            Return MUST be a valid JSON with this exact structure:
            {
              "mangaName": "Name of Manga (e.g., Blue Lock)",
              "themeColor": "representative hex of the manga (e.g., #0055ff)",
              "success": true,
              "characters": [
                {
                  "name": "Isagi Yoichi",
                  "confidence": 0.95,
                  "hairColor": "Navy Blue",
                  "eyeColor": "Sapphire Blue",
                  "clothingHex": "#1a1e2b"
                }
              ],
              "scenePrompt": "Detail scene description for image colorizers",
              "coloringTips": "Preserve high contrast lines and apply deep blue to Isagi's jersey."
            }
        """.trimIndent()

        val request = GeminiGenerateRequest(
            contents = listOf(
                GeminiContent(
                    parts = listOf(
                        GeminiPart(text = prompt),
                        GeminiPart(inlineData = GeminiInlineData(mimeType = "image/jpeg", data = base64Jpeg))
                    )
                )
            ),
            generationConfig = GeminiGenerationConfig(responseMimeType = "application/json"),
            systemInstruction = GeminiContent(parts = listOf(GeminiPart(text = systemPrompt)))
        )

        return try {
            val response = service.generateContent(apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (jsonText != null) {
                // Parse using Moshi adapter
                val adapter = moshi.adapter(GeminiAnalysisResult::class.java)
                adapter.fromJson(jsonText) ?: GeminiAnalysisResult.failure("JSON parsing error")
            } else {
                GeminiAnalysisResult.failure("No analysis returned from Gemini.")
            }
        } catch (e: Exception) {
            GeminiAnalysisResult.failure("Error calling Gemini API: ${e.localizedMessage}")
        }
    }
}

// JSON Parse class for Gemini output
@JsonClass(generateAdapter = true)
data class GeminiAnalysisResult(
    val mangaName: String = "Unknown Manga",
    val themeColor: String = "#03DAC5",
    val success: Boolean = false,
    val characters: List<GeminiDetectedCharacter>? = null,
    val scenePrompt: String? = null,
    val coloringTips: String? = null,
    val error: String? = null
) {
    companion object {
        fun failure(err: String) = GeminiAnalysisResult(
            mangaName = "تحليل غير متاح / Offline Mode",
            themeColor = "#F44336",
            success = false,
            error = err,
            coloringTips = "تأكد من إدخال مفتاح Gemini API وتوصيل الإنترنت للتلوين الذكي وتحديد الشخصيات تلقائياً."
        )
    }
}

@JsonClass(generateAdapter = true)
data class GeminiDetectedCharacter(
    val name: String,
    val confidence: Double,
    val hairColor: String,
    val eyeColor: String,
    val clothingHex: String
)
