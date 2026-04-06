# Akita GPB module akita-gpb-helpers-module
## Назначение сервиса
Сервис akita-gpb-helpers-module содержит абстрактные вспомогательные классы, не связанные с базовыми модулями, такие как DockerHelper, SshConnectionHelper и SshSteps. Он предназначен для поддержки дополнительных функций в фреймворке Akita-BDD и относится к домену вспомогательных инструментов автоматизации.
## Описание процесса
В сервисе реализован следующий бизнес-процесс: Обеспечение вспомогательных функций:
1. Предоставление вспомогательных классов для работы с Docker (DockerHelper).
2. Обеспечение SSH-подключений и шагов через SshConnectionHelper и SshSteps.
3. Интеграция с модулем akita-gpb-core-module для передачи данных и управления процессами.

<img src="https://bitbucket.dev.gazprombank.ru/projects/QA/repos/akita-gpb-core-module/raw/techdocs/docs/uml/scheme.png?at=refs%2Fheads%2Ftrunk" alt="Схема зависимостей" width="800" height="776">

## Требования
Данный модуль зависит от модуля akita-gpb-core-module этого же проекта. <br>
Но эти зависимости добавляются посредством собранных jar-файлов, а не ссылкой на исходники модулей.<br>
Соответственно, для успешной сборки данного модуля jar-архивы зависимых модулей должен быть предварительно опубликованы в репозитории (локальном или
публичном).

## Зависимости
```
ru.gazprombank.automation:akita-gpb-core-module<br>
```

## Code style
В akita-gpb-модулях используются плагины spotless и google-java-format

## Maintainers
Кайдаш Илья Васильевич - `ilya.kaydash@gazprombank.ru`<br>
Кислый Роман Александрович - `roman.kisly@gazprombank.ru`<br>
Мишечкин Павел Владимирович - `pavel.mishechkin@gazprombank.ru`<br>

## Контактная информация
За сервис отвечает команда "Инструменты тестирования" ([BS ИНТ](https://backstage.int.gazprombank.ru/catalog/default/group/qa-tools-team), [BS ВКР](https://backstage.dev.gazprombank.ru/catalog/default/group/qa-tools-team))
