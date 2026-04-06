# Akita GPB module akita-gpb-hooks-module

## Назначение сервиса
Сервис akita-gpb-hooks-module содержит хуки для фреймворка Akita-BDD, включая Allure (интеграция с Selenium, RestAssured и цензура паролей) и Docker (запуск и остановка Docker Compose). Он предназначен для расширения функциональности тестов и относится к домену автоматизации тестирования.
## Описание процесса
В сервисе реализован следующий бизнес-процесс: Управление хуками:
1. Выполнение хуков Allure для работы с Selenium, RestAssured и маскировки паролей.
2. Управление Docker Compose (запуск и остановка контейнеров).
3. Интеграция с модулями akita-gpb-core-module, akita-gpb-api-module, akita-gpb-ui-module и akita-gpb-kafka-mq-module для поддержки тестовых сценариев.

<img src="https://bitbucket.dev.gazprombank.ru/projects/QA/repos/akita-gpb-core-module/raw/techdocs/docs/uml/scheme.png?at=refs%2Fheads%2Ftrunk" alt="Схема зависимостей" width="800" height="776">

## Требования
Данный модуль зависит от модулей: akita-gpb-core-module, akita-gpb-api-module, akita-gpb-ui-module, akita-gpb-kafka-mq-module этого же проекта. <br>
Но эти зависимости добавляются посредством собранных jar-файлов, а не ссылкой на исходники модулей.<br>
Соответственно, для успешной сборки данного модуля jar-архивы зависимых модулей должен быть предварительно опубликованы в репозитории (локальном или
публичном).

## Зависимости
```
ru.gazprombank.automation:akita-gpb-core-module
ru.gazprombank.automation:akita-gpb-api-module
ru.gazprombank.automation:akita-gpb-ui-module
ru.gazprombank.automation:akita-gpb-kafka-mq-module
```

---

## Code style
В akita-gpb-модулях используются плагины spotless и google-java-format

## Maintainers
Кайдаш Илья Васильевич - `ilya.kaydash@gazprombank.ru`<br>
Кислый Роман Александрович - `roman.kisly@gazprombank.ru`<br>
Мишечкин Павел Владимирович - `pavel.mishechkin@gazprombank.ru`<br>

## Контактная информация
За сервис отвечает команда "Инструменты тестирования" ([BS ИНТ](https://backstage.int.gazprombank.ru/catalog/default/group/qa-tools-team), [BS ВКР](https://backstage.dev.gazprombank.ru/catalog/default/group/qa-tools-team))