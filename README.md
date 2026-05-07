# 🚀 QBOOK — Платформа бронирования

## Структура проекта

```
qbook/
├── backend/          — Java 17 + Spring Boot 3 (Railway)
├── frontend-site/    — HTML + CSS + JS для бизнеса (Vercel)
└── frontend-miniapp/ — React + Vite Telegram Mini App (Vercel)
```

## Быстрый старт Backend

### Требования
- Java 17
- Maven 3.9+

### Запуск локально

```bash
cd backend

# 1. Создать .env файл с переменными
cp .env.example .env
# Заполнить .env своими данными

# 2. Собрать проект
mvn clean package -DskipTests

# 3. Запустить
java -jar target/qbook-backend-1.0.0.jar
```

### Переменные окружения (Railway)

| Переменная | Описание |
|-----------|---------|
| `SUPABASE_DB_URL` | jdbc:postgresql://db.xxx.supabase.co:5432/postgres |
| `SUPABASE_DB_USER` | postgres |
| `SUPABASE_DB_PASSWORD` | пароль от БД |
| `SUPABASE_URL` | https://xxx.supabase.co |
| `SUPABASE_ANON_KEY` | anon ключ |
| `SUPABASE_SERVICE_KEY` | service_role ключ |
| `TELEGRAM_BOT_TOKEN` | токен бота |
| `TELEGRAM_BOT_USERNAME` | QBookBot |
| `REDIS_URL` | redis://... (Upstash) |
| `JWT_SECRET` | случайная строка 64+ символа |
| `RESEND_API_KEY` | ключ от Resend.com |
| `SITE_URL` | https://qbook-site.vercel.app |
| `MINI_APP_URL` | https://qbook-miniapp.vercel.app |

## API endpoints

- `GET /actuator/health` — проверка состояния
- `POST /api/auth/register` — регистрация бизнеса
- `POST /api/auth/login` — вход
- `POST /api/auth/telegram` — авторизация клиента через Telegram

## Деплой

### Backend → Railway
1. Зарегистрироваться на [railway.app](https://railway.app)
2. New Project → Deploy from GitHub repo
3. Добавить переменные окружения
4. Деплой автоматический из `backend/` директории

### Frontend → Vercel
1. Зарегистрироваться на [vercel.com](https://vercel.com)
2. Import Git Repository
3. Root Directory: `frontend-site/` или `frontend-miniapp/`

## Шаги разработки

- [x] Шаг 1: Spring Boot структура + зависимости + конфигурации
- [ ] Шаг 2: БД — все миграции + JPA entities + RLS
- [ ] Шаг 3: Auth — регистрация + JWT + сессии
- [ ] Шаг 4: Бизнес + профиль + медиа + сотрудники
- [ ] Шаг 5: Модуль ЕДА
- [ ] Шаг 6: Модуль УСЛУГИ
- [ ] Шаг 7: Модуль БРОНИРОВАНИЕ
- [ ] Шаг 8: Модуль МАГАЗИН
- [ ] Шаг 9: Финансы
- [ ] Шаг 10: Безопасность + антифрод + аудит
- [ ] Шаг 11: Бот @QBookBot
- [ ] Шаг 12: HTML сайт для бизнеса
- [ ] Шаг 13: React Mini App
- [ ] Шаг 14: Суперадмин + тестирование + прод
