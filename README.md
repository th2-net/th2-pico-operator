### Local Run (1.4.0)
1. clone pico-operator repo
2. build code using `gradle build` command
3. run with `"-Dconverter.config=./path/to/pico-operator-config"`
4. pass appropriate arguments:
    * `full` for generating both configs and queues, `queues` for generating only queues, `configs` for generating only configs.
    * `old` optional argument for generating dictionaries in old format. leave empty for new format
5. provide default configs for each box to be copied. bundle them in a separate directory. place logging files under `loggin` folder in this directory. 
add path to location of default configs in pico operator config file.

Generated configs will be placed in location specified by `generatedConfigsLocation` field in config

pico-operator-config example:

```yaml
repoLocation: location to schema configuration folder
generatedConfigsLocation: location to place generated configs in
schemaName: name for schema
rabbitMQManagement:
  host: rabbit mq server host
  managementPort: management port from rabbitMq (15672)
  applicationPort: general port for rabbitMq (5672)
  vhostName: name for the vhost on rabbit (should be crated before running the application)
  exchangeName: exchange name for global-notifications
  username: rabbit mq management user name
  password: rabbit mq management password
  persistence: flag for persistence feature on rabbitMq (true/false)
  schemaPermissions: configuration for permission of user that will be created fot this specific schema
    configure: ""
    read: ""
    write: ""
grpc:
  serverPorts:
    start: starting port number
    end: ending port number
prometheus:
   enabled: true or false - default true
   start: starting port number
   end: ending port number
defaultSchemaConfigs:
  location: location for default configs that should be copied for each schema
  configNames: map of <configName, configFileName.extension> for default configs that are needed for each schema
    cradle: cradle.json
    cradleManager: cradle_manager.json
    bookConfig: book_config.json
    grpcRouter: grpc_router.json
    mqRouter: mq_router.json
    rabbitMQ: rabbitMQ.json
    log4j2Config: log4j2.properties
    log4pyConfig: log4py.conf
    zeroLogConfig: zerolog.properties 
```

# Release notes

## 1.4.0

## Feature:
+ added log4j2Config, log4pyConfig, zeroLogConfig config names

## 1.3.0

### Updated:
+ bom: `4.4.0-dev` to `4.5.0-dev`
+ kotlin: `1.8.22`