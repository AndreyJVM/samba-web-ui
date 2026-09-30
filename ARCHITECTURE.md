# Архитектура приложения (Backend)

В данном документе описана слоистая архитектура (Layered/Clean Architecture) бэкенда на примере модуля файловой системы (`FileSystemService`). Этот же паттерн применяется ко всем остальным модулям (конфигурация Samba, пользователи, группы, логи).

## 1. Диаграмма классов (Class Diagram)

Схема ниже отображает взаимодействие слоев: от REST-контроллера через бизнес-логику к исполнителю инфраструктурных команд и формируемым DTO (Data Transfer Objects).

```mermaid
classDiagram
    namespace Controllers {
        class FileSystemApiController
    }
    
    namespace Services {
        class FileSystemService
    }
    
    namespace Infrastructure {
        class CommandExecutor
        class SshSessionManager
        class LinuxCommands
    }
    
    namespace DTO {
        class DirectoryBrowseResultDto
        class DirectoryItemDto
        class DiskUsageDto
    }

    class FileSystemApiController {
        <<RestController>>
        -FileSystemService fileSystemService
        +listDirectories(path, session) ResponseEntity
        +createDirectory(dto, session) ResponseEntity
        +getDiskUsage(path, session) ResponseEntity
    }
    
    class FileSystemService {
        <<Service>>
        -CommandExecutor commandExecutor
        -List~String~ allowedRoots
        +listDirectories(sessionId, path) DirectoryBrowseResultDto
        +createDirectory(sessionId, parentPath, name) void
        +getDiskUsage(sessionId, path) DiskUsageDto
        +requireAllowedPath(path) void
    }
    
    class CommandExecutor {
        <<Interface>>
        +execute(sessionId, command) String
    }
    
    class SshSessionManager {
        <<Component>>
        +execute(sessionId, command) String
    }
    
    class LinuxCommands {
        <<Utility>>
        +findDirectories(path)$ String
        +df(path)$ String
        +mkdir(path)$ String
        -escape(arg)$ String
    }
    
    class DirectoryBrowseResultDto {
        <<Record>>
        +String currentPath
        +String parentPath
        +List~DirectoryItemDto~ directories
    }
    
    class DirectoryItemDto {
        <<Record>>
        +String name
        +String fullPath
    }

    class DiskUsageDto {
        <<Record>>
        +String path
        +int usePercent
    }

    %% Связи
    FileSystemApiController --> FileSystemService : @Autowired
    FileSystemService --> CommandExecutor : delegates execution
    SshSessionManager ..|> CommandExecutor : implements
    FileSystemService ..> LinuxCommands : uses for generation
    FileSystemService ..> DirectoryBrowseResultDto : returns
    FileSystemService ..> DiskUsageDto : returns
    DirectoryBrowseResultDto *-- DirectoryItemDto : contains 1..*
```

### Пояснения к слоям:
1. **Controller Layer (Контроллеры)**: Тонкий слой (`FileSystemApiController`). Отвечает за прием HTTP-запросов, извлечение токенов/сессий, базовую валидацию `@Valid` и возврат клиенту форматированных JSON-ответов (`ResponseEntity`).
2. **Service Layer (Логика)**: Ядро приложения (`FileSystemService`). Здесь реализуется бизнес-логика: проверка макро-прав (whitelist путей `requireAllowedPath`), парсинг вывода системных утилит, форматирование байтов и оборачивание результата в Immutable-рекорды (Record DTO).
3. **Infrastructure Layer (Инфраструктура)**: Абстракция исполнения команд. Бизнес-логика не работает с SSH-клиентом напрямую, а зависит от интерфейса `CommandExecutor`. Разрешение зависимостей и конкретная реализация (`SshSessionManager`) инжектится во время выполнения контейнером Spring Boot.
4. **Security Utils / Builders (`LinuxCommands`)**: Защищенная изолированная песочница для формирования команд с обязательным экранированием(`escape()`) и защитой от Shell Injection и Path Traversal.

---

## 2. Диаграмма последовательности (Sequence Diagram)

Поток выполнения типового запроса (например, «Получить список папок»).

```mermaid
sequenceDiagram
    autonumber
    actor Client as Frontend UI
    participant C as FileSystemApiController
    participant S as FileSystemService
    participant L as LinuxCommands
    participant E as CommandExecutor (SSH)
    
    Client->>C: GET /api/fs/directories?path=/srv
    C->>S: listDirectories(session, "/srv")
    
    Note over S: 1. Валидация пути<br/>2. Проверка белого списка (allowedRoots)
    S->>S: requireAllowedPath("/srv")
    
    S->>L: findDirectories("/srv")
    Note over L: Экранирование ввода
    L-->>S: "sudo find -L '/srv' -mindepth 1..."
    
    S->>E: execute(session, "sudo find...")
    Note over E: Подключение через JSch по SSH
    E-->>S: Raw CLI String Output
    
    Note over S: Парсинг строк и сортировка
    S->>S: map to DirectoryItemDto
    
    S-->>C: DirectoryBrowseResultDto
    C-->>Client: HTTP 200 OK (JSON)
```

### Ключевые паттерны проектирования:
* **Dependency Injection & Inversion of Control (IoC):** Внедрение зависимостей через единый канонический конструктор (Service -> Interface).
* **Defense-in-depth (Глубокая защита):** Валидация на уровне контроллера + проверка прав и белого списка файловых путей в UI-сервисе + строк-экранирование в Command Builder.
* **DTO Pattern (Record):** Неизменяемые Data Transfer Objects для защиты от мутаций контрактов фронтенда (иммутабельность в Java 17+ Records). 