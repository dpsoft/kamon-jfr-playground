## sbt project compiled with Scala 3

### Usage

This is a normal sbt project. You can compile code with `sbt compile`, run it with `sbt run`, and `sbt console` will start a Scala 3 REPL.

For more information on the sbt-dotty plugin, see the
[dotty-example-project](https://github.com/lampepfl/dotty-example-project/blob/master/README.md).

### Docker services

To start `Prometheus`, `Grafana`, `Kafka` and `Pinot` run the following command from the project root:

```bash
docker-compose -f docker/docker-compose.yml up -d
```