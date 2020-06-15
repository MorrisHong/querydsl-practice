# QueryDsl
QueryDsl ?
 - 오픈소스 프로젝트다.
 - 일반적으로 복잡한 Creteria를 대체하는 JPQL 빌더다.
 - JPA의 표준스펙이 아니므로 약간의 설정이 더 필요하다.
 - 복잡한 쿼리와 동적쿼리를 깔쌈하게 해결해준다.
 - 쿼리를 자바 코드로 작성할 수 있다. 따라서 문법오류를 컴파일단계에서 잡아줄 수 있다.


jpql
```java
...
String username = "gracelove"
String query = "select m from Member m" +
 "where m.username = :username";

List<Member> result = em.createQuery(query, Member.class)
							.getResult.List();

...
```
jpql로 작성할 때의 문제점.  
위 코드는 오류가 있다. 발견하기 쉬운가? 게다가 오류가 발견되는 시점은 컴파일단계가 아니라 런타임때 발견할 수 있다.

querydsl
```java
String username = "gracelove
List<Member> result = queryFactory
							.select(member)
							.from(member)
							.where(member.username.eq(username))
							.fetch();
```
자바 코드기 때문에 ide의 도움을 받을 수 있다. 또 컴파일 단계에서 오류를 발견할 수 있다. 게다가 메서드추출도 할 수 있어서 재사용성 또한 높아진다.




사용방법
QType을 뽑고 QType을 이용해서 쿼리를 작성한다.




QMember m = new QMember("m1"); // 셀프조인할 때만 쓰자.
QMember m = QMember.member; //추천!


JPAQuery<Member> query = new JPAQuery<>()
JPAQueryFactory queryFactory = new JPAQueryFactory(em);  // 권고 



#jpa
