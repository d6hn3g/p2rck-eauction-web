package com.github.dghng36.eauction.e_auction_system.unit.modules.auction.auctionRoom.scheduler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import org.mockito.junit.jupiter.MockitoExtension;

import com.github.dghng36.eauction.e_auction_system.unit.support.JobExecutorTasksMockHelper;
import com.github.dghng36.eauction.infra.config.async.JobExecutorTasks;
import com.github.dghng36.eauction.modules.auction.auctionRoom.scheduler.AuctionRoomScheduler;
import com.github.dghng36.eauction.modules.auction.auctionRoom.service.AuctionRoomSchedulerService;

@ExtendWith(MockitoExtension.class)
public class AuctionRoomSchedulerTest {
    @Mock private AuctionRoomSchedulerService auctionRoomSchedulerService;
    @Mock private JobExecutorTasks jobExecutorTasks;

    @InjectMocks
    private AuctionRoomScheduler auctionRoomScheduler;

    @BeforeEach
    void setUp() {
        JobExecutorTasksMockHelper.runSynchronously(jobExecutorTasks);
    }

    @Test
    void processAuctionLifeCycles_ShouldExecuteAllTasksParallelAndJoin() {
        auctionRoomScheduler.processAuctionLifeCycles();

        verify(auctionRoomSchedulerService, times(1)).notifyAllUpcomingAuctionRooms();
        verify(auctionRoomSchedulerService, times(1)).startAllUpcomingAuctionRooms();
        verify(auctionRoomSchedulerService, times(1)).endAllOngoingAuctionRooms();
    }
}
