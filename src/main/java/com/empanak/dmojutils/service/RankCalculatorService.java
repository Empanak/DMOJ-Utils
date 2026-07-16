package com.empanak.dmojutils.service;

import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import com.empanak.dmojutils.dmojDTO.contest.Problem;
import com.empanak.dmojutils.dmojDTO.contest.Ranking;
import com.empanak.dmojutils.dmojDTO.contest.ResponseContestData;
import com.empanak.dmojutils.dmojDTO.contestList.ContestData;
import com.empanak.dmojutils.dmojDTO.submissions.ResponseSubmissionsData;
import com.empanak.dmojutils.dmojDTO.submissions.Submission;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

public class RankCalculatorService {
    private static final Map<String, ResponseContestData> pointsContestCache = new HashMap<>(), upsolvingContestCache = new HashMap<>();
    private static final Map<String, ResponseSubmissionsData> submissionsCache = new HashMap<>();


    public static List<Map.Entry<String, Pair<Integer, Long>>> calculateAccumulatedRank(List<ContestData> contests){
        Map<String, MutablePair<Integer, Long>> rankMap = new HashMap<>(); // <username, <points, cumulative_time>> map para poder usar al usuario como llave
        for(ContestData contest : contests){ //Itera sobre los concursos seleccionados
            ResponseContestData contestData = loadContestData(contest.key, pointsContestCache);
            for(Ranking ranking : contestData.data.object.rankings){ //Itera sobre los usuarios del concurso
                rankMap.compute(ranking.user, (k, pair) -> { //Usa al usuario como llave del map
                    if(pair == null){
                        return new MutablePair<>(ranking.score, ranking.cumulative_time); //Crea un par de puntos y tiempo
                    }
                    pair.setLeft(pair.getLeft() + ranking.score);
                    pair.setRight(pair.getRight() + ranking.cumulative_time);
                    return pair;
                });
            }
        }
        return sortRank(rankMap);
    }

    public static List<Map.Entry<String, Pair<Integer, Long>>> calculateUpsolvingRank(List<ContestData> contests, Map<String, Set<String>> excludedProblems){
        Map<String, MutablePair<Integer, Long>> rankMap = new HashMap<>(); //<username, <solved_problems, cumulative_time>>
        Map<String, Map<String, Long>> problemsSolved = new HashMap<>(); //<username, <problem, time>>
        for(ContestData contest : contests){ //Itera sobre los concursos seleccionados
            ResponseContestData contestData = loadContestData(contest.key, upsolvingContestCache);
            /*
            * Set de participantes para solo contar a los que participaron ese dia
            * Se mapean los usuarios a partir de los rankings, se toma a cada objeto ranking y se usa su usuario como llave
             */
            Set<String> participants = contestData.data.object.rankings.stream().map(ranking -> ranking.user).collect(Collectors.toSet());
            for(Problem problem : contestData.data.object.problems){ //Itera sobre los problemas del concurso
                ResponseSubmissionsData submissions = loadSubmissionsData(problem.code, submissionsCache); //Obtiene los envios del problema
                for(Submission submission : submissions.data.objects){
                    if(!participants.contains(submission.user)) continue;
                    problemsSolved.computeIfAbsent(submission.user, k -> new HashMap<>());
                    if(submission.date.isAfter(contestData.data.object.end_time)){ // Si el envio es despues del concurso
                        problemsSolved.get(submission.user).compute(submission.problem, (k, minTime) -> {
                            long seconds = Duration.between(contestData.data.object.end_time, submission.date).toSeconds();
                            if(minTime == null){
                                return seconds;
                            }
                            return minTime < seconds ? minTime : seconds;
                        });
                    }else if(submission.contest != null){ //Hecho en tiempo de concurso
                        problemsSolved.get(submission.user).compute(submission.problem, (k, minTime) -> -1L);
                    }
                    if(excludedProblems.get(submission.user) != null && excludedProblems.get(submission.user).contains(submission.problem)){
                        problemsSolved.get(submission.user).compute(submission.problem, (k, minTime) -> -1L);
                    }
                }
            }
        }
        for(Map.Entry<String, Map<String, Long>> entry : problemsSolved.entrySet()){
            int validProblems = (int) entry.getValue().entrySet().stream().filter(e -> e.getValue() != -1).count();
            long cumulativeTime = entry.getValue().values().stream().filter(e -> e > -1).mapToLong(Long::longValue).sum();
            rankMap.compute(entry.getKey(), (k, v) -> new MutablePair<>((validProblems), cumulativeTime));
        }
        return sortRank(rankMap);
    }

    private static ResponseContestData loadContestData(String contestKey, Map<String, ResponseContestData> contestCache) {
        if(contestCache.get(contestKey) == null){
            ResponseContestData contestData = DmojCallsService.getContestData(contestKey);
            contestCache.put(contestKey, contestData);
            return contestData;
        }else{
            return contestCache.get(contestKey);
        }
    }

    private static ResponseSubmissionsData loadSubmissionsData(String problemCode, Map<String, ResponseSubmissionsData> submissionsCache){
        if(submissionsCache.get(problemCode) == null){
            ResponseSubmissionsData submissionsData = DmojCallsService.getSubmissionsByProblemAndStatus(problemCode, DmojCallsService.STATUS.AC);
            submissionsCache.put(problemCode, submissionsData);
            return submissionsData;
        }else{
            return submissionsCache.get(problemCode);
        }
    }

    private static List<Map.Entry<String, Pair<Integer, Long>>> sortRank(Map<String, MutablePair<Integer, Long>> rankMap) {
        return rankMap.entrySet()
                .stream()
                .map(entry -> Map.entry(
                        entry.getKey(),
                        Pair.of(
                                entry.getValue().getLeft(),
                                entry.getValue().getRight()
                        )
                )).sorted((a, b) -> { //Ordena
                    if (a.getValue().getLeft().equals(b.getValue().getLeft())) { //Si los puntos son iguales, ordena por tiempo
                        return a.getValue().getRight()
                                .compareTo(b.getValue().getRight()); //Ordena por tiempo de forma creciente (a < b)
                    }
                    return b.getValue().getLeft()
                            .compareTo(a.getValue().getLeft()); //Ordena por puntos de forma decreciente (b > a)
                }).toList();
    }
}
