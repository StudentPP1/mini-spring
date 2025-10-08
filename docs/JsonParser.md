# JSON Tokenizer & Parser — Документація

## TL;DR

* **Tokenizer**: перетворює вхідний JSON-текст на послідовність токенів (`TokenType` + значення).
* **Parser**: читає токени курсором і будує значення JSON (object/array/primitive) або одразу **біндить** у POJO через Reflection (Binder).

---

## Цілі та межі

### Цілі

* Мінімальний, швидкий, зрозумілий **стрімінговий** токенайзер (без зайвих алокацій).
* Парсер з **рекурсивним спуском**.

### Обмеження (поточні)

* Підтримуються: `string` (з escape + `\uXXXX`), `number` (int/frac/exp), `true/false/null`, об’єкти `{}`, масиви `[]`.
* Юнікод поза BMP зводиться до сурогатних пар як два `char` (достатньо для Java `String`).
* Без коментарів, без специфіки JSON5.

---

### Підтримувані токени (`TokenType`)

* Структурні: `START_OBJECT {`, `END_OBJECT }`, `START_ARRAY [`, `END_ARRAY ]`, `COLON :`, `COMMA ,`
* Значення: `FIELD_NAME`, `STRING_VALUE`, `NUMBER_VALUE`, `TRUE_VALUE`, `FALSE_VALUE`, `NULL_VALUE`
* Службовий: `EOF`

### Алгоритм (спрощено)

1. **skipWhitespace**: пропускаємо пробіли/таб/нові рядки.
2. Читаємо символ:

    * `{`/`}`/`[`/`]`/`:`/`,` → відповідний токен.
    * `"` → `parseString`: читаємо рядок; якщо після лапки (`"`) стоїть `:` (через whitespace) — це `FIELD_NAME`, інакше `STRING_VALUE`. Підтримка escape + `\uXXXX`.
    * `-` або цифра → `parseNumber`: знак, ціла частина (`0` або `1..9` + цифри), дробова (`.digits`), експонента (`e|E[+/-]?digits`), перевірка **boundary**.
    * `true|false|null` + **boundary** → відповідний токен.
    * Інакше — помилка `Unexpected char`.

### Ключові методи (контракти)

* `parseString(char[] chars, int quotePos) -> Parsed<Token>`
  Повертає токен та `nextPos` (позиція **після** закривальної `"`)
  Обробляє `\" \\ \/ \b \f \n \r \t \uXXXX`.
* `parseNumber(char[] chars, int start) -> Parsed<Token>`
  Валідний JSON-формат числа; boundary: наступний символ ∈ { whitespace, ',', ']', '}', EOF }.
* `isLiteralBoundary(char[] chars, int pos)`
  Кидає помилку, якщо boundary не валідний для завершення значення.
* `startsWith(chars, pos, "true")` з перевіркою довжини.

### Алгоритм

* `parse()`:
    * якщо `START_OBJECT` → `parseObject()`
    * якщо `START_ARRAY` → `parseArray()`
    * якщо `STRING/NUMBER/TRUE/FALSE/NULL` → `parsePrimitive()`

* `parseObject()`:
    * `START_OBJECT`
    * поки не `END_OBJECT`: `FIELD_NAME`, `COLON`, `parse()` для значення; опційно `COMMA`
    * `END_OBJECT`

* `parseArray()`:
    * `START_ARRAY`
    * поки не `END_ARRAY`: `parse()`; опційно `COMMA`
    * `END_ARRAY`

### Поведінка

* Входить у `START_OBJECT`, створює інстанс `T` (no-arg ctor).
* Для кожного `FIELD_NAME` шукає відповідний `Field` у `T`.
* Визначає `Type` поля (generic для `List<T>`), читає значення рекурсивно.
* Конвертує примітиви: `BigDecimal -> int/long/double/…`, рядки → `enum`/`String`.
* Невідомі поля: **ігноруємо** або опціонально кидаємо помилку (режим).

---

## 9) Приклад

Вхід:

```json
{"id": 1, "tags": ["a","b"], "ok": true}
```

Токени:

```
START_OBJECT
FIELD_NAME("id") COLON NUMBER_VALUE(1) COMMA
FIELD_NAME("tags") COLON START_ARRAY STRING_VALUE("a") COMMA STRING_VALUE("b") END_ARRAY COMMA
FIELD_NAME("ok") COLON TRUE_VALUE
END_OBJECT
EOF
```

---

# 🔧 Підхід

Є два нормальні варіанти:

**A) 2-фазний:**

1. токени → проміжна модель (AST): `Map<String,Object>` / `List<Object>` / примітиви;
2. AST → POJO через рефлексію (типи/поля/generic-и).

* Простий для відлагодження, але більше пам’яті.

**B) Стрімінговий (рекомендую):**

1. читаємо токени та **одразу** сетимо поля об’єкта;
2. для вкладених об’єктів/масивів рекурсивно викликаємо binder.

* Менше зайвих структур, швидше.

Нижче — варіант B (стрімінговий).

---

# 🔁 Алгоритм (стрімінговий)

1. `readObject(Class<T>)`:

    * очікує `START_OBJECT`;
    * створює `T instance`;
    * поки не `END_OBJECT`: прочитати `FIELD_NAME` → знайти `Field` за ім’ям → `readValue(fieldType)` → `field.set(instance, value)`;
    * повернути інстанс.

2. `readValue(Class<?> type)`:

    * якщо токен `START_OBJECT` → `readObject(type)` (для POJO або Map).
    * якщо `START_ARRAY` → `readArray(elemType)` і завернути у: масив/`List`/`Set` (в залежності від `type`).
    * якщо примітивний токен (`VALUE_STRING/NUMBER/TRUE/FALSE/NULL`) → `convertPrimitive(type, token)`.

3. `readArray(elemType)`:

    * очікує `START_ARRAY`;
    * поки не `END_ARRAY`: `readValue(elemType)` і додати в список;
    * повернути `List<?>`.

4. `convertPrimitive(type, token)`:

    * `String`, `int/Integer`, `long/Long`, `double/Double`, `BigDecimal`, `boolean/Boolean`, `enum`, `UUID`, `LocalDate` (якщо треба).
    * `NULL` → `null` (для примітивів — кинути помилку або дефолт).

5. Визначення generic-ів:

    * для `List<User>` дістань `User` із `Field.getGenericType()` (як `ParameterizedType`).

---

# 💡 Поради

* Кешуй `buildFieldIndex(Class<?>)` у `ConcurrentHashMap<Class<?>, Map<String,Field>>`.
* Додай підтримку `@JsonName("...")`, щоб мапити інші імена.
* Оброби **невідомі поля**: або ігноруй, або кидай помилку — залежно від режиму.
* Для `List<T>` детект будь ласка тип елемента через `ParameterizedType`.
* Для `Map<String,V>` (якщо потрібно) — окрема гілка, де `FIELD_NAME` → `readValue(V)`.

