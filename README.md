# BLOCC Temperature and Humidity Reading Contract Application

This is a Java application used to interact with
the [BLOCC Temperature and Humidity Reading Chaincode](https://github.com/TonyWu3027/blocc-temp-humidity-chaincode).

## Usage

To use it, one needs to create a `Contract` via `BloccContractGateway::getContract()` and
instantiate a `TemperatureHumidityReadingContractApp` object with the contract. Example of
invoking `addReading` transaction using this application is shown
in [TemperatureHumidityExample](./src/main/java/uk/ac/ic/doc/blocc/example/TemperatureHumidityExample.java).

> Note that, the example can only be run with the `test-network` brought up **without** `-ca` flag.