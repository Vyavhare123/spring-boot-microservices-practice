# Microservice Learning Flow

This project has one simple end-to-end flow:

Client -> API Gateway -> Order Service -> Feign Client -> Eureka -> Product Service -> Order Service response

## Start Services

Start them in this order:

1. `EUREKA-SERVER/EUREKA-SERVER` on port `8761`
2. `product-service/product-service` on port `8081`
3. `order-service/order-service` on port `8082`
4. `API-GATEWAY/API-GATEWAY` on port `8080`

Each service can be started from its own folder:

```powershell
.\mvnw.cmd spring-boot:run
```

## One Request To Understand The Flow

Send this request to the API Gateway:

```http
POST http://localhost:8080/api/orders
Content-Type: application/json

{
  "productId": 1,
  "quantity": 2
}
```

What happens:

1. API Gateway routes `/api/orders` to `order-service`.
2. Order Service receives `productId` and `quantity`.
3. Order Service uses OpenFeign with service name `product-service`.
4. Eureka and Spring Cloud LoadBalancer find the running Product Service instance.
5. Product Service checks stock and reduces quantity.
6. Order Service calculates the total price and returns the confirmed order.

The product service starts with demo products, so product id `1` works immediately after startup.

Product Service stores products with Spring Data JPA in MySQL.
Order Service stores orders with Spring Data JPA in MySQL.

Before starting `product-service` and `order-service`, make sure MySQL is running with this login:

```text
username: root
password: root
```

Product Service connects to:

```text
jdbc:mysql://localhost:3306/productdb
```

Order Service connects to:

```text
jdbc:mysql://localhost:3306/orderdb
```

If the `root` user has permission, the `productdb` and `orderdb` databases will be created automatically.
