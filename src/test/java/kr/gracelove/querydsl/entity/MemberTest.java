package kr.gracelove.querydsl.entity;

import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

import static kr.gracelove.querydsl.entity.QMember.*;
import static kr.gracelove.querydsl.entity.QTeam.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

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
     *
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
}