### Local Run (1.6.0)
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
repoLocation: th2-infra-schema # location to schema configuration folder
generatedConfigsLocation: workspace/configs # location to place generated configs in
schemaName: schema # name for schema
rabbitMQManagement:
  host: localhost # rabbit mq server host
  managementPort: 15672 # management port from rabbitMq (15672)
  applicationPort: 5672 # general port for rabbitMq (5672)
  vhostName: th2 # name for the vhost on rabbit (should be crated before running the application)
  exchangeName: global-notification # exchange name for global-notifications
  username: "${RABBITMQ_USER}" # rabbit mq management user name
  password: "${RABBITMQ_PASS}" # rabbit mq management password
  persistence: true #  flag for persistence feature on rabbitMq (true/false)
  schemaPermissions: # configuration for permission of user that will be created fot this specific schema
    configure: ""
    read: ".*"
    write: ".*"
grpc:
  serverPorts:
    start: 8091 # starting port number
    end: 8189 # ending port number
prometheus:
   enabled: false # true or false - default true
   start: 9000 # starting port number
   end: 9090 # ending port number
defaultEnvironmentVariables: # map of <environment variable name, list of values> to apply for all run components
   JAVA_TOOL_OPTIONS:
      - "-XX:+ExitOnOutOfMemoryError"
      - "-Dlog4j2.shutdownHookEnabled=false"
defaultSchemaConfigs:
  location: cfg/defaultConfigs # location for default configs that should be copied for each schema
  configNames: # map of <configName, configFileName.extension> for default configs that are needed for each schema
    cradle: cradle.json
    cradleManager: cradle_manager.json
    grpcRouter: grpc_router.json
    mqRouter: mq_router.json
    rabbitMQ: rabbitMQ.json
    log4j2Config: log4j2.properties
    log4pyConfig: log4py.conf
    zeroLogConfig: zerolog.properties 
```

# Release notes

## 1.6.0
+ Added `defaultEnvironmentVariables` into pico config file.

## 1.5.0

## Feature:
+ Use book name from infra manager config or from component custom resource  

## Deprecated
+ defaultSchemaConfigs.configNames.bookConfig is deprecated. Default book name is taken from infra manager config

## 1.4.0

## Feature:
+ added log4j2Config, log4pyConfig, zeroLogConfig config names

## Fix:
+ Fix using `cradle manager` and `grpc router` configs from component custom resource.

## 1.3.0

### Updated:
+ bom: `4.4.0-dev` to `4.5.0-dev`
+ kotlin: `1.8.22`