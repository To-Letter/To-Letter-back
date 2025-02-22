package com.toletter.Service;

import com.toletter.Error.ErrorCode;
import com.toletter.Error.ErrorException;
import com.toletter.Repository.EmitterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
@Transactional
@RequiredArgsConstructor
public class AlarmService {
    private final EmitterRepository emitterRepository;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private static final Long TIMEOUT = 10*60*1000L; // 10분

    // SSE 연결
    public SseEmitter connect(String loginEmail){
        SseEmitter emitter = emitterRepository.save(loginEmail, new SseEmitter(TIMEOUT));

        emitter.onCompletion(() -> {
            emitterRepository.deleteById(loginEmail);
        });
        emitter.onTimeout(() -> {
            emitterRepository.deleteById(loginEmail);
        });

        try {
            // 최초 연결 시 메시지를 안 보내면 503 Service Unavailable 에러 발생
            emitter.send(SseEmitter.event().name("connect").data(loginEmail + " connected!"));
        } catch (IOException e) {
            throw new ErrorException("e : " + e, 404, ErrorCode.NOT_FOUND_EXCEPTION);
        }
        return emitter;
    }

    // 알림 보내기
    private void sendToClient(SseEmitter emitter, String loginEmail, String letterId){
        try{
            emitter.send(SseEmitter.event().id(loginEmail).name("message").data(letterId));
        } catch (IOException e) {
            emitterRepository.deleteById(loginEmail);
            throw new ErrorException("e : " + e, 404, ErrorCode.NOT_FOUND_EXCEPTION);
        }
    }

    // 알림 보내려고 데이터 추가
    public void send(String loginEmail, String letterId){
        Map<String, SseEmitter> emitters = emitterRepository.findAllEmitterWithByMemberId(loginEmail);
        emitters.forEach((key, emitter) -> {
            emitterRepository.saveEventCache(key, letterId);
            sendToClient(emitter, key, letterId);
        });
    }

    // 로그아웃 후 모든 알림을 삭제
    public void delete(String loginEmail){
        emitterRepository.disconnect(loginEmail);
    }

    // 테스트 및 프론트 작업을 위해 1-5분으로 해놨지만 그 후에는 1-5일로 할 예정
    public void scheduleTask(String loginEmail, String letterId, int time) {
        scheduler.schedule(() -> {
            try{
                System.out.println("새로운 알람이 왔어요!!!");
                this.send(loginEmail, letterId);
            }catch (Exception e){
                throw new ErrorException("스케줄러 에러 :  " + e.getMessage(), 400, ErrorCode.RUNTIME_EXCEPTION);
            }
        }, time, TimeUnit.MINUTES);
    }

    @PreDestroy
    public void shutdownScheduler() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(60, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
    }

}
