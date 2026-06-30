package com.github.dghng36.eauction.modules.auction.auctionRoom.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.github.dghng36.eauction.infra.config.async.JobExecutorTasks;
import com.github.dghng36.eauction.modules.auction.auctionRoom.service.AuctionRoomSchedulerService;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Component
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = lombok.AccessLevel.PRIVATE)
public class AuctionRoomScheduler {
    AuctionRoomSchedulerService auctionRoomSchedulerService;
    JobExecutorTasks jobExecutorTasks;

    @Scheduled(cron = "0 */1 * * * *")
    public void processAuctionLifeCycles() {
        jobExecutorTasks.runAllAndJoin(
            auctionRoomSchedulerService::notifyAllUpcomingAuctionRooms,
            auctionRoomSchedulerService::startAllUpcomingAuctionRooms,
            auctionRoomSchedulerService::endAllOngoingAuctionRooms
        );
    }
}
