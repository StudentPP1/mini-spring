
```java
public interface Servlet {
  void init(ServletConfig config) throws Exception;
  void service(com.test.servlet.http.HttpServletRequest req,
               com.test.servlet.http.HttpServletResponse res) throws Exception;
  void destroy();
}

public abstract class GenericServlet implements Servlet {
  protected ServletConfig config;
  @Override public void init(ServletConfig config) { this.config = config; }
  @Override public void destroy() {}
  protected ServletContext getServletContext() { return config.getServletContext(); }
}

public abstract class HttpServlet extends GenericServlet {
  @Override public final void service(HttpServletRequest req, HttpServletResponse res) throws Exception {
    String m = req.getMethod();
    switch (m) {
      case "GET"  -> doGet(req, res);
      case "POST" -> doPost(req, res);
      case "PUT"  -> doPut(req, res);
      case "DELETE" -> doDelete(req, res);
      case "OPTIONS" -> doOptions(req, res);
      default -> { res.setStatus(405, "Method Not Allowed"); }
    }
  }
  protected void doGet   (HttpServletRequest req, HttpServletResponse res) throws Exception {}
  protected void doPost  (HttpServletRequest req, HttpServletResponse res) throws Exception {}
  protected void doPut   (HttpServletRequest req, HttpServletResponse res) throws Exception {}
  protected void doDelete(HttpServletRequest req, HttpServletResponse res) throws Exception {}
  protected void doOptions(HttpServletRequest req, HttpServletResponse res) throws Exception {
    res.setHeader("Allow", "GET,POST,PUT,DELETE,OPTIONS");
    res.setStatus(204, "No Content");
  }
}
```

```java
public interface ServletConfig {
  ServletContext getServletContext();
  Map<String,String> getInitParameters();
}

public interface ServletContext {
  <T> void setAttribute(String key, T value);
  <T> T getAttribute(String key, Class<T> type);
  ServletRegistration addServlet(String name, Servlet servlet);
  FilterRegistration  addFilter (String name, Filter filter);
}

public interface ServletRegistration {
  void addMapping(String... urlPatterns);   // "/health", "/note/*"
  void setLoadOnStartup(int order);         // опційно
}

public interface FilterRegistration {
  void addMapping(String... urlPatterns);
}

public interface Filter {
  void init(ServletConfig config) throws Exception;
  void doFilter(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws Exception;
  void destroy();
}

public interface FilterChain {
  void doFilter(HttpServletRequest req, HttpServletResponse res) throws Exception;
}

public interface ServletContextInitializer {
  void onStartup(ServletContext ctx) throws Exception;   // програмна реєстрація
}
```

```java
public final class MiniServletContainer {
  private final DefaultServletContext context = new DefaultServletContext();
  private final List<ServletContextInitializer> initializers;

  public MiniServletContainer(List<ServletContextInitializer> initializers) {
    this.initializers = initializers;
  }

  public void init() throws Exception {
    // 1) викликаємо програмну реєстрацію
    for (var i : initializers) i.onStartup(context);
    // 2) ініціалізуємо сервіслети і фільтри
    context.initAll();
  }

  /** Один вхід на запит: адаптуємо твої HttpRequest/Response до HttpServlet* і запускаємо ланцюжок */
  public void handle(com.test.http.HttpRequest rawReq, com.test.http.HttpResponse rawRes) throws Exception {
    var req = RequestAdapter.from(rawReq);
    var res = ResponseAdapter.to(rawRes);
    var target = context.findTarget(req.getMethod(), req.getRequestURI()); // сервлет + фільтри за url
    var chain = new DefaultFilterChain(target.filters(), (r, s) -> target.servlet().service(r, s));
    chain.doFilter(req, res);
  }

  public void destroy() { context.destroyAll(); }
}
```

### Контекст і реєстрація

```java
final class DefaultServletContext implements ServletContext, ServletConfig {
  private final Map<String,Object> attrs = new ConcurrentHashMap<>();
  private final List<RegisteredServlet> servlets = new ArrayList<>();
  private final List<RegisteredFilter>  filters  = new ArrayList<>();

  @Override public <T> void setAttribute(String k, T v) { attrs.put(k, v); }
  @Override @SuppressWarnings("unchecked")
  public <T> T getAttribute(String k, Class<T> t) { return (T) attrs.get(k); }

  @Override public ServletRegistration addServlet(String name, Servlet servlet) {
    var rs = new RegisteredServlet(name, servlet);
    servlets.add(rs); return rs;
  }
  @Override public FilterRegistration addFilter(String name, Filter filter) {
    var rf = new RegisteredFilter(name, filter);
    filters.add(rf); return rf;
  }

  public void initAll() throws Exception {
    for (var f : filters)  f.filter().init(this);
    for (var s : servlets) s.servlet().init(this);
  }
  public void destroyAll() {
    servlets.forEach(s -> s.servlet().destroy());
    filters.forEach(f -> f.filter().destroy());
  }

  Target findTarget(String method, String path) {
    // знайти перший сервлет, чий pattern матчиться path; зібрати фільтри, що матчаться path
    var servlet = servlets.stream().filter(s -> UrlPatternMatcher.matches(s.mappings(), path)).findFirst()
                 .orElseThrow(() -> new RuntimeException("Not Found: " + path));
    var chainFilters = filters.stream()
        .filter(f -> UrlPatternMatcher.matches(f.mappings(), path))
        .map(RegisteredFilter::filter).toList();
    return new Target(servlet.servlet(), chainFilters);
  }

  // допоміжні типи
  record Target(Servlet servlet, List<Filter> filters) {}
    
  static final class RegisteredServlet implements ServletRegistration {
    final String name; final Servlet servlet; final List<String> map = new ArrayList<>();
    RegisteredServlet(String n, Servlet s) { name=n; servlet=s; }
    @Override public void addMapping(String... p) { map.addAll(List.of(p)); }
    @Override public void setLoadOnStartup(int order) {}
    Servlet servlet() { return servlet; }
    List<String> mappings() { return map; }
  }
  
  static final class RegisteredFilter implements FilterRegistration {
    final String name; final Filter filter; final List<String> map = new ArrayList<>();
    RegisteredFilter(String n, Filter f) { name=n; filter=f; }
    @Override public void addMapping(String... p) { map.addAll(List.of(p)); }
    Filter filter() { return filter; }
    List<String> mappings() { return map; }
  }

  // ServletConfig:
  @Override public ServletContext getServletContext() { return this; }
  @Override public Map<String,String> getInitParameters() { return Map.of(); }
}
```

### Фільтр-ланцюжок

```java
public final class DefaultFilterChain implements FilterChain {
  private final List<Filter> filters;
  private final Target target;
  private int index = 0;
  public interface Target { void invoke(HttpServletRequest req, HttpServletResponse res) throws Exception; }
  public DefaultFilterChain(List<Filter> filters, Target target) {
    this.filters = filters; this.target = target;
  }
  @Override public void doFilter(HttpServletRequest req, HttpServletResponse res) throws Exception {
    if (index < filters.size()) filters.get(index++).doFilter(req, res, this);
    else target.invoke(req, res);
  }
}
```

> Якщо твій `HttpResponse` не має `setStatus(...)`/`bodyOutputStream()`, зроби невеликий буфер усередині адаптера і в кінці склей заголовки + тіло (як ти вже робиш у `toByteArray()`).

# 4) Твої «бізнес-сервлети» і фільтри

```
com.test.app.servlet/
  HealthServlet.java
  NoteCreateServlet.java
com.test.app.filter/
  LoggingFilter.java
  CorsFilter.java
```

```java
public final class HealthServlet extends com.test.servlet.http.HttpServlet {
  @Override protected void doGet(HttpServletRequest req, HttpServletResponse res) throws Exception {
    res.setStatus(200, "OK");
    res.setHeader("Content-Type","application/json");
    res.getOutputStream().write("{\"status\":\"ok\"}".getBytes(StandardCharsets.UTF_8));
  }
}

public final class NoteCreateServlet extends com.test.servlet.http.HttpServlet {
  @Override protected void doPost(HttpServletRequest req, HttpServletResponse res) throws Exception {
    String json = new String(req.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    Note note = com.test.mapper.convertor.ObjectMapper.parse(json, Note.class);
    String out = com.test.mapper.convertor.ObjectMapper.write(note);
    res.setStatus(201, "Created");
    res.setHeader("Content-Type","application/json");
    res.getOutputStream().write(out.getBytes(StandardCharsets.UTF_8));
  }
}
```

```java
public final class LoggingFilter implements com.test.servlet.api.Filter {
  @Override public void init(ServletConfig config) {}
  @Override public void destroy() {}
  @Override public void doFilter(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws Exception {
    System.out.println(">> " + req.getMethod() + " " + req.getRequestURI());
    chain.doFilter(req, res);
  }
}
public final class CorsFilter implements com.test.servlet.api.Filter {
  @Override public void init(ServletConfig config) {}
  @Override public void destroy() {}
  @Override public void doFilter(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws Exception {
    res.setHeader("Access-Control-Allow-Origin","*");
    if ("OPTIONS".equals(req.getMethod())) { res.setStatus(204,"No Content"); return; }
    chain.doFilter(req, res);
  }
}
```

# 5) Програмна реєстрація (аналог `web.xml` → `onStartup`)

```
com.test.app.init/
  AppInitializer.java
```

```java
public final class AppInitializer implements com.test.servlet.api.ServletContextInitializer {
  @Override
  public void onStartup(ServletContext ctx) {
    // сервлети
    ctx.addServlet("health", new com.test.app.servlet.HealthServlet())
       .addMapping("/health");
    ctx.addServlet("noteCreate", new com.test.app.servlet.NoteCreateServlet())
       .addMapping("/note/*"); // або "/note/create"

    // фільтри
    ctx.addFilter("logging", new com.test.app.filter.LoggingFilter())
       .addMapping("/*");
    ctx.addFilter("cors", new com.test.app.filter.CorsFilter())
       .addMapping("/*");
  }
}
```

# 6) Сервер і main: мінімум логіки

```
com.test.server/
  HttpAcceptor.java       // слухає порт, приймає сокети, викликає container.handle(...)
com.test.boot/
  Application.java
```

```java
public final class HttpServer implements AutoCloseable {
  private final int port;
  private final java.util.concurrent.ExecutorService pool;
  private final MiniServletContainer container;
  private volatile boolean running;

  public HttpServer(int port, int threads, MiniServletContainer container) {
    this.port = port;
    this.pool = java.util.concurrent.Executors.newFixedThreadPool(threads);
    this.container = container;
  }

  public void start() throws Exception {
    container.init();
    var ss = new ServerSocket(port);
    running = true;
    while (running) {
      Socket s = ss.accept();
      pool.execute(() -> {
        try (s) {
          var in  = s.getInputStream();
          var out = s.getOutputStream();
          // твій існуючий low-level парсинг:
          com.test.http.HttpRequest  rawReq = com.test.http.HttpRequest.build(in);
          com.test.http.HttpResponse rawRes = com.test.http.HttpResponse.build(com.test.http.HttpStatus.OK);
          container.handle(rawReq, rawRes);                 // тут відпрацюють фільтри і сервлет
          out.write(rawRes.toByteArray()); out.flush();
        } catch (Exception e) { /* 500 writer як у тебе */ }
      });
    }
  }

  @Override public void close() { running = false; pool.shutdown(); container.destroy(); }
}
```

```java
public final class Application {
  public static void main(String[] args) throws Exception {
    var container = new MiniServletContainer(List.of(new com.test.app.init.AppInitializer()));
    try (var server = new HttpServer(8080, 10, container)) {
      Runtime.getRuntime().addShutdownHook(new Thread(server::close));
      server.start();
    }
  }
}
```

# 7) Як тестувати

```bash
curl -i http://localhost:8080/health
curl -i -X POST http://localhost:8080/note/create \
  -H "Content-Type: application/json" \
  -d '{"title":"First","content":"Hi"}'
```

---

## Що ти отримаєш

* **Ті ж самі абстракції і життєвий цикл**, як у Java Servlet API: `init → service → destroy`, `FilterChain`, `ServletContext`, програмна реєстрація.
* `main` майже нічого не знає: просто піднімає контейнер і HTTP acceptor.
* Далі ти можеш:

    * додати `RequestDispatcher.forward/include` (якщо треба),
    * реалізувати path-params у `UrlPatternMatcher`,
    * підключити свій `ExceptionResolver` усередині контейнера (як глобальний error handler).

Це максимально наближено до класичної моделі сервлетів, але працює поверх твого власного HTTP-сервера й уже написаного парсера.
