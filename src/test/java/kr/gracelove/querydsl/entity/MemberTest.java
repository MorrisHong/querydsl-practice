package kr.gracelove.querydsl.entity;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import kr.gracelove.querydsl.dto.MemberDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
import java.util.List;

import static kr.gracelove.querydsl.entity.QMember.*;
import static kr.gracelove.querydsl.entity.QTeam.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by GraceLove
 * Github  : https://github.com/gracelove91
 * Blog    : https://gracelove91.tistory.com
 * Email   : govlmo91@gmail.com
 *
 * @author : Eunmo Hong
 * @since : 2020/06/07
 */

@SpringBootTest
@Transactional
class MemberTest {

    @PersistenceContext
    EntityManager em;
    JPAQueryFactory queryFactory;

    @BeforeEach
    public void testEntity() {
        queryFactory = new JPAQueryFactory(em);

        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);

        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);
        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
    }

    @Test
    void jpql() {
        //member1 찾기
        String username = "member1";
        String query = "select m from Member m " +
                "where username = :username";

        Member findMember = em.createQuery(query, Member.class)
                .setParameter("username", username)
                .getSingleResult();

        assertEquals(username, findMember.getUsername());
    }

    @Test
    void querydsl() {
        //member1 찾기
        String username = "member1";

//        QMember member = new QMember("m1"); // 셀프조인할 때만 쓰는걸 추천.
//        QMember member = QMember.member;    // 일반적으 추천.

        Member findMember = queryFactory
                .select(member)
                .from(member)
                .where(member.username.eq(username))
                .fetchOne();

        assertEquals(username, findMember.getUsername());
    }

    @Test
    void search() {
        // username = member1 & age = 10
        String username = "member1";
        int age = 10;

        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq(username)
                        .and(member.age.eq(age)))
                .fetchOne();

        assertEquals(age, findMember.getAge());
        assertEquals(username, findMember.getUsername());
    }

    @Test
    void searchAndParam() { // where절 보자. search()와 같음. null을 무시한다.
        // username = member1 & age = 10
        String username = "member1";
        int age = 10;

        Member findMember = queryFactory
                .selectFrom(member)
                .where(
                        member.username.eq(username),
                        member.age.eq(age)
                )
                .fetchOne();

        assertEquals(age, findMember.getAge());
        assertEquals(username, findMember.getUsername());
    }

    @Test
    void resultFetch() {
        List<Member> fetch = queryFactory
                .selectFrom(member)
                .fetch(); //여러건 조회 없으면 빈 리스트 반환.

        Member fetchOne = queryFactory
                .selectFrom(QMember.member)
                .fetchOne();// 단건 조회

        Member fetchFirst = queryFactory
                .selectFrom(QMember.member)
                .fetchFirst();// limit(1).fetchOne()


        //////// fetchResults() /////////////////////////////
        QueryResults<Member> memberQueryResults = queryFactory
                .selectFrom(member)
                .fetchResults(); // 쿼리 두방나감. 토탈쿼리도 가져와야되기 때문에.

        long total = memberQueryResults.getTotal();
        List<Member> results = memberQueryResults.getResults();
        /////////////////////////////////////////////////////////
        ///// count 쿼리는 웬만하면 직접날리자. 이 메서드는 최적화 안되는 경우가 있다.

        long fetchCount = queryFactory
                .selectFrom(member)
                .fetchCount();// count 쿼리나감.
    }


    /**
     * 회원 정렬순서.
     * 1. 회원 나이 내림차순 (desc)
     * 2. 회원 이름 올림차순(asc)
     * 단 2에서 회원 이름이 없으면 마지막에 출력(nulls last)
     */
    @Test
    void sort() {
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(
                        member.age.desc(),
                        member.username.asc().nullsLast()
                )
                .fetch();

        Member member5 = result.get(0);
        Member member6 = result.get(1);
        Member memberNull = result.get(2);

        assertEquals("member5", member5.getUsername());
        assertEquals("member6", member6.getUsername());
        assertNull(memberNull.getUsername());

    }

    @Test
    void paging1() {
        List<Member> fetch = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1) //0부터 시작.
                .limit(2)
                .fetch();

        fetch.forEach(System.out::println);

        assertEquals(2, fetch.size());
    }

    @Test
    void paging2() {
        QueryResults<Member> queryResults = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1) //0부터 시작.
                .limit(2)
                .fetchResults();

        assertEquals(4, queryResults.getTotal());
        assertEquals(2, queryResults.getResults().size());
        // 0, 1, 2, 3  -> 총 네 건
        // 1, 2 -> 총 두 건
    }

    @Test
    void aggregation() {
        List<Tuple> fetch = queryFactory
                .select(
                        member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min()
                )
                .from(member)
                .fetch();

        //TUPLE -> querydsl이 제공하는 것.(여러개 타입)

        Tuple tuple = fetch.get(0);
        assertEquals(4, tuple.get(member.count()));
        assertEquals(100, tuple.get(member.age.sum()));
        assertEquals(25, tuple.get(member.age.avg()));
        assertEquals(40, tuple.get(member.age.max()));
        assertEquals(10, tuple.get(member.age.min()));
    }

    /**
     * 팀의 이름과 각 팀 멤버의 평균 연령을 구하라.
     */
    @Test
    void group() {
        List<Tuple> fetch = queryFactory
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .orderBy(team.name.asc())
                .groupBy(team.name)
                .fetch();

        Tuple teamA = fetch.get(0);
        Tuple teamB = fetch.get(1);

        assertEquals("teamA", teamA.get(team.name));
        assertEquals(15, teamA.get(member.age.avg()));

        assertEquals("teamB", teamB.get(team.name));
        assertEquals(35, teamB.get(member.age.avg()));
    }


    /**
     * TeamA의 소속된 모든회원 찾기.
     */
    @Test
    void join() {
        List<Member> fetch = queryFactory
                .selectFrom(member)
                .join(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();

        assertThat(fetch)
                .extracting("username")
                .containsExactly("member1", "member2");
    }

    /**
     * 세타조인
     * 연관관계가 없어도 조인할 수 있다.
     * 회원이름이 팀 이름과 같은 회원 조회.
     * <p>
     * 모든 회원, 모든 팀 조인하고 where절에서 필터링
     * 제약사항 : left join, right join 같은 외부조인 불가.
     * but 조인 on을 이용하면 외부조인 가능하다.
     */
    @Test
    void thetaJoin() {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));

        List<Member> fetch = queryFactory
                .select(member)
                .from(member, team)
                .where(member.username.eq(team.name))
                .fetch();
    }

    /**
     * 회원과 팀을 조인. 팀 이름이 teamA인 팀만 조인하고 회원은 모두 조회.
     * JPQL :
     * select m, t
     * from Member m
     * left join m.team t
     * on t.name = 'teamA'
     * <p>
     * 이너조인 쓴다면 그냥 where쓰자..
     */
    @Test
    void joinOnFiltering() {
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team)
                .on(team.name.eq("teamA"))
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    /**
     * 연관관계 없는 엔티티 외부조회.
     * 회원의 이름이 팀 이름과 같은 대상 외부조인
     */
    @Test
    void joinOnNoRelation() {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));

        List<Tuple> fetch = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(team)
                .on(member.username.eq(team.name))
                .fetch();

        for (Tuple tuple : fetch) {
            System.out.println("TUPLE : " + tuple);
        }

    }

    /**
     * 페치조인은 SQL에서 제공하는 기능x
     * 연관된 엔티티를 SQL 한번에 조회하는 기능. 주로 성능최적화에 쓰임.
     */
    // 페치조인 미적용.
    @PersistenceUnit
    EntityManagerFactory emf;

    @Test
    void fetchJoinNo() {
        em.flush();
        em.clear();


        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertFalse(loaded);

        System.out.println(findMember.getTeam());

    }

    @Test
    void fetchJoinUse() {
        em.flush();
        em.clear();


        Member findMember = queryFactory
                .selectFrom(member)
                .join(member.team, team).fetchJoin()
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertTrue(loaded);

    }

    /**
     * 서브쿼리
     * 나이가 가장 많은 회원 조회.
     */
    @Test
    void subquery() {

        QMember memberSub = new QMember("memberSub"); // alias 달라야한다.

        List<Member> fetch = queryFactory
                .selectFrom(member)
                .where(member.age.eq(
                        //40
                        JPAExpressions
                                .select(memberSub.age.max())
                                .from(memberSub)
                ))
                .fetch();

        assertThat(fetch)
                .extracting("age")
                .containsExactly(40);
    }

    /**
     * 서브쿼리2
     * 나이가 평균 이상인 회원 조회.
     */
    @Test
    void subqueryGoe() {

        QMember memberSub = new QMember("memberSub"); // alias 달라야한다.

        List<Member> fetch = queryFactory
                .selectFrom(member)
                .where(member.age.goe(
                        //25
                        JPAExpressions
                                .select(memberSub.age.avg())
                                .from(memberSub)
                ))
                .fetch();

        assertThat(fetch)
                .extracting("age")
                .containsExactly(30, 40);
    }

    /**
     * 서브쿼리2
     * 나이가 10살 초과인 회원 조회.
     */
    @Test
    void subqueryIn() {

        QMember memberSub = new QMember("memberSub"); // alias 달라야한다.

        List<Member> fetch = queryFactory
                .selectFrom(member)
                .where(member.age.in(
                        JPAExpressions
                                .select(memberSub.age)
                                .from(memberSub)
                                .where(memberSub.age.gt(10))
                ))
                .fetch();

        assertThat(fetch)
                .extracting("age")
                .containsExactly(20, 30, 40);
    }

    @Test
    void selectSubquery() {
        QMember memberSub = new QMember("memberSub");
        List<Tuple> fetch = queryFactory
                .select(member.username,
                        JPAExpressions
                                .select(memberSub.age.avg())
                                .from(memberSub))
                .from(member)
                .fetch();

        for (Tuple tuple : fetch) {
            System.out.println("tuple = " + tuple);
        }
    }

    //JPA JPQL 서브쿼리의 한계? from절 서브쿼리 지원x(인라인 뷰)
    //해결법? 네이티브사용, 서브쿼리를 join으로 변경, 쿼리를 2번 분리해서 실행.

    /**
     * 프로젝션
     * 대상이 하나.
     */
    @Test
    void simpleProjection() {
//        List<Member> fetch1 = queryFactory
//                .select(member)
//                .from(member)
//                .fetch();

        List<String> fetch = queryFactory
                .select(member.username)
                .from(member)
                .fetch();

        for (String username : fetch) {
            System.out.println("username = " + username);
        }
    }

    /**
     * TUPLE 프로젝션
     */
    @Test
    void tupleProjection() {
        List<Tuple> fetch = queryFactory
                .select(member.username, member.age)
                .from(member)
                .fetch();

        for (Tuple tuple : fetch) {
            String username = tuple.get(member.username);
            Integer age = tuple.get(member.age);
            System.out.println("username = " + username);
            System.out.println("age = " + age);
        }
    }
    // 튜플은 쿼리DSL에 종속적이다. 하부 기술을 쉽게 바꿀 수 있게 repository 에서 사용하고 바깥으로 내보낼 때는 일반적인 객체로 내보내자.(DTO 등)


    /**
     * 프로젝션 - dto조회
     * jpql로 하는 법.(생성자 방식만 지원)
     */
    @Test
    void findDtoByJPQL() {
        List<MemberDto> resultList = em.createQuery("select new kr.gracelove.querydsl.dto.MemberDto(m.username, m.age) from Member m", MemberDto.class)
                .getResultList();
        resultList.forEach(System.out::println);
    }

    /**
     * 프로젝션 - dto조회
     * querydsl로 하는 법 (setter) setter로 한다고해도 퍼블릭 기본생성자 필요하다.
     */
    @Test
    void findDtoBySetter() {
        List<MemberDto> fetch = queryFactory
                .select(Projections.bean(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();
        fetch.forEach(System.out::println);
    }

    /**
     * 프로젝션 - dto조회
     * querydsl로 하는 법 (field) 역시 기본생성자 필요.
     */
    @Test
    void findDtoByField() {
        List<MemberDto> fetch = queryFactory
                .select(Projections.fields(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();
        fetch.forEach(System.out::println);
    }

    /**
     * 프로젝션 - dto조회
     * querydsl로 하는 법 (constructor) 역시 기본생성자 필요. 순서 맞아야한다.
     */
    @Test
    void findDtoByConstructor() {
        List<MemberDto> fetch = queryFactory
                .select(Projections.constructor(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();
        fetch.forEach(System.out::println);
    }

    /**
     * 동적쿼리 - BooleanBuilder
     * 나가는 query 볼것. 조건 중 하나가 null이면?
     */
    @Test
    void dynamicQuery_BooleanBuilder() {
        String username = "member1";
        Integer age = 10;

        List<Member> result = searchMember1(username, age);
        assertEquals(1, result.size());
    }

    private List<Member> searchMember1(String username, Integer age) {
        BooleanBuilder builder = new BooleanBuilder();
        if (username != null) {
            builder.and(member.username.eq(username));
        }
        if (age != null) {
            builder.and(member.age.eq(age));
        }

        return queryFactory
                .selectFrom(member)
                .where(builder)
                .fetch();
    }

    /**
     * 동적쿼리 - Where 다중 파라미터
     */
    @Test
    void dynamicQuery_whereParam() {
        String username = "member1";
        Integer age = 10;

        List<Member> result = searchMember2(username, age);
        assertEquals(1, result.size());
    }

    private List<Member> searchMember2(String username, Integer age) {
        return queryFactory
                .selectFrom(member)
                .where(usernameEq(username), ageEq(age))
//                .where(allEq(username, age))
                .fetch();
    }

    private BooleanExpression usernameEq(String username) {
        return username != null ? member.username.eq(username) : null;
    }

    private BooleanExpression ageEq(Integer age) {
        return age != null ? member.age.eq(age) : null;
    }

    /**
     * 메서드로 뽑을 수 있으니까 조합을 할 수 있다.
     */
    private BooleanExpression allEq(String username, Integer age) {
        return usernameEq(username).and(ageEq(age));
    }


    /**
     * 수정, 삭제 벌크 연산
     * 쿼리 한번으로 대량의 데이터 수정.
     */
    @Test
    void bulkUpdate() {

        long numberOfUpdateColumns = queryFactory
                .update(member)
                .set(member.username, "비회원")
                .where(member.age.lt(30))
                .execute();

        assertEquals(2, numberOfUpdateColumns);
        /**
         * 현재 영속성 컨텍스트 상황.
         *
         *         member1 = 10 -> member1
         *         member2 = 20 -> member2
         *         member3 = 30 -> member3
         *         member4 = 40 -> member4
         *
         * 현재 DB 상황
         *         member1 = 10 -> 비회원
         *         member2 = 20 -> 비회원
         *         member3 = 30 -> member3
         *         member4 = 40 -> member4
         *
         * 벌크연산은 영속성컨텍스트 무시하고 바로때려버리기 때문에 DB의 값과 영속성컨텍스트 값이 다름.
         *
        */

//        List<Member> fetch = queryFactory
//                .selectFrom(member)
//                .fetch();
//
//        fetch.forEach(System.out::println);

        //해결하려면? em.flush, clear 쓰자.
        //벌크연산한 뒤에는 영속성컨텍스트 초기화 시켜주자.
        em.flush();
        em.clear();

        List<Member> fetch2 = queryFactory
                .selectFrom(member)
                .fetch();

        fetch2.forEach(System.out::println);

    }
}