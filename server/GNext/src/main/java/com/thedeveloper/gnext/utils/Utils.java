package com.thedeveloper.gnext.utils;

import com.thedeveloper.gnext.entity.MessageEntity;
import com.thedeveloper.gnext.entity.StorisEntity;
import com.thedeveloper.gnext.entity.UserEntity;
import com.thedeveloper.gnext.entity.WalletEventEntity;
import com.thedeveloper.gnext.enums.EventResult;
import com.thedeveloper.gnext.enums.WalletEventType;
import com.thedeveloper.gnext.service.MessageService;
import com.thedeveloper.gnext.service.StorisService;
import com.thedeveloper.gnext.service.UserService;
import com.thedeveloper.gnext.service.WalletEventService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class Utils {
    @Autowired
    StorisService storisService;
    @Autowired
    UserService userService;
    @Autowired
    WalletEventService walletEventService;
    @Autowired
    MessageService messageService;

    static int FREE_PERIOD = 7;
    static int SUB_DAY = 1;
    static int SUB_PRICE = 100;
    @PostConstruct
    public void init() throws InterruptedException {
        storiesService();
        subscriptionService();
        messageService();
    }
    WalletEventEntity paySubscription(UserEntity user){
        WalletEventEntity walletEventEntity = new WalletEventEntity();
        walletEventEntity.setResult(EventResult.DONE);
        walletEventEntity.setDate(new Date());
        walletEventEntity.setUser(user);
        walletEventEntity.setOld_sum(user.getWallet());
        walletEventEntity.setType(WalletEventType.PAYMENT);
        walletEventEntity.setSum(SUB_PRICE);
        return walletEventEntity;
    }
    void subscriptionService(){
        log.info("Start subscription service");
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true){
                    int timeout = 10;
                    List<UserEntity> users = userService.findAfterSevenDays();
                    for(UserEntity user : users){
                        if(user.isSubscription()){
                            if(user.getWallet()>=SUB_PRICE && (user.getSubStart()==null || Duration.between(user.getSubStart().toInstant(), new Date().toInstant()).toDays()>=SUB_DAY)){
                                walletEventService.save(paySubscription(user));
                                user.setSubscription(true);
                                user.setSubStart(new Date());
                                user.setWallet(user.getWallet()-SUB_PRICE);
                                userService.save(user);
                                log.info("User {} extended subscription", user.getPhone());
                            }else{
                                if(user.getSubStart()==null ||Duration.between(user.getSubStart().toInstant(), new Date().toInstant()).toDays()>=SUB_DAY){
                                    user.setSubscription(false);
                                    userService.save(user);
                                    log.info("User {} cancel subscription", user.getPhone());
                                }
                            }
                        }else{
                            if(user.getWallet()>=SUB_PRICE){
                                walletEventService.save(paySubscription(user));
                                user.setSubscription(true);
                                user.setWallet(user.getWallet()-SUB_PRICE);
                                user.setSubStart(new Date());
                                userService.save(user);
                                log.info("User {} add subscription", user.getPhone());
                            }
                        }
                    }
                    try {
                        TimeUnit.SECONDS.sleep(timeout);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }).start();
    }
    void storiesService() throws InterruptedException {
        log.info("Start stories service");
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true){
                    for(StorisEntity storis : storisService.findAll()){
                        Duration duration = Duration.between(storis.getCreateDate().toInstant(), new Date().toInstant());
                        if(duration.toHours()>=24){
                            log.info("Story {} time in {}:{}", storis.getId(), duration.toHours(), duration.toMinutes()-duration.toHours()*60);
                            storisService.delete(storis);
                        }
                    }
                    try {
                        TimeUnit.SECONDS.sleep(1);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }).start();
    }
    void messageService() {
        log.info("Start message service");
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true){
                    for(MessageEntity message : messageService.findAfterTwoDays()){
                        Duration duration = Duration.between(message.getTime().toInstant(), new Date().toInstant());
                        if(duration.toHours()>=48){
                            log.info("Message {} time in {}:{} content {}", message.getId(), duration.toHours(), duration.toMinutes()-duration.toHours()*60, message.getContent());
                            messageService.delete(message);
                        }
                    }
                    try {
                        TimeUnit.HOURS.sleep(1);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }).start();
    }

}
