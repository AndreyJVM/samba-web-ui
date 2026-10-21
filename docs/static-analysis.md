# Статический анализ

Workflow `.github/workflows/codeql.yml` запускает GitHub CodeQL при push в `master`/`main`,
для Pull Request в эти ветки и вручную через Actions. Результаты появляются в разделе
**Security → Code scanning** репозитория.

## Область анализа

| Задание | Код | Подготовка |
| --- | --- | --- |
| `Analyze (java-kotlin)` | Java/Spring Boot backend | Компиляция Maven с JDK 21 без frontend и тестов. |
| `Analyze (javascript-typescript)` | TypeScript/React frontend | Анализ исходников без установки npm-зависимостей и сборки. |
| `Analyze (python)` | Python-помощник и его тесты | Анализ исходников без запуска помощника. |

Конфигурация `.github/codeql/config.yml` включает набор `security-and-quality`.
Сгенерированные файлы, frontend-зависимости и встроенный Node исключены из поиска исходников
для интерпретируемых языков. Java извлекается из явно заданной компиляции Maven.

Анализ выполняется отдельно от сборки релиза. Он не заменяет unit- и интеграционные тесты,
ESLint или проверку установки DEB на целевой ОС. Токену задания разрешены чтение исходников
и запись результатов code scanning; секреты Docker Hub ему не передаются.

## Quality Gate для Pull Request

Успешный workflow подтверждает выполнение анализа и загрузку результатов, но сам по себе
не означает отсутствия найденных проблем и не запрещает слияние PR.

После первого успешного анализа основной ветки:

1. Убедитесь, что в **Security → Code scanning** появились результаты всех трёх языков.
   Если для репозитория включён CodeQL Default setup, переведите его на Advanced setup,
   чтобы он не дублировал этот workflow.
2. Создайте или обновите активный branch ruleset для основной ветки в
   **Settings → Rules → Rulesets**.
3. Включите **Require code scanning results** и выберите инструмент **CodeQL**.
4. Для начала блокируйте новые security alerts уровня **High и Critical**, а для обычных
   alerts — уровень **Error**. Остальные предупреждения разбирайте в очереди техдолга.
5. Добавьте три задания `Analyze (...)` в обязательные status checks. Дождитесь появления
   их реальных имён в GitHub после первого запуска; не подменяйте их проверкой обычного CI.
6. Проверьте работу правила на Pull Request: отсутствие анализа или новая проблема выше
   выбранного порога должны блокировать слияние.

Настройки ruleset хранятся в GitHub и не включаются автоматически от добавления YAML-файла.
Issue #8 можно считать завершённой после успешного запуска и проверки действующего Quality Gate.

## Разбор результатов

Для каждой находки проверяйте путь данных и воспроизводимость. Для исправленной ошибки
добавляйте регрессионную проверку, если она подтверждает значимое поведение. Ложное
срабатывание отклоняйте с конкретным обоснованием; не отключайте весь набор запросов
ради одного предупреждения.

Документация:

- [Настройка CodeQL Advanced setup](https://docs.github.com/en/code-security/code-scanning/creating-an-advanced-setup-for-code-scanning/customizing-your-advanced-setup-for-code-scanning).
- [Защита слияния по результатам code scanning](https://docs.github.com/en/code-security/code-scanning/managing-your-code-scanning-configuration/set-code-scanning-merge-protection).
