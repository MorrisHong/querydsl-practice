package kr.gracelove.querydsl.entity;

import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

import static kr.gracelove.querydsl.entity.QMember.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

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

}