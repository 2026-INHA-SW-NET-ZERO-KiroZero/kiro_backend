---
name: select-spring-design-pattern
description: Use when choosing or applying Builder, Factory, Strategy, Spring Event Observer, Decorator, or Adapter in Spring Boot code. Not for routine CRUD, simple fixes, or speculative abstraction.
---

# Spring Design Pattern Selection

A concise guide for choosing and applying design patterns in Spring Boot code.

## 1. When to Use

- When a specific design pattern is requested
- When object creation, branching logic, or external integration logic becomes complex
- When one implementation must be selected among multiple candidates
- When multiple follow-up actions are required after an event occurs
- When existing code is over-abstracted and needs simplification

## 2. Questions to Ask First

- Is a simple `new`, method extraction, or `if` statement enough?
- Is the same branching or object creation logic repeated in multiple places?
- Is there a real possibility that new implementations will be added?
- Will this improve testability and maintainability?
- Can this be solved naturally with Spring DI?

If there is no clear benefit, do not apply a pattern.

## 3. Quick Selection Table

| Situation | Pattern |
|---|---|
| Many parameters and many optional values | Builder |
| Object creation changes depending on type | Factory |
| Algorithms or policies need to be swapped | Strategy |
| Multiple follow-up actions are needed after a state change | Observer / Spring Event |
| Additional behavior needs to be added to an existing object | Decorator |
| External or legacy interfaces need to be adapted | Adapter |

## 4. Builder

- Use when constructor parameters are numerous, required and optional values are mixed, and object creation code is hard to read.
- Avoid when the object has only a few fields or when `@Builder` is added meaninglessly to a simple DTO.

```java
User user = User.builder()
    .name("kim")
    .email("kim@example.com")
    .age(20)
    .build();
```

## 5. Factory

- Use when created objects differ depending on a string, enum, or request type.
- Use when `switch` or `if-else` object creation logic is scattered across multiple places.
- Purpose: centralize object creation responsibility.
- Caution: do not create a Factory if a simple `new SomeClass()` is enough.

```java
@Component
public class NotificationFactory {
    private final Map<String, NotificationSender> senders;

    public NotificationFactory(List<NotificationSender> list) {
        this.senders = list.stream()
            .collect(Collectors.toMap(NotificationSender::type, Function.identity()));
    }

    public NotificationSender get(String type) {
        return Optional.ofNullable(senders.get(type))
            .orElseThrow(() -> new IllegalArgumentException("Unknown type: " + type));
    }
}
```

## 6. Strategy

- Use when there are multiple algorithms or policies for the same purpose.
- Use when behavior must be selected at runtime.
- Examples: discount, payment, sorting, or notification methods that can be swapped.

```java
public interface DiscountPolicy {
    Money discount(Order order);
}
```

- Factory decides “what to create.”
- Strategy decides “which behavior to use.”
- Factory and Strategy can be used together, where the Factory selects the appropriate Strategy implementation.

## 7. Observer / Spring Event

- Use when multiple additional tasks are needed after core logic is completed.
- Examples: reduce inventory, send an email, or issue a coupon after an order is placed.
- Purpose: separate follow-up tasks and reduce coupling between services.

```java
public record OrderPlacedEvent(Long orderId) {}

publisher.publishEvent(new OrderPlacedEvent(order.getId()));

@EventListener
public void handle(OrderPlacedEvent event) {
    // follow-up processing
}
```

- Caution: do not hide essential logic inside events if it must always succeed.
- Consider `@TransactionalEventListener` when execution must happen after a transaction.
- For asynchronous events, design failure handling and logging together.

## 8. Decorator

- Use when you want to add behavior without modifying an existing object.
- Examples: logging, caching, validation, compression, or encryption as composable add-ons.

```java
public class LoggingSender implements NotificationSender {
    private final NotificationSender delegate;

    public void send(String message) {
        log.info("send: {}", message);
        delegate.send(message);
    }
}
```

- Caution: deep chains make debugging difficult.
- Check first whether Spring AOP, Filter, or Interceptor would be simpler.

## 9. Adapter

- Use when an external API or legacy code interface does not match your current code.
- Purpose: prevent external dependencies from spreading directly into the internal service layer.
- Benefit: creates an internal interface that is easier to test.

```java
@Component
public class KakaoMapAdapter implements MapClient {
    public RouteResult findRoute(RouteRequest request) {
        KakaoResponse response = kakaoMapApi.search(request.toKakaoRequest());
        return RouteResult.from(response);
    }
}
```

## 10. Combination Examples

| Problem | Combination |
|---|---|
| Select Email/SMS/Push notification method | Strategy + Factory |
| Execute multiple tasks after an order is placed | Spring Event |
| Design replaceable external map API integration | Adapter + Strategy |
| Create objects by request type | Factory |
| Apply optional add-on behavior | Decorator |
| Create complex request objects | Builder |

## 11. Anti-Patterns

| Anti-Pattern | Problem | Alternative |
|---|---|---|
| Factory overuse | Makes simple creation unnecessarily complex | `new` or DI |
| Strategy overuse | Only increases the number of implementations | Method extraction |
| Singleton abuse | Global state, hard to test | Spring Bean |
| Deep Decorator chains | Hard to trace execution flow | AOP or explicit composition |
| Event overuse | Hides core business flow | Synchronous call |
| No Adapter | External types spread across the codebase | Define an internal interface |

## 12. Final Decision Criteria

- Do not apply a pattern for simple CRUD.
- Do not abstract based only on future possibilities when there is no current problem.
- If branching is repeated, consider Factory or Strategy.
- Wrap external system boundaries with Adapter.
- Consider Spring Event for separating follow-up tasks.
- Use Builder to handle complex object creation.
- For add-on behavior, choose the simplest option among Decorator, AOP, and Interceptor.

## 13. Response Principles

1. First decide whether the requested pattern is appropriate.
2. If it is not appropriate, suggest a simpler alternative.
3. If it is appropriate, provide the minimal structure first.
4. Consider Spring Bean, DI, and testability together.
5. Limit the scope of application to avoid over-abstraction.