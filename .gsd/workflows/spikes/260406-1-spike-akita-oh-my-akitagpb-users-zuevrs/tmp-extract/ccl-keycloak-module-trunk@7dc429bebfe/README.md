# ccl-keycloak-module
## Назначение сервиса
Сервис ccl-keycloak-module предназначен для взаимодействия с сервисом авторизации Keycloak, обеспечивая получение токенов доступа (Bearer) для указанных пользователей. Токены кэшируются локально на время теста и используются в REST-запросах или для доступа к UI-страницам. Сервис относится к домену аутентификации и авторизации в автоматизации тестирования.
## Описание процесса
В сервисе реализован следующий бизнес-процесс: Управление Keycloak-токенами:
1. Получение токена доступа для пользователя через Keycloak с использованием конфигурации из .conf-файла.
2. Кэширование токенов в локальном хранилище на время теста с очисткой перед каждым тестом через @Before-хук.
3. Использование токенов в последующих REST-запросах или UI-взаимодействиях.

<img src="https://bitbucket.dev.gazprombank.ru/projects/QA/repos/akita-gpb-core-module/raw/techdocs/docs/uml/scheme.png?at=refs%2Fheads%2Ftrunk" alt="Схема зависимостей" width="800" height="776">

## Конфигурация
Перед каждым тестом кэш (локальное хранилище) токенов очищается в @Before-хуке.<br>
Для работы модуля необходимо в .conf-файле вашего проекта указать host-url Keycloak-сервиса:
```
hosts {
    keycloak {
        url = "https://ebg-keycloak-demo.xxx.xxx.gazprombank.ru"
    }
}
```
а также в .conf-файле вашего проекта необходимо в блоке users добавить конфиги для каждого используемого пользователя:
```
users {
    # Пользователь кейлоак
    keycloakUser {
        login = "autotestuser"
        password = "pass"
        client_id = "sso_test"
        name = "Пользователь Автотестов"
    }
    # Пользователь экосистемы
    ecoUser {
        login = "test123"
        password = "pass"
        code = "1"
        client_id = "c90da473-04d7-470d-9fc9-c695d398e9af"
    }
}
```
Тогда шаг модуля можно использовать так:
```
И получить access token из Keycloak для пользователя с именем "keycloakUser" и сохранить в переменную "bearerToken"
...
И выполнен POST запрос на URL "${hosts.limitManager.url}/test-api/bpm" с headers и parameters из таблицы. Полученный ответ сохранен в переменную "response"
    | type   | name          | value                                               |
    | header | Content-Type  | application/json; charset=utf-8                     |
	| header | Authorization | Bearer ${bearerToken}                               |
	| body   |               | ${test.data.base.path}/common/services/request.json |
```

## Список шагов модуля
```
@И("^получить access token из Keycloak для пользователя с именем \"(.+)\" и сохранить в переменную \"(.+)\"$")
```

## Список хуков модуля
- _**cleanLocalStorage**_ - @Before-хук (выполняется для каждого теста), очищает кэшированные в локальном хранилище ранее полученные токены
  пользователей.<br>

## Требования
Данный модуль зависит от модуля ccl-helpers-module этого же проекта. <br>
Но эта зависимость добавляется посредством собранного jar-файла, а не ссылкой на исходники модуля.<br>
Соответственно, для успешной сборки данного модуля jar-архив зависимого модуля должен быть предварительно опубликован в репозитории (локальном или
публичном).<br>
Также для работы модуля необходимо в .conf-файле вашего проекта указать host-url Keycloak-сервиса и параметры для каждого используемого
пользователя (см. Описание).

## Зависимости
```
ru.gazprombank.automation:ccl-helpers-module
```

## Code style
В akita-gpb-модулях используются плагины spotless и google-java-format

## Maintainers
Кайдаш Илья Васильевич - `ilya.kaydash@gazprombank.ru`<br>
Кислый Роман Александрович - `roman.kisly@gazprombank.ru`<br>
Мишечкин Павел Владимирович - `pavel.mishechkin@gazprombank.ru`<br>
