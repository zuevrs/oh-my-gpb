# Akita GPB module akita-gpb-api-module
## Назначение сервиса
Сервис akita-gpb-api-module предназначен для упрощения автоматизации тестирования API в рамках фреймворка Akita-BDD. Он предоставляет классы для реализации BDD-шагов, которые позволяют отправлять HTTP-запросы (GET, POST, PUT, DELETE, PATCH) к API и обрабатывать ответы. Модуль включает библиотеки JsonPath (Jayway и RestAssured) для парсинга и извлечения данных из JSON-ответов, а также поддержку проверки XML-ответов. Сервис относится к домену автоматизации тестирования API.
## Описание процесса
В сервисе реализован следующий бизнес-процесс: Автоматизация тестирования API:
1. Выполнение HTTP-запроса (GET, POST, PUT, DELETE, PATCH) к указанному URL.
2. Сохранение ответа в переменную или проверка кода ответа.
3. Извлечение данных из JSON/XML-ответа с использованием JsonPath или XmlPath.
4. Проверка существования или значений элементов в ответе.
5. Сохранение значений из ответа (cookie, header, body) в переменные.
6. Валидация JSON/XML-ответов на соответствие схемам.
7. Обработка ошибок и формирование результатов тестирования.

![Scheme.PNG](./techdocs/docs/uml/scheme.png)

## Список шагов модуля
Общие шаги:
```
@И("^выполнен (GET|POST|PUT|DELETE|PATCH) запрос на URL \"(.*)\". Полученный ответ сохранен в переменную \"(.*)\"$")
@И("^выполнен GET\\(SSE\\) запрос на URL \"(.*)\". Полученный ответ сохранен в переменную \"(.*)\"$")
@И("^выполнен (GET|POST|PUT|DELETE|PATCH) запрос на URL \"(.*)\" с headers и parameters из таблицы. Полученный ответ сохранен в переменную \"(.*)\"$")
@И("^выполнен (GET|POST|PUT|DELETE|PATCH) запрос на URL \"(.*)\". Ожидается код ответа: (\\d+)$")
@И("^выполнен (GET|POST|PUT|DELETE|PATCH) запрос на URL \"(.*)\" с headers и parameters из таблицы. Ожидается код ответа: (\\d+)$")
@И("^в ответе \"(.*)\" код ответа равен (\\d+)$")
@И("^из ответа \"(.*)\" cookie с именем \"(.*)\" сохранен в переменную \"(.*)\"$")
@И("^из ответа \"(.*)\" header с именем \"(.*)\" сохранен в переменную \"(.*)\"$")
@И("^из ответа \"(.*)\" значение тела по пути \"(.*)\" сохранено в переменную \"(.*)\"$")
@И("^из ответа \"(.*)\" значение тела сохранено в переменную \"(.*)\"$")
@Тогда("^в json (?:строке|файле) \"(.*)\" элементы, найденные по jsonpath из таблицы, существуют$")
@И("^в json (?:строке|файле) \"(.*)\" элементы, найденные по jsonpath из таблицы, не существуют$")
@Тогда("^в json строке \"(.*)\" значения, найденные по jsonpath, равны значениям из таблицы$")
@Тогда("^в ответе \"(.*)\" значения, найденные по jsonpath, равны значениям из таблицы$")
@Тогда("^значения из json строки \"(.*)\", найденные по jsonpath из таблицы, сохранены в переменные$")
@Тогда("^значения из ответа \"(.*)\", найденные по jsonpath из таблицы, сохранены в переменные$")
@И("^значения json \"(.*)\", соответствует схеме \"(.*)\"$")
@И("^в xml (?:строке|файле) \"(.*)\" значения, найденные по xmlpath, равны значениям из таблицы$")
@И("^в xml (?:строке|файле) \"(.*)\" элементы, найденные по xmlpath из таблицы, существуют$")
@И("^в xml (?:строке|файле) \"(.*)\" элементы, найденные по xmlpath из таблицы, не существуют$")
@И("^значения из xml (?:строки|файла) \"(.*)\", найденные по xmlpath из таблицы, сохранены в переменные$")
@И("^значения xml \"(.*)\", соответствует схеме \"(.*)\"$")
```

## Примеры типовых сценариев API-тестирования  

(Cucumber + RestAssured + Akita)

### 1. Простой GET-запрос

```gherkin
@api @get
Сценарий: GET запрос. Получение одного поста
  Когда выполнен GET запрос на URL "https://example.com/posts/1". Полученный ответ сохранен в переменную "response"
  Тогда в ответе "response" код ответа равен 200

  # Пример ответа:
  # {
  #   "userId": 1,
  #   "postId": 1, 
  #   "title": "Значение поля title",
  #   "body": "Значение поля body"
  # }

  И из ответа "response" значение тела по пути "$.userId" сохранено в переменную "userId"
  И из ответа "response" значение тела по пути "$.title" сохранено в переменную "title"
  И из ответа "response" значение тела по пути "$.body" сохранено в переменную "body"

  Тогда значение переменной "userId" равно "1"
  И значение переменной "title" равно "Значение поля title"
  И значение переменной "body" равно "Значение поля body"`
  ```
  
### 2. GET с query-параметрами
  
```gherkin
@api @get
Сценарий: GET запрос. Получение постов пользователя с query-параметрами
  Когда выполнен GET запрос на URL "https://example.com/posts" с headers и parameters из таблицы. Полученный ответ сохранен в переменную "response"
    | type      | name     | value |
    | PARAMETER | userId   | 55    |
    | PARAMETER | _limit   | 2     |

  Тогда в ответе "response" код ответа равен 200

  # Пример ответа:
  # [
  #   {"userId":55, "postId":51, "title":"...", "body":"..."},
  #   {"userId":55, "postId":52, "title":"...", "body":"..."}
  # ]

  И в ответе "response" значения, найденные по jsonpath, равны значениям из таблицы:
    | $..userId.length()   | 2   |
    | $[0].userId          | 55  |
    | $[1].userId          | 55  |
```

### 3. POST — создание поста

**Файл test.json, который будет испольозован как переменная Акиты:**
```
{
   "title": "Значение поля title",
   "body": "Значение поля body",
   "userId": 55      
}
```

```gherkin
@api @post
Сценарий: POST запрос. Создание нового поста
  Когда выполнен POST запрос на URL "https://example.com/users" с headers и parameters из таблицы. Полученный ответ сохранен в переменную "response"
    | type   | name          | value             |
    | HEADER | Content-Type  | application/json  |
    | BODY   | body          | test.json         |

  Тогда в ответе "response" код ответа равен 201

  # Пример ответа:
  # {
  #   "title": "Значение поля title",
  #   "body": "Значение поля body",
  #   "userId": 55,
  #   "postId": 53
  # }

  И из ответа "response" значение тела по пути "$.title" сохранено в переменную "returnedTitle"
  И из ответа "response" значение тела по пути "$.userId" сохранено в переменную "returnedUserId"
  И из ответа "response" значение тела по пути "$.id" сохранено в переменную "newPostId"

  Тогда значение переменной "returnedTitle" равно "Значение поля title"
  И значение переменной "returnedUserId" равно "55"
  И значение переменной "newPostId" равно "53"
  ```

### 4. PUT — полное обновление поста

**Файл test.json, который будет испольозован как переменная Акиты:**
```
{ 
  "title": "Измененный параметр title для проверки PUT",
   "body": "Измененный параметр body для проверки PUT",
   "userId": 55,
   "postId": 53
}
```

```gherkin
@api @put
Сценарий: PUT запрос. Полное обновление поста
  Когда выполнен PUT запрос на URL "https://example.com/posts/53" с headers и parameters из таблицы. Полученный ответ сохранен в переменную "response"
    | type   | name          | value                              |
    | HEADER | Content-Type  | application/json                   |
    | BODY   | body          | test.json                          |

  Тогда в ответе "response" код ответа равен 200

  # Пример ответа:
  # {
  #   "title": "Измененный параметр title для проверки PUT",
  #   "body": "Измененный параметр body для проверки PUT",
  #   "userId": 55,
  #   "postId": 53
  # }

  И в ответе "response" значения, найденные по jsonpath, равны значениям из таблицы:
    | $.title    | Измененный параметр title для проверки PUT   |
    | $.body     | Измененный параметр body для проверки PUT    |
    | $.userId   | 55                                           |
```

### 5. PATCH — частичное обновление поста

```
{
    "title": "Измененный параметр title для проверки для проверки PATCH"
}
```

```gherkin
@api @patch
Сценарий: PATCH запрос. Частичное обновление поста
  Когда выполнен PATCH запрос на URL "https://example.com/posts/53" с headers и parameters из таблицы. Полученный ответ сохранен в переменную "response"
    | type   | name          | value                    |
    | HEADER | Content-Type  | application/json         |
    | BODY   | body          | test.json                |

  # Пример ответа:
  # {
  #   "title": "Измененный параметр title для проверки для проверки PATCH",
  #   "body": "Значение поля body",
  #   "userId": 55,
  #   "postId": 53
  # }   

  Тогда в ответе "response" код ответа равен 200
  И из ответа "response" значение тела по пути "$.title" сохранено в переменную "updatedTitle"
  Тогда значение переменной "updatedTitle" равно "Измененный параметр title для проверки для проверки PATCH"
  ```

### 6. DELETE — удаление

```gherkin
@api @delete
Сценарий: DELETE-запрос. Удаление поста
  Когда выполнен DELETE запрос на URL "https://example.com/posts/1"
  Тогда в ответе "response" код ответа равен 200
  # Обычно возвращается пустой объект {}
  ```

### 7. Multipart — отправка файлов и форм-данных

```gherkin
@api @multipart @post
Сценарий: POST запрос. Создание поста с прикреплённым файлом  (например, скриншотом или PDF)
  Когда выполнен POST запрос на URL "https://example.com/posts" с headers и parameters из таблицы. Полученный ответ сохранен в переменную "response"
   | type            | name      | value                                      |
   | MULTIPART       | title     | Новое  значение поля title                 |
   | MULTIPART       | body      | Новое  значение поля body                  |
   | MULTIPART       | userId    | 55                                         |
   | MULTIPART_FILE  | file      | test-data/screenshot_20260205.png          |

  Тогда в ответе "response" код ответа равен 201

  # Пример ответа:
  # {
  #   "title": "Новое  значение поля title",
  #   "body": "Новое  значение поля body",
  #   "userId": 55,
  #   "postId": 54,
  #   "screenshot": {
  #     "filename": "creenshot_20260205.png",
  #     "size": 342156,
  #     "mimeType": "image/png"}
  # }

   # Проверяем, что пост создался корректно
    И в ответе "response" значения, найденные по jsonpath, равны значениям из таблицы:
      | $.title                 | Новое  значение поля title                         |
      | $.body                  | Новое  значение поля body                          |
      | $.userId                | 55                                                 |
      | $.postId                | 54                                                 |
      | $.screenshot.filename   | screenshot_20260205.png                            |
      | $.screenshot.mimeType   | image/png                                          |
      | $.screenshot.size       | 342156                                             |
```

### 8. XML. Отправка GET-запроса

```gherkin
@api @xml @get
Сценарий: GET-запрос. Получение данных в формате XML. Получение 1 поста
  Когда выполнен GET запрос на URL "https://example.com/posts/1" с headers и parameters из таблицы. Полученный ответ сохранен в переменную "response"
    | type   | name    | value            |
    | HEADER | Accept  | application/xml  |

  Тогда в ответе "response" код ответа равен 200

  # Пример ответа:
  # <post>
  #   <title>Значение поля title</title>
  #   <body>Значение поля body</body>
  #   <userId>1</userId>
  #   <postId>1</postId>
  # </post>

  И в xml строке "response" значения, найденные по xmlpath, равны значениям из таблицы:
    | //post/title      | Значение поля title   |
    | //post/body       | Значение поля body    |
    | //post/postId     | 1                     |
```

### 9. XML. Отправка POST-запроса

**Файл test.xml, который будет испольозован как переменная Акиты:**

```
<?xml version="1.0" encoding="UTF-8"?>
<post>
    <userid>55</userid>
    <body>Новое значение поля body</body>
    <title>Новое значение поля title</title>
</post>
```

```gherkin
@api @xml @post
Сценарий: POST-запрос. Отправка XML в теле запроса.  Создание нового поста
  Когда выполнен POST запрос на URL "https://example.com/posts" с headers и parameters из таблицы. Полученный ответ сохранен в переменную "response"
    | type   | name          | value                 | 
    | HEADER | Content-Type  | application/xml       |
    | BODY   | body          | test.xml              |

  Тогда в ответе "response" код ответа равен 201

  # Пример ответа:
  # <post>
  #   <title>Новое значение поля title</title>
  #   <body>Новое значение поля body</body>
  #   <userId>55</userId>
  #   <postId>58</postId>
  # </post>

  И в xml строке "response" значения, найденные по xmlpath, равны значениям из таблицы:
    | //post/title      | Новое значение поля title   |
    | //post/body       | Новое значение поля body    |
    | //post/postId     | 58                          |
```

### 10. Проверка структуры ответа (JSON)

```gherkin
@api @get @json
Сценарий: GET-запрос. Проверка наличия обязательных полей в JSON
Когда выполнен GET запрос на URL "https://example.com/posts/1". Полученный ответ сохранен в переменную "response"
  Тогда в ответе "response" код ответа равен 200

  И в ответе "response" элементы, найденные по jsonpath из таблицы, существуют:
    | $.userId |
    | $.postId |
    | $.title  |
    | $.body   |
```

### 11. Проверка структуры ответа (XML)

```gherkin
@api @get @xml
Сценарий: GET-запрос. Проверка наличия обязательных полей в XML
  Когда выполнен GET запрос на URL "https://example.com/posts/1" с headers и parameters из таблицы. Полученный ответ сохранен в переменную "response"
    | type   | name     | value            |
    | HEADER | Accept   | application/xml  |

  Тогда в ответе "response" код ответа равен 200

  И в xml строке "response" значения, найденные по xmlpath, равны значениям из таблицы:
    | //post/title    | 
    | //post/body     | 
    | //post/postId   |
```

### 12. Параметризованные сценарии (примеры)

**Файл test.json, который будет испольозован как переменная Акиты:**

```
 {
  "title":"{jsonTitle}",
  "body":"{jsonBody}",
  "userId":{jsonUserId}
 }
```

```gherkin
@api @post
Сценарий-шаблон: POST-запрос. Создание постов с динамическими параметрами
  * установлено значение переменной "jsonTitle" равным "<title>"
  * установлено значение переменной "jsonBody" равным "<body>"
  * установлено значение переменной "jsonUserId" равным "<userId>"
  Когда выполнен POST запрос на URL "https://example.com/posts" с headers и parameters из таблицы. Полученный ответ сохранен в переменную "response"
    | type   | name          | value                              |
    | HEADER | Content-Type  | application/json                   |
    | BODY   | body          | test.json                          |

  Тогда в ответе "response" код ответа равен 201
  И из ответа "response" значение тела по пути "$.title" сохранено в переменную "returnedTitle"
  Тогда значение переменной "returnedTitle" равно "<title>"

Примеры:
  | title                              | userId |
  | Важное объявление                  | 55     |
  | Срочное обновление системы         | 12     |
  | Результаты тестирования API        | 99     |
  | Проверка новой функциональности    | 1      |
  ```

### 13. Простой GET-запрос с авторизацией

```gherkin
@api @get
Сценарий: GET запрос. Получение одного поста с авторизацией
  Когда выполнен GET запрос на URL "https://example.com/posts/1" с headers и parameters из таблицы. Полученный ответ сохранен в переменную "response"
  | type   | name          | value                |
  | HEADER | Content-Type  | application/json     |
  | HEADER | Authorization | ${token_variable}    |
  Тогда в ответе "response" код ответа равен 200

  # Пример ответа:
  # {
  #   "userId": 1,
  #   "postId": 1, 
  #   "title": "Значение поля title",
  #   "body": "Значение поля body"
  # }

  И из ответа "response" значение тела по пути "$.userId" сохранено в переменную "userId"
  И из ответа "response" значение тела по пути "$.title" сохранено в переменную "title"
  И из ответа "response" значение тела по пути "$.body" сохранено в переменную "body"

  Тогда значение переменной "userId" равно "1"
  И значение переменной "title" равно "Значение поля title"
  И значение переменной "body" равно "Значение поля body"`
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

## Это шаблон проекта для разработки собственных модулей для акиты.

**Как сделать свой модуль по шаблону:**

- выбрать новое имя для модуля и отвести [форк](https://bitbucket.dev.gazprombank.ru/projects/QA/repos/akita-gpb-module-template?fork) от репозитория [qa/akita-gpb-module-template](https://bitbucket.dev.gazprombank.ru/projects/QA/repos/akita-gpb-module-template/browse)
- заменить в [текущем](README.md) файле весь текст `akita-gpb-api-module` на имя вашего плагина 
- заполнить [gpb-manifest.json](gpb-manifest.json)
- указать версию вашего нового плагина в файле [version.properties](version.properties)
- заполнить `rootProject.name` в файле [settings.gradle](settings.gradle)
- проверить версии зависимостей в файле [gradle.properties](gradle.properties), поднять, если нужно
- проверить [build.gradle](build.gradle) и указать свои зависимости, если нужно
- сделать описание плагина по примеру ниже
- реализовать код модуля в пакете `src/main/java/ru/gazprombank/automation/akitagpb/modules`
- попробуйте собраться командой `.\gradlew clean build` или `.\gradlew clean build -x spotlessJavaCheck`


**Публикация модуля в Nexus через pipeline:**
- зарегистрируйте модуль [в списке общих](https://confluence.dev.gazprombank.ru/pages/viewpage.action?pageId=59244598) библиотек [джобом](https://teamcity.dev.gazprombank.ru/buildConfiguration/Tools_GpbLibraries_Register?buildTab=log&logView=flowAware&focusLine=0)
- добавьте информацию о модуле [в пайплайн](https://bitbucket.dev.gazprombank.ru/projects/QA/repos/kotlindsl-common/browse/.teamcity/files/yaml), который будет собирать и публиковать его в нексус
- в области тимсити [QA](https://teamcity.dev.gazprombank.ru/project/QA_Akita_Universal_VsQuality?mode=builds#all-projects) должен появиться подпроект с именем вашего модуля
- [обновите](https://teamcity.dev.gazprombank.ru/admin/editProject.html?projectId=QA_Akita_Universal&tab=versionedSettings) проект в тимсити, если не появился нужный раздел
- в репозиторие вашего модуля в битбакет на коммите должен стоять тег с версией по семвер, который будете собирать. Без тега не сможете запустить пайплайн
- запустите пайплайн `Universal/vs-quality/<имя модуля>/build/Start`

## Maintainers
Кайдаш Илья Васильевич - `ilya.kaydash@gazprombank.ru`<br>
Кислый Роман Александрович - `roman.kisly@gazprombank.ru`<br>
Мишечкин Павел Владимирович - `pavel.mishechkin@gazprombank.ru`<br>
