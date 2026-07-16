package com.empanak.dmojutils.repository;

import com.empanak.dmojutils.dmojDTO.contest.Contest;
import com.empanak.dmojutils.dmojDTO.contest.ResponseContestData;
import com.empanak.dmojutils.dmojDTO.contestList.ContestData;
import com.empanak.dmojutils.dmojDTO.contestList.ResponseContestList;
import com.empanak.dmojutils.service.DmojCallsService;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
//Pendiente crear repositorios a partir de lo que hay en otros archivos
public class ContestRepository implements Cache {
    private final Map<String, ContestData> contestListCache;
    private final Map<String, Contest> contestDataCache;

    public ContestRepository() {
        this.contestListCache = new ConcurrentHashMap<>();
        this.contestDataCache = new ConcurrentHashMap<>();
        getContestListCache();
    }

    public Map<String, ContestData> getContestListCache() {
        if(contestListCache.isEmpty()){
            ResponseContestList responseContestList = DmojCallsService.getContests();
            if(responseContestList == null || responseContestList.data.objects.length == 0){
                System.out.println("No se pudo obtener la lista de concursos");
                return null;
            }
            contestListCache.clear();
            for(ContestData contest : responseContestList.data.objects){
                contestListCache.put(contest.key, contest);
            }
        }
        return contestListCache;
    }

    public Contest getContestByKey(String key){
        if(contestDataCache.get(key) == null){
            ResponseContestData responseContestData = DmojCallsService.getContestData(key);
            if(responseContestData == null){
                return null;
            }
            contestDataCache.put(key, responseContestData.data.object);
        }
        return contestDataCache.get(key);
    }

    @Override
    public void cleanupCache() {
        contestListCache.clear();
        contestDataCache.clear();
    }
}

