# Akita GPB module akita-gpb-kafka-mq-module
## Назначение сервиса
Сервис akita-gpb-kafka-mq-module предназначен для взаимодействия с системами обмена сообщениями и стриминговыми платформами в фреймворке Akita-BDD. Он поддерживает работу с Kafka и MQ, обеспечивая отправку, получение и обработку сообщений. Сервис относится к домену автоматизации тестирования мессенджеров и очередей.
## Описание процесса
В сервисе реализован следующий бизнес-процесс: Управление сообщениями в Kafka и MQ:
1. Генерация UUID и сохранение в переменную.
2. Подключение к Kafka для отправки и получения сообщений с использованием параметров.
3. Извлечение заголовков и тел сообщений из Kafka-топиков.
4. Создание и управление подключениями к MQ с использованием таблицы параметров.
5. Отправка сообщений в очереди MQ и чтение сообщений по селекторам.
6. Закрытие соединений и валидация содержимого сообщений.

![Scheme.PNG](./techdocs/docs/uml/scheme.png)

## Список шагов модуля
Kafka шаги:
```
@И("^сгенерировать UUID и сохранить в переменную \"(.*)\"$")
@И("^выполнено подключение к kafka и отправлено сообщение с параметрами из таблицы$")
@И("^выполнено подключение к kafka и полученно сообщение из топика \"(.*)\" равно значениям из таблицы$")
@И("^выполнено подключение к kafka и из последнего сообщения из топика \"(.*)\" header \"(.*)\" сохранен в переменную \"(.*)\"")
@И("^выполнено подключение к kafka и из последнего сообщения из топика \"(.*)\" тело сообщения сохранено в переменную \"(.*)\"")
@И("^выполнено подключение к kafka и из сообщения из топика \"(.*)\" header \"(.*)\" со значением \"(.*)\" сохранен в переменную \"(.*)\"")
@И("^выполнено подключение к kafka и из сообщения по ключу \"(.*)\" из топика \"(.*)\" тело запроса сохранен в переменную \"(.*)\"")
@И("^выполнено подключение к kafka и поиск сообщения в topic \"(.*)\" header \"(.*)\" с значением \"(.*)\" и jsonpath \"(.*)\" со значением \"(.*)\"")
@И("^выполнено подключение к kafka и сообщение из топика \"(.*)\" header \"(.*)\" со значением \"(.*)\" сохранен в переменную \"(.*)\"")
@И("^из сообщения от kafka \"(.*)\" header \"(.*)\" сохранен в переменную \"(.*)\"")
@И("^получить сообщение из топика \"(.+)\", с типом (JSON|XML|TEXT) соответствующее параметрам из таблицы, и сохранить в переменную \"(.+)\"$")
@И("^выполнено подключение к kafka и из сообщения из топика \"(.*)\" тело сообщения сохранено в переменную \"(.*)\" и соответствует параметрам из таблицы")
@И("^отсутствует сообщение в топике \"(.+)\", с типом (JSON|XML|TEXT) соответствующее параметрам из таблицы$")
```

MQ шаги:
```
@И("^создано подключение к mq, с данными из таблицы. Результат сохранен в переменную \"(.*)\"$")
@И("^отправлено сообщение в очередь \"(.*)\" через MQ соединение \"(.*)\", с параметрами из таблицы$")
@И("^отправлено сообщение в очередь \"(.*)\" через MQ соединение \"(.*)\", с параметрами из таблицы и MessageID сохранен в переменную \"(.*)\"$")
@И("^прочитать сообщение из очереди \"(.*)\" по селектору \"(.*)\", через MQ соединение \"(.*)\". Результат сохранен в переменную \"(.*)\"$")
@И("^закрыть соединение \"(.*)\"$")
@И("^в сообщении \"(.*)\", содержится текст \"(.*)\"$")
```

Artemis шаги:
```
@И("^создано подключение к artemis$")
@И("^закрыть artemis соединение$")

@И("^отправлено сообщение в очередь \"(.*)\" с параметрами из таблицы$")
@И("^отправлено сообщение в очередь \"(.*)\" с параметрами из таблицы. MessageID сохранен в переменную \"(.*)\"$")

@И("^прочитано первое сообщение из очереди \"([^\"]+)\". Результат сохранен в переменную \"([^\"]+)\"$")
@И("^прочитано сообщение из очереди \"(.*)\" по property \"(.*)\" равному \"(.*)\". Результат сохранен в переменную \"(.*)\"$")
@И("^прочитано сообщение из очереди \"(.*)\" по property таблице$")
@И("^прочитано сообщение из очереди \"(.*)\" по селектору \"(.*)\". Результат сохранен в переменную \"(.*)\"$")

@И("^просмотрено первое сообщение из очереди \"([^\"]+)\". Результат сохранен в переменную \"([^\"]+)\"$")
@И("^просмотрено сообщение из очереди \"(.*)\" по property \"(.*)\" равному \"(.*)\". Результат сохранен в переменную \"(.*)\"$")
@И("^просмотрено сообщение из очереди \"(.*)\" по property таблице$")
@И("^просмотрено сообщение из очереди \"(.*)\" по селектору \"(.*)\". Результат сохранен в переменную \"(.*)\"$")

@И("^отправлено сообщение в топик \"(.*)\" с параметрами из таблицы$")

@И("^создана подписка на топик \"(.*)\" с именем \"(.*)\"$")
@И("^создана подписка на топик \"(.*)\" с именем \"(.*)\" по property \"(.*)\" равному \"(.*)\"$")
@И("^создана подписка на топик \"(.*)\" с именем \"(.*)\" с property таблицей$")

@И("^прочитано сообщение из топика по подписке \"(.*)\". Результат сохранен в переменную \"(.*)\"$")
@И("^удалена подписка на топик с именем \"(.*)\"$")

@И("^сообщение \"(.*)\" содержит текст \"(.*)\"$")
@И("^заголовки сообщения \"(.*)\" соответствуют таблице$")

@И("^json сообщение \"(.*)\" соответствует таблице$")
@И("^json сообщение \"(.*)\" соответствует файлу \"(.*)\" игнорируя поля$")
@И("^из json сообщения \"(.*)\" сохранено значение \"(.*)\" в переменную \"(.*)\"$")

@И("^xml сообщение \"(.*)\" соответствует таблице$")
@И("^xml сообщение \"(.*)\" соответствует файлу \"(.*)\" игнорируя поля$")
@И("^из xml сообщения \"(.*)\" сохранено значение \"(.*)\" в переменную \"(.*)\"$")
```

# Примеры сценариев тестирования Kafka

## 1. Работа с переменными и отправка самых простых сообщений

```gherkin
#language:ru
# Работа с переменными и отправка самых простых сообщений
# Всё без проверок получения — только отправка и сохранение значений

@kafka
Функционал: Первые шаги — переменные и отправка сообщений

  Сценарий: Генерируем уникальный идентификатор и сразу смотрим на него
    # Этот идентификатор будем использовать в заголовках и ключах
    И сгенерировать UUID и сохранить в переменную "requestId"

  Сценарий: Сохраняем простые строковые значения в переменные
    # Такие значения удобно переиспользовать в разных сценариях
    Когда установлено значение переменной "testEnvironment" равным "dev"
    И установлено значение переменной "customerLogin" равным "anna.smirnova@test.ru"
    И установлено значение переменной "orderExternalId" равным "ORD-20260212-ABC123"

  Сценарий: Отправляем самое простое JSON-сообщение без ключа и заголовков. Body не из файла
    # Самый минимальный пример — проверяем, что отправка вообще работает
    Когда выполнено подключение к kafka и отправлено сообщение с параметрами из таблицы
      | type  | name  | value                      |
      | topic | topic | test.hello.world           |
      | body  | body  | {"greeting":"hello world"} |

#  Файл hello-world.json
#  {"greeting":"hello world"}
  Сценарий: Отправляем самое простое JSON-сообщение без ключа и заголовков. Body из файла
    # Самый минимальный пример — проверяем, что отправка вообще работает
    Когда выполнено подключение к kafka и отправлено сообщение с параметрами из таблицы
      | type  | name  | value                                                                |
      | topic | topic | test.hello.world                                                     |
      | body  | body  | src/test/resources/documentation/files/greeting.xml/hello-world.json |

  Сценарий: Отправляем сообщение с ключом
    # Ключ помогает Kafka распределять сообщения по партициям
    Когда установлено значение переменной "orderExternalId" равным "ORD-20260212-ABC123"
    И выполнено подключение к kafka и отправлено сообщение с параметрами из таблицы
      | type  | name  | value                       |
      | topic | topic | orders                      |
      | key   | key   | ANY KEY                     |
      | body  | body  | payloads/order-created.json |

  Сценарий: Отправляем сообщение с ключом из переменной
    # Ключ помогает Kafka распределять сообщения по партициям
    Когда установлено значение переменной "orderExternalId" равным "ORD-20260212-ABC123"
    И выполнено подключение к kafka и отправлено сообщение с параметрами из таблицы
      | type  | name  | value                       |
      | topic | topic | orders                      |
      | key   | key   | ${orderExternalId}          |
      | body  | body  | payloads/order-created.json |

  Сценарий: Отправляем простое XML-сообщение
    Когда выполнено подключение к kafka и отправлено сообщение с параметрами из таблицы
      | type  | name  | value                    |
      | topic | topic | legacy.documents.in      |
      | body  | body  | payloads/minimal-doc.xml |

```
## 2. Отправка сообщений и проверка важных полей при получении

```gherkin
#language:ru
# Отправка сообщение и проверка важных полей при получении

@kafka
Функционал: Первые проверки — ждём и смотрим содержимое сообщений

  Сценарий: Ждём простое JSON-сообщение и проверяем одно поле
    # Самый простой позитивный сценарий отправки и получения
    Когда выполнено подключение к kafka и отправлено сообщение с параметрами из таблицы
      | type  | name  | value                      |
      | topic | topic | test.hello.world           |
      | body  | body  | {"greeting":"hello world"} |
    Тогда получить сообщение из топика "test.hello.world", с типом JSON соответствующее параметрам из таблицы, и сохранить в переменную "helloMsg"
      | $.greeting | hello world |

  Сценарий: Ждём простое JSON-сообщение и проверяем несколько полей
    # Самый простой позитивный сценарий отправки и получения
    Когда выполнено подключение к kafka и отправлено сообщение с параметрами из таблицы
      | type  | name  | value                                                               |
      | topic | topic | test.hello.world                                                    |
      | body  | body  | {"greeting":"hello world","customerLogin": "anna.smirnova@test.ru"} |
    Тогда получить сообщение из топика "test.hello.world", с типом JSON соответствующее параметрам из таблицы, и сохранить в переменную "helloMsg"
      | $.greeting      | hello world           |
      | $.customerLogin | anna.smirnova@test.ru |


#  Файл hello-world.json
#  {
#  "greeting": "hello world",
#  "customerLogin": "anna.smirnova@test.ru"
#  }
  Сценарий: Ждём простое JSON-сообщение и проверяем несколько полей. Body из файла
    # Самый простой позитивный сценарий отправки и получения
    Когда выполнено подключение к kafka и отправлено сообщение с параметрами из таблицы
      | type  | name  | value                                                   |
      | topic | topic | test.hello.world                                        |
      | body  | body  | src/test/resources/documentation/files/hello-world.json |
    Тогда получить сообщение из топика "test.hello.world", с типом JSON соответствующее параметрам из таблицы, и сохранить в переменную "helloMsg"
      | $.greeting      | hello world           |
      | $.customerLogin | anna.smirnova@test.ru |


# Файл hello-world-with-var.json
#  {
#  "greeting": "hello world",
#  "customerLogin": "anna.smirnova@test.ru",
#  "orderExternalId": "${orderExternalId}",
#  "uuid": "${UUID}"
#  }
  Сценарий: Ждём простое JSON-сообщение и проверяем несколько полей с переменными. Body из файла
    Когда сгенерировать UUID и сохранить в переменную "UUID"
    И установлено значение переменной "orderExternalId" равным "ORD-20260212-ABC123"
    И выполнено подключение к kafka и отправлено сообщение с параметрами из таблицы
      | type  | name  | value                                                            |
      | topic | topic | test.hello.world                                                 |
      | body  | body  | src/test/resources/documentation/files/hello-world-with-var.json |
    Тогда получить сообщение из топика "test.hello.world", с типом JSON соответствующее параметрам из таблицы, и сохранить в переменную "receivedDoc"
      | $.greeting        | hello world           |
      | $.customerLogin   | anna.smirnova@test.ru |
      | $.orderExternalId | ORD-20260212-ABC123   |
      | $.uuid            | ${UUID}               |


  Сценарий: Ждём XML-сообщение и проверяем один идентификатор
    Когда выполнено подключение к kafka и отправлено сообщение с параметрами из таблицы
      | type  | name  | value                                                                               |
      | topic | topic | test.hello.world                                                                    |
      | body  | body  | <?xml version="1.0" encoding="UTF-8"?><test><greeting>hello world</greeting></test> |
    Тогда получить сообщение из топика "test.hello.world", с типом XML соответствующее параметрам из таблицы, и сохранить в переменную "receivedDoc"
      | test/greeting | hello world |


#  Файл greeting.xml
#    <?xml version="1.0" encoding="UTF-8"?>
#    <test>
#      <greeting>hello world</greeting>
#      <customerLogin>anna.smirnova@test.ru</customerLogin>
#    </test>
  Сценарий: Ждём XML-сообщение и проверяем несколько идентификаторов. Body из файла
    Когда выполнено подключение к kafka и отправлено сообщение с параметрами из таблицы
      | type  | name  | value                                               |
      | topic | topic | test.hello.world                                    |
      | body  | body  | src/test/resources/documentation/files/greeting.xml |
    Тогда получить сообщение из топика "test.hello.world", с типом XML соответствующее параметрам из таблицы, и сохранить в переменную "receivedDoc"
      | test/greeting      | hello world           |
      | test/customerLogin | anna.smirnova@test.ru |


  Сценарий: Поиск по header в HEX и jsonpath
    Когда выполнено подключение к kafka и отправлено сообщение с параметрами из таблицы
      | type   | name   | value                                                               |
      | topic  | topic  | test.hello.world                                                    |
      | header | header | any_header                                                          |
      | body   | body   | {"greeting":"hello world","customerLogin": "anna.smirnova@test.ru"} |
    Тогда выполнено подключение к kafka и поиск сообщения в topic "test.hello.world" header "header" с значением "616E795F686561646572" и jsonpath "$.greeting" со значением "hello world"


```

## 3. Сохранение значений в переменные

```gherkin
#language:ru

@kafka
Функционал: Сохранение сообщений для дальнейшего использования

  Сценарий: Сохраняем всё тело сообщения
    Когда выполнено подключение к kafka и отправлено сообщение с параметрами из таблицы
      | type  | name  | value                      |
      | topic | topic | test.hello.world           |
      | body  | body  | {"greeting":"hello world"} |
    Тогда выполнено подключение к kafka и из сообщения из топика "test.hello.world" тело сообщения сохранено в переменную "found" и соответствует параметрам из таблицы
      | $.greeting | hello world |

  Сценарий: Сохраняем всё тело сообщения с множественными условиями
    Когда выполнено подключение к kafka и отправлено сообщение с параметрами из таблицы
      | type  | name  | value                                                               |
      | topic | topic | test.hello.world                                                    |
      | body  | body  | {"greeting":"hello world","customerLogin": "anna.smirnova@test.ru"} |
    Тогда выполнено подключение к kafka и из сообщения из топика "test.hello.world" тело сообщения сохранено в переменную "found" и соответствует параметрам из таблицы
      | $.greeting      | hello world           |
      | $.customerLogin | anna.smirnova@test.ru |

  Сценарий: Сохраняем всё тело сообщения формата JSON с множественными условиями
    Когда выполнено подключение к kafka и отправлено сообщение с параметрами из таблицы
      | type  | name  | value                                                               |
      | topic | topic | test.hello.world                                                    |
      | body  | body  | {"greeting":"hello world","customerLogin": "anna.smirnova@test.ru"} |
    Тогда получить сообщение из топика "test.hello.world", с типом JSON соответствующее параметрам из таблицы, и сохранить в переменную "found"
      | $.greeting      | hello world           |
      | $.customerLogin | anna.smirnova@test.ru |

  Сценарий: Сохраняем всё тело сообщения формата XML
    Когда выполнено подключение к kafka и отправлено сообщение с параметрами из таблицы
      | type  | name  | value                                                                               |
      | topic | topic | test.hello.world                                                                    |
      | body  | body  | <?xml version="1.0" encoding="UTF-8"?><test><greeting>hello world</greeting></test> |
    Тогда получить сообщение из топика "test.hello.world", с типом XML соответствующее параметрам из таблицы, и сохранить в переменную "found"
      | test/greeting | hello world |


  Сценарий: Сохраняем всё тело сообщения при совпадении по jsonPath
    Когда выполнено подключение к kafka и отправлено сообщение с параметрами из таблицы
      | type  | name  | value                      |
      | topic | topic | test.hello.world           |
      | body  | body  | {"greeting":"hello world"} |
    Тогда выполнено подключение к kafka и поиск сообщения в topic "test.hello.world" c параметрами из таблицы сохранено в переменную "found"
      | header/jsonPath | value       |
      | $.greeting      | hello world |


  Сценарий: Сохраняем всё тело сообщения при совпадении по jsonPath и header в uppercase HEX
    Когда выполнено подключение к kafka и отправлено сообщение с параметрами из таблицы
      | type   | name            | value                      |
      | topic  | topic           | test.hello.world           |
      | body   | body            | {"greeting":"hello world"} |
      | header | any_header_name | any_value                  |
    Тогда выполнено подключение к kafka и поиск сообщения в topic "test.hello.world" c параметрами из таблицы сохранено в переменную "found"
      | header/jsonPath | value              |
      | $.greeting      | hello world        |
      | any_header_name | 616E795F76616C7565 |


  Сценарий: Сохраняем всё тело сообщения с поиском по header в uppercase HEX
    Когда выполнено подключение к kafka и отправлено сообщение с параметрами из таблицы
      | type   | name            | value                                                               |
      | topic  | topic           | test.hello.world                                                    |
      | header | any_header_name | any_value                                                           |
      | body   | body            | {"greeting":"hello world","customerLogin": "anna.smirnova@test.ru"} |
    Тогда выполнено подключение к kafka и сообщение из топика "test.hello.world" header "header" со значением "616E795F76616C7565" сохранен в переменную "foundByHeader"


  Сценарий: Сохраняем всё тело сообщения с поиском по key
    Когда выполнено подключение к kafka и отправлено сообщение с параметрами из таблицы
      | type  | name  | value                                                               |
      | topic | topic | test.hello.world                                                    |
      | key   | key   | model.ClientRequest                                                 |
      | body  | body  | {"greeting":"hello world","customerLogin": "anna.smirnova@test.ru"} |
    Тогда выполнено подключение к kafka и из сообщения по ключу "model.ClientRequest" из топика "test.hello.world" тело запроса сохранен в переменную "foundByKey"

```

## 4. Проверка отсутствия сообщений в топике

```gherkin
@kafka
Функционал: Проверка отсутствия сообщений

  Сценарий: Отсутствие сообщения JSON при совпадении только по всем полям
    И выполнено подключение к kafka и отправлено сообщение с параметрами из таблицы
      | type   | name       | value                                                               |
      | header | __TypeId__ | clientRequest                                                       |
      | topic  | topic      | test.hello.world                                                    |
      | body   | body       | {"greeting":"hello world","customerLogin": "anna.smirnova@test.ru"} |
    И отсутствует сообщение в топике "test.hello.world", с типом JSON соответствующее параметрам из таблицы
      | $.greeting      | any other value       |
      | $.customerLogin | anna.smirnova@test.ru |


#  Файл greeting.xml
#    <?xml version="1.0" encoding="UTF-8"?>
#    <test>
#      <greeting>hello world</greeting>
#      <customerLogin>anna.smirnova@test.ru</customerLogin>
#    </test>
  Сценарий: Отсутствие сообщения XML при совпадении только по всем полям
    И выполнено подключение к kafka и отправлено сообщение с параметрами из таблицы
      | type   | name       | value                                               |
      | header | __TypeId__ | clientRequest                                       |
      | topic  | topic      | test.hello.world                                    |
      | body   | body       | src/test/resources/documentation/files/greeting.xml |
    И отсутствует сообщение в топике "test.hello.world", с типом XML соответствующее параметрам из таблицы
      | test/greeting      | any other value       |
      | test/customerLogin | anna.smirnova@test.ru |


```

# Примеры сценариев тестирования Artemis

---

## 1. Подключение

Добавьте тег `@artemis` к сценарию — соединение закроется автоматически после теста.

| Шаг | Описание |
|-----|----------|
| `И создано подключение к artemis` | Создаёт соединение из `artemis.properties`. ClientID генерируется автоматически (UUID). |
| `И закрыть artemis соединение` | Явное закрытие. Необязательно при `@artemis` — хук закроет сам. |

### artemis.properties

Файл должен лежать в `src/main/resources/artemis.properties`. Загружается автоматически при создании подключения.

```
src/
├── main/
│   └── resources/
│       └── artemis.properties   ← обязательно здесь
└── test/
    └── resources/
        └── testdata/            ← путь для примеров в этой статье, в вашем проекте может отличаться
            └── ...
```

Значения можно переопределить через `-D` параметры Maven/CI — они имеют приоритет над файлом:

```bash
mvn test -Dartemis.host=prod-broker -Dartemis.password=secret
```

```properties
# Подключение (обязательные)
artemis.host=localhost
artemis.port=61616
artemis.user=admin
artemis.password=admin

# Опциональные
artemis.protocol=tcp
artemis.ssl.enabled=false

# Таймаут ожидания сообщения в мс. По умолчанию 5000.
# Переопределить: -Dartemis.receive.timeout=30000
artemis.receive.timeout=5000

# SSL (обязательны если artemis.ssl.enabled=true)
# artemis.ssl.keyStore=certs/keystore.jks
# artemis.ssl.keyStorePassword=changeit
# artemis.ssl.trustStore=certs/truststore.jks
# artemis.ssl.trustStorePassword=changeit
```

---

## 2. Queue (Очередь)

Модель: **одно сообщение → один получатель**. Сообщение удаляется из очереди после чтения.

> ✅ Всегда добавляйте `PROPERTY` с `correlationId` — это позволяет читать по фильтру при параллельном запуске тестов.

### 2.1 Отправка

| Шаг | Описание |
|-----|----------|
| `И отправлено сообщение в очередь "PAYMENTS.REQUEST" с параметрами из таблицы` | Отправляет сообщение. Предупреждает если нет PROPERTY. |
| `И отправлено сообщение в очередь "PAYMENTS.REQUEST" с параметрами из таблицы. MessageID сохранен в переменную "messageId"` | Отправляет и сохраняет JMSMessageID. |

**Формат таблицы параметров:**

```gherkin
| type     | name          | value                        |
| PROPERTY | correlationId | ${correlationId}             |  ← для фильтрации при чтении
| BODY     | body          | testdata/create_payment.json |  ← файл, строка или переменная
| VAR      | myVar         | someValue                    |  ← сохранить значение в переменную
```

**Полный сценарий:**

```gherkin
@artemis
Сценарий: Отправить платёж в очередь
  И создано подключение к artemis
  И сгенерировать UUID и сохранить в переменную "correlationId"
  И отправлено сообщение в очередь "PAYMENTS.REQUEST" с параметрами из таблицы
    | type     | name          | value                        |
    | PROPERTY | correlationId | ${correlationId}             |
    | BODY     | body          | testdata/create_payment.json |
```

### 2.2 Чтение

| Шаг | Описание |
|-----|----------|
| `И прочитано первое сообщение из очереди "PAYMENTS.REQUEST". Результат сохранен в переменную "response"` | Читает первое сообщение. Только если очередь гарантированно пустая. |
| `И прочитано сообщение из очереди "PAYMENTS.RESPONSE" по property "correlationId" равному "${correlationId}". Результат сохранен в переменную "response"` | ✅ Основной способ. Фильтр на брокере по одному property. |
| `И прочитано сообщение из очереди "PAYMENTS.RESPONSE" по property таблице` | Несколько property — объединяются через `AND`. |
| `И прочитано сообщение из очереди "PAYMENTS.RESPONSE" по селектору "correlationId = '${correlationId}' AND orderType = 'EXPRESS'". Результат сохранен в переменную "response"` | Сложные условия: `OR`, `LIKE`, `IN`, числа (`amount > 1000`). |

**Примеры:**

```gherkin
# По одному property — основной способ
И прочитано сообщение из очереди "PAYMENTS.RESPONSE" по property "correlationId" равному "${correlationId}"
Результат сохранен в переменную "response"

# По нескольким property через таблицу — объединяются AND
И прочитано сообщение из очереди "PAYMENTS.RESPONSE" по property таблице
  | correlationId | ${correlationId} |
  | paymentType   | TRANSFER         |
Результат сохранен в переменную "response"

# Селектор — только для нестандартных условий
И прочитано сообщение из очереди "PAYMENTS.RESPONSE" по селектору "amount > 1000 AND status IN ('PENDING', 'PROCESSING')"
Результат сохранен в переменную "response"
```

**Полный сценарий:**

```gherkin
@artemis
Сценарий: Отправить запрос и прочитать ответ из очереди
  И создано подключение к artemis
  И сгенерировать UUID и сохранить в переменную "correlationId"
  # Тест → брокер: кладём запрос в PAYMENTS.REQUEST
  И отправлено сообщение в очередь "PAYMENTS.REQUEST" с параметрами из таблицы
    | type     | name          | value                                           |
    | PROPERTY | correlationId | ${correlationId}                                |
    | BODY     | body          | src/test/resources/testdata/create_payment.json |
  # Сервис читает из PAYMENTS.REQUEST, обрабатывает и кладёт ответ в PAYMENTS.RESPONSE
  И прочитано сообщение из очереди "PAYMENTS.RESPONSE" по property "correlationId" равному "${correlationId}"
  Результат сохранен в переменную "response"
  И из json сообщения "response" сохранено значение "$.payment_id" в переменную "paymentId"
```

> 🧪 **Для знакомства с шагами без сервиса** — можно отправить и прочитать из одной и той же очереди.
> В реальных тестах так не делается: сообщение кладёт тест, а ответ кладёт сервис.

```gherkin
@artemis
Сценарий: [Учебный] Отправить и прочитать из одной очереди
  И создано подключение к artemis
  И сгенерировать UUID и сохранить в переменную "correlationId"
  # Отправляем сами
  И отправлено сообщение в очередь "TEST.QUEUE" с параметрами из таблицы
    | type     | name          | value                                           |
    | PROPERTY | correlationId | ${correlationId}                                |
    | BODY     | body          | src/test/resources/testdata/create_payment.json |
  # Читаем то что только что отправили — сервис не нужен
  И прочитано сообщение из очереди "TEST.QUEUE" по property "correlationId" равному "${correlationId}"
  Результат сохранен в переменную "response"
  И json сообщение "response" соответствует таблице
    | $.amount   | 1000       |
    | $.currency | RUB        |
    | $.payerId  | client-123 |
```

### 2.3 Browse (без удаления)

Сообщение остаётся в очереди — используйте для проверки наличия без влияния на потребителей.

| Шаг | Описание |
|-----|----------|
| `И просмотрено первое сообщение из очереди "PAYMENTS.REQUEST". Результат сохранен в переменную "response"` | Первое сообщение без удаления. |
| `И просмотрено сообщение из очереди "AUDIT.QUEUE" по property "correlationId" равному "${correlationId}". Результат сохранен в переменную "auditMsg"` | По одному property без удаления. |
| `И просмотрено сообщение из очереди "AUDIT.QUEUE" по property таблице` | По нескольким property через `AND` без удаления. |
| `И просмотрено сообщение из очереди "AUDIT.QUEUE" по селектору "correlationId = '${correlationId}' AND action = 'PAYMENT_CREATED'". Результат сохранен в переменную "auditMsg"` | По сложному селектору без удаления. |

```gherkin
# Browse по нескольким property
И просмотрено сообщение из очереди "AUDIT.QUEUE" по property таблице
  | correlationId | ${correlationId} |
  | action        | PAYMENT_CREATED  |
Результат сохранен в переменную "auditMsg"
```

**Полный сценарий:**

```gherkin
@artemis
Сценарий: Проверить аудит-запись не удаляя сообщение
  И создано подключение к artemis
  И сгенерировать UUID и сохранить в переменную "correlationId"
  # Тест → брокер: кладём запрос в PAYMENTS.REQUEST
  И отправлено сообщение в очередь "PAYMENTS.REQUEST" с параметрами из таблицы
    | type     | name          | value                                           |
    | PROPERTY | correlationId | ${correlationId}                                |
    | BODY     | body          | src/test/resources/testdata/create_payment.json |
  # Сервис читает запрос и параллельно пишет запись в AUDIT.QUEUE
  # Browse — читаем без удаления, чтобы не мешать другим потребителям AUDIT.QUEUE
  И просмотрено сообщение из очереди "AUDIT.QUEUE" по property "correlationId" равному "${correlationId}"
  Результат сохранен в переменную "auditMsg"
  И json сообщение "auditMsg" соответствует таблице
    | $.action | PAYMENT_CREATED |
```

---

## 3. Topic (Топик)

Модель: **одно сообщение → все подписчики получают свою копию**. Используется durable подписка.

> ⚠️ Подписку нужно создавать **ДО** того как сервис отправит сообщение — иначе оно будет потеряно.

> ✅ Фильтр по property указывается при **создании подписки** — брокер фильтрует при поступлении, не при чтении.

### 3.1 Отправка

| Шаг | Описание |
|-----|----------|
| `И отправлено сообщение в топик "PAYMENT.EVENTS" с параметрами из таблицы` | Отправляет всем активным подписчикам. PROPERTY опционален. |

```gherkin
# Без фильтра — получат все подписчики
И отправлено сообщение в топик "PAYMENT.EVENTS" с параметрами из таблицы
  | type | name | value                                           |
  | BODY | body | src/test/resources/testdata/create_payment.json |

# С PROPERTY — только подписчики у которых совпадёт фильтр
И отправлено сообщение в топик "PAYMENT.EVENTS" с параметрами из таблицы
  | type     | name          | value                        |
  | BODY     | body          | testdata/create_payment.json |
  | PROPERTY | CorrelationID | corr-confirmed               |
  | PROPERTY | eventType     | PAYMENT_CONFIRMED            |
```

**Полный сценарий:**

```gherkin
@artemis
Сценарий: Тест публикует событие в топик
  И создано подключение к artemis
  И создана подписка на топик "PAYMENT.EVENTS" с именем "payment-consumer"
  И отправлено сообщение в топик "PAYMENT.EVENTS" с параметрами из таблицы
    | type     | name      | value                                           |
    | BODY     | body      | src/test/resources/testdata/create_payment.json |
    | PROPERTY | eventType | PAYMENT_CREATED                                 |
  И прочитано сообщение из топика по подписке "payment-consumer". Результат сохранен в переменную "event"
  И json сообщение "event" соответствует таблице
    | $.eventType | PAYMENT_CREATED |
  И удалена подписка на топик с именем "payment-consumer"
```

### 3.2 Подписка

| Шаг | Описание |
|-----|----------|
| `И создана подписка на топик "PAYMENT.EVENTS" с именем "payment-consumer"` | Без фильтра — получает все сообщения. |
| `И создана подписка на топик "PAYMENT.EVENTS" с именем "confirmed-consumer" по property "eventType" равному "PAYMENT_CONFIRMED"` | С одним property фильтром на брокере. |
| `И создана подписка на топик "PAYMENT.EVENTS" с именем "created-consumer" с property таблицей` | С несколькими фильтрами — объединяются через `AND`. |

```gherkin
# Получит только сообщения где ОБА условия совпадают
И создана подписка на топик "PAYMENT.EVENTS" с именем "created-consumer" с property таблицей
  | CorrelationID | ${correlationId} |
  | eventType     | PAYMENT_CREATED  |

# Результирующий JMS селектор на брокере:
# CorrelationID = '${correlationId}' AND eventType = 'PAYMENT_CREATED'
```

**Полный сценарий — несколько подписчиков с разными фильтрами:**

```gherkin
@artemis
Сценарий: Проверить фильтрацию событий по подписчикам
  И создано подключение к artemis
  # payment-consumer — получает все события
  И создана подписка на топик "PAYMENT.EVENTS" с именем "payment-consumer"
  # confirmed-consumer — только PAYMENT_CONFIRMED
  И создана подписка на топик "PAYMENT.EVENTS" с именем "confirmed-consumer"
    по property "eventType" равному "PAYMENT_CONFIRMED"
  # Отправляем PAYMENT_CREATED — только payment-consumer получит
  И отправлено сообщение в топик "PAYMENT.EVENTS" с параметрами из таблицы
    | type     | name      | value                                           |
    | BODY     | body      | src/test/resources/testdata/create_payment.json |
    | PROPERTY | eventType | PAYMENT_CREATED                                 |
  # Отправляем PAYMENT_CONFIRMED — получат оба
  И отправлено сообщение в топик "PAYMENT.EVENTS" с параметрами из таблицы
    | type     | name      | value                                            |
    | BODY     | body      | src/test/resources/testdata/confirm_payment.json |
    | PROPERTY | eventType | PAYMENT_CONFIRMED                                |
  И прочитано сообщение из топика по подписке "payment-consumer". Результат сохранен в переменную "firstEvent"
  И прочитано сообщение из топика по подписке "confirmed-consumer". Результат сохранен в переменную "confirmedEvent"
  И json сообщение "firstEvent" соответствует таблице
    | $.eventType | PAYMENT_CREATED |
  И json сообщение "confirmedEvent" соответствует таблице
    | $.eventType | PAYMENT_CONFIRMED |
  И удалена подписка на топик с именем "payment-consumer"
  И удалена подписка на топик с именем "confirmed-consumer"
```

### 3.3 Чтение

| Шаг | Описание |
|-----|----------|
| `И прочитано сообщение из топика по подписке "payment-consumer". Результат сохранен в переменную "event"` | Читает первое сообщение из персональной очереди подписчика. Фильтр уже применён брокером при создании подписки. |

### 3.4 Удаление подписки

| Шаг | Описание |
|-----|----------|
| `И удалена подписка на топик с именем "payment-consumer"` | Удаляет подписку с брокера. Автоматически при `@artemis`. |

---

## 4. Проверки сообщений

Применяются к сообщениям из Queue и Topic одинаково — после сохранения в переменную.
Перед каждой проверкой автоматически валидируется формат — при несоответствии выдаётся
информативная ошибка с первыми 300 символами тела сообщения.

### 4.1 JSON

| Шаг | Описание |
|-----|----------|
| `И json сообщение "response" соответствует таблице` | Проверяет поля по JSONPath. |
| `И json сообщение "response" соответствует файлу "testdata/expected_payment_response.json" игнорируя поля` | Сравнивает с эталонным файлом, игнорируя динамичные поля. |
| `И из json сообщения "response" сохранено значение "$.payment_id" в переменную "paymentId"` | Извлекает значение по JSONPath в переменную. |

```gherkin
И json сообщение "response" соответствует таблице
  | $.status           | SUCCESS     |
  | $.payment.amount   | 1000        |
  | $.payment.currency | RUB         |
  | $.payment.payerId  | client-123  |

И json сообщение "response" соответствует файлу "src/test/resources/testdata/expected_payment_response.json" игнорируя поля
  | $.payment_id |
  | $.timestamp  |
  | $.request_id |

И из json сообщения "response" сохранено значение "$.payment_id" в переменную "paymentId"
```

**Полный сценарий:**

```gherkin
@artemis
Сценарий: Проверить JSON ответ из очереди
  И создано подключение к artemis
  И сгенерировать UUID и сохранить в переменную "correlationId"
  # Тест → брокер: кладём запрос в PAYMENTS.REQUEST
  И отправлено сообщение в очередь "PAYMENTS.REQUEST" с параметрами из таблицы
    | type     | name          | value                                           |
    | PROPERTY | correlationId | ${correlationId}                                |
    | BODY     | body          | src/test/resources/testdata/create_payment.json |
  # Сервис читает из PAYMENTS.REQUEST, обрабатывает и кладёт ответ в PAYMENTS.RESPONSE
  И прочитано сообщение из очереди "PAYMENTS.RESPONSE" по property "correlationId" равному "${correlationId}"
  Результат сохранен в переменную "response"
  # Проверка отдельных полей
  И json сообщение "response" соответствует таблице
    | $.status           | SUCCESS    |
    | $.payment.amount   | 1000       |
    | $.payment.currency | RUB        |
    | $.payment.payerId  | client-123 |
  # Сравнение с эталонным файлом, игнорируя динамичные поля
  И json сообщение "response" соответствует файлу "testdata/expected_payment_response.json" игнорируя поля
    | $.payment_id |
    | $.timestamp  |
    | $.request_id |
  # Сохраняем ID для следующего шага
  И из json сообщения "response" сохранено значение "$.payment_id" в переменную "paymentId"
```

### 4.2 XML

| Шаг | Описание |
|-----|----------|
| `И xml сообщение "response" соответствует таблице` | Проверяет узлы по XPath. |
| `И xml сообщение "response" соответствует файлу "testdata/expected_payment_response.xml" игнорируя поля` | Сравнивает с эталонным XML файлом. |
| `И из xml сообщения "response" сохранено значение "//PaymentResponse/PaymentId" в переменную "paymentId"` | Извлекает значение по XPath в переменную. |

```gherkin
И xml сообщение "response" соответствует таблице
  | //PaymentResponse/Status   | OK   |
  | //PaymentResponse/Currency | RUB  |
  | //PaymentResponse/Amount   | 1000 |

И xml сообщение "response" соответствует файлу "testdata/expected_payment_response.xml" игнорируя поля
  | //PaymentResponse/PaymentId  |
  | //PaymentResponse/Timestamp  |

И из xml сообщения "response" сохранено значение "//PaymentResponse/PaymentId" в переменную "paymentId"
```

**Полный сценарий:**

```gherkin
@artemis
Сценарий: Проверить XML ответ из очереди
  И создано подключение к artemis
  И сгенерировать UUID и сохранить в переменную "correlationId"
  # Тест → брокер: кладём XML запрос в XML.REQUEST
  И отправлено сообщение в очередь "XML.REQUEST" с параметрами из таблицы
    | type     | name          | value                                           |
    | PROPERTY | correlationId | ${correlationId}                                |
    | BODY     | body          | src/test/resources/testdata/payment_request.xml |
  # Сервис читает из XML.REQUEST, обрабатывает и кладёт XML ответ в XML.RESPONSE
  И прочитано сообщение из очереди "XML.RESPONSE" по property "correlationId" равному "${correlationId}"
  Результат сохранен в переменную "response"
  И xml сообщение "response" соответствует таблице
    | //PaymentResponse/Status   | OK   |
    | //PaymentResponse/Currency | RUB  |
    | //PaymentResponse/Amount   | 1000 |
  И xml сообщение "response" соответствует файлу "src/test/resources/testdata/expected_payment_response.xml" игнорируя поля
    | //PaymentResponse/PaymentId |
    | //PaymentResponse/Timestamp |
  И из xml сообщения "response" сохранено значение "//PaymentResponse/PaymentId" в переменную "paymentId"
```

### 4.3 Текст и заголовки

| Шаг | Описание |
|-----|----------|
| `И сообщение "response" содержит текст "SUCCESS"` | Проверяет вхождение текста в тело сообщения. |
| `И заголовки сообщения "response" соответствуют таблице` | Проверяет JMS properties (заголовки) сообщения. |

```gherkin
И сообщение "response" содержит текст "SUCCESS"

И заголовки сообщения "response" соответствуют таблице
  | correlationId | ${correlationId} |
  | eventType     | PAYMENT_CREATED  |
  | version       | 2.0              |
```

**Полный сценарий:**

```gherkin
@artemis
Сценарий: Проверить текст и заголовки ответного сообщения
  И создано подключение к artemis
  И сгенерировать UUID и сохранить в переменную "correlationId"
  # Тест → брокер: кладём запрос в PAYMENTS.REQUEST
  И отправлено сообщение в очередь "PAYMENTS.REQUEST" с параметрами из таблицы
    | type     | name          | value                        |
    | PROPERTY | correlationId | ${correlationId}             |
    | BODY     | body          | testdata/create_payment.json |
  # Сервис читает из PAYMENTS.REQUEST, обрабатывает и кладёт ответ в PAYMENTS.RESPONSE
  И прочитано сообщение из очереди "PAYMENTS.RESPONSE" по property "correlationId" равному "${correlationId}"
  Результат сохранен в переменную "response"
  И сообщение "response" содержит текст "SUCCESS"
  И заголовки сообщения "response" соответствуют таблице
    | correlationId | ${correlationId} |
    | eventType     | PAYMENT_CREATED  |
    | version       | 2.0              |
```

---

## 5. Ключевые отличия Queue vs Topic

| | Queue | Topic |
|---|---|---|
| Получатель | Один | Все подписчики |
| Сообщение после чтения | Удаляется | Остаётся для других |
| Фильтр | При **чтении** (property / таблица / селектор) | При **создании подписки** (property / таблица) |
| Изоляция тестов | Через `correlationId` в PROPERTY | Через персональную очередь подписчика |
| Порядок шагов | Отправить → Прочитать | Подписаться → Триггер → Прочитать |

---

## 6. Тестовые данные

Все файлы располагаются в `src/test/resources/testdata/`.

---

### `create_payment.json`
Запрос на создание платежа. Используется в Queue отправке и E2E сценариях.

```json
{
  "amount": 1000,
  "currency": "RUB",
  "payerId": "client-123",
  "recipientId": "merchant-456",
  "description": "Оплата заказа #789",
  "paymentType": "TRANSFER"
}
```

---

### `confirm_payment.json`
Запрос на подтверждение платежа. Используется в сценариях цепочки запросов.

```json
{
  "action": "CONFIRM",
  "confirmedBy": "operator-001",
  "comment": "Подтверждено оператором"
}
```

---

### `invalid_payment.json`
Невалидный запрос с отрицательной суммой. Используется в негативных тестах.

```json
{
  "amount": -100,
  "currency": "RUB",
  "payerId": "client-123",
  "recipientId": "merchant-456"
}
```

---

### `expected_payment_response.json`
Эталонный ответ для сравнения через `соответствует файлу ... игнорируя поля`.
Поля `payment_id`, `timestamp`, `request_id` динамичные — указываются в таблице игнорирования.

```json
{
  "status": "SUCCESS",
  "payment_id": "IGNORED",
  "timestamp": "IGNORED",
  "request_id": "IGNORED",
  "payment": {
    "amount": 1000,
    "currency": "RUB",
    "payerId": "client-123",
    "recipientId": "merchant-456",
    "type": "TRANSFER"
  }
}
```

---

### `payment_request.xml`
XML запрос на создание платежа. Используется в XML Queue сценариях.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<PaymentRequest>
  <Amount>1000</Amount>
  <Currency>RUB</Currency>
  <PayerId>client-123</PayerId>
  <RecipientId>merchant-456</RecipientId>
  <Description>Оплата заказа #789</Description>
</PaymentRequest>
```

---

### `expected_payment_response.xml`
Эталонный XML ответ для сравнения через `соответствует файлу ... игнорируя поля`.
Поля `PaymentId` и `Timestamp` динамичные — указываются в таблице игнорирования.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<PaymentResponse>
  <Status>OK</Status>
  <Currency>RUB</Currency>
  <Amount>1000</Amount>
  <PaymentId>IGNORED</PaymentId>
  <Timestamp>IGNORED</Timestamp>
</PaymentResponse>
```

## Требования
Данный модуль зависит от модуля akita-gpb-core-module этого же проекта. <br>
Но эти зависимости добавляются посредством собранных jar-файлов, а не ссылкой на исходники модулей.<br>
Соответственно, для успешной сборки данного модуля jar-архивы зависимых модулей должен быть предварительно опубликованы в репозитории (локальном или
публичном).

## Зависимости
```
ru.gazprombank.automation:akita-gpb-core-module
```

## Code style
В akita-gpb-модулях используются плагины spotless и google-java-format

## Maintainers
Кайдаш Илья Васильевич - `ilya.kaydash@gazprombank.ru`<br>
Кислый Роман Александрович - `roman.kisly@gazprombank.ru`<br>
Мишечкин Павел Владимирович - `pavel.mishechkin@gazprombank.ru`<br>

## Контактная информация
За сервис отвечает команда "Инструменты тестирования" ([BS ИНТ](https://backstage.int.gazprombank.ru/catalog/default/group/qa-tools-team), [BS ВКР](https://backstage.dev.gazprombank.ru/catalog/default/group/qa-tools-team))