package ru.vstu.clustermonitor.data.retrofit

import android.app.Application
import android.content.Context
import android.util.Log
import co.metalab.asyncawait.async
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import ru.vstu.clustermonitor.MonitorApplication
import ru.vstu.clustermonitor.R
import ru.vstu.clustermonitor.data.interfaces.IMonitorRepository
import ru.vstu.clustermonitor.models.AuthRequest
import ru.vstu.clustermonitor.models.FailableModel
import ru.vstu.clustermonitor.models.QueueTask
import ru.vstu.clustermonitor.models.Sensor

/**
 * Repository provide all access to data
 */
class RetrofitMonitorRepository : IMonitorRepository
{
    private val _api: IMonitorApi
    private var _token: String
    private val _prefFileName: String = MonitorApplication.Instance.getString(R.string.pref_file)
    private val _prefName: String = MonitorApplication.Instance.getString(R.string.token_pref_name)

    private val TAG = "Repository"

    constructor()
    {
        _token = getToken()

        // TODO: auth again if login is invalid

        val client = OkHttpClient.Builder()
        client.addInterceptor {chain ->
            val original = chain.request()
            val builder = original.newBuilder()
                .header("Authorization", getToken())
            chain.proceed(builder.build())
        }

        val retrofit = Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl(MonitorApplication.Instance.getString(R.string.api_url))
                .client(client.build())
                .build()


        _api =  retrofit.create(IMonitorApi::class.java)
    }

     override fun isLoggedIn():Boolean = _token != ""

    override fun auth(login: String, password: String) : Boolean  {
        val response = _api.auth(AuthRequest(login, password)).execute()
        if (response.isSuccessful && response.body()!=null)
        {
            Log.d(TAG, "Success auth")
            val token = response.body()!!.access_token // I CHECK THAT BODY IS NOT NULL!!!
            saveToken("JWT $token")
            return true
        }
        else {
            Log.w(TAG, "Auth failed: ${response.code()}")
            return false
        }
    }

    override fun getQueueTasks(): FailableModel<List<QueueTask>> {
        val response = _api.queue().execute()
        if (response.isSuccessful && response.body() != null) {
            Log.d(TAG, "Queue tasks loaded")
            return FailableModel(response.body()!!)
        }
        else {
            Log.w(TAG, "Failed to load queue tasks: ${response.code()}")
            return FailableModel("Произошла ошибка: ${response.code()}")
        }
    }

    override fun getSensors(): FailableModel<List<Sensor>> {
        val response = _api.sensors().execute()
        if (response.isSuccessful && response.body() != null) {
            Log.d(TAG, "Sensors loaded")
            return FailableModel(response.body()!!)
        }
        else {
            Log.w(TAG, "Failed to load sensors: ${response.code()}")
            return FailableModel("Произошла ошибка: ${response.code()}")
        }
    }


    // Get locally saved token or will read it from Preferences if It not exists
    private fun getToken():String {
        if(_token == null) {
            val pref = MonitorApplication.Instance.getSharedPreferences(_prefFileName, Context.MODE_PRIVATE)
            _token = pref.getString(_prefName, "")
            Log.d(TAG, "Token is loaded from preferences")
        }
        else
            Log.d(TAG, "Using local token")

        return _token
    }

    // Write token to Preferences
    private fun saveToken(token: String) {
        val pref = MonitorApplication.Instance.getSharedPreferences(_prefFileName, Context.MODE_PRIVATE)
        val editor = pref.edit()
        editor.putString(_prefName, token)
        editor.commit()
        _token = token

        Log.d(TAG, "Token is saved to preferences")
    }
}