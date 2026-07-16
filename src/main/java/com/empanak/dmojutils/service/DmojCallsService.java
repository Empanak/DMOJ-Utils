package com.empanak.dmojutils.service;

import com.empanak.dmojutils.config.AppConfig;
import com.empanak.dmojutils.config.ConfigManager;
import com.empanak.dmojutils.dmojDTO.contestList.ResponseContestList;
import com.empanak.dmojutils.dmojDTO.contest.ResponseContestData;
import com.empanak.dmojutils.dmojDTO.submissions.ResponseSubmissionsData;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

@Service
public class DmojCallsService {
    public enum STATUS {
        AC, WA, TLE, MLE, RLE, CE, IE, PE, RE
    }

    public static boolean isConfigReady(){
        return ConfigManager.getConfig().getApiURL() != null && !ConfigManager.getConfig().getApiURL().isEmpty();
    }

    private static RestClient getClient(boolean withToken){
        AppConfig config = ConfigManager.getConfig();
        RestClient.Builder builder = RestClient.builder().baseUrl(config.getApiURL());
        if(withToken && config.getApiToken() != null && !config.getApiToken().isEmpty())
            builder.defaultHeader("Authorization", "Bearer " + config.getApiToken());
        return builder.build();
    }

    public static ResponseContestList getContests() {
        try {

            return getClient(true).get()
                    .uri("api/v2/contests")
                    .retrieve()
                    .body(ResponseContestList.class);
        }catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden e){
            System.out.println("Token no válido");
            return null;
        }catch (ResourceAccessException e){
            System.out.println("Error al acceder al servidor, comprobar URL o conexion con el servidor");
            return null;
        }catch (Exception e){
            System.out.println("Error inesperado");
            return null;
        }
    }

    public static ResponseContestData getContestData(String contestId) {
        return getClient(true).get()
                .uri("api/v2/contest/" + contestId)
                .retrieve()
                .body(ResponseContestData.class);
    }

    public static ResponseSubmissionsData getSubmissionsByProblem(String problemCode){
        return getClient(true).get()
                .uri("api/v2/submissions?problem=" + problemCode)
                .retrieve()
                .body(ResponseSubmissionsData.class);
    }

    public static ResponseSubmissionsData getSubmissionsByProblemAndStatus(String problemCode, STATUS status){
        return getClient(true).get()
                .uri("api/v2/submissions?problem=" + problemCode + (status != null ? "&result=" + status : ""))
                .retrieve()
                .body(ResponseSubmissionsData.class);
    }

    public static ResponseSubmissionsData getSubmissionsByProblemAndUser(String problemCode, String username, String result){
        return getClient(true).get()
                .uri("api/v2/submissions?problem=" + problemCode + "&user=" + username + (result != null ? "&result=" + result : ""))
                .retrieve()
                .body(ResponseSubmissionsData.class);
    }
}
