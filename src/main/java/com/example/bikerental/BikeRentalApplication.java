package com.example.bikerental;

import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.commandhandling.TargetAggregateIdentifier;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.commandhandling.model.AggregateIdentifier;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.spring.stereotype.Aggregate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.axonframework.commandhandling.model.AggregateLifecycle.apply;

@EnableDiscoveryClient
@SpringBootApplication
public class BikeRentalApplication {

    public static void main(String[] args) {
        SpringApplication.run(BikeRentalApplication.class, args);
    }

    @RestController
    public static class Controller {

        private static final Logger log = LoggerFactory.getLogger(Controller.class);
        private final CommandGateway commandGateway;

        public Controller(CommandGateway commandGateway) {
            this.commandGateway = commandGateway;
        }

        @GetMapping("/provision/{location}")
        public CompletableFuture<Object> provision(@PathVariable String location) {
            log.info("Sending a ProvisionBikeCommand for {}", location);
            return commandGateway.send(new ProvisionBikeCommmand(UUID.randomUUID().toString(), location))
                                 .exceptionally(Throwable::getMessage);
        }

        @GetMapping("/rent/{id}/{renter}")
        public CompletableFuture<String> rent(@PathVariable String id, @PathVariable String renter) {
            log.info("Sending a RentOutBikeCommand for {}", renter);
            return commandGateway.send(new RentOutBikeCommand(id, renter))
                                 .thenApply(r -> "ok")
                                 .exceptionally(Throwable::getMessage);
        }

        @GetMapping("/return/{id}/{location}")
        public CompletableFuture<String> returnBike(@PathVariable String id, @PathVariable String location) {
            log.info("Sending a ReturnBikeCommand for {}", location);
            return commandGateway.send(new ReturnBikeCommand(id, location))
                                 .thenApply(r -> "ok")
                                 .exceptionally(Throwable::getMessage);
        }

    }

    @ProcessingGroup("BikeStatus")
    @RestController
    public static class BikeRentalStatusUpdater {

        private final BikeStatusRepository repository;

        public BikeRentalStatusUpdater(BikeStatusRepository repository) {
            this.repository = repository;
        }

        @EventHandler
        public void on(BikeProvisionedEvent event) {
            repository.save(new BikeStatus(event.getId(), event.getLocation()));
        }

        @EventHandler
        public void on(BikeRentedOutEvent event) {
            repository.findOne(event.getId()).markRented(event.getRenter());
        }

        @EventHandler
        public void on(BikeReturnedEvent event) {
            repository.findOne(event.getId()).markReturned(event.getLocation());
        }

        @GetMapping("/status")
        public List<BikeStatus> statuses() {
            return repository.findAll();
        }
    }

    @Aggregate
    public static class Bike {

        private static final Logger log = LoggerFactory.getLogger(Bike.class);
        @AggregateIdentifier
        private String id;

        private boolean rented;

        public Bike() {
        }

        @CommandHandler
        public Bike(ProvisionBikeCommmand cmd) {
            log.info("Received a ProvisionBikeCommand for {}", cmd.getLocation());
            apply(new BikeProvisionedEvent(cmd.getId(), cmd.getLocation()));
        }

        @CommandHandler
        public void on(RentOutBikeCommand cmd) {
            log.info("Received a RentOutBikeCommand for {}", cmd.getRenter());

            Assert.state(!rented, "Bike is already rented out to someone else");
            apply(new BikeRentedOutEvent(cmd.getId(), cmd.getRenter()));
        }

        @CommandHandler
        public void on(ReturnBikeCommand cmd) {
            log.info("Received a ReturnBikeCommand for {}", cmd.getLocation());

            Assert.state(rented, "Where did you get that bike?!");
            apply(new BikeReturnedEvent(cmd.getId(), cmd.getLocation()));
        }

        @EventSourcingHandler
        protected void on(BikeProvisionedEvent event) {
            this.id = event.getId();
        }

        @EventSourcingHandler
        protected void on(BikeRentedOutEvent event) {
            this.rented = true;
        }

        @EventSourcingHandler
        protected void on(BikeReturnedEvent event) {
            this.rented = false;
        }
    }

    public static class BikeReturnedEvent {
        private final String id;
        private final String location;

        public BikeReturnedEvent(String id, String location) {
            this.id = id;
            this.location = location;
        }

        public String getId() {
            return id;
        }

        public String getLocation() {
            return location;
        }
    }

    public static class BikeRentedOutEvent {
        private final String id;
        private final String renter;

        public BikeRentedOutEvent(String id, String renter) {
            this.id = id;
            this.renter = renter;
        }

        public String getId() {
            return id;
        }

        public String getRenter() {
            return renter;
        }
    }

    public static class BikeProvisionedEvent {

        private final String id;
        private final String location;

        public BikeProvisionedEvent(String id, String location) {
            this.id = id;
            this.location = location;
        }

        public String getId() {
            return id;
        }

        public String getLocation() {
            return location;
        }
    }

    public static class ProvisionBikeCommmand {

        @TargetAggregateIdentifier
        private final String id;
        private final String location;

        public ProvisionBikeCommmand(String id, String location) {
            this.id = id;
            this.location = location;
        }

        public String getId() {
            return id;
        }

        public String getLocation() {
            return location;
        }
    }

    public static class RentOutBikeCommand {

        @TargetAggregateIdentifier
        private final String id;
        private final String renter;

        public RentOutBikeCommand(String id, String renter) {
            this.id = id;
            this.renter = renter;
        }

        public String getId() {
            return id;
        }

        public String getRenter() {
            return renter;
        }
    }

    public static class ReturnBikeCommand {

        @TargetAggregateIdentifier
        private final String id;
        private final String location;

        public ReturnBikeCommand(String id, String location) {
            this.id = id;
            this.location = location;
        }

        public String getId() {
            return id;
        }

        public String getLocation() {
            return location;
        }
    }
}
